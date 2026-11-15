package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
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

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Uma página de pedidos.
     *
     * @param status filtro opcional (null = todos os status)
     * @param ordem  mais recentes, ID crescente ou ID decrescente (null = mais recentes)
     * @param pagina número da página como aparece para o usuário, começando em 1
     */
    @Transactional(readOnly = true)
    public Page<Pedido> listar(Status status, OrdemPedidos ordem, int pagina) {
        int indice = Math.max(pagina, 1) - 1;
        Sort sort = (ordem == null ? OrdemPedidos.RECENTES : ordem).getSort();
        Pageable pageable = PageRequest.of(indice, PEDIDOS_POR_PAGINA, sort);

        return status == null
                ? pedidoRepository.listarComCliente(pageable)
                : pedidoRepository.listarComClientePorStatus(status, pageable);
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

    /** A regra de transição fica em Pedido.alterarStatus / Status.podeMudarPara. */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public Pedido alterarStatus(Integer id, Status novoStatus) {
        Pedido pedido = buscar(id);
        pedido.alterarStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }
}
