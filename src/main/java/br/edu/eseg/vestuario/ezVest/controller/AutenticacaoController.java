package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.carrinho.Carrinho;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Telas de entrada e de acesso negado. O POST do login e o do logout são tratados
 * pelo próprio Spring Security (ver SecurityConfig).
 */
@Controller
public class AutenticacaoController {

    private final Carrinho carrinho;

    public AutenticacaoController(Carrinho carrinho) {
        this.carrinho = carrinho;
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        // quem já está logado não precisa ver o formulário de novo
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/entrar";
        }
        return "login";
    }

    /**
     * Para onde cada perfil vai depois de entrar: equipe para o painel; cliente para a conta,
     * ou de volta ao carrinho se ele entrou no meio de uma compra.
     * (O carrinho continua o mesmo: o Spring Security troca o id da sessão no login, mas mantém o conteúdo.)
     */
    @GetMapping("/entrar")
    public String destinoDepoisDoLogin(Authentication auth) {
        boolean cliente = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        if (!cliente) {
            return "redirect:/admin";
        }
        return carrinho.isVazio() ? "redirect:/minha-conta" : "redirect:/carrinho";
    }

    /**
     * Página 403. Aceita qualquer método (não só GET): quando um POST é negado (ex.: VENDEDOR
     * tentando excluir, ou formulário sem o token CSRF), o Spring Security ENCAMINHA a própria
     * requisição POST para cá. Com @GetMapping, a resposta seria 405 em vez desta página.
     */
    @RequestMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
