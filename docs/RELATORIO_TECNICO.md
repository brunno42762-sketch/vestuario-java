# Relatório técnico – ezVest

Modernização do sistema legado de uma loja de roupas: de uma planilha com 4 abas para uma
aplicação web em Spring Boot, com banco relacional, perfis de acesso, carrinho e relatórios.

## 1. Problemas identificados

### No legado (Entrega 1, seção 2.5)

| Código | Problema | Evidência |
|---|---|---|
| P01 | Sem chaves, restrições nem tipos controlados | nenhuma PK/FK; canal, pagamento, status em texto livre |
| P02 | Referências órfãs | 52 pedidos com produto 1 e 7 com cliente 1, que não existem (59 pedidos, R$ 60.199,91) |
| P03 | Um produto por pedido | 5.999 ids únicos em 5.999 linhas |
| P04 | Colunas repetidas em Vendas | 7 de 21 colunas (ano, mês, trimestre, categoria, subcategoria, marca, estado_entrega) |
| P05 | Valores calculados guardados sem proteção | valor_total e lucro_estimado digitáveis |
| P06 | Sem histórico de preço e custo | preço da venda = preço atual em 100% das linhas |
| P07 | Status pobre e sem regra de transição | 4 valores finais; Cancelado → Entregue era possível |
| P08 | Cancelados somados na receita | 324 pedidos, R$ 257.048,16 (5,3%) |
| P09 | Estoque sem movimentação | 8 produtos abaixo do mínimo, sem alerta; vendas não baixam estoque |
| P10 | Tamanho de letra para calçado | PP a GG em 1.019 vendas de tênis |
| P11 | Estoque por produto, venda por tamanho | não dá para saber se o tamanho existe |
| P12 | Regra de desconto não documentada | 0%, 5%, 10%, 15% sem critério visível |

### Na implementação

- **ENUM do H2 congelado:** o Hibernate cria `status_pedido` como `ENUM('CANCELADO', ...)` com os
  valores da época; ao incluir CRIADO, bancos antigos recusavam o novo valor, e o `ddl-auto=update` não
  altera a coluna. Resolvido por `AjusteEsquemaBanco` (D15).
- **CHECK não criado em tabelas existentes:** mesma limitação do `ddl-auto`. Também resolvido por
  `AjusteEsquemaBanco`, que só cria o CHECK se nenhuma linha atual o viola.
- **Produto 1 reconstruído com gênero "Não informado":** um CHECK de lista no gênero impediria a
  migração num banco novo. O CHECK foi retirado; a regra fica no validador Java (D22).
- **Chave composta de item_pedido:** impediria o mesmo produto em dois tamanhos no pedido (D3).
- **POST negado virava 405:** `/acesso-negado` só aceitava GET; agora responde 403 para qualquer método.
- **Concorrência no estoque:** duas vendas das últimas peças poderiam deixar o estoque negativo.
  Resolvido com bloqueio pessimista (D20).

## 2. Soluções adotadas e justificativas

| Problema | Solução | Onde |
|---|---|---|
| P01 | PK, FK, NOT NULL, UNIQUE, colunas ENUM e 8 CHECK; validadores Java antes | entidades, `AjusteEsquemaBanco`, `ProdutoValidator`, `ClienteValidator` |
| P02 | produto 1 e cliente 1 reconstruídos a partir das vendas; migração confere os totais | `MigracaoLegado` |
| P03 | pedido + item_pedido; carrinho com vários produtos e tamanhos | `Pedido`, `ItemPedido`, `Carrinho`, `VendaService` |
| P04 | 3FN: ano/mês calculados na consulta; categoria lida do produto; entrega no cliente | modelo relacional |
| P05 | valor e lucro calculados só pelo sistema | `VendaService` |
| P06 | preço do momento guardado no item | `ItemPedido.precoUnitario` |
| P07 | enum `Status` com mapa de transições, conferido no domínio e no serviço; novo status CRIADO | `Status`, `Pedido.alterarStatus`, `PedidoService` |
| P08 | relatório exclui CANCELADO | `RelatorioService`, `/admin/relatorio` |
| P09 | venda baixa e cancelamento devolve o estoque, com bloqueio; lista de reposição no painel | `EstoqueService`, `PainelService` |
| Sem controle de acesso | 3 perfis, BCrypt, CSRF, proteção por URL e por método | `SecurityConfig`, `@PreAuthorize` |

As alternativas consideradas e o impacto de cada escolha estão em
[`DECISOES_TECNICAS.md`](DECISOES_TECNICAS.md). A ligação de cada requisito com o código e o teste
está na [`MATRIZ_RASTREABILIDADE.md`](MATRIZ_RASTREABILIDADE.md).

## 3. Antes e depois

| Pergunta | Legado | ezVest |
|---|---|---|
| Receita de 2024–2025 | R$ 4.852.558,18 (soma cancelados) | R$ 4.595.510,02 (sem os 324 cancelados) |
| Pedido com 2 peças | 2 linhas sem ligação | 1 pedido com 2 itens |
| Mudar Cancelado para Entregue | aceito (texto livre) | recusado: "O pedido N não pode mudar de Cancelado para Entregue." |
| Gravar preço 0 ou desconto de 50% | aceito | recusado pelo validador e pelo CHECK |
| Vender a última peça duas vezes | possível (estoque não movimenta) | a segunda venda é recusada |
| Quem pode alterar um produto | qualquer um com a planilha | só ADMIN, conferido no servidor |
| Pedidos de um cliente no período | filtro manual na planilha | `/admin/pedidos?cliente=103&de=2025-01-01&ate=2025-06-30` |

## 4. Testes realizados e resultados

103 testes automatizados (unitários e de integração, com banco H2 em memória e a migração completa),
**103 com sucesso**. Cobrem cenários válidos, inválidos e casos-limite (preço igual ao custo, estoque
exato, 60/61 caracteres, 80/81 no username, período de um dia, 7 gravações inválidas direto no banco).
Detalhes, como rodar e o que ficou fora: [`TESTES.md`](TESTES.md).

Verificações manuais: todos os fluxos do roteiro em `ENTREGA4.md`, em banco novo e sobre uma cópia
do banco da Entrega 2, em tela de celular (375 px) e de computador.

## 5. Limitações conhecidas

- **Estoque por produto, não por tamanho (P11, D13):** o carrinho confere o total do produto; pode
  vender um tamanho que na loja já acabou.
- **Tamanho de letra para calçado (P10):** mantido para preservar os dados legados; tênis continua de PP a GG.
- **Regra de desconto (P12):** não há regra documentada; as vendas novas têm desconto 0. Os descontos
  do legado são preservados.
- **Dinheiro em `double` (D21):** somas grandes podem diferir por centavos (o relatório por categoria
  difere do total em menos de R$ 5 em R$ 4,6 milhões, por arredondamento por item).
- **H2 em arquivo:** adequado para avaliação e uso local, não para vários servidores.
- **Pagamento simulado:** a forma de pagamento é registrada, sem integração com meio de pagamento.
- **Carrinho na sessão (D19):** some se a sessão expirar.
- **Status de andamento:** não há "pago" ou "enviado"; CRIADO cobre tudo antes da entrega.

## 6. Riscos residuais

- Quem tiver acesso ao arquivo `database/` ou ao H2 Console (só ADMIN) lê todos os dados.
- O gênero e a coleção do produto não têm CHECK no banco (D22): só o Java protege.
- Sem limite de tentativas de login (força bruta).
- Sem testes automáticos de interface e de concorrência real (ver `TESTES.md`).

## 7. Melhorias futuras

1. Estoque por produto e tamanho (tabela `estoque_tamanho`) e numeração para calçados.
2. Migrações versionadas (Flyway) no lugar de `ddl-auto` + `AjusteEsquemaBanco`.
3. `BigDecimal` para valores de dinheiro.
4. Banco servidor (PostgreSQL) e perfil de produção sem H2 Console.
5. Regra de desconto por fidelidade, depois de confirmada com a loja.
6. Status intermediários (PAGO, ENVIADO) e histórico de mudanças de status (quem e quando).
7. Bloqueio após várias tentativas de login e troca de senha pelo próprio usuário.
8. Testes de interface (Selenium/Playwright) e de concorrência.
