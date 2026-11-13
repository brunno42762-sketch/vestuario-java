package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Entrega;
import br.edu.eseg.vestuario.ezVest.service.ClienteService;
import br.edu.eseg.vestuario.ezVest.service.ClienteValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClienteValidator clienteValidator;

    public ClienteController(ClienteService clienteService, ClienteValidator clienteValidator) {
        this.clienteService = clienteService;
        this.clienteValidator = clienteValidator;
    }

    /**
     * Só aceita do formulário os campos que estão na tela. Sem isso, alguém poderia
     * enviar outros campos (ex.: "idCliente" ou "pedidos[0]...") e o Spring os preencheria.
     */
    @InitBinder("cliente")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("faixaEtaria", "genero", "cidade", "estado",
                "canalPreferido", "clienteFidelidade",
                "entrega.cidadeEntrega", "entrega.estadoEntrega");
    }

    // ---------- Listar ----------
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "listar-clientes";
    }

    // ---------- Criar ----------
    @GetMapping("/novo")
    public String novo(Model model) {
        Cliente cliente = new Cliente();
        cliente.setEntrega(new Entrega()); // o formulário precisa de um objeto para *{entrega.cidadeEntrega}
        model.addAttribute("cliente", cliente);
        return formulario(model, false);
    }

    @PostMapping
    public String criar(@ModelAttribute("cliente") Cliente cliente,
                        BindingResult erros, Model model, RedirectAttributes redirect) {
        clienteValidator.validate(cliente, erros);
        if (erros.hasErrors()) {
            return formulario(model, false);
        }
        try {
            Cliente salvo = clienteService.criar(cliente);
            redirect.addFlashAttribute("sucesso", "Cliente " + salvo.getIdCliente() + " criado.");
            return "redirect:/admin/clientes";
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage());
            return formulario(model, false);
        }
    }

    // ---------- Editar ----------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Cliente cliente = clienteService.buscar(id);
        if (cliente.getEntrega() == null) {
            cliente.setEntrega(new Entrega()); // cliente antigo sem entrega: mostra os campos vazios
        }
        model.addAttribute("cliente", cliente);
        return formulario(model, true);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id,
                            @ModelAttribute("cliente") Cliente cliente,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        cliente.setIdCliente(id);
        clienteValidator.validate(cliente, erros);
        if (erros.hasErrors()) {
            return formulario(model, true);
        }
        try {
            clienteService.atualizar(id, cliente);
            redirect.addFlashAttribute("sucesso", "Cliente " + id + " atualizado.");
            return "redirect:/admin/clientes";
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage());
            return formulario(model, true);
        }
    }

    // ---------- Excluir ----------
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            clienteService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Cliente " + id + " excluído.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    /** Coloca no Model as opções dos <select> e devolve o template do formulário. */
    private String formulario(Model model, boolean edicao) {
        model.addAttribute("edicao", edicao);
        model.addAttribute("faixasEtarias", ClienteValidator.FAIXAS_ETARIAS);
        model.addAttribute("generos", ClienteValidator.GENEROS);
        model.addAttribute("ufs", ClienteValidator.UFS);
        model.addAttribute("canais", Canal.values());
        return "criar-cliente";
    }
}
