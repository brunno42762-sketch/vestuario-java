package br.edu.eseg.vestuario.ezVest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Cadeia de segurança só para o H2 Console: ele não envia o token CSRF e roda dentro de
 * frames, então precisa de regras próprias. Acesso só para ADMIN (já logado pelo /login).
 */
@Configuration
public class H2ConsoleSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/h2-console/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            // sem login: manda para a tela de login; logado sem ser ADMIN: página 403
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .accessDeniedPage("/acesso-negado"));
        return http.build();
    }
}
