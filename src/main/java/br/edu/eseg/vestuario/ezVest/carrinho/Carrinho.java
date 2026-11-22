package br.edu.eseg.vestuario.ezVest.carrinho;

import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O carrinho de compras, guardado na SESSÃO do navegador (@SessionScope): cada visitante
 * tem o seu, e ele dura até a compra, o logout ou o fim da sessão.
 *
 * Não é uma tabela do banco de propósito: no UML não existe "Carrinho". Ele só vira
 * Pedido + ItemPedido na hora de finalizar a compra (VendaService).
 *
 * Guarda só o id do produto, o tamanho e a quantidade. O preço e o nome são lidos do banco
 * sempre que o carrinho é exibido, para nunca mostrar um preço desatualizado.
 *
 * Serializable: o servidor pode guardar a sessão em disco ao reiniciar.
 */
@Component
@SessionScope
public class Carrinho implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Uma linha do carrinho. */
    public record Item(Integer idProduto, Tamanho tamanho, int quantidade) implements Serializable {

        /** Identifica a linha: o mesmo produto em dois tamanhos são duas linhas. Ex.: "12-M". */
        public String getChave() {
            return chave(idProduto, tamanho);
        }
    }

    // LinkedHashMap: mantém a ordem em que os itens foram adicionados
    private final Map<String, Item> itens = new LinkedHashMap<>();

    public static String chave(Integer idProduto, Tamanho tamanho) {
        return idProduto + "-" + tamanho;
    }

    /** Adiciona peças. Se o produto já estiver no carrinho no mesmo tamanho, soma. */
    public synchronized void adicionar(Integer idProduto, Tamanho tamanho, int quantidade) {
        itens.merge(chave(idProduto, tamanho), new Item(idProduto, tamanho, quantidade),
                (atual, novo) -> new Item(idProduto, tamanho, atual.quantidade() + quantidade));
    }

    public synchronized void alterarQuantidade(String chave, int quantidade) {
        Item atual = itens.get(chave);
        if (atual != null) {
            itens.put(chave, new Item(atual.idProduto(), atual.tamanho(), quantidade));
        }
    }

    public synchronized void remover(String chave) {
        itens.remove(chave);
    }

    public synchronized void limpar() {
        itens.clear();
    }

    /** Cópia dos itens (quem chama não consegue mexer no carrinho por fora). */
    public synchronized List<Item> getItens() {
        return new ArrayList<>(itens.values());
    }

    public synchronized Item buscar(String chave) {
        return itens.get(chave);
    }

    /** Total de peças: o número que aparece na barra, ao lado de "Carrinho". */
    public synchronized int getQuantidadeTotal() {
        return itens.values().stream().mapToInt(Item::quantidade).sum();
    }

    /** Peças de um produto somando todos os tamanhos (o estoque é do produto). */
    public synchronized int quantidadeDoProduto(Integer idProduto) {
        return itens.values().stream()
                .filter(i -> i.idProduto().equals(idProduto))
                .mapToInt(Item::quantidade).sum();
    }

    public synchronized boolean isVazio() {
        return itens.isEmpty();
    }
}
