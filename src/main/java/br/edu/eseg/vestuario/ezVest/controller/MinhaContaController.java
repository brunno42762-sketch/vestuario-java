package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.security.PrincipalVestuario;
import br.edu.eseg.vestuario.ezVest.service.ClienteService;
import br.edu.eseg.vestuario.ezVest.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Área do perfil CLIENTE: só os próprios pedidos.
 * O id do cliente vem SEMPRE do usuário logado (@AuthenticationPrincipal), nunca da URL:
 * não existe um jeito de pedir "os pedidos do cliente X".
 */
@Controller
@RequestMapping("/minha-conta")
public class MinhaContaController {

    private static final int VIZINHOS = 2;

    private final PedidoService pedidoService;
    private final ClienteService clienteService;

    public MinhaContaController(PedidoService pedidoService, ClienteService clienteService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String meusPedidos(@AuthenticationPrincipal PrincipalVestuario usuario,
                              @RequestParam(defaultValue = "1") int pagina, Model model) {
        Integer idCliente = idClienteDo(usuario);
        if (pagina < 1) {
            return "redirect:/minha-conta";
        }

        Page<Pedido> pedidos = pedidoService.listarDoCliente(idCliente, pagina);
        int totalPaginas = pedidos.getTotalPages();
        if (pagina > 1 && pagina > totalPaginas) {
            return "redirect:/minha-conta?pagina=" + Math.max(totalPaginas, 1);
        }

        model.addAttribute("cliente", clienteService.buscar(idCliente));
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("paginaAtual", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("inicioJanela", Math.max(1, pagina - VIZINHOS));
        model.addAttribute("fimJanela", Math.max(1, Math.min(totalPaginas, pagina + VIZINHOS)));
        return "minha-conta";
    }

    @GetMapping("/pedidos/{id}")
    public String detalhe(@AuthenticationPrincipal PrincipalVestuario usuario,
                          @PathVariable Integer id, Model model) {
        // pedido de outra pessoa = 404, igual a um pedido que não existe
        Pedido pedido = pedidoService.buscarDoCliente(id, idClienteDo(usuario));
        model.addAttribute("pedido", pedido);
        model.addAttribute("cliente", clienteService.buscar(idClienteDo(usuario)));
        return "minha-conta-pedido";
    }

    /** O cliente cancela um pedido que ainda está "Criado"; as peças voltam para o estoque. */
    @PostMapping("/pedidos/{id}/cancelar")
    public String cancelar(@AuthenticationPrincipal PrincipalVestuario usuario,
                           @PathVariable Integer id, RedirectAttributes redirect) {
        try {
            pedidoService.cancelarDoCliente(id, idClienteDo(usuario));
            redirect.addFlashAttribute("sucesso", "Pedido " + id + " cancelado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/minha-conta/pedidos/" + id;
    }

    private Integer idClienteDo(PrincipalVestuario usuario) {
        if (usuario.getIdCliente() == null) {
            throw new RecursoNaoEncontradoException("Sua conta não está ligada a um cadastro de cliente.");
        }
        return usuario.getIdCliente();
    }
}
