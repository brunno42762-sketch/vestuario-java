# Entrega 2 – Backend, persistência e regras de negócio

## Como executar

1. Abrir o projeto no IntelliJ e deixar o Maven baixar as dependências.
2. Rodar `EzVestApplication`.
3. Acessar `http://localhost:8080/admin` (ou clicar em "Área do administrador" na página inicial).

Banco: H2 em arquivo (`database/ezVestData`), console em `/h2-console`
(JDBC URL `jdbc:h2:file:./database/ezVestData`, usuário `sa`, senha vazia, schema `APP`).

## Rotas

| Método | Rota | O que faz |
|---|---|---|
| GET  | `/?pagina=N` | Página inicial da loja: catálogo com 12 produtos por página |
| GET  | `/admin` | Painel: totais, pedidos por status e produtos para repor |
| GET  | `/admin/produtos` | Lista os produtos |
| GET  | `/admin/produtos/novo` | Formulário de novo produto |
| POST | `/admin/produtos` | Cria o produto |
| GET  | `/admin/produtos/{id}/editar` | Formulário preenchido |
| POST | `/admin/produtos/{id}` | Salva a edição |
| POST | `/admin/produtos/{id}/excluir` | Exclui o produto |
| GET  | `/admin/clientes` | Lista os clientes |
| GET  | `/admin/clientes/novo` | Formulário de novo cliente (com a entrega) |
| POST | `/admin/clientes` | Cria o cliente e a entrega |
| GET  | `/admin/clientes/{id}/editar` | Formulário preenchido |
| POST | `/admin/clientes/{id}` | Salva a edição |
| POST | `/admin/clientes/{id}/excluir` | Exclui o cliente (e a entrega) |
| GET  | `/admin/pedidos?pagina=N&status=S&ordem=O` | Lista os pedidos (20 por página), com filtro por status e ordem: mais recentes (padrão), ID crescente ou ID decrescente |
| POST | `/admin/pedidos/{id}/status` | Altera o status (só transições válidas) |

Toda a parte de gestão fica sob `/admin`. Na entrega 3, uma única regra do Spring Security
(`/admin/**` só para o perfil administrador) protege a área inteira.

A página inicial mostra o catálogo completo com **paginação** (`Pageable`/`Page` do Spring Data):
12 produtos por página, ordenados por categoria e subcategoria. Página inválida (`?pagina=0`) volta para a
primeira; página além do fim (`?pagina=999`) vai para a última.

## Camadas

`Controller` (rotas, formulários) → `Service` (regras de negócio) → `Repository` (Spring Data JPA) → banco.

## Regras de negócio implementadas

- **Validação de produto** (`ProdutoValidator`, escrito à mão com a interface `Validator` do Spring, sem dependências extras): campos de texto obrigatórios e com tamanho
  máximo; preço e custo maiores que zero; estoque e tempo de reposição não negativos.
- **Preço não pode ser menor que o custo** (regra entre dois campos, no mesmo validador).
- **Validação em duas camadas**: o controller valida para mostrar os erros no formulário, e o `ProdutoService` valida de novo antes de gravar, então nenhum produto inválido chega ao banco.
- **Produto vendido não pode ser excluído**: se já aparece em algum `ItemPedido`, a exclusão é bloqueada.
- **Validação de cliente** (`ClienteValidator`): faixa etária, gênero e canal só aceitam valores da lista;
  cidade obrigatória; UF entre as 27 válidas; entrega obrigatória (UML: Cliente 1 ◆—— 1 Entrega).
  As faixas etárias são as da planilha legada: 18-24, 25-34, 35-44, 45-54 e 55+.
- **Cliente com pedidos não pode ser excluído**: Cliente ◆—— Pedido é composição (cascade ALL),
  então excluir o cliente apagaria o histórico de vendas.
- **Edição de cliente não apaga pedidos**: o service busca o cliente no banco e copia só os campos do
  formulário, porque salvar o objeto do formulário (sem a lista de pedidos) com `orphanRemoval = true`
  apagaria os pedidos.
- **Campos permitidos nos formulários** (`@InitBinder` + `setAllowedFields`): o Spring só preenche os
  campos que estão na tela, evitando que alguém envie, por exemplo, um `idCliente` ou `pedidos[0]` forjado.
- **Alerta de reposição**: produtos com estoque atual ≤ estoque mínimo aparecem com o selo "repor" na lista
  e numa tabela própria no painel (`/admin`), ordenados do mais crítico para o menos crítico.
- **Transições de status do pedido** (`Status.proximosPermitidos()` e `Pedido.alterarStatus()`):

| De | Pode ir para |
|---|---|
| Entregue | Concluído, Troca, Cancelado |
| Concluído | — (final) |
| Troca | — (final) |
| Cancelado | — (final) |

Regra da Entrega 1 (seções 4.2 e 4.3): o pedido começa em Entregue (dentro do prazo de troca)
e termina em um dos três status finais.

A tela de pedidos é **paginada** (20 por página) e tem **filtro por status**: com os 5.999 pedidos
migrados, mostrar tudo de uma vez travava o navegador. A consulta usa `join fetch` para trazer o cliente
junto e uma `countQuery` separada para o total. A ordem (`OrdemPedidos`: mais recentes, ID crescente ou
ID decrescente) vira um `Sort` do Spring Data. Depois de alterar um status, o usuário volta para a
mesma página, o mesmo filtro e a mesma ordem. No painel, cada status leva à lista já filtrada.

A tela de pedidos mostra **todos** os status no seletor de propósito: a regra é verificada no
servidor, então escolher uma transição inválida mostra a mensagem de bloqueio e o banco não muda.

## Migração da planilha legada (RF08)

A classe `migracao/MigracaoLegado` segue a seção 6.5 da Entrega 1. Ela roda quando a aplicação
inicia, **só se o banco estiver vazio**, e grava tudo numa transação.

- **Origem:** as abas Produtos, Estoque, Clientes e Vendas da planilha, exportadas para CSV (UTF-8,
  separador `;`) em `src/main/resources/legado/`. Ler o `.xlsx` direto exigiria a biblioteca Apache POI
  (dependência nova).
- **Passos:** produtos + estoque (juntados pelo `id_produto`), clientes, uma entrega por cliente
  (com a cidade e o estado do cliente) e, para cada venda, 1 pedido e 1 item com o mesmo `id_pedido`.
  Textos viram enums ("Loja Física" → `FISICA`, "Concluído" → `CONCLUIDO`, "Sim" → `true`).
- **Ids do legado mantidos:** a carga usa `JdbcTemplate`, porque com `GenerationType.IDENTITY` o JPA
  sempre gera um id novo. No fim, o contador de ids de cada tabela é reiniciado após o maior id migrado.
- **Ids ausentes (P02):** o produto 1 e o cliente 1, usados em 59 vendas, são reconstruídos a partir
  das próprias vendas. O custo do produto 1 é (valor_total − lucro) ÷ quantidade, igual em todas as
  52 vendas. O que não dá para recuperar fica como "Não informado".
- **Conferência de totais:** contagens de produtos, clientes, entregas, pedidos e itens, nenhum pedido
  sem cliente ou item sem produto, e receita e lucro iguais aos da planilha. Se algo não bater, a
  transação é desfeita e nada fica gravado. O resultado vai para o log da aplicação.

**Resultado esperado no log:** 126 produtos (1 reconstruído), 800 clientes (1 reconstruído),
800 entregas, 5.999 pedidos e 5.999 itens; receita R$ 4.852.558,18; lucro R$ 2.417.872,92;
receita sem cancelados R$ 4.595.510,02.

**Para rodar a migração** num banco que já tem dados: pare a aplicação, apague
`database/ezVestData.mv.db` e inicie de novo.

## Tratamento de erros

- Produto ou pedido inexistente (ex.: `/admin/produtos/999/editar`) → página de erro 404 com mensagem.
- Id inválido na URL (ex.: `/admin/produtos/abc/editar`) → página de erro 400.
- Rota inexistente ou erro inesperado → `templates/error.html` no lugar da "Whitelabel Error Page".
- Erros de validação aparecem embaixo de cada campo do formulário.

## Como demonstrar

1. Criar um produto com o preço vazio ou negativo → mensagens embaixo dos campos.
2. Criar um produto com preço menor que o custo → mensagem no topo do formulário.
3. Criar, editar e excluir um produto e conferir no H2 Console (`SELECT * FROM APP.PRODUTO`).
4. Tentar excluir um produto que aparece em algum pedido (todos os 126 migrados aparecem) → exclusão bloqueada.
5. Em `/admin/pedidos`, mudar um pedido Cancelado para Entregue → bloqueado; mudar um Entregue para Concluído → permitido.
6. Acessar `/admin/produtos/999/editar` → página de erro 404.
7. Criar um cliente sem preencher a entrega → mensagens embaixo dos campos da entrega.
8. Editar o cliente 1 (reconstruído na migração, sem canal preferido) → o formulário exige escolher o canal antes de salvar.
9. Tentar excluir um cliente que tem pedidos → bloqueado; criar um cliente novo e excluí-lo → permitido.
