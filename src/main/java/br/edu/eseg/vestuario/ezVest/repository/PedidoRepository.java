package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /*
     * Listas paginadas para a tela de pedidos.
     * - "join fetch" traz o cliente na mesma consulta (sem uma consulta extra por pedido).
     * - "countQuery" é a consulta do total, usada para calcular o número de páginas.
     *   Ela é separada porque não dá para contar com "join fetch".
     * - A ordem (mais recentes primeiro) vem do Pageable, montado no PedidoService.
     */
    @Query(value = "select p from Pedido p join fetch p.cliente " + FILTRO,
           countQuery = "select count(p) from Pedido p " + FILTRO)
    Page<Pedido> filtrar(@Param("status") Status status,
                         @Param("canal") Canal canal,
                         @Param("de") LocalDate de,
                         @Param("ate") LocalDate ate,
                         @Param("idCliente") Integer idCliente,
                         Pageable pagina);

    /**
     * Filtros opcionais (RF08): cada parâmetro null desliga a condição dele
     * ("(:status is null or ...)"). Uma consulta só atende a qualquer combinação.
     */
    String FILTRO = "where (:status is null or p.statusPedido = :status)"
            + " and (:canal is null or p.canalVenda = :canal)"
            + " and (cast(:de as LocalDate) is null or p.dataPedido >= :de)"
            + " and (cast(:ate as LocalDate) is null or p.dataPedido <= :ate)"
            + " and (:idCliente is null or p.cliente.idCliente = :idCliente)";

    // ---------- Relatório de receita e lucro (RF09): sem pedidos cancelados, período opcional ----------

    String PERIODO = " and (cast(:de as LocalDate) is null or p.dataPedido >= :de)"
            + " and (cast(:ate as LocalDate) is null or p.dataPedido <= :ate)";

    /** Cada linha: [ano, mês, pedidos, receita, lucro]. */
    @Query("select year(p.dataPedido), month(p.dataPedido), count(p), sum(p.valorTotal), sum(p.lucroEstimado)"
            + " from Pedido p where p.statusPedido <> :cancelado" + PERIODO
            + " group by year(p.dataPedido), month(p.dataPedido)"
            + " order by year(p.dataPedido), month(p.dataPedido)")
    List<Object[]> receitaPorMes(@Param("cancelado") Status cancelado,
                                 @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Cada linha: [Canal, pedidos, receita, lucro]. */
    @Query("select p.canalVenda, count(p), sum(p.valorTotal), sum(p.lucroEstimado)"
            + " from Pedido p where p.statusPedido <> :cancelado" + PERIODO
            + " group by p.canalVenda order by sum(p.valorTotal) desc")
    List<Object[]> receitaPorCanal(@Param("cancelado") Status cancelado,
                                   @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    /** Quantidade de pedidos em cada status: cada linha é [Status, Long]. */
    @Query("select p.statusPedido, count(p) from Pedido p group by p.statusPedido")
    List<Object[]> contarPorStatus();

    /** Minha conta: os pedidos de um cliente (consulta derivada do nome do método). */
    Page<Pedido> findByClienteIdCliente(Integer idCliente, Pageable pagina);

    /**
     * Minha conta: um pedido com os itens e os produtos, SÓ se for do cliente informado.
     * Se o pedido for de outra pessoa, volta vazio, como se não existisse.
     */
    @Query("select p from Pedido p left join fetch p.itensPedido i left join fetch i.produto"
            + " where p.idPedido = :idPedido and p.cliente.idCliente = :idCliente")
    Optional<Pedido> buscarDoClienteComItens(@Param("idPedido") Integer idPedido,
                                             @Param("idCliente") Integer idCliente);

    /**
     * Bloqueia o pedido até o fim da transação. Usado ao mudar o status: se duas pessoas
     * cancelarem o mesmo pedido ao mesmo tempo, a segunda espera e já vê "Cancelado",
     * e o estoque não é devolvido duas vezes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.idPedido = :id")
    Optional<Pedido> bloquear(@Param("id") Integer id);

    /** Usado para impedir a exclusão de um cliente que tem pedidos. */
    boolean existsByClienteIdCliente(Integer idCliente);
}
