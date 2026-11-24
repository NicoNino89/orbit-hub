package br.com.orbithub.compartilhado.rotinas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Testes da regra de atraso das rotinas
 * Objetivo: Garantir que a rotina só é dada como atrasada depois de duas vezes o intervalo sem execução, contando
 *           também a subida da API.
 * Decisões: Rotina diária e rotina de hora em hora.
 */
class AgendadorDeRotinasTest {

    static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina diária
     * Objetivo: Conferir os limites de atraso de uma rotina diária.
     * Decisões: Duas vezes um dia = 48 horas.
     */
    @Test
    void diariaAtrasaDepoisDeDoisDias() {
        Instant subidaAntiga = AGORA.minus(Duration.ofDays(10));
        assertThat(AgendadorDeRotinas.atrasada("0 0 3 * * *", AGORA, subidaAntiga, AGORA.minus(Duration.ofHours(30))))
                .isFalse();
        assertThat(AgendadorDeRotinas.atrasada("0 0 3 * * *", AGORA, subidaAntiga, AGORA.minus(Duration.ofHours(49))))
                .isTrue();
        assertThat(AgendadorDeRotinas.atrasada("0 0 3 * * *", AGORA, subidaAntiga, null)).isTrue();
        assertThat(AgendadorDeRotinas.atrasada("0 0 3 * * *", AGORA, AGORA.minus(Duration.ofHours(5)), null)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina de hora em hora
     * Objetivo: Conferir os limites de atraso de uma rotina horária e a ordem dos códigos.
     * Decisões: Duas vezes uma hora = 2 horas.
     */
    @Test
    void horariaAtrasaDepoisDeDuasHoras() {
        Instant subida = AGORA.minus(Duration.ofDays(1));
        assertThat(AgendadorDeRotinas.atrasada("0 5 * * * *", AGORA, subida, AGORA.minus(Duration.ofMinutes(90))))
                .isFalse();
        assertThat(AgendadorDeRotinas.atrasada("0 5 * * * *", AGORA, subida, AGORA.minus(Duration.ofMinutes(130))))
                .isTrue();
        assertThat(ExecutorDeRotinas.ordem("R2")).isLessThan(ExecutorDeRotinas.ordem("R10"));
        assertThat(ExecutorDeRotinas.ordem("X")).isEqualTo(Integer.MAX_VALUE);
    }
}
