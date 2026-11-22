package br.edu.eseg.vestuario.ezVest.carrinho;

import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** O carrinho da sessão: linhas por produto + tamanho, somas e remoção. */
class CarrinhoTest {

    private final Carrinho carrinho = new Carrinho();

    @Test
    void mesmoProdutoEMesmoTamanhoSomaNaMesmaLinha() {
        carrinho.adicionar(10, Tamanho.M, 2);
        carrinho.adicionar(10, Tamanho.M, 3);

        assertThat(carrinho.getItens()).hasSize(1);
        assertThat(carrinho.buscar("10-M").quantidade()).isEqualTo(5);
    }

    @Test
    void tamanhosDiferentesSaoLinhasDiferentesMasOEstoqueEhDoProduto() {
        carrinho.adicionar(10, Tamanho.M, 2);
        carrinho.adicionar(10, Tamanho.G, 1);
        carrinho.adicionar(11, Tamanho.P, 4);

        assertThat(carrinho.getItens()).hasSize(3);
        assertThat(carrinho.quantidadeDoProduto(10)).isEqualTo(3);
        assertThat(carrinho.getQuantidadeTotal()).isEqualTo(7);
    }

    @Test
    void alterarRemoverELimpar() {
        carrinho.adicionar(10, Tamanho.M, 2);
        carrinho.adicionar(11, Tamanho.P, 1);

        carrinho.alterarQuantidade("10-M", 6);
        assertThat(carrinho.buscar("10-M").quantidade()).isEqualTo(6);

        carrinho.remover("11-P");
        assertThat(carrinho.getItens()).extracting(Carrinho.Item::getChave).containsExactly("10-M");

        carrinho.limpar();
        assertThat(carrinho.isVazio()).isTrue();
        assertThat(carrinho.getQuantidadeTotal()).isZero();
    }

    @Test
    void getItensDevolveUmaCopia() {
        carrinho.adicionar(10, Tamanho.M, 1);
        carrinho.getItens().clear();
        assertThat(carrinho.isVazio()).isFalse();
    }
}
