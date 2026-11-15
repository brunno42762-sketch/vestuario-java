package br.edu.eseg.vestuario.ezVest.security;

import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Revalida a conta a cada requisição. Se o ADMIN desativar o usuário, trocar o username,
 * o perfil ou o cliente vinculado, a sessão aberta é encerrada na próxima página.
 */
public class EstadoContaFilter extends OncePerRequestFilter {

    private final UsuarioRepository repository;

    public EstadoContaFilter(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof PrincipalVestuario principal) {
            Optional<Usuario> atual = repository.findById(principal.getId());
            boolean invalida = atual.isEmpty() || !atual.get().isAtivo()
                    || !atual.get().getUsername().equals(principal.getUsername())
                    || atual.get().getPerfil() != principal.getPerfil()
                    || !Objects.equals(idCliente(atual.get()), principal.getIdCliente());
            if (invalida) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static Integer idCliente(Usuario u) {
        return u.getCliente() == null ? null : u.getCliente().getIdCliente();
    }
}
