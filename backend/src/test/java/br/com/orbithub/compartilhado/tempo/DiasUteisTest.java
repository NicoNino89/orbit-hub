package br.com.orbithub.compartilhado.tempo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Testes dos dias úteis
 * Objetivo: Conferir a soma de dias úteis pulando o fim de semana, no fuso de Brasília.
 * Decisões: 24/11/2026 é uma terça-feira.
 */
class DiasUteisTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Somar dias úteis
     * Objetivo: Terça + 2 = quinta; sexta + 1 = segunda; sábado + 1 = terça (começa a contar na segunda); zero não muda.
     * Decisões: Horários em Brasília (UTC-3).
     */
    @Test
    void somaPulandoFimDeSemana() {
        assertThat(DiasUteis.somar(Instant.parse("2026-11-24T13:00:00Z"), 2)).isEqualTo(Instant.parse("2026-11-26T13:00:00Z"));
        assertThat(DiasUteis.somar(Instant.parse("2026-11-27T13:00:00Z"), 1)).isEqualTo(Instant.parse("2026-11-30T13:00:00Z"));
        assertThat(DiasUteis.somar(Instant.parse("2026-11-28T18:00:00Z"), 1)).isEqualTo(Instant.parse("2026-12-01T12:00:00Z"));
        assertThat(DiasUteis.somar(Instant.parse("2026-11-24T13:00:00Z"), 0)).isEqualTo(Instant.parse("2026-11-24T13:00:00Z"));
    }
}
