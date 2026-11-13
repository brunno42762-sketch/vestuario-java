package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Entrega;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * Regras de validação do Cliente e da sua Entrega.
 * Mesma ideia do ProdutoValidator: if's escritos à mão, sem dependências extras.
 */
@Component
public class ClienteValidator implements Validator {

    /** As mesmas faixas usadas na planilha legada. */
    public static final List<String> FAIXAS_ETARIAS =
            List.of("18-24", "25-34", "35-44", "45-54", "55+");

    public static final List<String> GENEROS =
            List.of("Feminino", "Masculino", "Outro", "Prefiro não informar");

    public static final List<String> UFS = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    @Override
    public boolean supports(Class<?> clazz) {
        return Cliente.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object alvo, Errors erros) {
        Cliente c = (Cliente) alvo;

        // ---- Valores que só podem vir de uma lista fechada ----
        // (o "== null" vem antes: List.of(...).contains(null) lança NullPointerException)
        if (c.getFaixaEtaria() == null || !FAIXAS_ETARIAS.contains(c.getFaixaEtaria())) {
            erros.rejectValue("faixaEtaria", "invalido", "Selecione a faixa etária.");
        }
        if (c.getGenero() == null || !GENEROS.contains(c.getGenero())) {
            erros.rejectValue("genero", "invalido", "Selecione o gênero.");
        }
        if (!erros.hasFieldErrors("canalPreferido") && c.getCanalPreferido() == null) {
            erros.rejectValue("canalPreferido", "obrigatorio", "Selecione o canal preferido.");
        }

        // ---- Localização do cliente ----
        cidade(erros, "cidade", c.getCidade());
        uf(erros, "estado", c.getEstado());

        // ---- Entrega (UML: Cliente 1 ◆—— 1 Entrega, então é obrigatória) ----
        Entrega e = c.getEntrega();
        if (e == null) {
            erros.reject("entregaObrigatoria", "Informe o endereço de entrega.");
        } else {
            cidade(erros, "entrega.cidadeEntrega", e.getCidadeEntrega());
            uf(erros, "entrega.estadoEntrega", e.getEstadoEntrega());
        }
    }

    private void cidade(Errors erros, String campo, String valor) {
        if (valor == null || valor.isBlank()) {
            erros.rejectValue(campo, "obrigatorio", "Informe a cidade.");
        } else if (valor.trim().length() > 60) {
            erros.rejectValue(campo, "tamanho", "A cidade deve ter no máximo 60 caracteres.");
        }
    }

    private void uf(Errors erros, String campo, String valor) {
        if (valor == null || !UFS.contains(valor.trim().toUpperCase())) {
            erros.rejectValue(campo, "invalido", "Selecione um estado (UF) válido.");
        }
    }
}
