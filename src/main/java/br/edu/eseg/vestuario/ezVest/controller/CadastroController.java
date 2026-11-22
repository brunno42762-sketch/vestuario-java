package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.cadastro.CadastroForm;
import br.edu.eseg.vestuario.ezVest.cadastro.CadastroService;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.service.ClienteValidator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "Criar conta": página pública para quem ainda não tem acesso. */
@Controller
@RequestMapping("/cadastro")
public class CadastroController {

    private final CadastroService cadastroService;
    private final ClienteValidator clienteValidator;

    public CadastroController(CadastroService cadastroService, ClienteValidator clienteValidator) {
        this.cadastroService = cadastroService;
        this.clienteValidator = clienteValidator;
    }

    /** Só os campos da tela (o perfil, por exemplo, é sempre CLIENTE e não vem do formulário). */
    @InitBinder("cadastro")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("username", "senha", "confirmacaoSenha",
                "cliente.faixaEtaria", "cliente.genero", "cliente.cidade", "cliente.estado",
                "cliente.canalPreferido", "cliente.clienteFidelidade",
                "cliente.entrega.cidadeEntrega", "cliente.entrega.estadoEntrega");
    }

    @GetMapping
    public String formulario(Authentication auth, Model model) {
        if (logado(auth)) {
            return "redirect:/";
        }
        model.addAttribute("cadastro", new CadastroForm());
        return pagina(model);
    }

    @PostMapping
    public String cadastrar(Authentication auth, @ModelAttribute("cadastro") CadastroForm form,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        if (logado(auth)) {
            return "redirect:/";
        }
        cadastroService.validarAcesso(form, erros);
        // Os campos do cliente estão em "cliente.xxx": o validador do Cliente trabalha dentro desse caminho
        erros.pushNestedPath("cliente");
        clienteValidator.validate(form.getCliente(), erros);
        erros.popNestedPath();

        if (erros.hasErrors()) {
            return pagina(model);
        }
        try {
            var usuario = cadastroService.cadastrar(form);
            redirect.addAttribute("cadastro", "ok");
            redirect.addAttribute("usuario", usuario.getUsername());
            return "redirect:/login"; // /login?cadastro=ok&usuario=fulano
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // Duas pessoas criando o mesmo username ao mesmo tempo: o índice único do banco barra a segunda
            erros.rejectValue("username", "duplicado", "Este nome de usuário já está em uso.");
        }
        return pagina(model);
    }

    private String pagina(Model model) {
        model.addAttribute("faixasEtarias", ClienteValidator.FAIXAS_ETARIAS);
        model.addAttribute("generos", ClienteValidator.GENEROS);
        model.addAttribute("ufs", ClienteValidator.UFS);
        model.addAttribute("canais", Canal.values());
        return "cadastro";
    }

    private boolean logado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
