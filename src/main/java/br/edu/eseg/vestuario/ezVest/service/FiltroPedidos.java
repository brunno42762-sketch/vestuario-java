package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filtros da lista de pedidos (RF08). Todos são opcionais: null = "não filtrar por isto".
 *
 * @param status    situação do pedido
 * @param canal     canal de venda
 * @param de        data inicial (inclusive)
 * @param ate       data final (inclusive)
 * @param idCliente id do cliente
 */
public record FiltroPedidos(Status status, Canal canal, LocalDate de, LocalDate ate, Integer idCliente) {

    public static FiltroPedidos nenhum() {
        return new FiltroPedidos(null, null, null, null, null);
    }

    /** O período está invertido (data inicial depois da final)? */
    public boolean isPeriodoInvalido() {
        return de != null && ate != null && de.isAfter(ate);
    }

    /** Só os filtros preenchidos, com os nomes dos parâmetros da URL (para links e redirects). */
    public Map<String, Object> comoParametros() {
        Map<String, Object> p = new LinkedHashMap<>();
        if (status != null) p.put("status", status.name());
        if (canal != null) p.put("canal", canal.name());
        if (de != null) p.put("de", de.toString());       // 2026-01-31: o formato do <input type="date">
        if (ate != null) p.put("ate", ate.toString());
        if (idCliente != null) p.put("cliente", idCliente);
        return p;
    }
}
