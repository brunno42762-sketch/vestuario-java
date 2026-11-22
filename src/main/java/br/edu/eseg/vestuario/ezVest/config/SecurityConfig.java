package br.edu.eseg.vestuario.ezVest.config;

import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import br.edu.eseg.vestuario.ezVest.security.EstadoContaFilter;
import br.edu.eseg.vestuario.ezVest.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Regras de acesso (entrega 3). A proteção é feita no SERVIDOR, em duas camadas:
 *   1. aqui, por URL e método HTTP;
 *   2. nos services e controllers, com @PreAuthorize.
 * Esconder botões na tela (sec:authorize) é só conforto: não é a proteção.
 *
 *                                  visitante  CLIENTE  VENDEDOR  ADMIN
 *   / (catálogo), /login               x         x        x        x
 *   /cadastro (criar conta)            x
 *   /carrinho (ver, adicionar...)      x         x
 *   /carrinho/finalizar (comprar)                x
 *   /minha-conta/** (e cancelar)                 x
 *   /admin/** (consultar)                                 x        x
 *   /admin/vendas (registrar venda)                       x        x
 *   clientes: cadastrar/editar                            x        x
 *   pedidos: alterar status                               x        x
 *   produtos: cadastrar/editar/excluir                             x
 *   clientes: excluir                                              x
 *   /admin/usuarios/**                                             x
 *   /admin/relatorio (receita e lucro)                             x
 */
@Configuration
@EnableMethodSecurity // liga o @PreAuthorize
public class SecurityConfig {

    /** Quem ainda não entrou OU quem entrou como CLIENTE. */
    private static final AuthorizationManager<RequestAuthorizationContext> VISITANTE_OU_CLIENTE =
            AuthorizationManagers.anyOf(AuthenticatedAuthorizationManager.anonymous(),
                    AuthorityAuthorizationManager.hasRole("CLIENTE"));

    /** BCrypt: a senha é guardada como hash com "sal" aleatório; não dá para recuperar a original. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UsuarioDetailsService details,
                                                            PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(details);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    @Order(2) // depois da cadeia do H2 Console (H2ConsoleSecurityConfig)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
                                                   UsuarioRepository usuarios) throws Exception {
        http.authenticationProvider(provider)
            // As regras são lidas de cima para baixo: vale a primeira que combinar.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/acesso-negado", "/error",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()

                // Criar conta: só faz sentido para quem ainda não entrou
                // (o CadastroController manda quem já está logado de volta para a loja)
                .requestMatchers("/cadastro").permitAll()

                // Carrinho: finalizar a compra é só do CLIENTE (o visitante vai para o login)...
                .requestMatchers("/carrinho/finalizar").hasRole("CLIENTE")
                // ...e o resto do carrinho é do visitante ou do CLIENTE. A equipe vende pela
                // tela /admin/vendas, então ADMIN e VENDEDOR não usam o carrinho.
                .requestMatchers("/carrinho", "/carrinho/**").access(VISITANTE_OU_CLIENTE)

                // Relatório de receita e lucro: só ADMIN (mostra lucro e margem)
                .requestMatchers("/admin/relatorio", "/admin/relatorio/**").hasRole("ADMIN")

                // Usuários: só ADMIN (qualquer método)
                .requestMatchers("/admin/usuarios", "/admin/usuarios/**").hasRole("ADMIN")

                // Exclusões: ADMIN
                .requestMatchers(HttpMethod.POST,
                        "/admin/produtos/*/excluir", "/admin/clientes/*/excluir").hasRole("ADMIN")

                // Catálogo de produtos: só ADMIN cadastra e edita
                .requestMatchers(HttpMethod.POST, "/admin/produtos", "/admin/produtos/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,
                        "/admin/produtos/novo", "/admin/produtos/*/editar").hasRole("ADMIN")

                // Clientes, pedidos e vendas: ADMIN ou VENDEDOR
                .requestMatchers(HttpMethod.POST,
                        "/admin/clientes", "/admin/clientes/*",
                        "/admin/pedidos/*/status", "/admin/vendas").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.GET,
                        "/admin/clientes/novo", "/admin/clientes/*/editar").hasAnyRole("ADMIN", "VENDEDOR")

                // Nenhum POST administrativo não declarado acima é liberado
                .requestMatchers(HttpMethod.POST, "/admin/**").denyAll()
                .requestMatchers(HttpMethod.PUT, "/**").denyAll()
                .requestMatchers(HttpMethod.PATCH, "/**").denyAll()
                .requestMatchers(HttpMethod.DELETE, "/**").denyAll()

                // Consultas da área administrativa: ADMIN ou VENDEDOR
                .requestMatchers(HttpMethod.GET, "/admin", "/admin/**").hasAnyRole("ADMIN", "VENDEDOR")

                // Área do cliente
                .requestMatchers("/minha-conta", "/minha-conta/**").hasRole("CLIENTE")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    // cada perfil vai para a sua área (ver AutenticacaoController)
                    .defaultSuccessUrl("/entrar", true)
                    .failureUrl("/login?error").permitAll())
            .logout(logout -> logout.logoutUrl("/logout")   // só POST, com o token CSRF
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true).permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acesso-negado"))
            .addFilterBefore(new EstadoContaFilter(usuarios), AuthorizationFilter.class);
        // CSRF permanece habilitado por padrão (os forms com th:action já enviam o token).
        return http.build();
    }
}
