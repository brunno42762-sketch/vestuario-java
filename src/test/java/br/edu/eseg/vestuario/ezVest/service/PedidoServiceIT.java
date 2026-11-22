package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.Perfis;
import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.*;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mudança de status, cancelamento e devolução do estoque. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoServiceIT {

    @Autowired PedidoService pedidos;
    @Autowired VendaService vendas;
    @Autowired ProdutoRepository produtos;
    @Autowired PedidoRepository pedidoRepository;

    private Produto produto;
    private int estoqueAntes;

    @BeforeEach
    void preparar() {
        produto = produtos.findAll().stream().filter(p -> p.getEstoqueAtual() >= 30).findFirst().orElseThrow();
        estoqueAntes = produto.getEstoqueAtual();
    }

    @AfterEach
    void sair() {
        Perfis.sair();
    }

    private Pedido vendaParaCliente(int idCliente, int pecas) {
        Perfis.como("VENDEDOR");
        return vendas.registrarNaLoja(idCliente, Canal.FISICA, FormaPagamento.PIX,
                List.of(new ItemVenda(produto.getIdProduto(), Tamanho.M, pecas)));
    }

    private int estoque() {
        return produtos.findById(produto.getIdProduto()).orElseThrow().getEstoqueAtual();
    }

    @Test
    void transicaoInvalidaEhBloqueadaNoServidor() {
        Pedido pedido = vendaParaCliente(103, 1);
        assertThatThrownBy(() -> pedidos.alterarStatus(pedido.getIdPedido(), Status.TROCA))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(pedidoRepository.findById(pedido.getIdPedido()).orElseThrow().getStatusPedido())
                .isEqualTo(Status.CRIADO);
    }

    @Test
    void cancelarPelaEquipeDevolveOEstoque() {
        Pedido pedido = vendaParaCliente(103, 4);
        assertThat(estoque()).isEqualTo(estoqueAntes - 4);

        pedidos.alterarStatus(pedido.getIdPedido(), Status.ENTREGUE);
        pedidos.alterarStatus(pedido.getIdPedido(), Status.CANCELADO);

        assertThat(estoque()).isEqualTo(estoqueAntes);
        // e cancelado é final: não devolve de novo
        assertThatThrownBy(() -> pedidos.alterarStatus(pedido.getIdPedido(), Status.CANCELADO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(estoque()).isEqualTo(estoqueAntes);
    }

    @Test
    void cancelarUmPedidoDoLegadoTambemDevolve() {
        Perfis.como("ADMIN");
        Pedido legado = pedidoRepository.findAll().stream()
                .filter(p -> p.getStatusPedido() == Status.ENTREGUE)
                .findFirst().orElseThrow();
        ItemPedido item = legado.getItensPedido().get(0);
        int antes = item.getProduto().getEstoqueAtual();

        pedidos.alterarStatus(legado.getIdPedido(), Status.CANCELADO);

        assertThat(produtos.findById(item.getProduto().getIdProduto()).orElseThrow().getEstoqueAtual())
                .isEqualTo(antes + item.getQuantidade());
    }

    @Test
    void clienteCancelaOProprioPedidoCriado() {
        Pedido pedido = vendaParaCliente(103, 2);
        Perfis.como("CLIENTE");

        pedidos.cancelarDoCliente(pedido.getIdPedido(), 103);

        assertThat(pedidoRepository.findById(pedido.getIdPedido()).orElseThrow().getStatusPedido())
                .isEqualTo(Status.CANCELADO);
        assertThat(estoque()).isEqualTo(estoqueAntes);
    }

    @Test
    void clienteNaoCancelaPedidoDeOutraPessoa() {
        Pedido pedido = vendaParaCliente(58, 1);
        Perfis.como("CLIENTE");
        // 404, igual a um pedido que não existe: não revela que o pedido existe
        assertThatThrownBy(() -> pedidos.cancelarDoCliente(pedido.getIdPedido(), 103))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void clienteNaoCancelaPedidoJaEntregue() {
        Pedido pedido = vendaParaCliente(103, 1);
        pedidos.alterarStatus(pedido.getIdPedido(), Status.ENTREGUE);
        Perfis.como("CLIENTE");
        assertThatThrownBy(() -> pedidos.cancelarDoCliente(pedido.getIdPedido(), 103))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda não foi entregue");
    }
}
