package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.service.PainelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de entrada da administração. Tudo o que é gestão fica sob /admin,
 * o que permite proteger a área inteira com uma regra só no Spring Security (entrega 3).
 */
@Controller
public class AdminController {

    private final PainelService painelService;

    public AdminController(PainelService painelService) {
        this.painelService = painelService;
    }

    @GetMapping("/admin")
    public String painel(Model model) {
        model.addAttribute("totalProdutos", painelService.totalProdutos());
        model.addAttribute("totalClientes", painelService.totalClientes());
        model.addAttribute("totalPedidos", painelService.totalPedidos());
        model.addAttribute("produtosParaRepor", painelService.produtosParaRepor());
        model.addAttribute("pedidosPorStatus", painelService.pedidosPorStatus());
        return "painel-admin";
    }
}
