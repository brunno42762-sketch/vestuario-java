package br.edu.eseg.vestuario.ezVest.migracao;

import br.edu.eseg.vestuario.ezVest.repository.ClienteRepository;
import br.edu.eseg.vestuario.ezVest.repository.ItemPedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A migração da planilha (RF10) rodou no banco de teste vazio: confere as quantidades e os
 * totais com os números do diagnóstico da Entrega 1.
 * Os outros testes desfazem o que gravam (@Transactional), então o banco tem só os dados legados.
 * Os ids dos pedidos legados vão de 2 a 6000 (há ids ausentes na planilha: problema P02).
 */
@SpringBootTest
@ActiveProfiles("test")
class MigracaoLegadoIT {

    @Autowired ProdutoRepository produtos;
    @Autowired ClienteRepository clientes;
    @Autowired PedidoRepository pedidos;
    @Autowired ItemPedidoRepository itens;
    @Autowired JdbcTemplate jdbc;

    @Test
    void quantidades() {
        assertThat(produtos.count()).isEqualTo(126);
        assertThat(clientes.count()).isEqualTo(800);
        assertThat(jdbc.queryForObject("select count(*) from APP.entrega", Long.class)).isEqualTo(800);
        assertThat(pedidos.count()).isEqualTo(5999);
        assertThat(itens.count()).isEqualTo(5999);
        assertThat(jdbc.queryForObject("select max(id_pedido) from APP.pedido", Integer.class)).isEqualTo(6000);
    }

    @Test
    void totaisDosPedidosLegados() {
        BigDecimal receita = jdbc.queryForObject(
                "select cast(sum(valor_total) as decimal(14,2)) from APP.pedido", BigDecimal.class);
        BigDecimal lucro = jdbc.queryForObject(
                "select cast(sum(lucro_estimado) as decimal(14,2)) from APP.pedido", BigDecimal.class);
        assertThat(receita).isEqualByComparingTo("4852558.18");
        assertThat(lucro).isEqualByComparingTo("2417872.92");
    }

    @Test
    void registrosReconstruidos() {
        // Produto 1 e cliente 1 não existiam nas abas da planilha (seção 6.5 da Entrega 1)
        assertThat(produtos.findById(1)).get()
                .satisfies(p -> assertThat(p.getCustoUnitario()).isEqualTo(121.25));
        assertThat(clientes.findById(1)).get()
                .satisfies(c -> assertThat(c.getFaixaEtaria()).isEqualTo("Não informado"));
    }
}
