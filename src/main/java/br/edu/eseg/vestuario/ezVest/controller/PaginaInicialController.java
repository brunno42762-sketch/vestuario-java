package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaginaInicialController {

    /** Quantos números de página aparecem de cada lado da página atual (ex.: 3 4 [5] 6 7). */
    private static final int VIZINHOS = 2;

    private final ProdutoService produtoService;

    public PaginaInicialController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    /** Página inicial da loja com o catálogo paginado: /, /?pagina=2, /?pagina=3 ... */
    @GetMapping("/")
    public String inicio(@RequestParam(defaultValue = "1") int pagina, Model model) {
        if (pagina < 1) {
            return "redirect:/";
        }

        Page<Produto> produtos = produtoService.catalogo(pagina);
        int totalPaginas = produtos.getTotalPages();

        // Página que não existe (ex.: ?pagina=999): manda para a última
        if (pagina > 1 && pagina > totalPaginas) {
            return "redirect:/?pagina=" + Math.max(totalPaginas, 1);
        }

        model.addAttribute("produtos", produtos);
        model.addAttribute("paginaAtual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        // Faixa de números mostrada na navegação, sem passar do início nem do fim
        model.addAttribute("inicioJanela", Math.max(1, pagina - VIZINHOS));
        model.addAttribute("fimJanela", Math.max(1, Math.min(totalPaginas, pagina + VIZINHOS)));
        return "index";
    }
}
