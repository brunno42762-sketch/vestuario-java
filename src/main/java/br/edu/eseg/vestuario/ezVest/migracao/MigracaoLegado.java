package br.edu.eseg.vestuario.ezVest.migracao;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.FormaPagamento;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.model.Tamanho;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * RF08 – Migração dos dados da planilha legada para o banco (Entrega 1, seção 6.5).
 *
 * Roda quando a aplicação inicia, mas só se o banco estiver vazio (o H2 é em arquivo,
 * então os dados ficam gravados e a carga acontece uma vez só).
 *
 * As abas da planilha foram exportadas para CSV (UTF-8, separador ";") em
 * src/main/resources/legado/. Ler o .xlsx direto exigiria a biblioteca Apache POI,
 * que é uma dependência nova.
 *
 * Os registros são gravados com JdbcTemplate (e não pelos repositories) para manter
 * os ids do legado: com GenerationType.IDENTITY o JPA sempre gera um id novo.
 *
 * Tudo roda numa transação só: se a conferência de totais no final falhar,
 * nada fica gravado.
 */
@Component
@Order(1) // roda antes da CargaAutomatica (@Order 2)
public class MigracaoLegado implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoLegado.class);
    private static final String NAO_INFORMADO = "Não informado";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;

    public MigracaoLegado(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transacao = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        if (!bancoVazio()) {
            log.info("Migração do legado não executada: o banco já tem dados.");
            return;
        }
        log.info("Banco vazio: iniciando a migração da planilha legada...");
        transacao.executeWithoutResult(status -> migrar());
    }

    private boolean bancoVazio() {
        for (String tabela : List.of("produto", "cliente", "entrega", "pedido", "item_pedido")) {
            Long total = jdbc.queryForObject("select count(*) from " + tabela, Long.class);
            if (total != null && total > 0) {
                return false;
            }
        }
        return true;
    }

    // =====================================================================
    // Migração
    // =====================================================================

    private void migrar() {
        List<Map<String, String>> produtos = lerCsv("produtos.csv");
        List<Map<String, String>> estoque = lerCsv("estoque.csv");
        List<Map<String, String>> clientes = lerCsv("clientes.csv");
        List<Map<String, String>> vendas = lerCsv("vendas.csv");

        int produtosReconstruidos = migrarProdutos(produtos, estoque, vendas);
        int clientesReconstruidos = migrarClientes(clientes, vendas);
        migrarEntregas();
        migrarPedidosEItens(vendas);
        reiniciarContadoresDeId();

        conferirTotais(produtos.size() + produtosReconstruidos,
                clientes.size() + clientesReconstruidos, vendas);
    }

    /** Passo 1: produtos + aba Estoque (juntadas pelo id_produto) + produtos órfãos das vendas. */
    private int migrarProdutos(List<Map<String, String>> produtos, List<Map<String, String>> estoque,
                               List<Map<String, String>> vendas) {
        Map<Integer, Map<String, String>> estoquePorId = indexar(estoque, "id_produto");

        String sql = "insert into produto (id_produto, categoria, subcategoria, cor, marca, preco_unitario,"
                + " custo_unitario, colecao, genero, estoque_atual, estoque_minimo, fornecedor,"
                + " tempo_reposicao_dias) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> linhas = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (Map<String, String> p : produtos) {
            int id = inteiro(p, "id_produto");
            Map<String, String> e = estoquePorId.get(id);
            if (e == null) {
                throw new IllegalStateException("Produto " + id + " não tem linha na aba Estoque.");
            }
            ids.add(id);
            linhas.add(new Object[]{id, p.get("categoria"), p.get("subcategoria"), p.get("cor"),
                    p.get("marca"), decimal(p, "preco_unitario"), decimal(p, "custo_unitario"),
                    p.get("colecao"), p.get("genero"), inteiro(e, "estoque_atual"),
                    inteiro(e, "estoque_minimo"), e.get("fornecedor"), inteiro(e, "tempo_reposicao_dias")});
        }

        // P02: ids usados nas vendas que não existem na aba Produtos (no legado, só o id 1)
        Map<Integer, List<Map<String, String>>> orfas = agruparOrfaos(vendas, "id_produto", ids);
        for (var entrada : orfas.entrySet()) {
            int id = entrada.getKey();
            Map<String, String> v = entrada.getValue().get(0);
            // custo = (valor_total - lucro_estimado) / quantidade; confere em todas as vendas
            double custo = custoReconstruido(id, entrada.getValue());
            linhas.add(new Object[]{id, v.get("categoria"), v.get("subcategoria"), NAO_INFORMADO,
                    v.get("marca"), decimal(v, "preco_unitario"), custo, NAO_INFORMADO, NAO_INFORMADO,
                    0, 0, NAO_INFORMADO, 0});
            log.warn("Produto {} reconstruído a partir de {} vendas (não existe na aba Produtos).",
                    id, entrada.getValue().size());
        }

        jdbc.batchUpdate(sql, linhas);
        log.info("Produtos migrados: {} ({} reconstruídos).", linhas.size(), orfas.size());
        return orfas.size();
    }

    private double custoReconstruido(int idProduto, List<Map<String, String>> vendas) {
        double custo = -1;
        for (Map<String, String> v : vendas) {
            double c = arredondar((decimal(v, "valor_total") - decimal(v, "lucro_estimado"))
                    / inteiro(v, "quantidade"));
            if (custo >= 0 && Math.abs(c - custo) > 0.01) {
                throw new IllegalStateException("Produto " + idProduto
                        + ": as vendas dão custos diferentes (" + custo + " e " + c + ").");
            }
            custo = c;
        }
        return custo;
    }

    /** Passo 2: clientes + clientes órfãos das vendas. */
    private int migrarClientes(List<Map<String, String>> clientes, List<Map<String, String>> vendas) {
        String sql = "insert into cliente (id_cliente, faixa_etaria, genero, cidade, estado,"
                + " canal_preferido, cliente_fidelidade) values (?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> linhas = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        for (Map<String, String> c : clientes) {
            int id = inteiro(c, "id_cliente");
            ids.add(id);
            linhas.add(new Object[]{id, c.get("faixa_etaria"), c.get("genero"), c.get("cidade"),
                    c.get("estado"), porNome(Canal.values(), Canal::getNome, c.get("canal_preferido")).name(),
                    simNao(c.get("cliente_fidelidade"))});
        }

        // P02: no legado, só o cliente 1. Cidade e estado vêm da entrega das vendas dele.
        Map<Integer, List<Map<String, String>>> orfaos = agruparOrfaos(vendas, "id_cliente", ids);
        for (var entrada : orfaos.entrySet()) {
            Map<String, String> v = entrada.getValue().get(0);
            linhas.add(new Object[]{entrada.getKey(), NAO_INFORMADO, NAO_INFORMADO,
                    v.get("cidade_entrega"), v.get("estado_entrega"), null, false});
            log.warn("Cliente {} reconstruído a partir de {} vendas (não existe na aba Clientes).",
                    entrada.getKey(), entrada.getValue().size());
        }

        jdbc.batchUpdate(sql, linhas);
        log.info("Clientes migrados: {} ({} reconstruídos).", linhas.size(), orfaos.size());
        return orfaos.size();
    }

    /** Passo 3: uma entrega por cliente, com a cidade e o estado do cliente (iguais aos das vendas). */
    private void migrarEntregas() {
        int total = jdbc.update("insert into entrega (estado_entrega, cidade_entrega, id_cliente)"
                + " select estado, cidade, id_cliente from cliente order by id_cliente");
        log.info("Entregas criadas: {}.", total);
    }

    /** Passo 4: cada linha de Vendas vira 1 pedido (mesmo id) + 1 item. */
    private void migrarPedidosEItens(List<Map<String, String>> vendas) {
        String sqlPedido = "insert into pedido (id_pedido, data_pedido, valor_total, lucro_estimado,"
                + " canal_venda, forma_pagamento, status_pedido, id_cliente) values (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "insert into item_pedido (preco_unitario, quantidade, desconto_percentual,"
                + " tamanho, id_pedido, id_produto) values (?, ?, ?, ?, ?, ?)";

        List<Object[]> pedidos = new ArrayList<>();
        List<Object[]> itens = new ArrayList<>();
        for (Map<String, String> v : vendas) {
            int idPedido = inteiro(v, "id_pedido");
            try {
                pedidos.add(new Object[]{idPedido, Date.valueOf(LocalDate.parse(v.get("data_pedido"))),
                        decimal(v, "valor_total"), decimal(v, "lucro_estimado"),
                        porNome(Canal.values(), Canal::getNome, v.get("canal_venda")).name(),
                        porNome(FormaPagamento.values(), FormaPagamento::getNome, v.get("forma_pagamento")).name(),
                        porNome(Status.values(), Status::getNome, v.get("status_pedido")).name(),
                        inteiro(v, "id_cliente")});
                itens.add(new Object[]{decimal(v, "preco_unitario"), inteiro(v, "quantidade"),
                        decimal(v, "desconto_percentual"), Tamanho.valueOf(v.get("tamanho")).name(),
                        idPedido, inteiro(v, "id_produto")});
            } catch (IllegalArgumentException e) {
                // diz em qual venda está o problema (a transação é desfeita)
                throw new IllegalStateException("Venda " + idPedido + ": " + e.getMessage(), e);
            }
        }

        jdbc.batchUpdate(sqlPedido, pedidos);
        jdbc.batchUpdate(sqlItem, itens);
        log.info("Pedidos migrados: {}. Itens migrados: {}.", pedidos.size(), itens.size());
    }

    /**
     * Os ids foram gravados à mão; o contador do banco ainda está em 1. Sem este passo,
     * o próximo produto cadastrado pela tela tentaria usar um id que já existe.
     */
    private void reiniciarContadoresDeId() {
        reiniciar("produto", "id_produto");
        reiniciar("cliente", "id_cliente");
        reiniciar("pedido", "id_pedido");
    }

    private void reiniciar(String tabela, String coluna) {
        Integer maior = jdbc.queryForObject("select coalesce(max(" + coluna + "), 0) from " + tabela, Integer.class);
        int proximo = (maior == null ? 0 : maior) + 1;
        jdbc.execute("alter table " + tabela + " alter column " + coluna + " restart with " + proximo);
    }

    // =====================================================================
    // Conferência de totais (RF08)
    // =====================================================================

    private void conferirTotais(int produtosEsperados, int clientesEsperados, List<Map<String, String>> vendas) {
        double receitaPlanilha = arredondar(vendas.stream().mapToDouble(v -> decimal(v, "valor_total")).sum());
        double lucroPlanilha = arredondar(vendas.stream().mapToDouble(v -> decimal(v, "lucro_estimado")).sum());

        List<String> erros = new ArrayList<>();
        conferir(erros, "produtos", contar("select count(*) from produto"), produtosEsperados);
        conferir(erros, "clientes", contar("select count(*) from cliente"), clientesEsperados);
        conferir(erros, "entregas", contar("select count(*) from entrega"), clientesEsperados);
        conferir(erros, "pedidos", contar("select count(*) from pedido"), vendas.size());
        conferir(erros, "itens", contar("select count(*) from item_pedido"), vendas.size());
        conferir(erros, "pedidos sem cliente", contar("select count(*) from pedido p"
                + " left join cliente c on c.id_cliente = p.id_cliente where c.id_cliente is null"), 0);
        conferir(erros, "itens sem produto", contar("select count(*) from item_pedido i"
                + " left join produto p on p.id_produto = i.id_produto where p.id_produto is null"), 0);

        double receitaBanco = arredondar(soma("select sum(valor_total) from pedido"));
        double lucroBanco = arredondar(soma("select sum(lucro_estimado) from pedido"));
        if (Math.abs(receitaBanco - receitaPlanilha) > 0.01) {
            erros.add("receita: banco " + receitaBanco + ", planilha " + receitaPlanilha);
        }
        if (Math.abs(lucroBanco - lucroPlanilha) > 0.01) {
            erros.add("lucro: banco " + lucroBanco + ", planilha " + lucroPlanilha);
        }

        if (!erros.isEmpty()) {
            // a exceção desfaz a transação: nada da migração fica gravado
            throw new IllegalStateException("Conferência da migração falhou: " + String.join("; ", erros));
        }

        double receitaSemCancelados = arredondar(soma(
                "select sum(valor_total) from pedido where status_pedido <> 'CANCELADO'"));
        log.info("Conferência OK: {} produtos, {} clientes, {} pedidos, nenhum pedido órfão.",
                produtosEsperados, clientesEsperados, vendas.size());
        log.info("Receita: R$ {} (igual à planilha). Lucro estimado: R$ {}. Receita sem cancelados: R$ {}.",
                String.format(Locale.ROOT, "%.2f", receitaBanco),
                String.format(Locale.ROOT, "%.2f", lucroBanco),
                String.format(Locale.ROOT, "%.2f", receitaSemCancelados));
    }

    private void conferir(List<String> erros, String item, long noBanco, long esperado) {
        if (noBanco != esperado) {
            erros.add(item + ": banco " + noBanco + ", esperado " + esperado);
        }
    }

    private long contar(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    private double soma(String sql) {
        Double d = jdbc.queryForObject(sql, Double.class);
        return d == null ? 0 : d;
    }

    // =====================================================================
    // Leitura e conversão dos valores do CSV
    // =====================================================================

    /** Lê um CSV de resources/legado: primeira linha = nomes das colunas, separador ";". */
    static List<Map<String, String>> lerCsv(String arquivo) {
        var recurso = new ClassPathResource("legado/" + arquivo);
        try (var leitor = new BufferedReader(new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            String cabecalho = leitor.readLine();
            if (cabecalho == null) {
                throw new IllegalStateException(arquivo + " está vazio.");
            }
            String[] colunas = cabecalho.replace("﻿", "").split(";", -1); // tira o BOM, se houver
            List<Map<String, String>> linhas = new ArrayList<>();
            String linha;
            int numero = 1;
            while ((linha = leitor.readLine()) != null) {
                numero++;
                if (linha.isBlank()) continue;
                String[] valores = linha.split(";", -1);
                if (valores.length != colunas.length) {
                    throw new IllegalStateException(arquivo + ", linha " + numero + ": esperava "
                            + colunas.length + " colunas e encontrou " + valores.length + ".");
                }
                Map<String, String> registro = new LinkedHashMap<>();
                for (int i = 0; i < colunas.length; i++) {
                    registro.put(colunas[i].trim(), valores[i].trim());
                }
                linhas.add(registro);
            }
            return linhas;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler legado/" + arquivo, e);
        }
    }

    /** Converte o texto do legado (ex.: "Loja Física") no valor do enum pelo nome de exibição. */
    static <E extends Enum<E>> E porNome(E[] valores, Function<E, String> nome, String texto) {
        for (E v : valores) {
            if (nome.apply(v).equalsIgnoreCase(texto)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Valor desconhecido no legado: \"" + texto + "\".");
    }

    static boolean simNao(String texto) {
        return switch (texto) {
            case "Sim" -> true;
            case "Não" -> false;
            default -> throw new IllegalArgumentException("Fidelidade deveria ser Sim ou Não: \"" + texto + "\".");
        };
    }

    private static Map<Integer, Map<String, String>> indexar(List<Map<String, String>> linhas, String coluna) {
        Map<Integer, Map<String, String>> mapa = new HashMap<>();
        for (Map<String, String> l : linhas) {
            mapa.put(inteiro(l, coluna), l);
        }
        return mapa;
    }

    /** Vendas cujo id (de produto ou de cliente) não existe na aba de origem, agrupadas por id. */
    private static Map<Integer, List<Map<String, String>>> agruparOrfaos(
            List<Map<String, String>> vendas, String coluna, Set<Integer> idsExistentes) {
        Map<Integer, List<Map<String, String>>> orfaos = new TreeMap<>();
        for (Map<String, String> v : vendas) {
            int id = inteiro(v, coluna);
            if (!idsExistentes.contains(id)) {
                orfaos.computeIfAbsent(id, k -> new ArrayList<>()).add(v);
            }
        }
        return orfaos;
    }

    private static int inteiro(Map<String, String> linha, String coluna) {
        return Integer.parseInt(linha.get(coluna));
    }

    private static double decimal(Map<String, String> linha, String coluna) {
        return Double.parseDouble(linha.get(coluna));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
