package br.edu.eseg.vestuario.ezVest.model;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
// Índices onde há filtro frequente (RNF02, Entrega 1, seção 6.4). A chave estrangeira id_cliente
// também tem índice: o H2 cria um para cada FK. Canal e forma de pagamento ficam sem índice:
// têm poucos valores, e o ganho seria pequeno.
@Table(name = "pedido", indexes = {
        @Index(name = "ix_pedido_data", columnList = "data_pedido"),
        @Index(name = "ix_pedido_status", columnList = "status_pedido")
})
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Integer idPedido;

    @Column(name = "data_pedido", nullable = false)
    private LocalDate dataPedido;

    @Column(name = "valor_total", nullable = false)
    private double valorTotal;

    @Column(name = "lucro_estimado")
    private double lucroEstimado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_venda", nullable = false, length = 20)
    private Canal canalVenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pedido", nullable = false, length = 20)
    private Status statusPedido;

    // UML: classe de associação ItemPedido (Pedido * —— * Produto)
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itensPedido = new ArrayList<>();

    // UML: Cliente 1 ◆—— * Pedido
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    public void adicionarItem(ItemPedido item) {
        itensPedido.add(item);
        item.setPedido(this);
    }

    public void removerItem(ItemPedido item) {
        itensPedido.remove(item);
        item.setPedido(null);
    }

    /**
     * Muda o status respeitando as transições definidas em {@link Status}.
     * Um status inválido nunca chega ao banco.
     */
    public void alterarStatus(Status novoStatus) {
        if (novoStatus == null) {
            throw new RegraNegocioException("Selecione o novo status.");
        }
        if (statusPedido != null && !statusPedido.podeMudarPara(novoStatus)) {
            throw new RegraNegocioException(String.format(
                    "O pedido %d não pode mudar de %s para %s.",
                    idPedido, statusPedido.getNome(), novoStatus.getNome()));
        }
        this.statusPedido = novoStatus;
    }
}
