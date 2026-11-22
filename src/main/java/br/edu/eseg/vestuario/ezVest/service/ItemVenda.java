package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Tamanho;

/**
 * Um item a vender: qual produto, em que tamanho e quantas peças.
 * O preço NÃO vem daqui: é sempre o do produto no banco, no momento da venda.
 */
public record ItemVenda(Integer idProduto, Tamanho tamanho, int quantidade) {
}
