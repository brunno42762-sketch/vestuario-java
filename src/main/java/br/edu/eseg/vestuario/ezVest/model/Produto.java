package br.edu.eseg.vestuario.ezVest.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
// Índice na ordem do catálogo (categoria, subcategoria) e na lista de reposição (RNF02)
@Table(name = "produto", indexes = {
        @Index(name = "ix_produto_categoria", columnList = "categoria, subcategoria")
})
@Getter
@Setter
@NoArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_produto")
    private Integer idProduto;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(nullable = false, length = 60)
    private String subcategoria;

    @Column(nullable = false, length = 40)
    private String cor;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(name = "preco_unitario", nullable = false)
    private double precoUnitario;

    @Column(name = "custo_unitario", nullable = false)
    private double custoUnitario;

    @Column(length = 60)
    private String colecao;

    @Column(nullable = false, length = 20)
    private String genero;

    @Column(name = "estoque_atual", nullable = false)
    private int estoqueAtual;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Column(nullable = false, length = 80)
    private String fornecedor;

    @Column(name = "tempo_reposicao_dias", nullable = false)
    private int tempoReposicaoDias;

    /** Indica se o produto chegou ao estoque mínimo e precisa ser reposto. */
    public boolean isAbaixoDoEstoqueMinimo() {
        return estoqueAtual <= estoqueMinimo;
    }
}
