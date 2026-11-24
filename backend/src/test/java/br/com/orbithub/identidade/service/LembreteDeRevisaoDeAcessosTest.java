package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.orbithub.compartilhado.rotinas.RotinaAgendada;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 13:01
 * Cartão Jira: KAN-104
 * Título: Testes do lembrete de revisão de acessos (R16)
 * Objetivo: Conferir que a rotina só lembra quando a revisão está vencida.
 * Decisões: Situação da revisão simulada.
 */
class LembreteDeRevisaoDeAcessosTest {

    static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:01
     * Cartão Jira: KAN-104
     * Título: Em dia e vencida
     * Objetivo: Em dia, nenhum item; vencida, um lembrete.
     * Decisões: Sem erro nos dois casos.
     */
    @Test
    void lembraSoQuandoVencida() {
        EquipeService equipe = mock(EquipeService.class);
        LembreteDeRevisaoDeAcessos rotina = new LembreteDeRevisaoDeAcessos(equipe);
        assertThat(rotina.codigo()).isEqualTo("R16");

        when(equipe.situacaoRevisao()).thenReturn(new EquipeService.SituacaoRevisao(AGORA, "Bia", AGORA.plusSeconds(60),
                false, 3));
        RotinaAgendada.Resultado emDia = rotina.executar(AGORA);
        assertThat(emDia.itens()).isZero();

        when(equipe.situacaoRevisao()).thenReturn(new EquipeService.SituacaoRevisao(null, null, AGORA.minusSeconds(60),
                true, 3));
        RotinaAgendada.Resultado vencida = rotina.executar(AGORA);
        assertThat(vencida.itens()).isEqualTo(1);
        assertThat(vencida.erros()).isZero();
    }
}
