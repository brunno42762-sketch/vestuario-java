package br.edu.eseg.vestuario.ezVest.exception;

/** Lançada quando uma operação viola uma regra de negócio (ex.: status inválido). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
