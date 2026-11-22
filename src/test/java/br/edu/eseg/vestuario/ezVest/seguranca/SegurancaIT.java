package br.edu.eseg.vestuario.ezVest.seguranca;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proteção no SERVIDOR (requisito 7 do guia): as requisições são feitas direto, sem passar pela
 * interface. Se a regra estivesse só escondendo botões, estes testes falhariam.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaIT {

    @Autowired MockMvc mvc;
    @Autowired ProdutoService produtoService;

    // ---------- Visitante ----------

    @Test
    void visitanteVeALojaMasNaoAAreaAdministrativa() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/carrinho")).andExpect(status().isOk());
        mvc.perform(get("/carrinho/finalizar")).andExpect(redirectedUrl("/login"));
    }

    // ---------- Login de verdade (usuário do banco, senha BCrypt) ----------

    @Test
    void loginComSenhaCertaEErrada() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("senha-de-teste-123"))
                .andExpect(authenticated().withRoles("ADMIN"))
                .andExpect(redirectedUrl("/entrar"));
        mvc.perform(formLogin("/login").user("admin").password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---------- VENDEDOR ----------

    @Test
    void vendedorConsultaMasNaoMexeEmProdutos() throws Exception {
        var vendedor = user("v").roles("VENDEDOR");
        mvc.perform(get("/admin/produtos").with(vendedor)).andExpect(status().isOk());
        mvc.perform(get("/admin/produtos/novo").with(vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/produtos/2/excluir").with(vendedor).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/relatorio").with(vendedor)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/usuarios").with(vendedor)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/vendas/nova").with(vendedor)).andExpect(status().isOk());
        mvc.perform(get("/carrinho").with(vendedor)).andExpect(status().isForbidden());
    }

    // ---------- CLIENTE ----------

    @Test
    void clienteNaoEntraNaAreaAdministrativa() throws Exception {
        var cliente = user("c").roles("CLIENTE");
        mvc.perform(get("/admin").with(cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/vendas/nova").with(cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/carrinho").with(cliente)).andExpect(status().isOk());
    }

    // ---------- ADMIN ----------

    @Test
    void adminAcessaTudoDaEquipe() throws Exception {
        var admin = user("a").roles("ADMIN");
        mvc.perform(get("/admin/produtos/novo").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/relatorio").with(admin)).andExpect(status().isOk());
        mvc.perform(get("/admin/usuarios").with(admin)).andExpect(status().isOk());
    }

    // ---------- CSRF e métodos não declarados ----------

    @Test
    void postSemTokenCsrfEhRecusadoComAPagina403() throws Exception {
        mvc.perform(post("/admin/vendas").with(user("a").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postNaoDeclaradoEDeleteSaoNegadosParaTodos() throws Exception {
        var admin = user("a").roles("ADMIN");
        mvc.perform(post("/admin/qualquer-coisa").with(admin).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/admin/produtos/2").with(admin).with(csrf())).andExpect(status().isForbidden());
    }

    // ---------- Segunda camada: @PreAuthorize no serviço ----------

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void mesmoChamandoOServicoDiretoOVendedorNaoCriaProduto() {
        assertThatThrownBy(() -> produtoService.criar(new Produto()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
