package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import br.edu.eseg.vestuario.ezVest.service.ProdutoService;
import br.edu.eseg.vestuario.ezVest.service.VendaService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Controller
public class PaginaInicialController {

    /** Quantos números de página aparecem de cada lado da página atual (ex.: 3 4 [5] 6 7). */
    private static final int VIZINHOS = 2;

    private final ProdutoService produtoService;

    public PaginaInicialController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    /** Página inicial da loja com o catálogo paginado: /, /?pagina=2, /?categoria=Calça&pagina=2 ... */
    @GetMapping("/")
    public String inicio(@RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(required = false) String categoria,
                         Model model, RedirectAttributes redirect) {
        List<ProdutoService.ResumoCategoria> categorias = produtoService.categorias();
        // Categoria que não existe (ou vazia): mostra o catálogo inteiro
        if (categoria != null && categorias.stream().noneMatch(c -> c.nome().equals(categoria))) {
            return "redirect:/";
        }
        if (categoria != null) {
            redirect.addAttribute("categoria", categoria);
        }
        if (pagina < 1) {
            return "redirect:/";
        }

        Page<Produto> produtos = produtoService.catalogo(pagina, categoria);
        int totalPaginas = produtos.getTotalPages();

        // Página que não existe (ex.: ?pagina=999): manda para a última
        if (pagina > 1 && pagina > totalPaginas) {
            redirect.addAttribute("pagina", Math.max(totalPaginas, 1));
            return "redirect:/";
        }

        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriaAtual", categoria);
        // Base dos links de página: "/" ou "/?categoria=Cal%C3%A7a" (o fragment acrescenta "pagina=N")
        model.addAttribute("basePaginacao", categoria == null ? "/"
                : UriComponentsBuilder.fromPath("/").queryParam("categoria", categoria).encode().toUriString());
        model.addAttribute("produtos", produtos);
        model.addAttribute("paginaAtual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        // Faixa de números mostrada na navegação, sem passar do início nem do fim
        model.addAttribute("inicioJanela", Math.max(1, pagina - VIZINHOS));
        model.addAttribute("fimJanela", Math.max(1, Math.min(totalPaginas, pagina + VIZINHOS)));
        // Carrossel do topo: os 6 produtos mais vendidos
        model.addAttribute("destaques", produtoService.maisVendidos(6));
        // Opções do botão "Adicionar ao carrinho" de cada produto
        model.addAttribute("tamanhos", Tamanho.values());
        model.addAttribute("maxQuantidade", VendaService.MAX_QUANTIDADE);
        return "index";
    }
}
