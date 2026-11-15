package br.edu.eseg.vestuario.ezVest.carga;

import br.edu.eseg.vestuario.ezVest.model.Perfil;
import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria o primeiro ADMIN a partir das variáveis de ambiente APP_ADMIN_USER e APP_ADMIN_PASSWORD.
 * Nenhuma senha fica escrita no código ou no repositório. Os outros usuários
 * (vendedores e clientes) são cadastrados pelo ADMIN na tela /admin/usuarios.
 */
@Configuration
public class CargaAutomatica {

    private static final Logger log = LoggerFactory.getLogger(CargaAutomatica.class);

    @Bean
    @Order(2) // depois da migração do legado (MigracaoLegado é @Order(1))
    public CommandLineRunner cadastrarAdministrador(UsuarioRepository repository,
                                                    PasswordEncoder passwordEncoder,
                                                    Environment environment) {
        return args -> {
            String username = environment.getProperty("APP_ADMIN_USER");
            String senha = environment.getProperty("APP_ADMIN_PASSWORD");

            if (username == null || username.isBlank() || senha == null || senha.isBlank()) {
                log.warn("APP_ADMIN_USER e APP_ADMIN_PASSWORD não configuradas: nenhum ADMIN foi criado.");
                return;
            }

            var existente = repository.findByUsername(username);
            if (existente.isPresent()) {
                Usuario u = existente.get();
                log.info("ADMIN '{}' já existe (perfil {}, ativo {}, senha confere: {}).",
                        u.getUsername(), u.getPerfil(), u.isAtivo(), passwordEncoder.matches(senha, u.getSenha()));
                return;
            }

            if (senha.length() < 8) {
                log.warn("APP_ADMIN_PASSWORD precisa ter pelo menos 8 caracteres: nenhum ADMIN foi criado.");
                return;
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username.trim());
            usuario.setSenha(passwordEncoder.encode(senha)); // grava só o hash
            usuario.setPerfil(Perfil.ADMIN);
            usuario.setAtivo(true);
            repository.saveAndFlush(usuario);
            log.info("Administrador cadastrado com sucesso: {}", usuario.getUsername());
        };
    }
}
