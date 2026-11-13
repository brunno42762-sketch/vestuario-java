package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /*
     * Listas paginadas para a tela de pedidos.
     * - "join fetch" traz o cliente na mesma consulta (sem uma consulta extra por pedido).
     * - "countQuery" é a consulta do total, usada para calcular o número de páginas.
     *   Ela é separada porque não dá para contar com "join fetch".
     * - A ordem (mais recentes primeiro) vem do Pageable, montado no PedidoService.
     */
    @Query(value = "select p from Pedido p join fetch p.cliente",
           countQuery = "select count(p) from Pedido p")
    Page<Pedido> listarComCliente(Pageable pagina);

    @Query(value = "select p from Pedido p join fetch p.cliente where p.statusPedido = :status",
           countQuery = "select count(p) from Pedido p where p.statusPedido = :status")
    Page<Pedido> listarComClientePorStatus(@Param("status") Status status, Pageable pagina);

    /** Quantidade de pedidos em cada status: cada linha é [Status, Long]. */
    @Query("select p.statusPedido, count(p) from Pedido p group by p.statusPedido")
    List<Object[]> contarPorStatus();

    /** Usado para impedir a exclusão de um cliente que tem pedidos. */
    boolean existsByClienteIdCliente(Integer idCliente);
}
