package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.service.FiltroPedidos;
import br.edu.eseg.vestuario.ezVest.service.OrdemPedidos;
import br.edu.eseg.vestuario.ezVest.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/pedidos")
public class PedidoController {

    /** Quantos números de página aparecem de cada lado da página atual. */
    private static final int VIZINHOS = 2;

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * /admin/pedidos?status=ENTREGUE&canal=FISICA&de=2026-01-01&ate=2026-03-31&cliente=103&ordem=ID_CRESCENTE&pagina=2
     * Todos os parâmetros são opcionais (RF08).
     */
    @GetMapping
    public String listar(@RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(required = false) Status status,
                         @RequestParam(required = false) Canal canal,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                         @RequestParam(required = false) Integer cliente,
                         @RequestParam(defaultValue = "RECENTES") OrdemPedidos ordem,
                         Model model, RedirectAttributes redirect) {
        FiltroPedidos filtro = new FiltroPedidos(status, canal, de, ate, cliente);
        if (filtro.isPeriodoInvalido()) {
            // Avisa e ignora só o período; os outros filtros continuam valendo
            model.addAttribute("erro", "A data inicial é depois da data final: o período foi ignorado.");
            filtro = new FiltroPedidos(status, canal, null, null, cliente);
        }

        // Se for preciso redirecionar, os filtros e a ordem vão junto na URL
        manterFiltroEOrdem(redirect, filtro, ordem);
        if (pagina < 1) {
            return "redirect:/admin/pedidos";
        }

        Page<Pedido> pedidos = pedidoService.listar(filtro, ordem, pagina);
        int totalPaginas = pedidos.getTotalPages();

        // Página que não existe (ex.: ?pagina=999): manda para a última
        if (pagina > 1 && pagina > totalPaginas) {
            redirect.addAttribute("pagina", Math.max(totalPaginas, 1));
            return "redirect:/admin/pedidos";
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("filtro", filtro);
        model.addAttribute("filtroAtivo", !filtro.comoParametros().isEmpty());
        // Filtros preenchidos, para os campos ocultos do formulário de status
        model.addAttribute("parametrosFiltro", filtro.comoParametros());
        // Base dos links de página: a mesma lista, com os mesmos filtros e ordem
        model.addAttribute("basePaginacao", urlDaLista(filtro, ordem));
        model.addAttribute("todosStatus", Status.values());
        model.addAttribute("canais", Canal.values());
        model.addAttribute("ordens", OrdemPedidos.values());
        model.addAttribute("ordem", ordem.name());
        model.addAttribute("paginaAtual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioJanela", Math.max(1, pagina - VIZINHOS));
        model.addAttribute("fimJanela", Math.max(1, Math.min(totalPaginas, pagina + VIZINHOS)));
        return "listar-pedidos";
    }

    /**
     * Altera o status e volta para a mesma página, os mesmos filtros e a mesma ordem
     * em que o usuário estava (chegam por campos ocultos do formulário).
     */
    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable Integer id,
                                @RequestParam(required = false) Status novoStatus,
                                @RequestParam(defaultValue = "1") int pagina,
                                @RequestParam(required = false) Status status,
                                @RequestParam(required = false) Canal canal,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                                @RequestParam(required = false) Integer cliente,
                                @RequestParam(defaultValue = "RECENTES") OrdemPedidos ordem,
                                RedirectAttributes redirect) {
        try {
            pedidoService.alterarStatus(id, novoStatus);
            redirect.addFlashAttribute("sucesso",
                    "Pedido " + id + " agora está " + novoStatus.getNome() + ".");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        // addAttribute vira parâmetro na URL do redirect: /admin/pedidos?pagina=3&status=ENTREGUE&...
        redirect.addAttribute("pagina", pagina);
        manterFiltroEOrdem(redirect, new FiltroPedidos(status, canal, de, ate, cliente), ordem);
        return "redirect:/admin/pedidos";
    }

    private void manterFiltroEOrdem(RedirectAttributes redirect, FiltroPedidos filtro, OrdemPedidos ordem) {
        filtro.comoParametros().forEach(redirect::addAttribute);
        if (ordem != OrdemPedidos.RECENTES) {
            redirect.addAttribute("ordem", ordem);
        }
    }

    /** "/admin/pedidos?status=ENTREGUE&ordem=ID_CRESCENTE" (o template acrescenta "&pagina=N"). */
    private String urlDaLista(FiltroPedidos filtro, OrdemPedidos ordem) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/admin/pedidos");
        filtro.comoParametros().forEach(url::queryParam);
        url.queryParam("ordem", ordem.name());
        return url.encode().toUriString();
    }
}
