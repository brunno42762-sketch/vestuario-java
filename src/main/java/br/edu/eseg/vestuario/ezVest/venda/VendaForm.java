package br.edu.eseg.vestuario.ezVest.venda;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.FormaPagamento;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Campos da tela "Nova venda" (VENDEDOR e ADMIN): cliente, canal, pagamento e
 * algumas linhas de itens. Linhas deixadas em branco são ignoradas.
 */
@Getter
@Setter
public class VendaForm {

    /** Quantas linhas de item a tela mostra. */
    public static final int LINHAS = 5;

    private Integer idCliente;
    private Canal canal = Canal.FISICA; // o caso mais comum para o vendedor: venda no balcão
    private FormaPagamento formaPagamento;
    private List<Linha> itens = new ArrayList<>();

    public VendaForm() {
        completarLinhas();
    }

    /** Garante LINHAS linhas na tela (ao voltar com erro, mantém o que foi digitado). */
    public void completarLinhas() {
        while (itens.size() < LINHAS) {
            itens.add(new Linha());
        }
    }

    @Getter
    @Setter
    public static class Linha {
        private Integer idProduto;
        private Tamanho tamanho;
        private Integer quantidade;

        public boolean isEmBranco() {
            return idProduto == null && tamanho == null && quantidade == null;
        }

        public boolean isCompleta() {
            return idProduto != null && tamanho != null && quantidade != null;
        }
    }
}
