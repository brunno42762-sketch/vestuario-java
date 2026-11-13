package br.edu.eseg.vestuario.ezVest.exception;

/** Lançada quando o registro pedido (ex.: /produtos/999/editar) não existe no banco. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
