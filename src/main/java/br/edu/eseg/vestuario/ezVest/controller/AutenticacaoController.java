package br.edu.eseg.vestuario.ezVest.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Telas de entrada e de acesso negado. O POST do login e o do logout são tratados
 * pelo próprio Spring Security (ver SecurityConfig).
 */
@Controller
public class AutenticacaoController {

    @GetMapping("/login")
    public String login(Authentication auth) {
        // quem já está logado não precisa ver o formulário de novo
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/entrar";
        }
        return "login";
    }

    /** Para onde cada perfil vai depois de entrar: cliente para a conta, equipe para o painel. */
    @GetMapping("/entrar")
    public String destinoDepoisDoLogin(Authentication auth) {
        boolean cliente = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        return cliente ? "redirect:/minha-conta" : "redirect:/admin";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
