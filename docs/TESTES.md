# Testes

## Como rodar

```bash
# Windows
mvnw.cmd test
# Linux/macOS
./mvnw test
```

No IntelliJ: botão direito em `src/test/java` → **Run 'All Tests'**.

Os testes usam o perfil `test` (`src/test/resources/application-test.properties`):

- banco **H2 em memória** (`jdbc:h2:mem:ezvestteste`): o banco de desenvolvimento (`database/`) não é tocado;
- a migração da planilha roda no início, então todo teste começa com os 126 produtos, 800 clientes e 5.999 pedidos do legado;
- o ADMIN de teste vem do próprio arquivo de propriedades (senha só de teste); não é preciso definir variáveis de ambiente;
- testes que gravam são `@Transactional`: tudo é desfeito no final de cada teste.

## Resultado da última execução

**103 testes, 103 com sucesso, 0 falhas** (Spring Boot 4.1.2, JUnit 6, Java 21+). Saída completa,
teste por teste: [`testes/resultado-testes.txt`](testes/resultado-testes.txt).

Prova de que os testes pegam erro de verdade: retirando o `@PreAuthorize` de
`ProdutoService.criar`, o teste `SegurancaIT.mesmoChamandoOServicoDiretoOVendedorNaoCriaProduto` falha.

## O que cada classe testa

| Classe | Tipo | Nº | O que verifica | Válido / inválido / limite |
|---|---|---|---|---|
| `model/StatusTest` | unitário | 19 | todas as transições do enum `Status` e `Pedido.alterarStatus` | 5 permitidas, 8 bloqueadas, 3 finais sem saída |
| `service/ProdutoValidatorTest` | unitário | 6 | regras do produto | preço = custo (limite, aceito), preço < custo, preço 0, estoque −1, estoque 0 (limite), texto acima do tamanho da coluna, gênero fora da lista |
| `service/ClienteValidatorTest` | unitário | 5 | regras do cliente e da entrega | cidade com 60 (aceita) e 61 caracteres (recusada), valores fora das listas, entrega obrigatória |
| `cadastro/CadastroServiceTest` | unitário (Mockito) | 14 | cadastro público | usernames válidos e inválidos (vazio, 2 caracteres, 80 aceito e 81 recusado, acento, espaço, @), repetido, confirmação diferente |
| `carrinho/CarrinhoTest` | unitário | 4 | linhas do carrinho | mesmo produto e tamanho soma; tamanhos diferentes são linhas diferentes |
| `carrinho/CarrinhoServiceTest` | unitário (Mockito) | 6 | estoque e limites ao adicionar | até o estoque exato (limite), uma peça além, outros tamanhos contam no estoque do produto, quantidade 0, −1 e 11 |
| `service/FiltroPedidosTest` | unitário | 3 | período dos filtros | período invertido, mesmo dia (limite), período aberto |
| `service/VendaServiceIT` | integração | 8 | venda completa | contas de valor e lucro, baixa do estoque, vender exatamente o estoque (limite), estoque insuficiente não grava nada, perfis |
| `service/PedidoServiceIT` | integração | 6 | status e cancelamento | transição inválida bloqueada no servidor, cancelamento devolve estoque, cliente só cancela o próprio pedido "Criado" |
| `service/UsuarioServiceIT` | integração | 6 | usuários | hash BCrypt, sempre 1 ADMIN ativo, ADMIN não se rebaixa nem se exclui, CLIENTE exige cliente livre |
| `repository/ConsultasIT` | integração | 14 | JPQL | filtros combinados comparados com contagem em Java, datas inclusivas, reposição, mais vendidos, categorias, relatório, 7 SQL inválidos recusados pelos CHECK |
| `migracao/MigracaoLegadoIT` | integração | 3 | migração | quantidades, receita e lucro iguais aos da planilha, produto 1 e cliente 1 reconstruídos |
| `seguranca/SegurancaIT` | integração (MockMvc) | 8 | segurança HTTP | visitante, CLIENTE, VENDEDOR e ADMIN em cada área; login certo e errado; POST sem CSRF; DELETE negado; service chamado direto |
| `EzVestApplicationTests` | integração | 1 | a aplicação sobe | |

## O que não é coberto por testes automáticos

| Funcionalidade | Por quê | Como foi verificada |
|---|---|---|
| Aparência das páginas (layout, cores, responsividade) | não há teste visual no projeto | manual, no navegador, em 375 px (celular) e 1280 px |
| Banner (carrossel) e botão de pausa | é JavaScript do Bootstrap no navegador | manual: troca a cada 6 s, setas, pausa, teclado |
| Conteúdo renderizado pelo Thymeleaf além do status HTTP | o `SegurancaIT` confere acesso e redirecionamento, não o HTML | manual, roteiro de demonstração em `ENTREGA4.md` |
| Duas vendas simultâneas das últimas peças | exige duas threads e duas transações reais | lógica: bloqueio pessimista (`select ... for update`); o caso sequencial é testado |
| `AjusteEsquemaBanco` num banco das entregas antigas | o banco de teste é sempre novo | manual: aplicação iniciada sobre uma cópia do banco da Entrega 2 (ENUM ganhou CRIADO, CHECK criados) |
| `EstadoContaFilter` (sessão encerrada quando o ADMIN desativa o usuário) | exige duas sessões abertas | manual, dois navegadores |
| H2 Console | ferramenta externa | manual, como ADMIN |
