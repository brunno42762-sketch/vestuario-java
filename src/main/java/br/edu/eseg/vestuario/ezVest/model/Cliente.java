package br.edu.eseg.vestuario.ezVest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "faixa_etaria", nullable = false, length = 15)
    private String faixaEtaria;

    @Column(nullable = false, length = 25)
    private String genero;

    @Column(nullable = false, length = 60)
    private String cidade;

    @Column(nullable = false, length = 2)
    private String estado;

    // Pode ser nulo: o cliente 1, reconstruído na migração do legado, não tem canal preferido.
    // Pela tela, o ClienteValidator continua exigindo o canal.
    @Enumerated(EnumType.STRING)
    @Column(name = "canal_preferido", length = 20)
    private Canal canalPreferido;

    @Column(name = "cliente_fidelidade", nullable = false)
    private boolean clienteFidelidade;

    // UML: Cliente 1 ◆—— * Pedido (composição)
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pedido> pedidos = new ArrayList<>();

    // UML: Cliente 1 ◆—— 1 Entrega (composição)
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Entrega entrega;

    public void adicionarPedido(Pedido pedido) {
        pedidos.add(pedido);
        pedido.setCliente(this);
    }

    public void removerPedido(Pedido pedido) {
        pedidos.remove(pedido);
        pedido.setCliente(null);
    }

    public void definirEntrega(Entrega entrega) {
        this.entrega = entrega;
        if (entrega != null) {
            entrega.setCliente(this);
        }
    }
}
