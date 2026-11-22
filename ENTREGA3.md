# Entrega 3 – Segurança, Thymeleaf e CSS próprio

## Fase 9 – Segurança (Spring Security)

A arquitetura segue o projeto-base `ControleAcademico` da disciplina.

### Perfis e permissões

| O quê | Visitante | CLIENTE | VENDEDOR | ADMIN |
|---|---|---|---|---|
| Catálogo da loja (`/`) | ✅ | ✅ | ✅ | ✅ |
| Criar conta (`/cadastro`) | ✅ | — | — | — |
| Carrinho: adicionar, alterar, remover (`/carrinho`) | ✅ | ✅ | — | — |
| Finalizar a compra (`/carrinho/finalizar`) | — | ✅ | — | — |
| Minha conta: os próprios pedidos (`/minha-conta`) | — | ✅ | — | — |
| Cancelar o próprio pedido ainda "Criado" | — | ✅ | — | — |
| Registrar venda (`/admin/vendas/nova`) | — | — | ✅ | ✅ |
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

### Como demonstrar o carrinho e as vendas

1. Sem login, adicionar dois tamanhos do mesmo produto → o número ao lado de "Carrinho" sobe.
2. Abrir o carrinho, mudar a quantidade, clicar em "Entrar e finalizar" → vai para o login.
3. Clicar em "Criar conta", preencher (testar senhas diferentes → erro no campo) → login com o
   usuário preenchido.
4. Entrar → volta para o carrinho, com os mesmos itens. Finalizar com Pix → pedido **Criado**.
5. Como ADMIN, ver em Produtos que o estoque baixou. Como cliente, cancelar o pedido → o estoque volta.
6. Como VENDEDOR, "Nova venda" para o cliente 103 na Loja Física → aparece no topo de Pedidos como Criado.
7. Pedir mais peças do que há em estoque → "Estoque insuficiente para ... disponível N".
8. Mudar o status: Criado → Troca é recusado; Criado → Entregue → Cancelado devolve o estoque.
9. Como ADMIN ou VENDEDOR, abrir `/carrinho` → 403. Como CLIENTE, abrir `/admin/vendas/nova` → 403.

## Carrinho, cadastro e vendas

### Status CRIADO

Todo pedido novo nasce **Criado**. As transições ficam em `model/Status`:

| De | Para |
|---|---|
| Criado | Entregue, Cancelado |
| Entregue | Concluído, Troca, Cancelado |
| Concluído, Troca, Cancelado | — (finais) |

Os pedidos do legado continuam começando em Entregue, como na Entrega 1.

**Banco que já existe:** no H2, o Hibernate cria as colunas de enum com o tipo nativo
`ENUM('CANCELADO', ...)`, que só aceita os valores da época da criação, e o `ddl-auto=update`
não muda o tipo de uma coluna. O `config/AjusteEsquemaBanco` confere isso ao iniciar e acrescenta
o CRIADO à coluna `pedido.status_pedido`. Em banco novo, não faz nada. Não é preciso apagar o banco.

### Estoque

- **Baixa na venda** (carrinho ou vendedor) e **volta no cancelamento** (pelo cliente ou pela equipe).
- O estoque é do produto, não do tamanho, como na planilha.
- `service/EstoqueService` trava as linhas dos produtos (`select ... for update`, via
  `@Lock(PESSIMISTIC_WRITE)` no `ProdutoRepository`) até o fim da transação. Se duas vendas
  das últimas peças chegarem juntas, a segunda espera a primeira e é recusada por falta de
  estoque. O estoque nunca fica negativo.
- O pedido também é travado ao mudar de status (`PedidoRepository.bloquear`), para um
  cancelamento duplo não devolver o estoque duas vezes.

### Venda (`service/VendaService`)

Cria o `Pedido` e os `ItemPedido` e baixa o estoque, tudo numa transação: se algo falhar,
nada fica gravado. As contas são as da planilha legada:

- preço unitário = o do produto **no momento da venda** (fica guardado no item);
- valor total = Σ preço × quantidade × (1 − desconto); desconto 0 nas vendas novas;
- lucro estimado = valor total − Σ custo × quantidade.

Dois métodos, com permissões diferentes no `@PreAuthorize`:

| Método | Quem | Canal | Cliente |
|---|---|---|---|
| `comprar` | CLIENTE | E-commerce | o do usuário logado (nunca vem do formulário) |
| `registrarNaLoja` | VENDEDOR, ADMIN | escolhido na tela (padrão: Loja Física) | escolhido na tela; o **1 (Não informado)** serve para quem não quer se identificar |

Limites: até 10 itens por pedido e de 1 a 10 peças por item.

### Carrinho (`carrinho/Carrinho`, `carrinho/CarrinhoService`, `controller/CarrinhoController`)

- Fica na **sessão** (`@SessionScope`), não no banco: no UML não existe "Carrinho". Ele só
  vira Pedido ao finalizar.
- Guarda só produto, tamanho e quantidade. Preço e nome são lidos do banco a cada exibição.
- O visitante pode montar o carrinho. Para finalizar, entra (ou cria a conta) e volta direto
  para o carrinho, que continua o mesmo depois do login.
- ADMIN e VENDEDOR não usam o carrinho (vendem pela tela "Nova venda").
- A conferência de estoque ao adicionar é um aviso; a que vale é a da venda, com o estoque travado.

### Cadastro (`cadastro/CadastroService`, `controller/CadastroController`)

- Página pública `/cadastro`, só para quem ainda não entrou (`@PreAuthorize("isAnonymous()")`).
- Cria, numa transação só, o `Cliente` (com a `Entrega`) e o `Usuario` com perfil CLIENTE.
- Usuário: 3 a 80 caracteres (letras sem acento, números, `.`, `-`, `_`), único.
- Senha: 8 a 72 caracteres (72 é o limite do BCrypt), com confirmação.
- Os dados do cliente usam o mesmo `ClienteValidator` da área administrativa.
- Depois de criar, vai para o login com o usuário já preenchido.

### Correção na página 403

Um POST negado (ex.: VENDEDOR tentando excluir, ou formulário sem o token CSRF) era
encaminhado para `/acesso-negado`, que só aceitava GET, e a resposta virava **405**. Agora a
página aceita qualquer método e responde 403 com a mensagem certa.

## Fase 10 – Thymeleaf: layout e fragments

As partes repetidas das páginas ficam em `templates/fragments/`. Cada página só chama o que
precisa com `th:replace`, passando parâmetros quando algo muda de uma página para outra.

| Fragment | O que é | Usado em |
|---|---|---|
| `base :: head(titulo)` | `<head>` com fontes, Bootstrap e CSS; só o título muda | as 15 páginas |
| `base :: rodape` | rodapé da loja | página inicial |
| `navegacao :: loja(ativo)` | barra da loja: Categorias, Catálogo, Meus pedidos (CLIENTE), Área do administrador (ADMIN/VENDEDOR) | inicial, login, minha conta, páginas de erro |
| `navegacao :: admin(ativo)` | barra do admin: Painel, Produtos, Clientes, Pedidos, Usuários (só ADMIN) | painel, listas e formulários do admin |
| `navegacao :: conta(ativo)` | lado direito das duas barras: **Entrar** (visitante) ou usuário + **Sair** | incluído pelos dois fragments acima |
| `mensagens :: alertas` | mensagens de sucesso e erro enviadas pelo controller (flash) | listas de produtos, clientes, pedidos e usuários |
| `paginacao :: paginacao(base, rotulo, ancora)` | Anterior · 1 … 4 5 [6] 7 8 … 300 · Próxima, mantendo os filtros | catálogo, pedidos, meus pedidos |

Exemplo (`listar-pedidos.html`):

```html
<head th:replace="~{fragments/base :: head('Pedidos')}"></head>
<th:block th:replace="~{fragments/navegacao :: admin('pedidos')}"></th:block>
<th:block th:replace="~{fragments/mensagens :: alertas}"></th:block>
<nav th:replace="~{fragments/paginacao :: paginacao(@{/admin/pedidos(status=${statusFiltro},ordem=${ordem})}, 'Páginas da lista de pedidos', '')}"></nav>
```

O parâmetro `ativo` marca o item da página atual (classe `active` e `aria-current="page"`).

Também na navegação: link "Pular para o conteúdo" (aparece ao apertar Tab) e `<main id="conteudo">`
em todas as páginas.

### Página inicial: banner e ilustrações

- **Banner do topo** (componente `carousel` do Bootstrap 6), de ponta a ponta da tela:
  palavra grande à esquerda, imagem no centro, chamada à direita, setas nas laterais e botão
  "Comprar" embaixo. São 7 slides:
  - o 1º é a coleção, com a foto (Godisable Jacob, Pexels; crédito no rodapé);
  - os outros 6 são os mais vendidos: soma das peças, sem contar pedidos cancelados e só entre
    produtos com estoque (`ItemPedidoRepository.maisVendidos`, com `group by` e `sum`). O fundo é
    um tom escuro da cor do produto, e "Comprar" leva à página do catálogo onde ele está
    (`/?pagina=8#produto-87`), destacando o cartão.
  - Troca automática a cada 6 s, com botão de pausa (acessibilidade: conteúdo que se move
    sozinho precisa poder ser parado). Setas, indicadores e pausa têm `aria-label`.
- **Ilustrações das peças** (`fragments/pecas.html`): um desenho SVG próprio para cada categoria
  (Camiseta, Calça, Jaqueta, Moletom, Tênis, Vestido), escolhido com `th:switch` e pintado com a cor
  do produto pelo CSS (`fill: var(--cor)`). Categoria desconhecida mostra um cabide. Aparecem nos
  cartões do catálogo e nos slides do banner.

## Fase 11 – CSS

O uso do Bootstrap foi **autorizado pelo professor**. A identidade visual (cores, fontes, botões,
tabelas, alertas, catálogo, paginação) é feita com CSS próprio em `static/css/style.css` e
`static/css/mensagens.css`.
