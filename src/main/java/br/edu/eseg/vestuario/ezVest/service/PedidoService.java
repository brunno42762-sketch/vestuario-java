package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Pedido;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    /** Quantos pedidos aparecem em cada página da tela de pedidos. */
    public static final int PEDIDOS_POR_PAGINA = 20;

    private final PedidoRepository pedidoRepository;
    private final EstoqueService estoqueService;

    public PedidoService(PedidoRepository pedidoRepository, EstoqueService estoqueService) {
        this.pedidoRepository = pedidoRepository;
        this.estoqueService = estoqueService;
    }

    /**
     * Uma página de pedidos.
     *
     * @param status filtro opcional (null = todos os status)
     * @param ordem  mais recentes, ID crescente ou ID decrescente (null = mais recentes)
     * @param pagina número da página como aparece para o usuário, começando em 1
     */
    @Transactional(readOnly = true)
    public Page<Pedido> listar(FiltroPedidos filtro, OrdemPedidos ordem, int pagina) {
        int indice = Math.max(pagina, 1) - 1;
        Sort sort = (ordem == null ? OrdemPedidos.RECENTES : ordem).getSort();
        Pageable pageable = PageRequest.of(indice, PEDIDOS_POR_PAGINA, sort);
        FiltroPedidos f = filtro == null ? FiltroPedidos.nenhum() : filtro;
        return pedidoRepository.filtrar(f.status(), f.canal(), f.de(), f.ate(), f.idCliente(), pageable);
    }

    // ---------------- Minha conta (perfil CLIENTE) ----------------

    /** Os pedidos do cliente logado, dos mais recentes para os mais antigos. */
    @Transactional(readOnly = true)
    public Page<Pedido> listarDoCliente(Integer idCliente, int pagina) {
        int indice = Math.max(pagina, 1) - 1;
        Pageable pageable = PageRequest.of(indice, PEDIDOS_POR_PAGINA, OrdemPedidos.RECENTES.getSort());
        return pedidoRepository.findByClienteIdCliente(idCliente, pageable);
    }

    /**
     * Um pedido do cliente logado, com os itens. Se o pedido existir mas for de OUTRO cliente,
     * a resposta é a mesma de um pedido inexistente (404): assim ninguém descobre, trocando o
     * número na URL, quais pedidos existem.
     */
    @Transactional(readOnly = true)
    public Pedido buscarDoCliente(Integer idPedido, Integer idCliente) {
        return pedidoRepository.buscarDoClienteComItens(idPedido, idCliente)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido " + idPedido + " não encontrado."));
    }

    // ---------------- Área administrativa ----------------

    @Transactional(readOnly = true)
    public Pedido buscar(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido " + id + " não encontrado."));
    }

    /**
     * A regra de transição fica em Pedido.alterarStatus / Status.podeMudarPara.
     * Ao cancelar, os itens voltam para o estoque.
     */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public Pedido alterarStatus(Integer id, Status novoStatus) {
        Pedido pedido = pedidoRepository.bloquear(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + id + " não encontrado."));
        mudarStatus(pedido, novoStatus);
        return pedido;
    }

    /**
     * Minha conta: o cliente cancela um pedido que ainda não foi entregue (status CRIADO).
     * Pedido de outra pessoa responde 404, igual a um pedido inexistente.
     */
    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public Pedido cancelarDoCliente(Integer idPedido, Integer idCliente) {
        Pedido pedido = pedidoRepository.bloquear(idPedido)
                .filter(p -> p.getCliente().getIdCliente().equals(idCliente))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + idPedido + " não encontrado."));
        if (pedido.getStatusPedido() != Status.CRIADO) {
            throw new RegraNegocioException("Só é possível cancelar um pedido que ainda não foi entregue.");
        }
        mudarStatus(pedido, Status.CANCELADO);
        return pedido;
    }

    private void mudarStatus(Pedido pedido, Status novoStatus) {
        pedido.alterarStatus(novoStatus); // lança RegraNegocioException se a transição não for permitida
        if (novoStatus == Status.CANCELADO) {
            estoqueService.devolver(pedido);
        }
        // O pedido está bloqueado e sendo acompanhado pelo JPA: as mudanças vão para o banco no commit.
    }
}
