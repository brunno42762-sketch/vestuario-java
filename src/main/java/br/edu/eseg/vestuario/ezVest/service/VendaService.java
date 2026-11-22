package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.*;
import br.edu.eseg.vestuario.ezVest.repository.ClienteRepository;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra vendas: cria o Pedido com os ItemPedido e baixa o estoque, tudo numa transação.
 *
 * Dois caminhos, com permissões diferentes (@PreAuthorize):
 *   - comprar:          o CLIENTE, pelo carrinho da loja (canal E-commerce);
 *   - registrarNaLoja:  o VENDEDOR ou o ADMIN, para qualquer cliente e canal.
 * Os dois usam o mesmo método privado registrar(...), com as mesmas regras.
 */
@Service
public class VendaService {

    /** Limites de um pedido, para evitar erros de digitação e pedidos absurdos. */
    public static final int MAX_ITENS = 10;
    public static final int MAX_QUANTIDADE = 10;

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final EstoqueService estoqueService;

    public VendaService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository,
                        EstoqueService estoqueService) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.estoqueService = estoqueService;
    }

    /** Compra feita pelo próprio cliente na loja online. O idCliente vem do usuário logado. */
    @Transactional
    @PreAuthorize("hasRole('CLIENTE')")
    public Pedido comprar(Integer idCliente, List<ItemVenda> itens, FormaPagamento formaPagamento) {
        return registrar(idCliente, Canal.ECOMMERCE, formaPagamento, itens);
    }

    /** Venda registrada pela equipe (loja física, marketplace...). */
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public Pedido registrarNaLoja(Integer idCliente, Canal canal, FormaPagamento formaPagamento,
                                  List<ItemVenda> itens) {
        return registrar(idCliente, canal, formaPagamento, itens);
    }

    private Pedido registrar(Integer idCliente, Canal canal, FormaPagamento formaPagamento,
                             List<ItemVenda> itens) {
        if (canal == null) {
            throw new RegraNegocioException("Selecione o canal de venda.");
        }
        if (formaPagamento == null) {
            throw new RegraNegocioException("Selecione a forma de pagamento.");
        }
        Cliente cliente = clienteRepository.findById(idCliente == null ? -1 : idCliente)
                .orElseThrow(() -> new RegraNegocioException("Selecione um cliente cadastrado."));

        Map<String, ItemVenda> porLinha = juntarRepetidos(itens);

        // Quantas peças de cada produto (dois tamanhos do mesmo produto saem do mesmo estoque)
        Map<Integer, Integer> quantidades = new LinkedHashMap<>();
        porLinha.values().forEach(i -> quantidades.merge(i.idProduto(), i.quantidade(), Integer::sum));

        // Confere e baixa o estoque, com as linhas dos produtos bloqueadas
        Map<Integer, Produto> produtos = estoqueService.baixar(quantidades);

        Pedido pedido = new Pedido();
        pedido.setDataPedido(LocalDate.now());
        pedido.setCanalVenda(canal);
        pedido.setFormaPagamento(formaPagamento);
        pedido.setStatusPedido(Status.CRIADO);
        pedido.setCliente(cliente);

        // Mesmas contas da planilha legada:
        //   valor total = preço x quantidade x (1 - desconto)
        //   lucro       = valor total - custo x quantidade
        double valorTotal = 0;
        double lucro = 0;
        for (ItemVenda i : porLinha.values()) {
            Produto produto = produtos.get(i.idProduto());
            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setTamanho(i.tamanho());
            item.setQuantidade(i.quantidade());
            item.setPrecoUnitario(produto.getPrecoUnitario()); // preço do momento da venda
            item.setDescontoPercentual(0);
            pedido.adicionarItem(item);

            double valorItem = produto.getPrecoUnitario() * i.quantidade();
            valorTotal += valorItem;
            lucro += valorItem - produto.getCustoUnitario() * i.quantidade();
        }
        pedido.setValorTotal(arredondar(valorTotal));
        pedido.setLucroEstimado(arredondar(lucro));

        // cascade = ALL em Pedido.itensPedido: salvar o pedido salva os itens
        return pedidoRepository.save(pedido);
    }

    /** Valida os itens e junta linhas repetidas (mesmo produto e mesmo tamanho). */
    private Map<String, ItemVenda> juntarRepetidos(List<ItemVenda> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("Inclua pelo menos um produto.");
        }
        Map<String, ItemVenda> porLinha = new LinkedHashMap<>();
        for (ItemVenda i : itens) {
            if (i.idProduto() == null || i.tamanho() == null) {
                throw new RegraNegocioException("Cada item precisa de produto e tamanho.");
            }
            if (i.quantidade() < 1 || i.quantidade() > MAX_QUANTIDADE) {
                throw new RegraNegocioException(
                        "A quantidade de cada item deve ser de 1 a " + MAX_QUANTIDADE + ".");
            }
            porLinha.merge(i.idProduto() + "-" + i.tamanho(), i,
                    (a, b) -> new ItemVenda(a.idProduto(), a.tamanho(), a.quantidade() + b.quantidade()));
        }
        if (porLinha.size() > MAX_ITENS) {
            throw new RegraNegocioException("Um pedido pode ter no máximo " + MAX_ITENS + " itens.");
        }
        for (ItemVenda i : porLinha.values()) {
            if (i.quantidade() > MAX_QUANTIDADE) {
                throw new RegraNegocioException("A quantidade de cada item deve ser de 1 a "
                        + MAX_QUANTIDADE + " (o mesmo produto e tamanho foi informado mais de uma vez).");
            }
        }
        return porLinha;
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
