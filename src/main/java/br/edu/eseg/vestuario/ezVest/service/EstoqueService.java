package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.ItemPedido;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Entrada e saída de estoque. O estoque é do produto (não do tamanho), como na planilha legada.
 *
 * MANDATORY: estes métodos só rodam dentro de uma transação já aberta (a da venda ou a do
 * cancelamento). Assim a baixa do estoque e a gravação do pedido acontecem juntas: ou as duas
 * dão certo, ou nenhuma fica gravada.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class EstoqueService {

    private final ProdutoRepository produtoRepository;

    public EstoqueService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    /**
     * Venda: confere e baixa o estoque.
     *
     * @param quantidades id do produto -> quantidade vendida
     * @return os produtos (já bloqueados e atualizados), pelo id
     */
    public Map<Integer, Produto> baixar(Map<Integer, Integer> quantidades) {
        Map<Integer, Produto> produtos = bloquear(quantidades);

        // Confere tudo antes de mexer em qualquer produto
        for (var q : quantidades.entrySet()) {
            Produto produto = produtos.get(q.getKey());
            if (produto == null) {
                throw new RegraNegocioException("O produto " + q.getKey() + " não existe mais.");
            }
            if (produto.getEstoqueAtual() < q.getValue()) {
                throw new RegraNegocioException(String.format(
                        "Estoque insuficiente para %s %s – %s: pedido %d, disponível %d.",
                        produto.getCategoria(), produto.getSubcategoria(), produto.getCor(),
                        q.getValue(), produto.getEstoqueAtual()));
            }
        }
        quantidades.forEach((id, quantidade) -> {
            Produto produto = produtos.get(id);
            produto.setEstoqueAtual(produto.getEstoqueAtual() - quantidade);
        });
        return produtos;
    }

    /** Cancelamento: os itens do pedido voltam para o estoque. */
    public void devolver(Pedido pedido) {
        Map<Integer, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedido item : pedido.getItensPedido()) {
            quantidades.merge(item.getProduto().getIdProduto(), item.getQuantidade(), Integer::sum);
        }
        Map<Integer, Produto> produtos = bloquear(quantidades);
        quantidades.forEach((id, quantidade) -> {
            Produto produto = produtos.get(id);
            produto.setEstoqueAtual(produto.getEstoqueAtual() + quantidade);
        });
        // Não precisa de save: os produtos estão sendo acompanhados pelo JPA dentro da transação,
        // e a alteração é gravada no commit.
    }

    private Map<Integer, Produto> bloquear(Map<Integer, Integer> quantidades) {
        return produtoRepository.bloquearPorIds(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Produto::getIdProduto, Function.identity()));
    }
}
