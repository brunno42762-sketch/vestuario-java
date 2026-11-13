package br.edu.eseg.vestuario.ezVest.model;

public enum Canal {

    MARKETPLACE("Marketplace"),
    ECOMMERCE("E-commerce"),
    FISICA("Loja Física");

    private final String nome;

    Canal(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
