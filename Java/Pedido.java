import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class Pedido {

	private int idPedido;

	private LocalDate dataPedido;

	private double valorTotal;

	private double lucroEstimado;

	private Canal canalVenda;

	private FormaPagamento formaPagamento;

	private List<ItemPedido> itensPedido;

	private Status statusPedido;

	private Collection<Produto> _ItemPedido;

	private Cliente cliente;

	private Entrega entrega;

	private Canal canal;

	private Status status;

}
