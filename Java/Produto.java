import java.util.Collection;

public class Produto {

	private int idProduto;

	private String categoria;

	private String subcategoria;

	private String cor;

	private String marca;

	private double precoUnitario;

	private double custoUnitario;

	private String colecao;

	private String genero;

	private int estoqueAtual;

	private int estoqueMinimo;

	private String fornecedor;

	private int tempoReposicaoDias;

	private Produto produto;

	private Cliente cliente;

	private Estoque estoque;


	private Collection<Pedido> _ItemPedido;

}
