package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.repository.ClienteRepository;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Reúne os números mostrados no painel do administrador (/admin). */
@Service
public class PainelService {

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public PainelService(ProdutoRepository produtoRepository,
                         ClienteRepository clienteRepository,
                         PedidoRepository pedidoRepository) {
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public long totalProdutos() {
        return produtoRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalClientes() {
        return clienteRepository.count();
    }

    @Transactional(readOnly = true)
    public long totalPedidos() {
        return pedidoRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Produto> produtosParaRepor() {
        return produtoRepository.listarAbaixoDoEstoqueMinimo();
    }

    /**
     * Pedidos por status, com TODOS os status (inclusive os que têm 0 pedidos),
     * na ordem em que aparecem no enum.
     */
    @Transactional(readOnly = true)
    public Map<Status, Long> pedidosPorStatus() {
        Map<Status, Long> contagem = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            contagem.put(s, 0L);
        }
        for (Object[] linha : pedidoRepository.contarPorStatus()) {
            contagem.put((Status) linha[0], (Long) linha[1]);
        }
        return contagem;
    }
}
