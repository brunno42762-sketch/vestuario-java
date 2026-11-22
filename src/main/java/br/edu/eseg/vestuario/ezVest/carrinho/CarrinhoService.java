package br.edu.eseg.vestuario.ezVest.carrinho;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import br.edu.eseg.vestuario.ezVest.service.ItemVenda;
import br.edu.eseg.vestuario.ezVest.service.VendaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras do carrinho. A conferência de estoque daqui é um AVISO antecipado ao cliente;
 * a conferência que vale é a do VendaService, na hora de finalizar, com o estoque bloqueado.
 */
@Service
public class CarrinhoService {

    /** Uma linha do carrinho já com os dados do produto, para exibir. */
    public record Linha(String chave, Produto produto, Tamanho tamanho, int quantidade) {
        public double getSubtotal() {
            return produto.getPrecoUnitario() * quantidade;
        }

        /** Até quantas peças dá para escolher nesta linha (limite por item e estoque). */
        public int getMaximo() {
            return Math.max(1, Math.min(VendaService.MAX_QUANTIDADE, produto.getEstoqueAtual()));
        }
    }

    /** O carrinho pronto para a tela. */
    public record Resumo(List<Linha> linhas, double total, int pecas) {
        public boolean isVazio() {
            return linhas.isEmpty();
        }
    }

    private final Carrinho carrinho;
    private final ProdutoRepository produtoRepository;

    public CarrinhoService(Carrinho carrinho, ProdutoRepository produtoRepository) {
        this.carrinho = carrinho;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public void adicionar(Integer idProduto, Tamanho tamanho, int quantidade) {
        if (tamanho == null) {
            throw new RegraNegocioException("Escolha o tamanho.");
        }
        Produto produto = produtoRepository.findById(idProduto == null ? -1 : idProduto)
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
        var atual = carrinho.buscar(Carrinho.chave(idProduto, tamanho));
        if (atual == null && carrinho.getItens().size() >= VendaService.MAX_ITENS) {
            throw new RegraNegocioException("O carrinho pode ter no máximo " + VendaService.MAX_ITENS + " itens.");
        }
        int novaQuantidade = quantidade + (atual == null ? 0 : atual.quantidade());
        validarQuantidade(produto, novaQuantidade,
                carrinho.quantidadeDoProduto(idProduto) - (atual == null ? 0 : atual.quantidade()));
        carrinho.adicionar(idProduto, tamanho, quantidade);
    }

    @Transactional(readOnly = true)
    public void alterarQuantidade(String chave, int quantidade) {
        var item = carrinho.buscar(chave);
        if (item == null) {
            throw new RegraNegocioException("Este item não está mais no carrinho.");
        }
        Produto produto = produtoRepository.findById(item.idProduto())
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));
        validarQuantidade(produto, quantidade, carrinho.quantidadeDoProduto(item.idProduto()) - item.quantidade());
        carrinho.alterarQuantidade(chave, quantidade);
    }

    public void remover(String chave) {
        carrinho.remover(chave);
    }

    /** Os itens com os dados atuais dos produtos. Produto que deixou de existir sai do carrinho. */
    @Transactional(readOnly = true)
    public Resumo resumo() {
        List<Carrinho.Item> itens = carrinho.getItens();
        Map<Integer, Produto> produtos = produtoRepository
                .findAllById(itens.stream().map(Carrinho.Item::idProduto).distinct().toList())
                .stream().collect(Collectors.toMap(Produto::getIdProduto, Function.identity()));

        List<Linha> linhas = new ArrayList<>();
        for (Carrinho.Item i : itens) {
            Produto produto = produtos.get(i.idProduto());
            if (produto == null) {
                carrinho.remover(i.getChave());
                continue;
            }
            linhas.add(new Linha(i.getChave(), produto, i.tamanho(), i.quantidade()));
        }
        double total = Math.round(linhas.stream().mapToDouble(Linha::getSubtotal).sum() * 100.0) / 100.0;
        int pecas = linhas.stream().mapToInt(Linha::quantidade).sum();
        return new Resumo(linhas, total, pecas);
    }

    /** Os itens no formato que o VendaService recebe. */
    public List<ItemVenda> itensParaVenda() {
        return carrinho.getItens().stream()
                .map(i -> new ItemVenda(i.idProduto(), i.tamanho(), i.quantidade()))
                .toList();
    }

    public void esvaziar() {
        carrinho.limpar();
    }

    /**
     * @param quantidade      quantidade que a linha vai ter
     * @param outrasDoProduto peças do mesmo produto em OUTROS tamanhos já no carrinho
     */
    private void validarQuantidade(Produto produto, int quantidade, int outrasDoProduto) {
        if (quantidade < 1 || quantidade > VendaService.MAX_QUANTIDADE) {
            throw new RegraNegocioException(
                    "A quantidade deve ser de 1 a " + VendaService.MAX_QUANTIDADE + " peças.");
        }
        int disponivel = produto.getEstoqueAtual() - outrasDoProduto;
        if (quantidade > disponivel) {
            throw new RegraNegocioException(disponivel <= 0
                    ? "Não há mais peças deste produto em estoque."
                    : "Só há " + disponivel + " peça(s) deste produto em estoque.");
        }
    }
}
