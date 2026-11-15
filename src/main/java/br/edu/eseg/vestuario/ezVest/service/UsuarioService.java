package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Perfil;
import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.ClienteRepository;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Cadastro de usuários (só ADMIN). Regras:
 *  - username obrigatório, até 80 caracteres e único;
 *  - senha com pelo menos 8 caracteres (obrigatória no cadastro; na edição, em branco = manter);
 *  - o sistema sempre mantém pelo menos um ADMIN ativo;
 *  - o ADMIN não pode trocar o próprio username, perder o próprio acesso nem se excluir;
 *  - perfil CLIENTE exige um cliente existente, que não esteja ligado a outro usuário.
 * Erros de regra viram IllegalArgumentException, tratada pelo controller.
 */
@Service
@PreAuthorize("hasRole('ADMIN')") // 2ª camada: vale para todos os métodos
public class UsuarioService {

    private final UsuarioRepository repository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder encoder;

    @PersistenceContext
    private EntityManager entityManager;

    public UsuarioService(UsuarioRepository repository, ClienteRepository clienteRepository,
                          PasswordEncoder encoder) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repository.listarComCliente();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional
    public void salvar(UUID id, String username, String novaSenha, Perfil perfil, boolean ativo,
                       Integer idCliente, UUID administradorId) {
        if (username == null || username.isBlank() || username.trim().length() > 80)
            throw new IllegalArgumentException("Informe um username válido de até 80 caracteres.");
        if (perfil == null)
            throw new IllegalArgumentException("Selecione um perfil.");
        username = username.trim();
        boolean novo = id == null;
        boolean mudarSenha = novaSenha != null && !novaSenha.isEmpty();
        if (novo && !mudarSenha)
            throw new IllegalArgumentException("Informe a senha do usuário.");
        if ((novo || mudarSenha) && novaSenha.length() < 8)
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");

        Usuario usuario = novo ? new Usuario() : buscar(id);
        if (!novo && usuario.getId().equals(administradorId)) {
            if (!usuario.getUsername().equals(username))
                throw new IllegalArgumentException("Não é permitido alterar o próprio username durante a sessão.");
            if (perfil != Perfil.ADMIN || !ativo)
                throw new IllegalArgumentException("Não é permitido remover o próprio acesso administrativo.");
        }
        if (repository.findByUsername(username)
                .filter(outro -> novo || !outro.getId().equals(usuario.getId())).isPresent())
            throw new IllegalArgumentException("Nome de usuário já cadastrado.");

        Cliente cliente = clienteDoPerfil(perfil, idCliente, usuario);

        // Bloqueio de linhas em operações que podem remover o último ADMIN ativo
        if (!novo && usuario.getPerfil() == Perfil.ADMIN && usuario.isAtivo()
                && (perfil != Perfil.ADMIN || !ativo)) {
            List<Usuario> admins = repository.bloquearAdministradoresAtivos(Perfil.ADMIN);
            entityManager.refresh(usuario); // estado atualizado depois de esperar o bloqueio
            if (usuario.getPerfil() == Perfil.ADMIN && usuario.isAtivo() && admins.size() <= 1)
                throw new IllegalArgumentException("O sistema deve manter pelo menos um ADMIN ativo.");
        }

        usuario.setUsername(username);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        usuario.setCliente(cliente);
        if (novo || mudarSenha)
            usuario.setSenha(encoder.encode(novaSenha));
        repository.saveAndFlush(usuario);
    }

    @Transactional
    public void excluir(UUID id, UUID administradorId) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(administradorId))
            throw new IllegalArgumentException("Não é permitido excluir a própria conta.");
        if (usuario.getPerfil() == Perfil.ADMIN && usuario.isAtivo()) {
            List<Usuario> admins = repository.bloquearAdministradoresAtivos(Perfil.ADMIN);
            entityManager.refresh(usuario);
            if (usuario.getPerfil() == Perfil.ADMIN && usuario.isAtivo() && admins.size() <= 1)
                throw new IllegalArgumentException("Não é permitido excluir o último ADMIN ativo.");
        }
        repository.delete(usuario);
        repository.flush();
    }

    /** Perfil CLIENTE: exige um cliente existente e livre. Outros perfis: sem cliente. */
    private Cliente clienteDoPerfil(Perfil perfil, Integer idCliente, Usuario usuario) {
        if (perfil != Perfil.CLIENTE) {
            return null;
        }
        if (idCliente == null)
            throw new IllegalArgumentException("Para o perfil Cliente, informe o ID do cliente vinculado.");
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalArgumentException("O cliente " + idCliente + " não existe."));
        repository.findByClienteIdCliente(idCliente)
                .filter(outro -> usuario.getId() == null || !outro.getId().equals(usuario.getId()))
                .ifPresent(outro -> {
                    throw new IllegalArgumentException("O cliente " + idCliente
                            + " já está vinculado ao usuário " + outro.getUsername() + ".");
                });
        return cliente;
    }
}
