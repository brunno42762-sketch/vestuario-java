package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.service.FiltroPedidos;
import br.edu.eseg.vestuario.ezVest.service.OrdemPedidos;
import br.edu.eseg.vestuario.ezVest.service.PedidoService;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import br.edu.eseg.vestuario.ezVest.service.RelatorioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Consultas JPQL, relatório e restrições CHECK do banco. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsultasIT {

    @Autowired PedidoRepository pedidoRepository;
    @Autowired PedidoService pedidoService;
    @Autowired ProdutoService produtoService;
    @Autowired RelatorioService relatorioService;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired JdbcTemplate jdbc;

    // ---------- RF08: filtros ----------

    @Test
    void filtrosCombinadosBatemComUmaContagemFeitaEmJava() {
        LocalDate de = LocalDate.of(2025, 1, 1), ate = LocalDate.of(2025, 6, 30);
        var filtro = new FiltroPedidos(Status.CONCLUIDO, Canal.ECOMMERCE, de, ate, null);

        long pelaConsulta = pedidoService.listar(filtro, OrdemPedidos.RECENTES, 1).getTotalElements();
        long emJava = pedidoRepository.findAll().stream()
                .filter(p -> p.getStatusPedido() == Status.CONCLUIDO && p.getCanalVenda() == Canal.ECOMMERCE)
                .filter(p -> !p.getDataPedido().isBefore(de) && !p.getDataPedido().isAfter(ate))
                .count();
        assertThat(pelaConsulta).isEqualTo(emJava).isPositive();
    }

    @Test
    void semFiltroTrazTudoEDatasSaoInclusivas() {
        assertThat(pedidoService.listar(FiltroPedidos.nenhum(), null, 1).getTotalElements())
                .isEqualTo(pedidoRepository.count());

        Pedido qualquer = pedidoRepository.findAll().get(0);
        LocalDate dia = qualquer.getDataPedido();
        var doDia = pedidoService.listar(new FiltroPedidos(null, null, dia, dia, qualquer.getCliente().getIdCliente()),
                OrdemPedidos.RECENTES, 1);
        assertThat(doDia.getContent()).extracting(Pedido::getIdPedido).contains(qualquer.getIdPedido());
    }

    // ---------- Página inicial ----------

    @Test
    void maisVendidosTemEstoqueEOLinkLevaAPaginaCertaDoCatalogo() {
        List<ProdutoService.Destaque> destaques = produtoService.maisVendidos(6);
        assertThat(destaques).hasSize(6);
        for (ProdutoService.Destaque d : destaques) {
            assertThat(d.produto().getEstoqueAtual()).isPositive();
            assertThat(produtoService.catalogo(d.pagina()).getContent())
                    .extracting(p -> p.getIdProduto()).contains(d.produto().getIdProduto());
        }
    }

    @Test
    void categoriasVemDoBanco() {
        var categorias = produtoService.categorias();
        assertThat(categorias).extracting(ProdutoService.ResumoCategoria::nome)
                .containsExactly("Calça", "Camiseta", "Jaqueta", "Moletom", "Tênis", "Vestido");
        assertThat(categorias.stream().mapToLong(ProdutoService.ResumoCategoria::produtos).sum()).isEqualTo(126);
    }

    // ---------- RF06: lista de reposição ----------

    @Test
    void listaDeReposicaoTemOs8ProdutosDaPlanilhaENadaComEstoqueSobrando() {
        var repor = produtoRepository.listarAbaixoDoEstoqueMinimo();
        // Entrega 1, seção 2.4: na planilha, 8 produtos estão abaixo do mínimo
        assertThat(repor).extracting(p -> p.getIdProduto()).contains(8, 18, 32, 41, 70, 74, 83, 112);
        assertThat(repor).allMatch(p -> p.getEstoqueAtual() <= p.getEstoqueMinimo());
        // o mais crítico (maior falta) vem primeiro
        assertThat(repor).isSortedAccordingTo(java.util.Comparator.comparingInt(
                p -> p.getEstoqueAtual() - p.getEstoqueMinimo()));
    }

    // ---------- RF09: relatório ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void relatorioSemCanceladosESomasConsistentes() {
        var r = relatorioService.gerar(null, null);
        double esperado = jdbc.queryForObject(
                "select sum(valor_total) from APP.pedido where status_pedido <> 'CANCELADO'", Double.class);

        assertThat(r.total().receita()).isCloseTo(esperado, within(0.01));
        assertThat(r.porMes().linhas().stream().mapToDouble(RelatorioService.Linha::receita).sum())
                .isCloseTo(esperado, within(0.05));
        // por categoria a conta é por item: pode diferir por centavos de arredondamento
        assertThat(r.porCategoria().linhas().stream().mapToDouble(RelatorioService.Linha::receita).sum())
                .isCloseTo(esperado, within(5.0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void relatorioDeUmMes() {
        var r = relatorioService.gerar(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        assertThat(r.porMes().linhas()).extracting(RelatorioService.Linha::rotulo).containsExactly("jan/2025");
    }

    // ---------- RNF01: restrições CHECK no banco ----------

    @ParameterizedTest(name = "o banco recusa: {0}")
    @ValueSource(strings = {
            "update APP.produto set preco_unitario = -1 where id_produto = (select min(id_produto) from APP.produto)",
            "update APP.produto set custo_unitario = preco_unitario + 1 where id_produto = (select min(id_produto) from APP.produto)",
            "update APP.produto set estoque_atual = -1 where id_produto = (select min(id_produto) from APP.produto)",
            "update APP.item_pedido set quantidade = 0 where id_item_pedido = (select min(id_item_pedido) from APP.item_pedido)",
            "update APP.item_pedido set desconto_percentual = 0.2 where id_item_pedido = (select min(id_item_pedido) from APP.item_pedido)",
            "update APP.pedido set valor_total = -10 where id_pedido = (select min(id_pedido) from APP.pedido)",
            "update APP.pedido set status_pedido = 'PERDIDO' where id_pedido = (select min(id_pedido) from APP.pedido)"
    })
    void bancoRecusaDadosInvalidosMesmoSemPassarPeloJava(String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
