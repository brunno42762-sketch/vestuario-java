package br.edu.eseg.vestuario.ezVest.carrinho;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regras do carrinho, com o repositório simulado (Mockito): não precisa de banco.
 * Casos-limite: exatamente o estoque, uma peça a mais, outros tamanhos do mesmo produto.
 */
class CarrinhoServiceTest {

    private final ProdutoRepository repositorio = mock(ProdutoRepository.class);
    private final Carrinho carrinho = new Carrinho();
    private final CarrinhoService servico = new CarrinhoService(carrinho, repositorio);
    private Produto produto;

    @BeforeEach
    void produtoComCincoPecas() {
        Produto p = new Produto();
        produto = p;
        p.setIdProduto(7);
        p.setEstoqueAtual(5);
        p.setPrecoUnitario(100);
        when(repositorio.findById(7)).thenReturn(Optional.of(p));
        when(repositorio.findById(anyInt())).thenAnswer(inv ->
                inv.getArgument(0, Integer.class) == 7 ? Optional.of(p) : Optional.empty());
    }

    @Test
    void adicionaAteOEstoqueExato() {
        servico.adicionar(7, Tamanho.M, 5);
        assertThat(carrinho.quantidadeDoProduto(7)).isEqualTo(5);
    }

    @Test
    void recusaUmaPecaAlemDoEstoque() {
        assertThatThrownBy(() -> servico.adicionar(7, Tamanho.M, 6))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("5 peça");
        assertThat(carrinho.isVazio()).isTrue();
    }

    @Test
    void contaAsPecasDosOutrosTamanhosDoMesmoProduto() {
        servico.adicionar(7, Tamanho.M, 3);
        assertThatThrownBy(() -> servico.adicionar(7, Tamanho.G, 3))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2 peça");
        servico.adicionar(7, Tamanho.G, 2); // exatamente o que sobra
        assertThat(carrinho.quantidadeDoProduto(7)).isEqualTo(5);
    }

    @Test
    void exigeTamanhoEProdutoExistente() {
        assertThatThrownBy(() -> servico.adicionar(7, null, 1)).hasMessageContaining("tamanho");
        assertThatThrownBy(() -> servico.adicionar(999, Tamanho.M, 1)).hasMessageContaining("não encontrado");
    }

    @Test
    void quantidadeForaDoIntervalo() {
        assertThatThrownBy(() -> servico.adicionar(7, Tamanho.M, 0)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.adicionar(7, Tamanho.M, -1)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> servico.adicionar(7, Tamanho.M, 11))   // limite: no máximo 10 por item
                .hasMessageContaining("de 1 a 10");
    }

    @Test
    void resumoCalculaSubtotalETotal() {
        servico.adicionar(7, Tamanho.M, 2);
        servico.adicionar(7, Tamanho.G, 1);
        when(repositorio.findAllById(org.mockito.ArgumentMatchers.anyIterable()))
                .thenReturn(java.util.List.of(produto));

        CarrinhoService.Resumo resumo = servico.resumo();

        assertThat(resumo.linhas()).hasSize(2);
        assertThat(resumo.pecas()).isEqualTo(3);
        assertThat(resumo.total()).isEqualTo(300.0);
    }
}
