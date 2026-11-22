package br.edu.eseg.vestuario.ezVest.cadastro;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import br.edu.eseg.vestuario.ezVest.model.Cliente;
import br.edu.eseg.vestuario.ezVest.model.Perfil;
import br.edu.eseg.vestuario.ezVest.model.Usuario;
import br.edu.eseg.vestuario.ezVest.repository.UsuarioRepository;
import br.edu.eseg.vestuario.ezVest.service.ClienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * "Criar conta" na loja: cria, numa transação só, o Cliente (com a Entrega) e o Usuario
 * com perfil CLIENTE ligado a ele. Se qualquer parte falhar, nada fica gravado.
 */
@Service
public class CadastroService {

    /** Letras sem acento, números, ponto, hífen e sublinhado; de 3 a 80 caracteres. */
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,80}");

    private final ClienteService clienteService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    public CadastroService(ClienteService clienteService, UsuarioRepository usuarioRepository,
                           PasswordEncoder encoder) {
        this.clienteService = clienteService;
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    /** Regras do acesso. Os campos do cliente são validados pelo ClienteValidator. */
    public void validarAcesso(CadastroForm form, Errors erros) {
        String username = form.getUsername() == null ? "" : form.getUsername().trim();
        if (!USERNAME.matcher(username).matches()) {
            erros.rejectValue("username", "invalido",
                    "Use de 3 a 80 caracteres: letras sem acento, números, ponto, hífen ou sublinhado.");
        } else if (usuarioRepository.findByUsername(username).isPresent()) {
            erros.rejectValue("username", "duplicado", "Este nome de usuário já está em uso.");
        }

        String senha = form.getSenha() == null ? "" : form.getSenha();
        if (senha.length() < 8) {
            erros.rejectValue("senha", "curta", "A senha deve ter pelo menos 8 caracteres.");
        } else if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            // limite do BCrypt: o que passa de 72 bytes seria ignorado
            erros.rejectValue("senha", "longa", "A senha deve ter no máximo 72 caracteres.");
        }
        if (!senha.equals(form.getConfirmacaoSenha())) {
            erros.rejectValue("confirmacaoSenha", "diferente", "As senhas não conferem.");
        }
    }

    /** Grava o cliente e o usuário. Chame validarAcesso antes. */
    @Transactional
    @PreAuthorize("isAnonymous()")
    public Usuario cadastrar(CadastroForm form) {
        String username = form.getUsername().trim();
        // Confere de novo dentro da transação; o índice único do banco é a última garantia
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RegraNegocioException("Este nome de usuário já está em uso.");
        }

        Cliente cliente = clienteService.criarPeloCadastro(form.getCliente());

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setSenha(encoder.encode(form.getSenha()));
        usuario.setPerfil(Perfil.CLIENTE);
        usuario.setAtivo(true);
        usuario.setCliente(cliente);
        return usuarioRepository.saveAndFlush(usuario);
    }
}
