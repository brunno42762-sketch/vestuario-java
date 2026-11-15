# Entrega 3 – Segurança, Thymeleaf e CSS próprio

## Fase 9 – Segurança (Spring Security)

A arquitetura segue o projeto-base `ControleAcademico` da disciplina.

### Perfis e permissões

| O quê | Visitante | CLIENTE | VENDEDOR | ADMIN |
|---|---|---|---|---|
| Catálogo da loja (`/`) | ✅ | ✅ | ✅ | ✅ |
| Minha conta: os próprios pedidos (`/minha-conta`) | — | ✅ | — | — |
| Painel e listas (`/admin/**`, consultar) | — | — | ✅ | ✅ |
| Alterar status de pedidos | — | — | ✅ | ✅ |
| Cadastrar e editar clientes | — | — | ✅ | ✅ |
| Cadastrar, editar e excluir produtos | — | — | — | ✅ |
| Excluir clientes | — | — | — | ✅ |
| Usuários (`/admin/usuarios`) | — | — | — | ✅ |
| H2 Console | — | — | — | ✅ |

Visitante que tenta abrir uma página protegida vai para o login. Usuário logado sem permissão
recebe a página 403 (`/acesso-negado`).

### Como a proteção funciona (no servidor, em duas camadas)

1. **Por URL e método HTTP** — `config/SecurityConfig`. As regras são lidas de cima para baixo
   e vale a primeira que combinar. POSTs administrativos não declarados, PUT, PATCH e DELETE
   são negados para todos (`denyAll`).
2. **Por método** — `@PreAuthorize` nos services (`ProdutoService`, `ClienteService`,
   `PedidoService`, `UsuarioService`) e no `UsuarioWebController`. Mesmo que alguém chegue ao
   método por outro caminho, a operação é bloqueada.

Esconder botões na tela com `sec:authorize` (ex.: o VENDEDOR não vê "Remover" nos produtos)
é só conforto: a proteção real são as duas camadas acima.

### Classes

| Classe | Papel |
|---|---|
| `model/Usuario` | id UUID, `username`, `senha` (hash BCrypt), `perfil`, `ativo` e, para o perfil CLIENTE, o `Cliente` vinculado |
| `model/Perfil` | ADMIN (1), VENDEDOR (2), CLIENTE (3) |
| `security/PrincipalVestuario` | o usuário logado (`UserDetails`), com o id do cliente para a Minha conta |
| `service/UsuarioDetailsService` | busca o usuário no login |
| `security/EstadoContaFilter` | revalida a conta a cada requisição: se o usuário for desativado ou mudar de username, perfil ou cliente, a sessão aberta é encerrada (`/login?expired`) |
| `config/SecurityConfig` | regras de acesso, login, logout, página 403 |
| `config/H2ConsoleSecurityConfig` | cadeia própria para o H2 Console (sem CSRF, frames da mesma origem), só ADMIN |
| `carga/CargaAutomatica` | cria o primeiro ADMIN a partir de variáveis de ambiente |
| `service/UsuarioService` + `controller/UsuarioWebController` | cadastro de usuários, só ADMIN |

### Regras do cadastro de usuários

- username obrigatório, até 80 caracteres e único;
- senha com pelo menos 8 caracteres (na edição, em branco = manter a atual); guardada só como hash BCrypt;
- o sistema mantém sempre pelo menos um ADMIN ativo (com bloqueio de linhas no banco);
- o ADMIN não pode trocar o próprio username, remover o próprio acesso nem excluir a própria conta;
- perfil CLIENTE exige um cliente existente, que não esteja vinculado a outro usuário;
- um cliente vinculado a um usuário não pode ser excluído.

### Minha conta (perfil CLIENTE)

O id do cliente vem sempre do usuário logado, nunca da URL. Um pedido de outra pessoa
(`/minha-conta/pedidos/{id}`) responde 404, igual a um pedido inexistente: assim ninguém
descobre, trocando o número na URL, quais pedidos existem.

### CSRF

Fica habilitado. Todos os formulários usam `th:action`, que acrescenta o token. O logout é um
POST (botão "Sair"), nunca um link.

### Como executar

O primeiro ADMIN vem das variáveis de ambiente (nenhuma senha fica no código):

- `APP_ADMIN_USER` — por exemplo `admin`
- `APP_ADMIN_PASSWORD` — pelo menos 8 caracteres

No IntelliJ: *Run → Edit Configurations… → EzVestApplication → Environment variables*:
`APP_ADMIN_USER=admin;APP_ADMIN_PASSWORD=<sua senha>`.

Depois, entre como ADMIN e cadastre em **Painel → Gerenciar usuários**:

- um VENDEDOR;
- um CLIENTE vinculado ao cliente **103** (Porto Alegre, 17 pedidos, 4 ainda "Entregue")
  ou ao **58** (Salvador, 16 pedidos, com os 4 status).

### Como demonstrar

1. Sem login, abrir `/admin` → vai para o login.
2. Entrar como VENDEDOR → painel sem "Novo produto"; lista de produtos sem Editar/Remover.
3. Como VENDEDOR, digitar `/admin/produtos/novo` na barra de endereço → 403 (protegido no servidor).
4. Como VENDEDOR, alterar o status de um pedido "Entregue" → permitido.
5. Entrar como CLIENTE → vai direto para "Meus pedidos"; ver o detalhe de um pedido.
6. Como CLIENTE, abrir `/minha-conta/pedidos/<id de um pedido de outro cliente>` → 404.
7. Como CLIENTE, abrir `/admin` → 403.
8. Como ADMIN, desativar o VENDEDOR enquanto ele está logado em outro navegador → na próxima
   página, a sessão dele é encerrada.
