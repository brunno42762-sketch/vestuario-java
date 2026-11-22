# Entrega 4 – Integração, testes e documentação (fases 11 a 13)

## O que entrou nesta entrega

### Requisitos que faltavam da Entrega 1

- **RF08 – Filtros de pedidos** (`/admin/pedidos`): status, canal, período (de/até), cliente e
  ordem, combináveis, em GET (o link pode ser copiado). A paginação mantém os filtros. Período
  invertido mostra erro e é ignorado. Em Clientes, o botão **Pedidos** abre a lista já filtrada.
- **RF09 – Relatório de receita e lucro** (`/admin/relatorio`, só ADMIN): sem pedidos cancelados,
  por mês, canal e categoria, com período opcional, margem e barras proporcionais.
- **RNF01 – Restrições CHECK** no banco (preço, custo, estoques, quantidade, desconto 0 a 0,15, valor).
- **RNF02 – Índices** nomeados em `pedido.data_pedido`, `pedido.status_pedido`, `item_pedido` e
  `produto(categoria, subcategoria)`.

### Integração (fase 11)

- **Categorias da página inicial vêm do banco**, com contagem de produtos e subcategorias; clicar
  filtra o catálogo (`/?categoria=Tênis`).
- Fluxos completos testados da tela ao banco: carrinho → login → compra → estoque → cancelamento;
  venda da equipe; mudança de status; filtros; relatório.

### Testes (fase 12)

103 testes automatizados, todos passando. Ver [`docs/TESTES.md`](docs/TESTES.md).

### Documentação (fase 13)

[`README.md`](README.md) (instalação e execução) e a pasta [`docs/`](docs/): relatório técnico,
matriz de rastreabilidade, decisões técnicas, modelos relacionais legado e evoluído, diagrama de
classes e testes.

## Roteiro de demonstração

Antes: variáveis `APP_ADMIN_USER` e `APP_ADMIN_PASSWORD` definidas; criar um VENDEDOR em
**Painel → Gerenciar usuários**.

| # | Passo | Resultado esperado | Mostra |
|---|---|---|---|
| 1 | Sem login, abrir `/` | banner, categorias, catálogo | Thymeleaf + dados do banco |
| 2 | Clicar em uma categoria | catálogo só daquela categoria | consulta JPA |
| 3 | Adicionar 2 tamanhos do mesmo produto e abrir o carrinho | 2 linhas, contador no menu | carrinho em sessão |
| 4 | "Entrar e finalizar" → "Criar conta" | conta criada; volta ao carrinho depois do login | cadastro, segurança |
| 5 | Finalizar com Pix | pedido **Criado** em "Meus pedidos" | venda, transação |
| 6 | Como ADMIN, ver o produto | estoque baixou | estoque |
| 7 | Como cliente, cancelar o pedido | estoque voltou | regra de negócio |
| 8 | Como cliente, abrir `/admin` | 403 | autorização |
| 9 | Como VENDEDOR, "Nova venda" para o cliente 103 | pedido Criado no topo de Pedidos | perfil VENDEDOR |
| 10 | Pedidos: mudar um pedido Criado para **Troca** | recusado com mensagem | transição de status bloqueada |
| 11 | Como VENDEDOR, abrir `/admin/produtos/novo` e `/admin/relatorio` | 403 nos dois | proteção no servidor |
| 12 | Pedidos: filtrar status Cancelado, canal Loja Física, 2025 | lista filtrada, paginação mantém filtros | RF08 |
| 13 | Como ADMIN, Relatório sem período | receita R$ 4.595.510,02 (sem cancelados; o legado somava R$ 4.852.558,18) | RF09, antes e depois |
| 14 | H2 Console: `update APP.produto set preco_unitario = 0 where id_produto = 2` | erro de CHECK `ck_produto_preco` | integridade no banco |
| 15 | `mvnw test` | 103 testes, 0 falhas | testes |

## Git

Tags sugeridas no commit final de cada entrega: `entrega-1` … `entrega-4`.
A pasta `database/` não vai para o repositório (`.gitignore`): o banco é recriado a partir da
planilha na primeira execução.
