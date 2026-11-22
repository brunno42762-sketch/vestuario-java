package br.edu.eseg.vestuario.ezVest.cadastro;

import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regras do acesso no "Criar conta": nome de usuário e senha. */
class CadastroServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final CadastroService servico = new CadastroService(null, usuarios, null);

    private Errors validar(String username, String senha, String confirmacao) {
        CadastroForm form = new CadastroForm();
        form.setUsername(username);
        form.setSenha(senha);
        form.setConfirmacaoSenha(confirmacao);
        Errors erros = new BeanPropertyBindingResult(form, "cadastro");
        servico.validarAcesso(form, erros);
        return erros;
    }

    @ParameterizedTest(name = "username válido: \"{0}\"")
    @ValueSource(strings = {"ana", "maria.silva", "joao_2026", "a-b"})
    void usernamesValidos(String username) {
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(validar(username, "senhaforte", "senhaforte").hasErrors()).isFalse();
    }

    @ParameterizedTest(name = "username inválido: \"{0}\"")
    @ValueSource(strings = {"", "ab", "joão", "com espaço", "maria@x"})
    void usernamesInvalidos(String username) {
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(validar(username, "senhaforte", "senhaforte").hasFieldErrors("username")).isTrue();
    }

    @Test
    void usernameCom80CaracteresPassaE81NaoPassa() {   // caso-limite (coluna de 80)
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(validar("a".repeat(80), "senhaforte", "senhaforte").hasFieldErrors("username")).isFalse();
        assertThat(validar("a".repeat(81), "senhaforte", "senhaforte").hasFieldErrors("username")).isTrue();
    }

    @Test
    void usernameRepetido() {
        when(usuarios.findByUsername("maria")).thenReturn(Optional.of(new Usuario()));
        assertThat(validar("maria", "senhaforte", "senhaforte").getFieldError("username").getCode())
                .isEqualTo("duplicado");
    }

    @Test
    void senhaDe7Caracteres_NaoPassa_DeOito_Passa() {   // caso-limite
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(validar("maria", "1234567", "1234567").hasFieldErrors("senha")).isTrue();
        assertThat(validar("maria", "12345678", "12345678").hasFieldErrors("senha")).isFalse();
    }

    @Test
    void senhaAcimaDe72BytesNaoPassa() {                // limite do BCrypt
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        String s72 = "a".repeat(72);
        String s73 = "a".repeat(73);
        assertThat(validar("maria", s72, s72).hasFieldErrors("senha")).isFalse();
        assertThat(validar("maria", s73, s73).hasFieldErrors("senha")).isTrue();
    }

    @Test
    void confirmacaoDiferente() {
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThat(validar("maria", "senhaforte", "outrasenha").hasFieldErrors("confirmacaoSenha")).isTrue();
    }
}
