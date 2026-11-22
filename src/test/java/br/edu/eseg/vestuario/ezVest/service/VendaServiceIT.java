package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.*;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Venda (VendaService) com o banco de verdade. @Transactional: cada teste é desfeito no fim,
 * então um teste não interfere no outro.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VendaServiceIT {

    @Autowired VendaService vendas;
    @Autowired ProdutoRepository produtos;

    private Produto produto;      // um produto com bastante estoque
    private int estoqueAntes;

    @BeforeEach
    void escolherProduto() {
        produto = produtos.findAll().stream().filter(p -> p.getEstoqueAtual() >= 30).findFirst().orElseThrow();
        estoqueAntes = produto.getEstoqueAtual();
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void vendaCalculaValorELucroEBaixaOEstoque() {
        Pedido pedido = vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, 3)));

        double valorEsperado = Math.round(produto.getPrecoUnitario() * 3 * 100) / 100.0;
        double lucroEsperado = Math.round((produto.getPrecoUnitario() - produto.getCustoUnitario()) * 3 * 100) / 100.0;
        assertThat(pedido.getIdPedido()).isNotNull();
        assertThat(pedido.getStatusPedido()).isEqualTo(Status.CRIADO);
        assertThat(pedido.getDataPedido()).isEqualTo(LocalDate.now());
        assertThat(pedido.getValorTotal()).isEqualTo(valorEsperado);
        assertThat(pedido.getLucroEstimado()).isEqualTo(lucroEsperado);
        assertThat(pedido.getItensPedido()).singleElement().satisfies(i -> {
            assertThat(i.getPrecoUnitario()).isEqualTo(produto.getPrecoUnitario()); // preço do momento
            assertThat(i.getDescontoPercentual()).isZero();
        });
        assertThat(produtos.findById(produto.getIdProduto()).orElseThrow().getEstoqueAtual())
                .isEqualTo(estoqueAntes - 3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void linhasRepetidasSaoJuntadasETamanhosDiferentesNao() {
        Pedido pedido = vendas.registrarNaLoja(58, Canal.MARKETPLACE, FormaPagamento.BOLETO, List.of(
                new ItemVenda(produto.getIdProduto(), Tamanho.M, 2),
                new ItemVenda(produto.getIdProduto(), Tamanho.M, 1),
                new ItemVenda(produto.getIdProduto(), Tamanho.G, 4)));

        assertThat(pedido.getItensPedido()).hasSize(2);
        assertThat(pedido.getItensPedido()).extracting(ItemPedido::getQuantidade).containsExactlyInAnyOrder(3, 4);
        assertThat(produtos.findById(produto.getIdProduto()).orElseThrow().getEstoqueAtual())
                .isEqualTo(estoqueAntes - 7); // o estoque é do produto, somando os tamanhos
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void estoqueInsuficienteNaoGravaNadaENaoMexeNoEstoque() {
        produto.setEstoqueAtual(2);
        produtos.saveAndFlush(produto);

        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, 3))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente")
                .hasMessageContaining("disponível 2");
        assertThat(produtos.findById(produto.getIdProduto()).orElseThrow().getEstoqueAtual()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void exatamenteOEstoqueDisponivelPodeSerVendido() {   // caso-limite
        produto.setEstoqueAtual(3);
        produtos.saveAndFlush(produto);
        vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, 3)));
        assertThat(produtos.findById(produto.getIdProduto()).orElseThrow().getEstoqueAtual()).isZero();
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void dadosInvalidos() {
        Integer id = produto.getIdProduto();
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX, List.of()))
                .hasMessageContaining("pelo menos um produto");
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(id, Tamanho.M, 0)))).hasMessageContaining("de 1 a 10");
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(id, Tamanho.M, 11)))).hasMessageContaining("de 1 a 10");
        assertThatThrownBy(() -> vendas.registrarNaLoja(99999, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(id, Tamanho.M, 1)))).hasMessageContaining("cliente");
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, null,
                List.of(new ItemVenda(id, Tamanho.M, 1)))).hasMessageContaining("forma de pagamento");
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(99999, Tamanho.M, 1)))).hasMessageContaining("não existe");
        // nada disso baixou o estoque
        assertThat(produtos.findById(id).orElseThrow().getEstoqueAtual()).isEqualTo(estoqueAntes);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteCompraPeloSiteSempreComoECommerce() {
        Pedido pedido = vendas.comprar(103, List.of(new ItemVenda(produto.getIdProduto(), Tamanho.P, 1)),
                FormaPagamento.CARTAO_CREDITO);
        assertThat(pedido.getCanalVenda()).isEqualTo(Canal.ECOMMERCE);
        assertThat(pedido.getCliente().getIdCliente()).isEqualTo(103);
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clienteNaoRegistraVendaDeLoja() {
        assertThatThrownBy(() -> vendas.registrarNaLoja(103, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, 1))))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void visitanteNaoCompra() {
        assertThatThrownBy(() -> vendas.comprar(103,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, 1)), FormaPagamento.PIX))
                .isInstanceOf(AccessDeniedException.class);
    }
}
