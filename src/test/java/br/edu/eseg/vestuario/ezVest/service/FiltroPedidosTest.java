package br.edu.eseg.vestuario.ezVest.service;

import br.edu.eseg.vestuario.ezVest.model.Canal;
import br.edu.eseg.vestuario.ezVest.model.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Filtros da lista de pedidos (RF08). */
class FiltroPedidosTest {

    @Test
    void periodoInvertidoEhDetectado() {
        var f = new FiltroPedidos(null, null, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 1, 1), null);
        assertThat(f.isPeriodoInvalido()).isTrue();
    }

    @Test
    void mesmoDiaEPeriodoAbertoSaoValidos() {
        LocalDate dia = LocalDate.of(2025, 1, 1);
        assertThat(new FiltroPedidos(null, null, dia, dia, null).isPeriodoInvalido()).isFalse();
        assertThat(new FiltroPedidos(null, null, dia, null, null).isPeriodoInvalido()).isFalse();
    }

    @Test
    void parametrosSoComOsFiltrosPreenchidos() {
        var f = new FiltroPedidos(Status.ENTREGUE, Canal.FISICA, LocalDate.of(2025, 1, 31), null, 103);
        assertThat(f.comoParametros()).containsExactly(
                java.util.Map.entry("status", "ENTREGUE"),
                java.util.Map.entry("canal", "FISICA"),
                java.util.Map.entry("de", "2025-01-31"),
                java.util.Map.entry("cliente", 103));
        assertThat(FiltroPedidos.nenhum().comoParametros()).isEmpty();
    }
}
