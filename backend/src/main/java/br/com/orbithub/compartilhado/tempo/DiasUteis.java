package br.com.orbithub.compartilhado.tempo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Dias úteis
 * Objetivo: Somar dias úteis a um instante, para os prazos de atendimento dos times (verificação, curadoria, moderação).
 * Decisões: Fuso de Brasília; sábado e domingo não contam. Feriados nacionais ainda não (entram com o painel de
 *           operação, KAN-82): o prazo fica, no máximo, um dia mais curto em semana de feriado.
 */
public final class DiasUteis {

    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Sem instâncias
     * Objetivo: Impedir a criação de objetos desta classe utilitária.
     * Decisões: Construtor privado.
     */
    private DiasUteis() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Somar dias úteis
     * Objetivo: Devolver o instante depois de N dias úteis, mantendo a hora do dia.
     * Decisões: Começando num fim de semana, conta a partir da segunda-feira seguinte.
     */
    public static Instant somar(Instant inicio, long dias) {
        ZonedDateTime data = inicio.atZone(BRASILIA);
        while (fimDeSemana(data)) {
            data = data.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        }
        long restantes = dias;
        while (restantes > 0) {
            data = data.plusDays(1);
            if (!fimDeSemana(data)) {
                restantes--;
            }
        }
        return data.toInstant();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fim de semana
     * Objetivo: Dizer se a data cai no sábado ou no domingo.
     * Decisões: No fuso de Brasília.
     */
    private static boolean fimDeSemana(ZonedDateTime data) {
        return data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
