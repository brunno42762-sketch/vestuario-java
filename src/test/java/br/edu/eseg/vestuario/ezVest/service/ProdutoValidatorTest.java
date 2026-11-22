package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;

/** Validação do produto: cenário válido, inválidos e casos-limite. */
class ProdutoValidatorTest {

    private final ProdutoValidator validador = new ProdutoValidator();

    private Produto valido() {
        Produto p = new Produto();
        p.setCategoria("Camiseta");
        p.setSubcategoria("Básica");
        p.setCor("Azul");
        p.setMarca("Pulse");
        p.setColecao("Verão");
        p.setGenero("Unissex");
        p.setFornecedor("Malharia Serra");
        p.setPrecoUnitario(89.90);
        p.setCustoUnitario(40);
        p.setEstoqueAtual(10);
        p.setEstoqueMinimo(5);
        p.setTempoReposicaoDias(15);
        return p;
    }

    private Errors validar(Produto p) {
        Errors erros = new BeanPropertyBindingResult(p, "produto");
        validador.validate(p, erros);
        return erros;
    }

    @Test
    void produtoValidoNaoTemErros() {
        assertThat(validar(valido()).hasErrors()).isFalse();
    }

    @Test
    void precoIgualAoCustoEhPermitido() {          // caso-limite
        Produto p = valido();
        p.setPrecoUnitario(40);
        assertThat(validar(p).hasErrors()).isFalse();
    }

    @Test
    void precoMenorQueOCustoEhRecusado() {
        Produto p = valido();
        p.setPrecoUnitario(39.99);
        Errors erros = validar(p);
        assertThat(erros.getGlobalErrors()).extracting("code").containsExactly("precoMenorQueCusto");
    }

    @Test
    void precoZeroEstoqueNegativoESemCategoria() {
        Produto p = valido();
        p.setPrecoUnitario(0);
        p.setEstoqueAtual(-1);
        p.setCategoria("  ");
        Errors erros = validar(p);
        assertThat(erros.hasFieldErrors("precoUnitario")).isTrue();
        assertThat(erros.hasFieldErrors("estoqueAtual")).isTrue();
        assertThat(erros.hasFieldErrors("categoria")).isTrue();
    }

    @Test
    void estoqueZeroEhPermitido() {                // caso-limite
        Produto p = valido();
        p.setEstoqueAtual(0);
        assertThat(validar(p).hasFieldErrors("estoqueAtual")).isFalse();
    }

    @Test
    void textoAcimaDoTamanhoDaColunaEGeneroForaDaLista() {
        Produto p = valido();
        p.setCor("x".repeat(41));                  // a coluna tem 40
        p.setGenero("Infantil");
        Errors erros = validar(p);
        assertThat(erros.hasFieldErrors("cor")).isTrue();
        assertThat(erros.hasFieldErrors("genero")).isTrue();
    }
}
