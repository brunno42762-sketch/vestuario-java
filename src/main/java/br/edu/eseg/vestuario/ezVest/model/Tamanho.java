package br.edu.eseg.vestuario.ezVest.model;

public enum Tamanho {

    PP("PP"),
    P("P"),
    M("M"),
    G("G"),
    GG("GG");

    private final String tamanho;

    Tamanho(String tamanho) {
        this.tamanho = tamanho;
    }

    public String getTamanho() {
        return tamanho;
    }
}
