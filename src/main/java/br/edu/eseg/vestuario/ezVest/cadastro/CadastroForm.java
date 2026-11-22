package br.edu.eseg.vestuario.ezVest.cadastro;

import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Entrega;
import lombok.Getter;
import lombok.Setter;

/**
 * Os campos da tela "Criar conta": o acesso (usuário e senha) e os dados do cliente.
 * Não é uma entidade: o CadastroService transforma isto em um Cliente (com a Entrega)
 * e um Usuario com perfil CLIENTE.
 */
@Getter
@Setter
public class CadastroForm {

    private String username;
    private String senha;
    private String confirmacaoSenha;

    // O formulário preenche cliente.cidade, cliente.entrega.cidadeEntrega...
    private Cliente cliente = new Cliente();

    public CadastroForm() {
        cliente.setEntrega(new Entrega());
    }
}
