package br.edu.eseg.vestuario.ezVest.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação do pedido e as transições permitidas entre elas
 * (Entrega 1, seções 4.2 e 4.3).
 *
 *   ENTREGUE  -> CONCLUIDO, TROCA ou CANCELADO   (é onde o pedido começa)
 *   CONCLUIDO, TROCA e CANCELADO                 -> finais (não mudam mais)
 */
public enum Status {

    CANCELADO("Cancelado"),
    CONCLUIDO("Concluído"),
    ENTREGUE("Entregue"),
    TROCA("Troca");

    private final String nome;

    Status(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /** Status para os quais este status pode mudar. */
    public Set<Status> proximosPermitidos() {
        return switch (this) {
            case ENTREGUE -> EnumSet.of(CONCLUIDO, TROCA, CANCELADO);
            case CONCLUIDO, TROCA, CANCELADO -> EnumSet.noneOf(Status.class);
        };
    }

    public boolean podeMudarPara(Status novo) {
        return novo != null && proximosPermitidos().contains(novo);
    }

    public boolean isFinal() {
        return proximosPermitidos().isEmpty();
    }
}
