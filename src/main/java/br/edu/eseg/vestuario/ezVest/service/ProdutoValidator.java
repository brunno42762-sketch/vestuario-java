package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * Regras de validação do Produto, escritas à mão com a interface Validator do Spring
 * (sem Bean Validation / @NotBlank, para não adicionar dependências ao projeto).
 *
 * rejectValue(campo, código, mensagem) marca o erro no campo; o Thymeleaf mostra
 * a mensagem com th:errors. reject(código, mensagem) é um erro geral do formulário.
 */
@Component
public class ProdutoValidator implements Validator {

    public static final List<String> GENEROS = List.of("Masculino", "Feminino", "Unissex");

    @Override
    public boolean supports(Class<?> clazz) {
        return Produto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object alvo, Errors erros) {
        Produto p = (Produto) alvo;

        // ---- Textos obrigatórios e tamanho máximo (igual ao length do @Column) ----
        texto(erros, "categoria", p.getCategoria(), "a categoria", 60, true);
        texto(erros, "subcategoria", p.getSubcategoria(), "a subcategoria", 60, true);
        texto(erros, "cor", p.getCor(), "a cor", 40, true);
        texto(erros, "marca", p.getMarca(), "a marca", 60, true);
        texto(erros, "colecao", p.getColecao(), "a coleção", 60, false);
        texto(erros, "fornecedor", p.getFornecedor(), "o fornecedor", 80, true);

        if (p.getGenero() == null || !GENEROS.contains(p.getGenero())) {
            erros.rejectValue("genero", "invalido", "Selecione o gênero.");
        }

        // ---- Números ----
        // hasFieldErrors: se o campo já veio com erro de conversão (ex.: vazio), não repete a mensagem
        if (!erros.hasFieldErrors("precoUnitario") && p.getPrecoUnitario() <= 0) {
            erros.rejectValue("precoUnitario", "positivo", "O preço unitário deve ser maior que zero.");
        }
        if (!erros.hasFieldErrors("custoUnitario") && p.getCustoUnitario() <= 0) {
            erros.rejectValue("custoUnitario", "positivo", "O custo unitário deve ser maior que zero.");
        }
        if (!erros.hasFieldErrors("estoqueAtual") && p.getEstoqueAtual() < 0) {
            erros.rejectValue("estoqueAtual", "naoNegativo", "O estoque atual não pode ser negativo.");
        }
        if (!erros.hasFieldErrors("estoqueMinimo") && p.getEstoqueMinimo() < 0) {
            erros.rejectValue("estoqueMinimo", "naoNegativo", "O estoque mínimo não pode ser negativo.");
        }
        if (!erros.hasFieldErrors("tempoReposicaoDias") && p.getTempoReposicaoDias() < 0) {
            erros.rejectValue("tempoReposicaoDias", "naoNegativo", "O tempo de reposição não pode ser negativo.");
        }

        // ---- Regra entre dois campos ----
        if (!erros.hasFieldErrors("precoUnitario") && !erros.hasFieldErrors("custoUnitario")
                && p.getPrecoUnitario() > 0 && p.getCustoUnitario() > 0
                && p.getPrecoUnitario() < p.getCustoUnitario()) {
            erros.reject("precoMenorQueCusto", "O preço unitário não pode ser menor que o custo unitário.");
        }
    }

    private void texto(Errors erros, String campo, String valor, String nome, int max, boolean obrigatorio) {
        if (valor == null || valor.isBlank()) {
            if (obrigatorio) {
                erros.rejectValue(campo, "obrigatorio", "Informe " + nome + ".");
            }
        } else if (valor.trim().length() > max) {
            erros.rejectValue(campo, "tamanho",
                    "O campo deve ter no máximo " + max + " caracteres.");
        }
    }
}
