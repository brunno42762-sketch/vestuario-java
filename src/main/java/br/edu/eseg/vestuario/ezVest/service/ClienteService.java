package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.exception.RecursoNaoEncontradoException;
import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Entrega;
import br.edu.eseg.vestuario.ezVest.repository.ClienteRepository;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteValidator clienteValidator;

    public ClienteService(ClienteRepository clienteRepository,
                          PedidoRepository pedidoRepository,
                          ClienteValidator clienteValidator) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteValidator = clienteValidator;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clienteRepository.listarComEntrega();
    }

    @Transactional(readOnly = true)
    public Cliente buscar(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente " + id + " não encontrado."));
    }

    @Transactional
    public Cliente criar(Cliente cliente) {
        cliente.setIdCliente(null); // o id é sempre gerado pelo banco
        normalizar(cliente);
        validarRegras(cliente);

        // Liga os dois lados da associação 1:1 (Entrega é o lado dono: tem a coluna id_cliente)
        Entrega entrega = cliente.getEntrega();
        entrega.setIdEntrega(null);
        cliente.definirEntrega(entrega);

        // cascade = ALL em Cliente.entrega: salvar o cliente também salva a entrega
        return clienteRepository.save(cliente);
    }

    /**
     * Na edição NÃO salvamos o objeto que veio do formulário. Ele não tem a lista de
     * pedidos, e como Cliente.pedidos tem orphanRemoval = true, salvá-lo apagaria
     * todos os pedidos do cliente. Por isso buscamos o cliente do banco e copiamos
     * só os campos do formulário para ele.
     */
    @Transactional
    public Cliente atualizar(Integer id, Cliente dados) {
        Cliente existente = buscar(id);
        normalizar(dados);
        validarRegras(dados);

        existente.setFaixaEtaria(dados.getFaixaEtaria());
        existente.setGenero(dados.getGenero());
        existente.setCidade(dados.getCidade());
        existente.setEstado(dados.getEstado());
        existente.setCanalPreferido(dados.getCanalPreferido());
        existente.setClienteFidelidade(dados.isClienteFidelidade());

        // Atualiza a entrega que já existe (ou cria, para clientes antigos sem entrega)
        Entrega entrega = existente.getEntrega();
        if (entrega == null) {
            entrega = new Entrega();
            existente.definirEntrega(entrega);
        }
        entrega.setCidadeEntrega(dados.getEntrega().getCidadeEntrega());
        entrega.setEstadoEntrega(dados.getEntrega().getEstadoEntrega());

        // "existente" está gerenciado pelo JPA dentro da transação: as mudanças vão
        // para o banco no fim do método. O save deixa isso explícito.
        return clienteRepository.save(existente);
    }

    @Transactional
    public void excluir(Integer id) {
        Cliente cliente = buscar(id);
        // Cliente ◆—— Pedido é composição (cascade ALL): excluir o cliente apagaria
        // os pedidos dele, que são histórico de vendas. Por isso bloqueamos.
        if (pedidoRepository.existsByClienteIdCliente(id)) {
            throw new RegraNegocioException("O cliente " + id
                    + " não pode ser excluído porque tem pedidos registrados.");
        }
        clienteRepository.delete(cliente); // a entrega vai junto (cascade)
    }

    private void validarRegras(Cliente cliente) {
        Errors erros = new BeanPropertyBindingResult(cliente, "cliente");
        clienteValidator.validate(cliente, erros);
        if (erros.hasErrors()) {
            throw new RegraNegocioException(erros.getAllErrors().get(0).getDefaultMessage());
        }
    }

    /** Tira espaços extras e deixa a UF em maiúsculas antes de validar e salvar. */
    private void normalizar(Cliente c) {
        c.setCidade(trim(c.getCidade()));
        c.setEstado(upper(c.getEstado()));
        if (c.getEntrega() != null) {
            c.getEntrega().setCidadeEntrega(trim(c.getEntrega().getCidadeEntrega()));
            c.getEntrega().setEstadoEntrega(upper(c.getEntrega().getEstadoEntrega()));
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String upper(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }
}
