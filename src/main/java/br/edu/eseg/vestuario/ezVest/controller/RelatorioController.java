package br.edu.eseg.vestuario.ezVest.controller;

import br.edu.eseg.vestuario.ezVest.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** /admin/relatorio?de=2025-01-01&ate=2025-12-31 (RF09). Só ADMIN (SecurityConfig + @PreAuthorize no serviço). */
@Controller
@RequestMapping("/admin/relatorio")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping
    public String ver(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                      Model model) {
        if (de != null && ate != null && de.isAfter(ate)) {
            model.addAttribute("erro", "A data inicial é depois da data final: o período foi ignorado.");
            de = null;
            ate = null;
        }
        model.addAttribute("relatorio", relatorioService.gerar(de, ate));
        model.addAttribute("de", de);
        model.addAttribute("ate", ate);
        return "relatorio";
    }
}
