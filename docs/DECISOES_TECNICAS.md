# Inventário das decisões técnicas

As decisões D1 a D13 vêm da Entrega 1 (seção 7). A coluna **Situação** diz se a implementação
seguiu o plano ou mudou, e por quê. D14 em diante foram tomadas durante a implementação.

## Decisões da Entrega 1

| Nº | Decisão | Alternativas | Escolha e justificativa | Impacto | Situação |
|---|---|---|---|---|---|
| D1 | Banco de dados | H2 em memória, H2 em arquivo, PostgreSQL/MySQL | **Mudou: H2 em arquivo** (`./database/ezVestData`). Continua sem instalação, mas os pedidos, clientes e usuários criados na aplicação não somem quando ela para. Com banco em memória, uma venda feita na demonstração sumiria ao reiniciar. | A migração roda só uma vez (banco vazio). Para recomeçar do zero, basta apagar a pasta `database/`. | Alterada |
| D2 | Pedido e itens | uma tabela como o legado; pedido + item_pedido | pedido + item_pedido, como no diagrama | o carrinho grava 1 pedido com N itens; o legado vira 1 pedido com 1 item | Seguida |
| D3 | Chaves primárias | natural, número novo, id do legado | Id do legado em cliente, produto e pedido. **Mudou em item_pedido:** chave própria `id_item_pedido` em vez de (id_pedido, id_produto). A chave composta impediria o mesmo produto em **dois tamanhos** no mesmo pedido (uma camiseta P e uma M), que o carrinho permite. Entrega também ganhou id próprio, com `id_cliente` UNIQUE. | Unicidade 1 entrega por cliente garantida pelo UNIQUE. No item, linhas iguais (mesmo produto e tamanho) são juntadas pelo `VendaService`. | Alterada |
| D4 | Estoque | tabela própria; dentro de Produto | dentro de Produto | sem junção para ver estoque | Seguida |
| D5 | Entrega | por pedido; ligada ao cliente | ligada ao cliente (1 para 1) | outro endereço por pedido exigiria mudar o modelo | Seguida |
| D6 | Status | texto; tabela; enum | enum `Status` com o mapa de transições + coluna ENUM no banco. Ganhou **CRIADO** (pedido novo, ainda não entregue). | mudar a lista exige mudar código e banco (`AjusteEsquemaBanco` faz isso em bancos antigos) | Seguida, ampliada |
| D7 | Canal, pagamento, tamanho | texto; enum | enums com coluna ENUM no banco | idem D6 | Seguida |
| D8 | Valores derivados | calcular sempre; guardar | ano/mês/trimestre calculados na consulta; valor total e lucro guardados e calculados pelo `VendaService` | teste confere as contas (`VendaServiceIT`) | Seguida |
| D9 | Preço da venda | preço atual; copiar no item | copiado no item | mudar o preço não altera vendas antigas | Seguida |
| D10 | Fidelidade | texto; boolean | boolean | a carga converte Sim/Não | Seguida |
| D11 | Migração | importador Java; CSV + SQL com tabelas temporárias | **Mudou: importador Java** (`MigracaoLegado`) lendo os CSV com `JdbcTemplate`. Com banco em arquivo e `ddl-auto`, um script SQL rodaria a cada início ou exigiria controle manual; em Java dá para rodar só com banco vazio, numa transação, e conferir os totais no final. Ler o `.xlsx` direto exigiria Apache POI (dependência nova), por isso as abas foram exportadas para CSV. | se a conferência falhar, nada é gravado | Alterada |
| D12 | Pedidos órfãos | apagar; deixar sem vínculo; reconstruir | reconstruir produto 1 e cliente 1 com "Não informado" | por isso o gênero do produto não tem CHECK (ver D22) | Seguida |
| D13 | Estoque por tamanho | por produto; por produto e tamanho | por produto | limite conhecido (P11) | Seguida |

## Decisões tomadas na implementação

| Nº | Decisão | Alternativas | Escolha e justificativa | Impacto |
|---|---|---|---|---|
| D14 | Gerar o esquema | scripts SQL (Flyway); `ddl-auto=update` | `ddl-auto=update`: as entidades são a única fonte do esquema e não há dependência nova | o Hibernate não altera ENUM nem cria CHECK em tabela existente → D15 |
| D15 | Ajuste de bancos antigos | apagar o banco; ajustar ao iniciar | `config/AjusteEsquemaBanco` atualiza a lista do ENUM e cria os CHECK que faltam. Só cria um CHECK se nenhuma linha atual o viola (senão avisa no log). | quem já tinha o banco das entregas anteriores não precisa apagá-lo |
| D16 | Senhas | texto; hash simples; BCrypt | BCrypt (`PasswordEncoder`), com sal; senha de 8 a 72 caracteres (limite do BCrypt) | não dá para recuperar a senha, só trocar |
| D17 | Primeiro ADMIN | fixo no código; `data.sql`; variável de ambiente | variáveis `APP_ADMIN_USER` e `APP_ADMIN_PASSWORD` (`CargaAutomatica`) | nenhuma senha no repositório (exigência da seção 9 do guia) |
| D18 | Onde proteger | só esconder botões; por URL; por URL + método | duas camadas: `SecurityConfig` (URL e método HTTP) e `@PreAuthorize` nos services. `sec:authorize` na tela é só conforto. | o teste `SegurancaIT` chama o service direto como VENDEDOR e é bloqueado |
| D19 | Carrinho | tabela no banco; sessão | sessão (`@SessionScope`): não existe Carrinho no UML; ele só vira Pedido ao finalizar. Guarda só produto, tamanho e quantidade; preço é lido do banco. | o carrinho se perde se a sessão expirar (aceitável) |
| D20 | Concorrência no estoque | sem controle; versão otimista (`@Version`); bloqueio pessimista | bloqueio pessimista (`select ... for update`) nos produtos, sempre na ordem do id (evita deadlock), e no pedido ao mudar status | duas vendas das últimas peças: a segunda espera e é recusada; o estoque nunca fica negativo; cancelamento duplo não devolve duas vezes |
| D21 | Valores de dinheiro | `BigDecimal`; `double` | `double`, como no UML e na planilha; as somas do relatório são arredondadas na tela | diferenças de centavos possíveis nas somas (ver relatório técnico, limitações) |
| D22 | CHECK de listas de texto (gênero, coleção) | CHECK no banco; só no Java | só no `ProdutoValidator`/`ClienteValidator`: o produto 1 reconstruído tem "Não informado", que não é da lista | o banco aceita outros textos se alguém gravar por SQL direto |
| D23 | Lucro por categoria no relatório | ignorar; dividir proporcionalmente | o lucro é do pedido; cada item recebe a parte proporcional ao seu valor | com pedidos de 1 item (todo o legado) o valor é exato |
| D24 | Categorias da loja | lista fixa no HTML; do banco | do banco (`ProdutoService.categorias()`, `group by`) | nova categoria aparece sozinha |
| D25 | Imagens dos produtos | fotos; ilustrações | ilustrações SVG próprias por categoria, pintadas com a cor do produto; uma única foto (Pexels, com crédito) no banner | sem direitos de imagem nem arquivos por produto |
| D26 | Interface | CSS puro; Bootstrap | Bootstrap (exigido pelo guia, autorizado pelo professor) + CSS próprio para a identidade visual | |
| D27 | Telas | uma página por estado; fragments | fragments Thymeleaf (`head`, navegação, alertas, paginação) | mudar o menu é mudar um arquivo |
| D28 | Testes | só manuais; JUnit com H2 em memória | JUnit + Spring Boot Test com perfil `test` (H2 em memória, migração roda a cada execução) | os testes não mexem no banco de desenvolvimento |
