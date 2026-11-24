package br.com.orbithub.compartilhado.rotinas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 13:01
 * Cartão Jira: KAN-104
 * Título: Testes do agendador e do vigia das rotinas
 * Objetivo: Conferir que cada rotina é agendada (mais o vigia) e que o vigia marca como atrasada só a rotina ativa
 *           que passou do prazo, na métrica orbit.rotina.atrasada.
 * Decisões: Executor e registro de execuções simulados; relógio ajustável para passar do prazo.
 */
class VigiaDeRotinasTest {

    static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    ExecutorDeRotinas executor;
    ExecucaoDeRotinaRepository execucoes;
    SimpleMeterRegistry metricas;
    RelogioAjustavel relogio;
    AgendadorDeRotinas agendador;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:01
     * Cartão Jira: KAN-104
     * Título: Preparação
     * Objetivo: Montar o agendador com três rotinas diárias: uma sem execução, uma pausada e uma em dia.
     * Decisões: Ordem fixa das rotinas.
     */
    @BeforeEach
    void preparar() {
        executor = mock(ExecutorDeRotinas.class);
        execucoes = mock(ExecucaoDeRotinaRepository.class);
        metricas = new SimpleMeterRegistry();
        relogio = new RelogioAjustavel(AGORA);
        Map<String, RotinaAgendada> rotinas = new LinkedHashMap<>();
        for (String codigo : new String[] {"R1", "R2", "R3"}) {
            RotinaAgendada rotina = mock(RotinaAgendada.class);
            when(rotina.cron()).thenReturn("0 0 3 * * *");
            rotinas.put(codigo, rotina);
        }
        when(executor.rotinas()).thenReturn(rotinas);
        when(executor.pausada("R2")).thenReturn(true);
        agendador = new AgendadorDeRotinas(executor, execucoes, metricas, relogio);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:01
     * Cartão Jira: KAN-104
     * Título: Agendamento
     * Objetivo: Conferir uma tarefa por rotina mais o vigia, e que a tarefa dispara a execução agendada da rotina.
     * Decisões: Roda a primeira tarefa à mão.
     */
    @Test
    void agendaCadaRotinaEOVigia() {
        ScheduledTaskRegistrar registro = new ScheduledTaskRegistrar();
        agendador.configureTasks(registro);
        assertThat(registro.getTriggerTaskList()).hasSize(4);
        registro.getTriggerTaskList().get(0).getRunnable().run();
        verify(executor).executarAgendada("R1");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:01
     * Cartão Jira: KAN-104
     * Título: Vigia marca a atrasada
     * Objetivo: Três dias depois da subida, só a rotina ativa sem execução recente fica marcada como atrasada.
     * Decisões: A pausada nunca conta como atrasada.
     */
    @Test
    void vigiaMarcaSoAAtrasada() {
        relogio.avancar(Duration.ofDays(3));
        ExecucaoDeRotina recente = mock(ExecucaoDeRotina.class);
        when(recente.getIniciadaEm()).thenReturn(relogio.instant().minus(Duration.ofHours(12)));
        when(execucoes.findFirstByRotinaAndDisparoOrderByIniciadaEmDesc("R1", ExecutorDeRotinas.AUTOMATICO))
                .thenReturn(Optional.empty());
        when(execucoes.findFirstByRotinaAndDisparoOrderByIniciadaEmDesc("R2", ExecutorDeRotinas.AUTOMATICO))
                .thenReturn(Optional.empty());
        when(execucoes.findFirstByRotinaAndDisparoOrderByIniciadaEmDesc("R3", ExecutorDeRotinas.AUTOMATICO))
                .thenReturn(Optional.of(recente));

        agendador.vigiar();

        assertThat(atrasada("R1")).isEqualTo(1.0);
        assertThat(atrasada("R2")).isZero();
        assertThat(atrasada("R3")).isZero();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:01
     * Cartão Jira: KAN-104
     * Título: Valor da métrica
     * Objetivo: Ler a métrica de atraso de uma rotina.
     * Decisões: 1 = atrasada.
     */
    private double atrasada(String codigo) {
        return metricas.get("orbit.rotina.atrasada").tag("rotina", codigo).gauge().value();
    }
}
