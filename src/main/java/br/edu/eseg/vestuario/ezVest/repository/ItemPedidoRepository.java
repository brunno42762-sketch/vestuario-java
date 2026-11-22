package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.ItemPedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    /** Usado para impedir a exclusão de um produto que já foi vendido. */
    boolean existsByProdutoIdProduto(Integer idProduto);

    /**
     * Ids dos produtos mais vendidos (soma das peças, sem contar pedidos cancelados),
     * só entre os que ainda têm estoque. O Pageable limita quantos vêm (ex.: os 6 primeiros).
     */
    @Query("select i.produto.idProduto from ItemPedido i"
            + " where i.pedido.statusPedido <> :cancelado and i.produto.estoqueAtual > 0"
            + " group by i.produto.idProduto"
            + " order by sum(i.quantidade) desc, i.produto.idProduto")
    List<Integer> maisVendidos(@Param("cancelado") Status cancelado, Pageable limite);

    /**
     * Relatório por categoria (RF09). A categoria é do produto, então a conta é feita por item:
     *   receita do item = preço x quantidade x (1 - desconto)   (a mesma conta do valor total do pedido)
     *   lucro do item   = lucro do pedido x (receita do item / valor total do pedido)
     * O lucro é guardado por pedido; num pedido com itens de categorias diferentes, ele é dividido
     * na proporção do valor de cada item.
     * Cada linha: [categoria, peças, receita, lucro].
     */
    @Query("select i.produto.categoria, sum(i.quantidade),"
            + " sum(i.precoUnitario * i.quantidade * (1 - i.descontoPercentual)),"
            + " sum(p.lucroEstimado * (i.precoUnitario * i.quantidade * (1 - i.descontoPercentual)) / p.valorTotal)"
            + " from ItemPedido i join i.pedido p"
            + " where p.statusPedido <> :cancelado and p.valorTotal > 0"
            + " and (cast(:de as LocalDate) is null or p.dataPedido >= :de)"
            + " and (cast(:ate as LocalDate) is null or p.dataPedido <= :ate)"
            + " group by i.produto.categoria order by 3 desc")
    List<Object[]> receitaPorCategoria(@Param("cancelado") Status cancelado,
                                       @Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
