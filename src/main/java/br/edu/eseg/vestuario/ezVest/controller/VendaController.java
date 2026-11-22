package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.FormaPagamento;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import br.edu.eseg.vestuario.ezVest.service.ClienteService;
import br.edu.eseg.vestuario.ezVest.service.ItemVenda;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import br.edu.eseg.vestuario.ezVest.service.VendaService;
import br.edu.eseg.vestuario.ezVest.venda.VendaForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Venda registrada pela equipe (VENDEDOR ou ADMIN): balcão da loja, marketplace... */
@Controller
@RequestMapping("/admin/vendas")
public class VendaController {

    private final VendaService vendaService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public VendaController(VendaService vendaService, ClienteService clienteService,
                           ProdutoService produtoService) {
        this.vendaService = vendaService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    /** Só os campos da tela; "itens[*]" aceita as linhas itens[0], itens[1]... */
    @InitBinder("venda")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("idCliente", "canal", "formaPagamento",
                "itens[*].idProduto", "itens[*].tamanho", "itens[*].quantidade");
        binder.setAutoGrowCollectionLimit(VendaForm.LINHAS); // ninguém cria itens[999] pelo formulário
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("venda", new VendaForm());
        return formulario(model);
    }

    @PostMapping
    public String registrar(@ModelAttribute("venda") VendaForm form, BindingResult erros,
                            Model model, RedirectAttributes redirect) {
        List<ItemVenda> itens = validar(form, erros);
        if (erros.hasErrors()) {
            return formulario(model);
        }
        try {
            Pedido pedido = vendaService.registrarNaLoja(form.getIdCliente(), form.getCanal(),
                    form.getFormaPagamento(), itens);
            // %,.2f em português: 4.445,40
            redirect.addFlashAttribute("sucesso", String.format(Locale.of("pt", "BR"),
                    "Venda registrada: pedido %d, R$ %,.2f.", pedido.getIdPedido(), pedido.getValorTotal()));
            redirect.addAttribute("ordem", "ID_DECRESCENTE"); // o pedido novo aparece no topo da lista
            return "redirect:/admin/pedidos";
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage()); // ex.: estoque insuficiente
            return formulario(model);
        }
    }

    /** Confere o preenchimento da tela; as regras de negócio ficam no VendaService. */
    private List<ItemVenda> validar(VendaForm form, BindingResult erros) {
        if (form.getIdCliente() == null && !erros.hasFieldErrors("idCliente")) {
            erros.rejectValue("idCliente", "obrigatorio", "Selecione o cliente.");
        }
        if (form.getCanal() == null && !erros.hasFieldErrors("canal")) {
            erros.rejectValue("canal", "obrigatorio", "Selecione o canal.");
        }
        if (form.getFormaPagamento() == null && !erros.hasFieldErrors("formaPagamento")) {
            erros.rejectValue("formaPagamento", "obrigatorio", "Selecione a forma de pagamento.");
        }

        List<ItemVenda> itens = new ArrayList<>();
        for (int i = 0; i < form.getItens().size(); i++) {
            VendaForm.Linha linha = form.getItens().get(i);
            String campo = "itens[" + i + "]";
            if (linha == null || linha.isEmBranco() || erros.hasFieldErrors(campo + ".*")) {
                continue;
            }
            if (!linha.isCompleta()) {
                erros.rejectValue(campo + ".quantidade", "incompleta",
                        "Preencha produto, tamanho e quantidade (ou deixe a linha toda em branco).");
            } else if (linha.getQuantidade() < 1 || linha.getQuantidade() > VendaService.MAX_QUANTIDADE) {
                erros.rejectValue(campo + ".quantidade", "intervalo",
                        "De 1 a " + VendaService.MAX_QUANTIDADE + " peças.");
            } else {
                itens.add(new ItemVenda(linha.getIdProduto(), linha.getTamanho(), linha.getQuantidade()));
            }
        }
        if (itens.isEmpty() && !erros.hasErrors()) {
            erros.reject("semItens", "Inclua pelo menos um produto.");
        }
        return itens;
    }

    private String formulario(Model model) {
        VendaForm form = (VendaForm) model.getAttribute("venda");
        if (form != null) {
            form.completarLinhas();
        }
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("produtos", produtoService.listar());
        model.addAttribute("canais", Canal.values());
        model.addAttribute("formasPagamento", FormaPagamento.values());
        model.addAttribute("tamanhos", Tamanho.values());
        model.addAttribute("maxQuantidade", VendaService.MAX_QUANTIDADE);
        return "criar-venda";
    }
}
