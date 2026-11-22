package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Status;
import br.edu.eseg.vestuario.ezVest.repository.ItemPedidoRepository;
import br.edu.eseg.vestuario.ezVest.repository.PedidoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Relatório de receita e lucro (RF09, prometido na Entrega 1): por mês, canal e categoria,
 * sempre SEM os pedidos cancelados. Só o ADMIN vê (mostra lucro e margem).
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioService {

    private static final String[] MESES =
            {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};

    /** Uma linha do relatório. quantidade = pedidos (mês, canal) ou peças (categoria). */
    public record Linha(String rotulo, long quantidade, double receita, double lucro) {
        public double getMargem() {
            return receita == 0 ? 0 : lucro / receita * 100;
        }
    }

    /** Uma tabela do relatório, com o maior valor (para o tamanho das barras). */
    public record Tabela(List<Linha> linhas) {
        public double getMaiorReceita() {
            return linhas.stream().mapToDouble(Linha::receita).max().orElse(0);
        }

        /** Largura da barra de uma linha, em % da maior receita. */
        public double largura(Linha l) {
            double maior = getMaiorReceita();
            return maior == 0 ? 0 : l.receita() / maior * 100;
        }
    }

    public record Relatorio(Linha total, Tabela porMes, Tabela porCanal, Tabela porCategoria) {
    }

    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public RelatorioService(PedidoRepository pedidoRepository, ItemPedidoRepository itemPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    /** @param de/ate período opcional (null = sem limite) */
    @Transactional(readOnly = true)
    public Relatorio gerar(LocalDate de, LocalDate ate) {
        List<Linha> meses = pedidoRepository.receitaPorMes(Status.CANCELADO, de, ate).stream()
                .map(r -> new Linha(MESES[numero(r[1]) - 1] + "/" + numero(r[0]),
                        numero(r[2]), valor(r[3]), valor(r[4])))
                .toList();

        List<Linha> canais = pedidoRepository.receitaPorCanal(Status.CANCELADO, de, ate).stream()
                .map(r -> new Linha(((Canal) r[0]).getNome(), numero(r[1]), valor(r[2]), valor(r[3])))
                .toList();

        List<Linha> categorias = itemPedidoRepository.receitaPorCategoria(Status.CANCELADO, de, ate).stream()
                .map(r -> new Linha((String) r[0], numero(r[1]), valor(r[2]), valor(r[3])))
                .toList();

        // O total vem dos canais: todo pedido tem exatamente um canal
        Linha total = new Linha("Total",
                canais.stream().mapToLong(Linha::quantidade).sum(),
                arredondar(canais.stream().mapToDouble(Linha::receita).sum()),
                arredondar(canais.stream().mapToDouble(Linha::lucro).sum()));

        return new Relatorio(total, new Tabela(meses), new Tabela(canais), new Tabela(categorias));
    }

    private static int numero(Object o) {
        return ((Number) o).intValue();
    }

    private static double valor(Object o) {
        return o == null ? 0 : arredondar(((Number) o).doubleValue());
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
