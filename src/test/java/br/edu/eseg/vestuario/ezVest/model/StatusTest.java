package br.edu.eseg.vestuario.ezVest.model;

import br.edu.eseg.vestuario.ezVest.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de transição do status do pedido (Entrega 1, seção 4.3, mais o CRIADO). */
class StatusTest {

    @ParameterizedTest(name = "{0} -> {1} é permitido")
    @CsvSource({
            "CRIADO, ENTREGUE", "CRIADO, CANCELADO",
            "ENTREGUE, CONCLUIDO", "ENTREGUE, TROCA", "ENTREGUE, CANCELADO"
    })
    void transicoesPermitidas(Status de, Status para) {
        assertThat(de.podeMudarPara(para)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} é bloqueado")
    @CsvSource({
            "CRIADO, CONCLUIDO", "CRIADO, TROCA", "CRIADO, CRIADO",
            "ENTREGUE, CRIADO", "ENTREGUE, ENTREGUE",
            "CONCLUIDO, TROCA", "TROCA, CONCLUIDO", "CANCELADO, ENTREGUE", "CANCELADO, CRIADO"
    })
    void transicoesBloqueadas(Status de, Status para) {
        assertThat(de.podeMudarPara(para)).isFalse();
    }

    @ParameterizedTest(name = "{0} é final: não muda para nenhum status")
    @EnumSource(value = Status.class, names = {"CONCLUIDO", "TROCA", "CANCELADO"})
    void statusFinais(Status status) {
        assertThat(status.isFinal()).isTrue();
        for (Status outro : Status.values()) {
            assertThat(status.podeMudarPara(outro)).isFalse();
        }
    }

    @Test
    @DisplayName("CRIADO e ENTREGUE não são finais")
    void naoFinais() {
        assertThat(Status.CRIADO.isFinal()).isFalse();
        assertThat(Status.ENTREGUE.isFinal()).isFalse();
    }

    @Test
    @DisplayName("Pedido.alterarStatus aplica a regra e lança erro na transição inválida")
    void pedidoAplicaARegra() {
        Pedido pedido = new Pedido();
        pedido.setStatusPedido(Status.CRIADO);

        pedido.alterarStatus(Status.ENTREGUE);
        assertThat(pedido.getStatusPedido()).isEqualTo(Status.ENTREGUE);

        assertThatThrownBy(() -> pedido.alterarStatus(Status.CRIADO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(pedido.getStatusPedido()).isEqualTo(Status.ENTREGUE); // não mudou
    }
}
