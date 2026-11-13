package br.edu.eseg.vestuario.ezVest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
public class Entrega {

    // Não existe no UML: toda entidade JPA precisa de um identificador
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Integer idEntrega;

    @Column(name = "estado_entrega", nullable = false, length = 2)
    private String estadoEntrega;

    @Column(name = "cidade_entrega", nullable = false, length = 60)
    private String cidadeEntrega;

    // UML: Cliente 1 ◆—— 1 Entrega
    @OneToOne(optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;
}
