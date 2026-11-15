package br.edu.eseg.vestuario.ezVest.security;

import br.edu.eseg.vestuario.ezVest.model.Perfil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Identidade imutável da sessão (o "usuário logado" para o Spring Security).
 * Guarda também o id do cliente, para a "Minha conta" saber de quem são os pedidos.
 */
public final class PrincipalVestuario implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String username;
    private final String senha;
    private final Perfil perfil;
    private final boolean ativo;
    private final Integer idCliente;

    public PrincipalVestuario(UUID id, String username, String senha, Perfil perfil,
                              boolean ativo, Integer idCliente) {
        this.id = id;
        this.username = username;
        this.senha = senha;
        this.perfil = perfil;
        this.ativo = ativo;
        this.idCliente = idCliente;
    }

    public UUID getId() {
        return id;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    /** Só preenchido para o perfil CLIENTE. */
    public Integer getIdCliente() {
        return idCliente;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    /** O Spring Security entende perfis como "ROLE_" + nome: hasRole('ADMIN') procura ROLE_ADMIN. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
