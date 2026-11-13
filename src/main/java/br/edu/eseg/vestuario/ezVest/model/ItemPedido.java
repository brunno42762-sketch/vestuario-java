package br.edu.eseg.vestuario.ezVest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Classe de associação entre Pedido e Produto (Pedido * —— * Produto).
 */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {

    // Não existe no UML: toda entidade JPA precisa de um identificador
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_pedido")
    private Integer idItemPedido;

    @Column(name = "preco_unitario")
    private double precoUnitario;

    private int quantidade;

    @Column(name = "desconto_percentual")
    private double descontoPercentual;

    @Enumerated(EnumType.STRING)
    private Tamanho tamanho;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_produto", nullable = false)
    private Produto produto;
}
