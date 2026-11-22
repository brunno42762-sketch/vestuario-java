package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Entrega;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;

/** Validação do cliente e da entrega (obrigatória, Cliente 1 —— 1 Entrega). */
class ClienteValidatorTest {

    private final ClienteValidator validador = new ClienteValidator();

    private Cliente valido() {
        Cliente c = new Cliente();
        c.setFaixaEtaria("25-34");
        c.setGenero("Feminino");
        c.setCanalPreferido(Canal.ECOMMERCE);
        c.setCidade("Curitiba");
        c.setEstado("PR");
        Entrega e = new Entrega();
        e.setCidadeEntrega("Curitiba");
        e.setEstadoEntrega("pr");                   // minúscula também vale
        c.setEntrega(e);
        return c;
    }

    private Errors validar(Cliente c) {
        Errors erros = new BeanPropertyBindingResult(c, "cliente");
        validador.validate(c, erros);
        return erros;
    }

    @Test
    void clienteValido() {
        assertThat(validar(valido()).hasErrors()).isFalse();
    }

    @Test
    void valoresForaDasListasFechadas() {
        Cliente c = valido();
        c.setFaixaEtaria("10-17");
        c.setGenero(null);                          // null não pode causar NullPointerException
        c.setEstado("XX");
        Errors erros = validar(c);
        assertThat(erros.hasFieldErrors("faixaEtaria")).isTrue();
        assertThat(erros.hasFieldErrors("genero")).isTrue();
        assertThat(erros.hasFieldErrors("estado")).isTrue();
    }

    @Test
    void cidadeCom60CaracteresPassaE61NaoPassa() {   // caso-limite
        Cliente c = valido();
        c.setCidade("a".repeat(60));
        assertThat(validar(c).hasFieldErrors("cidade")).isFalse();
        c.setCidade("a".repeat(61));
        assertThat(validar(c).hasFieldErrors("cidade")).isTrue();
    }

    @Test
    void entregaEhObrigatoria() {
        Cliente c = valido();
        c.setEntrega(null);
        assertThat(validar(c).getGlobalErrors()).extracting("code").containsExactly("entregaObrigatoria");
    }

    @Test
    void errosDaEntregaFicamNoCampoDaEntrega() {
        Cliente c = valido();
        c.getEntrega().setCidadeEntrega("");
        assertThat(validar(c).hasFieldErrors("entrega.cidadeEntrega")).isTrue();
    }
}
