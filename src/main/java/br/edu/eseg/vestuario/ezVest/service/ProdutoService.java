package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.repository.ItemPedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;

@Service
public class ProdutoService {

    /** Quantos produtos aparecem em cada página do catálogo da loja. */
    public static final int PRODUTOS_POR_PAGINA = 12;

    private final ProdutoRepository produtoRepository;
    private final ItemPedidoRepository itemPedidoRepository;
    private final ProdutoValidator produtoValidator;

    public ProdutoService(ProdutoRepository produtoRepository,
                          ItemPedidoRepository itemPedidoRepository,
                          ProdutoValidator produtoValidator) {
        this.produtoRepository = produtoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.produtoValidator = produtoValidator;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return produtoRepository.findAllByOrderByIdProdutoAsc();
    }

    /**
     * Uma página do catálogo da loja (página inicial).
     *
     * @param pagina número da página como aparece para o usuário, começando em 1
     *               (o Spring Data conta a partir de 0, por isso o "- 1")
     */
    @Transactional(readOnly = true)
    public Page<Produto> catalogo(int pagina) {
        int indice = Math.max(pagina, 1) - 1;
        Sort ordem = Sort.by("categoria", "subcategoria", "idProduto");
        return produtoRepository.findAll(PageRequest.of(indice, PRODUTOS_POR_PAGINA, ordem));
    }

    @Transactional(readOnly = true)
    public Produto buscar(Integer id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto " + id + " não encontrado."));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // 2ª camada: mesmo fora da rota, só o ADMIN cria
    public Produto criar(Produto produto) {
        produto.setIdProduto(null); // o id é sempre gerado pelo banco
        limparTextos(produto);
        validarRegras(produto);
        return produtoRepository.save(produto);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Produto atualizar(Integer id, Produto dados) {
        buscar(id); // garante que existe (senão lança 404)
        dados.setIdProduto(id);
        limparTextos(dados);
        validarRegras(dados);
        return produtoRepository.save(dados);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(Integer id) {
        Produto produto = buscar(id);
        if (itemPedidoRepository.existsByProdutoIdProduto(id)) {
            throw new RegraNegocioException("O produto " + id
                    + " não pode ser excluído porque já aparece em pedidos.");
        }
        produtoRepository.delete(produto);
    }

    /**
     * Valida de novo antes de gravar: mesmo que alguém chame o service sem passar
     * pelo formulário, um produto inválido nunca chega ao banco.
     */
    private void validarRegras(Produto produto) {
        Errors erros = new BeanPropertyBindingResult(produto, "produto");
        produtoValidator.validate(produto, erros);
        if (erros.hasErrors()) {
            String mensagem = erros.getAllErrors().get(0).getDefaultMessage();
            throw new RegraNegocioException(mensagem);
        }
    }

    /** Tira espaços extras do começo e do fim dos textos antes de salvar. */
    private void limparTextos(Produto p) {
        p.setCategoria(trim(p.getCategoria()));
        p.setSubcategoria(trim(p.getSubcategoria()));
        p.setCor(trim(p.getCor()));
        p.setMarca(trim(p.getMarca()));
        p.setColecao(trim(p.getColecao()));
        p.setFornecedor(trim(p.getFornecedor()));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
