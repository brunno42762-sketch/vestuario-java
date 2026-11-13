package br.edu.eseg.vestuario.ezVest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Trata, em um só lugar, os erros lançados por qualquer controller
 * e mostra a página templates/erro.html no lugar da "Whitelabel Error Page".
 */
@ControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(RecursoNaoEncontradoException e, Model model) {
        return paginaDeErro(model, 404, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String parametroInvalido(MethodArgumentTypeMismatchException e, Model model) {
        return paginaDeErro(model, 400, "Endereço inválido",
                "O valor \"" + e.getValue() + "\" não é válido para " + e.getName() + ".");
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String regraNegocio(RegraNegocioException e, Model model) {
        return paginaDeErro(model, 409, "Operação não permitida", e.getMessage());
    }

    private String paginaDeErro(Model model, int codigo, String titulo, String mensagem) {
        model.addAttribute("codigo", codigo);
        model.addAttribute("titulo", titulo);
        model.addAttribute("mensagem", mensagem);
        return "erro";
    }
}
