package br.edu.eseg.vestuario.ezVest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Para os testes que precisam trocar de perfil no meio (ex.: o VENDEDOR registra a venda e
 * depois o CLIENTE cancela): coloca um usuário com o perfil pedido no contexto de segurança.
 */
public final class Perfis {

    private Perfis() {
    }

    public static void como(String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "teste-" + perfil.toLowerCase(), "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }

    public static void sair() {
        SecurityContextHolder.clearContext();
    }
}
