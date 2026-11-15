package br.edu.eseg.vestuario.ezVest.model;

/**
 * Perfis de acesso (entrega 3).
 *
 *   ADMIN     gerencia tudo: catálogo, exclusões e usuários
 *   VENDEDOR  atende: clientes e pedidos, sem mexer no catálogo nem excluir cadastros
 *   CLIENTE   vê só os próprios pedidos
 */
public enum Perfil {

    ADMIN(1, "Administrador"),
    VENDEDOR(2, "Vendedor"),
    CLIENTE(3, "Cliente");

    private final int codigo;
    private final String descricao;

    Perfil(int codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}
