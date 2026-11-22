package br.edu.eseg.vestuario.ezVest.config;

import br.edu.eseg.vestuario.ezVest.model.Status;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ajustes no esquema que o ddl-auto=update do Hibernate NÃO faz em bancos que já existem:
 *   1. acrescentar valores novos de enum às colunas (ex.: o status CRIADO);
 *   2. criar as restrições CHECK (RNF01), a última barreira contra dados inválidos, mesmo
 *      que alguém grave direto no banco, sem passar pelas validações do Java.
 *
 * Sobre o item 1:
 * Por quê: no H2, o Hibernate cria as colunas @Enumerated(STRING) com o tipo nativo
 * ENUM('CANCELADO','CONCLUIDO',...), que só aceita os valores que existiam na criação.
 * O ddl-auto=update cria tabelas e colunas novas, mas NÃO altera o tipo de uma coluna.
 * Sem este ajuste, gravar um pedido CRIADO num banco antigo daria erro.
 *
 * Roda uma vez na inicialização, depois do Hibernate (recebe o EntityManagerFactory)
 * e antes de o servidor começar a atender. Num banco novo, ou já ajustado, não faz nada.
 */
@Component
public class AjusteEsquemaBanco implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AjusteEsquemaBanco.class);

    private final JdbcTemplate jdbc;

    // O EntityManagerFactory só está aqui para este ajuste rodar DEPOIS do Hibernate criar as tabelas
    public AjusteEsquemaBanco(DataSource dataSource, EntityManagerFactory hibernateJaIniciado) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * Restrições CHECK (Entrega 1, seção 6.4): nome, tabela e condição. As mesmas regras dos validadores do Java.
     * O gênero do produto ficou sem CHECK: o produto 1, reconstruído na migração (decisão D12),
     * tem "Não informado", e a restrição impediria a carga da planilha num banco novo.
     */
    private static final String[][] RESTRICOES = {
            {"ck_produto_preco", "PRODUTO", "preco_unitario > 0"},
            {"ck_produto_custo", "PRODUTO", "custo_unitario >= 0 and custo_unitario <= preco_unitario"},
            {"ck_produto_estoque", "PRODUTO", "estoque_atual >= 0 and estoque_minimo >= 0"},
            {"ck_produto_reposicao", "PRODUTO", "tempo_reposicao_dias >= 0"},
            {"ck_item_quantidade", "ITEM_PEDIDO", "quantidade > 0"},
            {"ck_item_preco", "ITEM_PEDIDO", "preco_unitario >= 0"},
            // Entrega 1, seção 6.4: o legado só usa 0%, 5%, 10% e 15%
            {"ck_item_desconto", "ITEM_PEDIDO", "desconto_percentual >= 0 and desconto_percentual <= 0.15"},
            {"ck_pedido_valor", "PEDIDO", "valor_total >= 0"},
    };

    @Override
    public void afterPropertiesSet() {
        ajustarEnum("PEDIDO", "STATUS_PEDIDO", Status.class);
        for (String[] r : RESTRICOES) {
            criarRestricao(r[0], r[1], r[2]);
        }
    }

    /**
     * Cria a restrição se ela ainda não existir. Se algum dado antigo já violar a regra,
     * não cria (o banco recusaria) e avisa no log, em vez de impedir o sistema de subir.
     */
    private void criarRestricao(String nome, String tabela, String condicao) {
        Integer existe = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints"
                        + " where constraint_schema = 'APP' and constraint_name = ?",
                Integer.class, nome.toUpperCase());
        if (existe != null && existe > 0) {
            return;
        }
        Integer violando = jdbc.queryForObject(
                "select count(*) from APP." + tabela + " where not (" + condicao + ")", Integer.class);
        if (violando != null && violando > 0) {
            log.warn("Restrição {} não criada: {} linha(s) de {} não atendem a \"{}\".", nome, violando, tabela, condicao);
            return;
        }
        jdbc.execute("alter table APP." + tabela + " add constraint " + nome + " check (" + condicao + ")");
        log.info("Restrição {} criada em {}: {}.", nome, tabela, condicao);
    }

    private void ajustarEnum(String tabela, String coluna, Class<? extends Enum<?>> tipo) {
        // Valores que a coluna aceita hoje (vazio se a coluna não for ENUM ou não existir)
        List<String> atuais = jdbc.queryForList(
                "select v.value_name from information_schema.columns c"
                        + " join information_schema.enum_values v on v.object_schema = c.table_schema"
                        + "  and v.object_name = c.table_name and v.object_type = 'TABLE'"
                        + "  and v.enum_identifier = c.dtd_identifier"
                        + " where c.table_schema = 'APP' and c.table_name = ? and c.column_name = ?"
                        + " order by v.value_ordinal",
                String.class, tabela, coluna);

        // Mesma ordem que o Hibernate usa ao criar a coluna: alfabética
        List<String> esperados = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).sorted().toList();
        if (atuais.isEmpty() || atuais.containsAll(esperados)) {
            return; // nada a fazer
        }

        String valores = esperados.stream().map(v -> "'" + v + "'").collect(Collectors.joining(", "));
        jdbc.execute("alter table APP." + tabela + " alter column " + coluna
                + " set data type enum(" + valores + ")");
        log.info("Coluna {}.{} ajustada para aceitar {} (antes: {}).", tabela, coluna, esperados, atuais);
    }
}
