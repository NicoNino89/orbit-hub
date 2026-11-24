package br.com.orbithub.compartilhado.rotinas;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Agendamento e vigia das rotinas
 * Objetivo: Disparar cada rotina na sua frequência (horário de Brasília) e alertar quando uma rotina deixa de rodar no
 *           horário esperado.
 * Decisões: Uma tarefa agendada por rotina, a partir do cron que ela declara. O vigia roda a cada 15 minutos: rotina
 *           ativa sem execução automática há mais de duas vezes o intervalo dela (contado também desde a subida da
 *           API) está atrasada; vai para o log como ERROR "Rotina atrasada" (alerta do Azure Monitor) e para o
 *           indicador orbit_rotina_atrasada (alerta do Prometheus). Desligável por orbit.rotinas.ligadas, para os
 *           testes não dispararem rotinas sozinhos.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "orbit.rotinas.ligadas", havingValue = "true", matchIfMissing = true)
class AgendadorDeRotinas implements SchedulingConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(AgendadorDeRotinas.class);
    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    static final String VIGIA = "0 */15 * * * *";

    private final ExecutorDeRotinas executor;
    private final ExecucaoDeRotinaRepository execucoes;
    private final Clock relogio;
    private final Instant subida;
    private final Map<String, AtomicInteger> atrasadas = new ConcurrentHashMap<>();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação do agendador
     * Objetivo: Receber o executor, as execuções, as métricas e o relógio, e criar um indicador de atraso por rotina.
     * Decisões: O indicador começa em 0 (em dia).
     */
    AgendadorDeRotinas(ExecutorDeRotinas executor, ExecucaoDeRotinaRepository execucoes, MeterRegistry metricas,
                       Clock relogio) {
        this.executor = executor;
        this.execucoes = execucoes;
        this.relogio = relogio;
        this.subida = relogio.instant();
        executor.rotinas().keySet().forEach(codigo -> {
            AtomicInteger valor = new AtomicInteger();
            atrasadas.put(codigo, valor);
            Gauge.builder("orbit.rotina.atrasada", valor, AtomicInteger::get).tag("rotina", codigo).register(metricas);
        });
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Registrar as tarefas
     * Objetivo: Agendar cada rotina e o vigia.
     * Decisões: Cron no fuso de Brasília.
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar registro) {
        executor.rotinas().forEach((codigo, rotina) -> registro.addTriggerTask(
                () -> executor.executarAgendada(codigo), new CronTrigger(rotina.cron(), BRASILIA)));
        registro.addTriggerTask(this::vigiar, new CronTrigger(VIGIA, BRASILIA));
        LOG.info("Rotinas agendadas: {}", executor.rotinas().keySet());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Vigiar atrasos
     * Objetivo: Marcar e alertar as rotinas ativas que não rodaram no horário esperado.
     * Decisões: Pausada não conta como atrasada.
     */
    void vigiar() {
        Instant agora = relogio.instant();
        executor.rotinas().forEach((codigo, rotina) -> {
            boolean atrasada = !executor.pausada(codigo) && atrasada(rotina.cron(), agora, subida,
                    execucoes.findFirstByRotinaAndDisparoOrderByIniciadaEmDesc(codigo, ExecutorDeRotinas.AUTOMATICO)
                            .map(ExecucaoDeRotina::getIniciadaEm).orElse(null));
            atrasadas.get(codigo).set(atrasada ? 1 : 0);
            if (atrasada) {
                LOG.error("Rotina atrasada: {} não roda desde o horário esperado", codigo);
            }
        });
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Está atrasada
     * Objetivo: Dizer se já passou mais de duas vezes o intervalo da rotina sem execução automática.
     * Decisões: O intervalo é a distância entre dois disparos seguidos do cron; a referência é a última execução ou,
     *           se nunca rodou, a subida da API (para uma rotina diária não acusar atraso logo depois de publicar).
     */
    static boolean atrasada(String cron, Instant agora, Instant subida, Instant ultimaExecucao) {
        CronExpression expressao = CronExpression.parse(cron);
        ZonedDateTime base = agora.atZone(BRASILIA);
        ZonedDateTime proxima = expressao.next(base);
        ZonedDateTime seguinte = proxima == null ? null : expressao.next(proxima);
        if (proxima == null || seguinte == null) {
            return false;
        }
        Duration intervalo = Duration.between(proxima, seguinte);
        Instant referencia = ultimaExecucao != null && ultimaExecucao.isAfter(subida) ? ultimaExecucao : subida;
        return Duration.between(referencia, agora).compareTo(intervalo.multipliedBy(2)) > 0;
    }
}
