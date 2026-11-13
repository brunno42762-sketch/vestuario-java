package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // "left join fetch": traz a entrega junto, inclusive de clientes que ainda não têm entrega
    @Query("select c from Cliente c left join fetch c.entrega order by c.idCliente")
    List<Cliente> listarComEntrega();
}
