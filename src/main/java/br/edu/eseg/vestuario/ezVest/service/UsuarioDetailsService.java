package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import br.edu.eseg.vestuario.ezVest.security.PrincipalVestuario;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chamado pelo Spring Security no login: busca o usuário pelo username digitado.
 * A conferência da senha (BCrypt) é feita pelo DaoAuthenticationProvider.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        Integer idCliente = u.getCliente() == null ? null : u.getCliente().getIdCliente();
        return new PrincipalVestuario(u.getId(), u.getUsername(), u.getSenha(), u.getPerfil(),
                u.isAtivo(), idCliente);
    }
}
