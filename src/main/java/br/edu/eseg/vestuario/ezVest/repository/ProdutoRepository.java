package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    List<Produto> findAllByOrderByIdProdutoAsc();

    /** Produtos que chegaram ao estoque mínimo, os mais críticos primeiro. */
    @Query("select p from Produto p where p.estoqueAtual <= p.estoqueMinimo"
            + " order by (p.estoqueAtual - p.estoqueMinimo), p.idProduto")
    List<Produto> listarAbaixoDoEstoqueMinimo();
}
