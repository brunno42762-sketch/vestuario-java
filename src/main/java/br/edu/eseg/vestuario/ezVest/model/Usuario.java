package br.edu.eseg.vestuario.ezVest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Quem pode entrar no sistema. Não faz parte do domínio da loja (por isso não está no UML
 * do legado): existe para a autenticação e os perfis da entrega 3.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    /** UUID: a identidade da sessão não depende do username, que pode mudar. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    /** Só o hash BCrypt, nunca a senha em texto. */
    @Column(nullable = false, length = 255)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * Só para o perfil CLIENTE: o cliente do legado que este usuário representa.
     * É o que permite mostrar a ele apenas os próprios pedidos.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", unique = true)
    private Cliente cliente;
}
