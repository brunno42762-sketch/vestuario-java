package br.edu.eseg.vestuario.ezVest.model;

public enum FormaPagamento {

    PIX("Pix"),
    CARTAO_DEBITO("Cartão Débito"),
    CARTAO_CREDITO("Cartão Crédito"),
    BOLETO("Boleto");

    private final String nome;

    FormaPagamento(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
