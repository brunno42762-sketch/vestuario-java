package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Integer> {

    /** Usado para impedir a exclusão de um produto que já foi vendido. */
    boolean existsByProdutoIdProduto(Integer idProduto);
}
