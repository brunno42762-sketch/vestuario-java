package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.service.OrdemPedidos;
import br.edu.eseg.vestuario.ezVest.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/pedidos")
public class PedidoController {

    /** Quantos números de página aparecem de cada lado da página atual. */
    private static final int VIZINHOS = 2;

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** /admin/pedidos?pagina=3&status=ENTREGUE&ordem=ID_CRESCENTE (os três parâmetros são opcionais) */
    @GetMapping
    public String listar(@RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(required = false) Status status,
                         @RequestParam(defaultValue = "RECENTES") OrdemPedidos ordem,
                         Model model, RedirectAttributes redirect) {
        // Se for preciso redirecionar, o filtro e a ordem vão junto na URL
        manterFiltroEOrdem(redirect, status, ordem);
        if (pagina < 1) {
            return "redirect:/admin/pedidos";
        }

        Page<Pedido> pedidos = pedidoService.listar(status, ordem, pagina);
        int totalPaginas = pedidos.getTotalPages();

        // Página que não existe (ex.: ?pagina=999): manda para a última
        if (pagina > 1 && pagina > totalPaginas) {
            redirect.addAttribute("pagina", Math.max(totalPaginas, 1));
            return "redirect:/admin/pedidos";
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("todosStatus", Status.values());
        model.addAttribute("ordens", OrdemPedidos.values());
        // Texto vazio quando não há filtro: os links de página levam "status=" e o Spring lê como "todos"
        model.addAttribute("statusFiltro", status == null ? "" : status.name());
        model.addAttribute("ordem", ordem.name());
        model.addAttribute("paginaAtual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioJanela", Math.max(1, pagina - VIZINHOS));
        model.addAttribute("fimJanela", Math.max(1, Math.min(totalPaginas, pagina + VIZINHOS)));
        return "listar-pedidos";
    }

    /**
     * Altera o status e volta para a mesma página, o mesmo filtro e a mesma ordem
     * em que o usuário estava (os três chegam por campos ocultos do formulário).
     */
    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable Integer id,
                                @RequestParam(required = false) Status novoStatus,
                                @RequestParam(defaultValue = "1") int pagina,
                                @RequestParam(required = false) Status status,
                                @RequestParam(defaultValue = "RECENTES") OrdemPedidos ordem,
                                RedirectAttributes redirect) {
        try {
            pedidoService.alterarStatus(id, novoStatus);
            redirect.addFlashAttribute("sucesso",
                    "Pedido " + id + " agora está " + novoStatus.getNome() + ".");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // addAttribute vira parâmetro na URL do redirect: /admin/pedidos?pagina=3&status=ENTREGUE&ordem=...
        redirect.addAttribute("pagina", pagina);
        manterFiltroEOrdem(redirect, status, ordem);
        return "redirect:/admin/pedidos";
    }

    private void manterFiltroEOrdem(RedirectAttributes redirect, Status status, OrdemPedidos ordem) {
        if (status != null) {
            redirect.addAttribute("status", status);
        }
        if (ordem != OrdemPedidos.RECENTES) {
            redirect.addAttribute("ordem", ordem);
        }
    }
}
