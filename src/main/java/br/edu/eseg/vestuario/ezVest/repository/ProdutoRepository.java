package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Integer> {

    List<Produto> findAllByOrderByIdProdutoAsc();

    /** Produtos que chegaram ao estoque mínimo, os mais críticos primeiro. */
    @Query("select p from Produto p where p.estoqueAtual <= p.estoqueMinimo"
            + " order by (p.estoqueAtual - p.estoqueMinimo), p.idProduto")
    List<Produto> listarAbaixoDoEstoqueMinimo();

    /** Catálogo filtrado por categoria (consulta derivada do nome do método). */
    Page<Produto> findByCategoria(String categoria, Pageable pagina);

    /** Seção "Categorias" da página inicial. Cada linha: [categoria, subcategoria, quantidade de produtos]. */
    @Query("select p.categoria, p.subcategoria, count(p) from Produto p"
            + " group by p.categoria, p.subcategoria order by p.categoria, p.subcategoria")
    List<Object[]> contarPorSubcategoria();

    /**
     * Quantos produtos vêm ANTES deste no catálogo (mesma ordem do catálogo: categoria,
     * subcategoria e id). Serve para descobrir em que página do catálogo ele aparece.
     */
    @Query("select count(p) from Produto p where p.categoria < :categoria"
            + " or (p.categoria = :categoria and p.subcategoria < :subcategoria)"
            + " or (p.categoria = :categoria and p.subcategoria = :subcategoria and p.idProduto < :id)")
    long contarAntesNoCatalogo(@Param("categoria") String categoria,
                               @Param("subcategoria") String subcategoria,
                               @Param("id") Integer id);

    /**
     * Bloqueia as linhas dos produtos até o fim da transação ("select ... for update").
     * Usado ao vender e ao cancelar: se duas compras chegarem juntas, a segunda espera
     * a primeira terminar e já enxerga o estoque atualizado (ninguém vende a mesma peça duas vezes).
     * A ordem por id evita que duas transações se bloqueiem uma à outra (deadlock).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.idProduto in :ids order by p.idProduto")
    List<Produto> bloquearPorIds(@Param("ids") Collection<Integer> ids);
}
