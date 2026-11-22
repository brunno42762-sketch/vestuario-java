package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.carrinho.CarrinhoService;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.FormaPagamento;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import br.edu.eseg.vestuario.ezVest.security.PrincipalVestuario;
import br.edu.eseg.vestuario.ezVest.service.ClienteService;
import br.edu.eseg.vestuario.ezVest.service.VendaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Carrinho da loja.
 *   Ver e mexer no carrinho:  visitante ou CLIENTE   (a equipe registra vendas em /admin/vendas)
 *   Finalizar a compra:       só CLIENTE              (o visitante é mandado para o login)
 * As regras ficam no SecurityConfig e, para a compra, também no @PreAuthorize do VendaService.
 */
@Controller
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;
    private final VendaService vendaService;
    private final ClienteService clienteService;

    public CarrinhoController(CarrinhoService carrinhoService, VendaService vendaService,
                              ClienteService clienteService) {
        this.carrinhoService = carrinhoService;
        this.vendaService = vendaService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String ver(Model model) {
        model.addAttribute("carrinho", carrinhoService.resumo());
        return "carrinho";
    }

    /** Botão "Adicionar" do catálogo. Volta para a mesma página do catálogo. */
    @PostMapping("/itens")
    public String adicionar(@RequestParam(required = false) Integer idProduto,
                            @RequestParam(required = false) Tamanho tamanho,
                            @RequestParam(defaultValue = "1") int quantidade,
                            @RequestParam(defaultValue = "1") int pagina,
                            @RequestParam(required = false) String categoria,
                            RedirectAttributes redirect) {
        try {
            carrinhoService.adicionar(idProduto, tamanho, quantidade);
            redirect.addFlashAttribute("sucesso", quantidade + " peça(s) no tamanho " + tamanho
                    + " adicionada(s) ao carrinho.");
            redirect.addFlashAttribute("mostrarLinkCarrinho", true);
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // Volta para a mesma página (e categoria) do catálogo
        redirect.addAttribute("pagina", Math.max(pagina, 1));
        if (categoria != null && !categoria.isBlank()) {
            redirect.addAttribute("categoria", categoria);
        }
        return "redirect:/#catalogo";
    }

    @PostMapping("/itens/{chave}/quantidade")
    public String alterarQuantidade(@PathVariable String chave, @RequestParam int quantidade,
                                    RedirectAttributes redirect) {
        try {
            carrinhoService.alterarQuantidade(chave, quantidade);
            redirect.addFlashAttribute("sucesso", "Quantidade atualizada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/carrinho";
    }

    @PostMapping("/itens/{chave}/remover")
    public String remover(@PathVariable String chave, RedirectAttributes redirect) {
        carrinhoService.remover(chave);
        redirect.addFlashAttribute("sucesso", "Item removido do carrinho.");
        return "redirect:/carrinho";
    }

    // ---------------- Finalizar (só CLIENTE) ----------------

    @GetMapping("/finalizar")
    public String revisar(@AuthenticationPrincipal PrincipalVestuario usuario, Model model) {
        var resumo = carrinhoService.resumo();
        if (resumo.isVazio()) {
            return "redirect:/carrinho";
        }
        model.addAttribute("carrinho", resumo);
        model.addAttribute("cliente", clienteService.buscar(idClienteDo(usuario)));
        model.addAttribute("formasPagamento", FormaPagamento.values());
        return "carrinho-finalizar";
    }

    @PostMapping("/finalizar")
    public String finalizar(@AuthenticationPrincipal PrincipalVestuario usuario,
                            @RequestParam(required = false) FormaPagamento formaPagamento,
                            RedirectAttributes redirect) {
        try {
            // O cliente vem SEMPRE do usuário logado, nunca do formulário
            Pedido pedido = vendaService.comprar(idClienteDo(usuario),
                    carrinhoService.itensParaVenda(), formaPagamento);
            carrinhoService.esvaziar(); // só depois de a venda ter sido gravada
            redirect.addFlashAttribute("sucesso", "Pedido " + pedido.getIdPedido()
                    + " criado! Acompanhe o status por aqui.");
            return "redirect:/minha-conta/pedidos/" + pedido.getIdPedido();
        } catch (RegraNegocioException e) {
            // Ex.: o estoque acabou entre colocar no carrinho e finalizar. O carrinho continua igual.
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/carrinho/finalizar";
        }
    }

    private Integer idClienteDo(PrincipalVestuario usuario) {
        if (usuario.getIdCliente() == null) {
            throw new RegraNegocioException("Sua conta não está ligada a um cadastro de cliente.");
        }
        return usuario.getIdCliente();
    }
}
