package br.edu.eseg.vestuario.ezVest.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Situação do pedido e as transições permitidas entre elas
 * (Entrega 1, seções 4.2 e 4.3, mais o status CRIADO da entrega 3).
 *
 *   CRIADO    -> ENTREGUE ou CANCELADO            (todo pedido novo começa aqui)
 *   ENTREGUE  -> CONCLUIDO, TROCA ou CANCELADO    (os pedidos do legado começam aqui)
 *   CONCLUIDO, TROCA e CANCELADO                  -> finais (não mudam mais)
 *
 * Ao passar para CANCELADO, os itens voltam para o estoque (PedidoService).
 *
 * Os valores ficam em ordem alfabética, como no banco. Se um valor novo for criado,
 * o AjusteEsquemaBanco o acrescenta à coluna dos bancos que já existem.
 */
public enum Status {

    CANCELADO("Cancelado"),
    CONCLUIDO("Concluído"),
    CRIADO("Criado"),
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
            case CRIADO -> EnumSet.of(ENTREGUE, CANCELADO);
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
