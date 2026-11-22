package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Perfil;
import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras do cadastro de usuários (só ADMIN). O admin "admin" vem do application-test.properties. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "ADMIN")
class UsuarioServiceIT {

    @Autowired UsuarioService servico;
    @Autowired UsuarioRepository repositorio;

    private Usuario admin;

    @BeforeEach
    void buscarAdmin() {
        admin = repositorio.findByUsername("admin").orElseThrow();
    }

    @Test
    void criaVendedorComSenhaGuardadaComoHash() {
        servico.salvar(null, "vendedor1", "senha-forte-1", Perfil.VENDEDOR, true, null, admin.getId());
        Usuario u = repositorio.findByUsername("vendedor1").orElseThrow();
        assertThat(u.getSenha()).startsWith("$2").isNotEqualTo("senha-forte-1"); // BCrypt
    }

    @Test
    void adminNaoExcluiNemRebaixaASiMesmo() {
        assertThatThrownBy(() -> servico.excluir(admin.getId(), admin.getId()))
                .hasMessageContaining("própria conta");
        assertThatThrownBy(() -> servico.salvar(admin.getId(), "admin", "", Perfil.VENDEDOR, true, null, admin.getId()))
                .hasMessageContaining("próprio acesso");
    }

    @Test
    void sistemaMantemPeloMenosUmAdminAtivo() {
        UUID outroAdmin = UUID.randomUUID(); // outra pessoa logada tentando desativar o único ADMIN
        assertThatThrownBy(() -> servico.salvar(admin.getId(), "admin", "", Perfil.ADMIN, false, null, outroAdmin))
                .hasMessageContaining("pelo menos um ADMIN ativo");
    }

    @Test
    void usernameRepetidoESenhaCurta() {
        assertThatThrownBy(() -> servico.salvar(null, "admin", "senha-forte-1", Perfil.VENDEDOR, true, null, admin.getId()))
                .hasMessageContaining("já cadastrado");
        assertThatThrownBy(() -> servico.salvar(null, "novo", "1234567", Perfil.VENDEDOR, true, null, admin.getId()))
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void perfilClienteExigeClienteLivre() {
        assertThatThrownBy(() -> servico.salvar(null, "cli1", "senha-forte-1", Perfil.CLIENTE, true, null, admin.getId()))
                .hasMessageContaining("ID do cliente");
        servico.salvar(null, "cli1", "senha-forte-1", Perfil.CLIENTE, true, 103, admin.getId());
        assertThatThrownBy(() -> servico.salvar(null, "cli2", "senha-forte-1", Perfil.CLIENTE, true, 103, admin.getId()))
                .isInstanceOf(IllegalArgumentException.class); // o 103 já está vinculado
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void vendedorNaoGerenciaUsuarios() {
        assertThatThrownBy(() -> servico.listar()).isInstanceOf(AccessDeniedException.class);
    }
}
