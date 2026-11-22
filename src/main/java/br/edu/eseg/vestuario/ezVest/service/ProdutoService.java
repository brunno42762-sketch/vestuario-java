package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Produto;
import br.edu.eseg.vestuario.ezVest.model.Status;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    /** Um produto em destaque na página inicial e a página do catálogo em que ele aparece. */
    public record Destaque(Produto produto, int pagina) {
    }

    /** Os mais vendidos com estoque, para o carrossel da página inicial. */
    @Transactional(readOnly = true)
    public List<Destaque> maisVendidos(int quantos) {
        List<Integer> ids = itemPedidoRepository.maisVendidos(Status.CANCELADO, PageRequest.of(0, quantos));
        Map<Integer, Produto> produtos = produtoRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Produto::getIdProduto, Function.identity()));
        // findAllById não garante a ordem: monta a lista na ordem do ranking
        return ids.stream()
                .map(produtos::get)
                .filter(Objects::nonNull)
                .map(p -> new Destaque(p, paginaNoCatalogo(p)))
                .toList();
    }

    private int paginaNoCatalogo(Produto p) {
        long antes = produtoRepository.contarAntesNoCatalogo(p.getCategoria(), p.getSubcategoria(), p.getIdProduto());
        return (int) (antes / PRODUTOS_POR_PAGINA) + 1;
    }

    /** Uma categoria da página inicial: nome, quantos produtos tem e as subcategorias. */
    public record ResumoCategoria(String nome, long produtos, List<String> subcategorias) {
    }

    /** As categorias que existem no banco (a seção "Categorias" deixa de ser texto fixo no HTML). */
    @Transactional(readOnly = true)
    public List<ResumoCategoria> categorias() {
        Map<String, List<Object[]>> porCategoria = new LinkedHashMap<>();
        for (Object[] linha : produtoRepository.contarPorSubcategoria()) {
            porCategoria.computeIfAbsent((String) linha[0], c -> new ArrayList<>()).add(linha);
        }
        return porCategoria.entrySet().stream()
                .map(e -> new ResumoCategoria(e.getKey(),
                        e.getValue().stream().mapToLong(l -> ((Number) l[2]).longValue()).sum(),
                        e.getValue().stream().map(l -> (String) l[1]).toList()))
                .toList();
    }

    public Page<Produto> catalogo(int pagina) {
        return catalogo(pagina, null);
    }

    /** Catálogo paginado; categoria null = todas. */
    public Page<Produto> catalogo(int pagina, String categoria) {
        int indice = Math.max(pagina, 1) - 1;
        Sort ordem = Sort.by("categoria", "subcategoria", "idProduto");
        PageRequest pageable = PageRequest.of(indice, PRODUTOS_POR_PAGINA, ordem);
        return categoria == null
                ? produtoRepository.findAll(pageable)
                : produtoRepository.findByCategoria(categoria, pageable);
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
