package br.edu.eseg.vestuario.ezVest.repository;

import br.edu.eseg.vestuario.ezVest.model.Perfil;
import br.edu.eseg.vestuario.ezVest.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByUsername(String username);

    /** Lista da tela de usuários, com o cliente vinculado (quando houver). */
    @Query("select u from Usuario u left join fetch u.cliente order by u.perfil, u.username")
    List<Usuario> listarComCliente();

    /** Impede excluir um cliente que tem um usuário de acesso ligado a ele. */
    boolean existsByClienteIdCliente(Integer idCliente);

    /** Um cliente só pode estar ligado a um usuário. */
    Optional<Usuario> findByClienteIdCliente(Integer idCliente);

    /** Bloqueia as linhas de ADMIN ativos durante operações que podem remover o último ADMIN. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.perfil = :perfil and u.ativo = true order by u.id")
    List<Usuario> bloquearAdministradoresAtivos(@Param("perfil") Perfil perfil);
}
