# ezVest – modernização do sistema de uma loja de roupas

Projeto individual de Programação Web (ESEG, Engenharia de Computação, 2º semestre de 2026).
O sistema legado era uma planilha com 4 abas (Produtos, Estoque, Clientes, Vendas) e 5.999 vendas de
2024 e 2025. O ezVest leva esses dados para um banco relacional e oferece uma loja on-line com
carrinho, área da equipe (ADMIN e VENDEDOR), área do cliente e relatório de receita e lucro.

## Tecnologias

| | |
|---|---|
| Linguagem | Java (o `pom.xml` define `java.version` 26) |
| Framework | Spring Boot 4.1.2: Spring MVC, Spring Data JPA, Spring Security |
| Persistência | JPA/Hibernate 7, banco **H2 em arquivo** (`./database/ezVestData`), schema `APP` |
| Interface | Thymeleaf (+ `thymeleaf-extras-springsecurity6`), HTML, CSS próprio, Bootstrap |
| Testes | JUnit, Spring Boot Test, Spring Security Test, Mockito, AssertJ |
| Build | Maven (wrapper incluído: `mvnw` / `mvnw.cmd`) |

## Pré-requisitos

- **JDK 26** (ou altere `<java.version>` no `pom.xml` para a versão do seu JDK, 21 ou mais).
- Nada mais: o Maven vem pelo wrapper e o H2 é embutido. Não é preciso instalar banco de dados.

## Configuração: variáveis de ambiente

O primeiro usuário ADMIN é criado a partir de variáveis de ambiente. **Nenhuma senha fica no código.**

| Variável | Exemplo | Regra |
|---|---|---|
| `APP_ADMIN_USER` | `admin` | nome do primeiro ADMIN |
| `APP_ADMIN_PASSWORD` | (escolha a sua) | pelo menos 8 caracteres |

Elas só são usadas se ainda não existir nenhum ADMIN no banco.

**IntelliJ:** *Run → Edit Configurations… → EzVestApplication → Environment variables*:
`APP_ADMIN_USER=admin;APP_ADMIN_PASSWORD=<sua senha>`

**Terminal (Windows PowerShell):**
```powershell
$env:APP_ADMIN_USER="admin"; $env:APP_ADMIN_PASSWORD="<sua senha>"
.\mvnw.cmd spring-boot:run
```

**Terminal (Linux/macOS):**
```bash
APP_ADMIN_USER=admin APP_ADMIN_PASSWORD='<sua senha>' ./mvnw spring-boot:run
```

Abra http://localhost:8080.

## O que acontece na primeira execução

1. O Hibernate cria as tabelas (`ddl-auto=update`).
2. `AjusteEsquemaBanco` cria as restrições CHECK (e atualiza bancos de entregas anteriores).
3. `MigracaoLegado` carrega a planilha (CSV em `src/main/resources/legado/`) **se o banco estiver
   vazio**: 126 produtos, 800 clientes, 800 entregas, 5.999 pedidos e 5.999 itens, e confere os totais.
4. `CargaAutomatica` cria o ADMIN das variáveis de ambiente.

Para recomeçar do zero, pare a aplicação e apague a pasta `database/`.

## Perfis e primeiros passos

| Perfil | Pode |
|---|---|
| Visitante | ver a loja, montar o carrinho, criar conta |
| CLIENTE | comprar, ver e cancelar os próprios pedidos ainda "Criado" |
| VENDEDOR | consultar a área administrativa, registrar vendas, alterar status, cadastrar clientes |
| ADMIN | tudo do vendedor + produtos, exclusões, usuários, relatório, H2 Console |

Depois de entrar como ADMIN: **Painel → Gerenciar usuários** para criar um VENDEDOR. Um CLIENTE
pode ser criado pelo "Criar conta" da loja ou pelo ADMIN, vinculado a um cliente existente (ex.: 103).

**H2 Console** (só ADMIN): http://localhost:8080/h2-console, JDBC URL
`jdbc:h2:file:./database/ezVestData`, usuário `sa`, senha em branco.

## Testes

```bash
./mvnw test        # Windows: mvnw.cmd test
```

Usam um H2 em memória (o banco `database/` não é tocado) e não precisam das variáveis de ambiente.
Resultado e cobertura: [`docs/TESTES.md`](docs/TESTES.md).

## Estrutura

```
src/main/java/br/edu/eseg/vestuario/ezVest/
  model/        entidades JPA e enums (Produto, Cliente, Entrega, Pedido, ItemPedido, Usuario, Status...)
  repository/   Spring Data JPA (consultas JPQL, filtros, relatório, bloqueios)
  service/      regras de negócio, transações, @PreAuthorize, validadores
  carrinho/     carrinho de compras (sessão)
  cadastro/     "Criar conta" pública
  venda/        formulário de venda da equipe
  controller/   rotas HTTP → Model → templates
  config/       Spring Security, H2 Console, ajuste do esquema
  security/     usuário logado e filtro de estado da conta
  migracao/     carga da planilha legada
  carga/        primeiro ADMIN
  exception/    exceções e tratamento de erros
src/main/resources/
  templates/    páginas Thymeleaf e fragments/
  static/       CSS e imagem do banner
  legado/       abas da planilha em CSV
src/test/       testes automatizados
docs/           documentação técnica
UML/            diagrama de classes da Entrega 1 (Astah)
Documentação/   documento da Entrega 1
```

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/RELATORIO_TECNICO.md`](docs/RELATORIO_TECNICO.md) | problemas, soluções, testes, limitações, riscos e melhorias |
| [`docs/MATRIZ_RASTREABILIDADE.md`](docs/MATRIZ_RASTREABILIDADE.md) | requisito → problema → requisito novo → justificativa → implementação → teste |
| [`docs/DECISOES_TECNICAS.md`](docs/DECISOES_TECNICAS.md) | inventário de decisões, alternativas e impactos |
| [`docs/MODELO_RELACIONAL.md`](docs/MODELO_RELACIONAL.md) | modelo legado e evoluído, chaves, restrições, índices, migração |
| [`docs/DIAGRAMA_CLASSES.md`](docs/DIAGRAMA_CLASSES.md) | diagrama de classes, associações JPA, camadas |
| [`docs/TESTES.md`](docs/TESTES.md) | testes, resultados e o que não é coberto |
| [`ENTREGA2.md`](ENTREGA2.md), [`ENTREGA3.md`](ENTREGA3.md), [`ENTREGA4.md`](ENTREGA4.md) | o que foi feito em cada entrega e como demonstrar |
| `Documentação/Entrega1_Vestuario.docx` | diagnóstico do legado e modelagem (Entrega 1) |

## Entregas no Git

Cada entrega é marcada com uma tag: `entrega-1`, `entrega-2`, `entrega-3`, `entrega-4`.

## Créditos

Foto do banner: Godisable Jacob, [Pexels](https://www.pexels.com/photo/908989/). Dados da planilha: fictícios.
