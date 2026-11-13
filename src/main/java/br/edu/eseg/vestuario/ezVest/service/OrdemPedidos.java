package br.edu.eseg.vestuario.ezVest.service;

import org.springframework.data.domain.Sort;

/**
 * Ordens possíveis da lista de pedidos. Chega pela URL (?ordem=ID_CRESCENTE);
 * um valor fora desta lista vira erro 400 no TratadorDeErros.
 */
public enum OrdemPedidos {

    RECENTES("Mais recentes", Sort.by(Sort.Order.desc("dataPedido"), Sort.Order.desc("idPedido"))),
    ID_CRESCENTE("ID crescente", Sort.by(Sort.Order.asc("idPedido"))),
    ID_DECRESCENTE("ID decrescente", Sort.by(Sort.Order.desc("idPedido")));

    private final String nome;
    private final Sort sort;

    OrdemPedidos(String nome, Sort sort) {
        this.nome = nome;
        this.sort = sort;
    }

    public String getNome() {
        return nome;
    }

    public Sort getSort() {
        return sort;
    }
}
