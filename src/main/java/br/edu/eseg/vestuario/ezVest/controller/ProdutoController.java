package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import br.edu.eseg.vestuario.ezVest.service.ProdutoValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final ProdutoValidator produtoValidator;

    public ProdutoController(ProdutoService produtoService, ProdutoValidator produtoValidator) {
        this.produtoService = produtoService;
        this.produtoValidator = produtoValidator;
    }

    /** Só aceita do formulário os campos que estão na tela (o id vem da URL, nunca do form). */
    @InitBinder("produto")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("categoria", "subcategoria", "cor", "marca", "colecao", "genero",
                "precoUnitario", "custoUnitario", "estoqueAtual", "estoqueMinimo",
                "fornecedor", "tempoReposicaoDias");
    }

    // ---------- Listar ----------
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", produtoService.listar());
        return "listar-produtos";
    }

    // ---------- Criar ----------
    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produto", new Produto());
        return formulario(model, false);
    }

    @PostMapping
    public String criar(@ModelAttribute("produto") Produto produto,
                        BindingResult erros, Model model, RedirectAttributes redirect) {
        produtoValidator.validate(produto, erros);
        if (erros.hasErrors()) {
            return formulario(model, false);
        }
        try {
            Produto salvo = produtoService.criar(produto);
            redirect.addFlashAttribute("sucesso", "Produto " + salvo.getIdProduto() + " criado.");
            return "redirect:/admin/produtos";
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage());
            return formulario(model, false);
        }
    }

    // ---------- Editar ----------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("produto", produtoService.buscar(id));
        return formulario(model, true);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id,
                            @ModelAttribute("produto") Produto produto,
                            BindingResult erros, Model model, RedirectAttributes redirect) {
        produto.setIdProduto(id);
        produtoValidator.validate(produto, erros);
        if (erros.hasErrors()) {
            return formulario(model, true);
        }
        try {
            produtoService.atualizar(id, produto);
            redirect.addFlashAttribute("sucesso", "Produto " + id + " atualizado.");
            return "redirect:/admin/produtos";
        } catch (RegraNegocioException e) {
            erros.reject("regra", e.getMessage());
            return formulario(model, true);
        }
    }

    // ---------- Excluir ----------
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            produtoService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Produto " + id + " excluído.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/produtos";
    }

    private String formulario(Model model, boolean edicao) {
        model.addAttribute("edicao", edicao);
        model.addAttribute("generos", ProdutoValidator.GENEROS);
        return "criar-produtos";
    }
}
