package br.com.orbithub.compartilhado.rotinas;

import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Executor comum das rotinas
 * Objetivo: Rodar uma rotina com trava entre instâncias, registro da execução, respeito à pausa e alertas.
 * Decisões: A trava é a inserção do ciclo com ON CONFLICT DO NOTHING na chave (rotina, ciclo): só uma instância
 *           consegue gravar e roda; as outras desistem em silêncio. O ciclo automático é o minuto agendado (todas as
 *           instâncias disparam no mesmo minuto). Falha (exceção ou erros contados) vai para o log como ERROR com o
 *           prefixo "Rotina falhou", que alimenta o alerta do Azure Monitor, e para o contador
 *           orbit_rotina_falhas_total, que alimenta o do Prometheus. Rotina pausada não roda no automático, mas pode
 *           ser executada manualmente.
 */
@Service
public class ExecutorDeRotinas {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorDeRotinas.class);
    static final String AUTOMATICO = "AUTOMATICO";
    static final String MANUAL = "MANUAL";

    private final Map<String, RotinaAgendada> rotinas;
    private final JdbcTemplate jdbc;
    private final MeterRegistry metricas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação do executor
     * Objetivo: Indexar as rotinas pelo código.
     * Decisões: Código repetido é erro de configuração e falha na inicialização.
     */
    public ExecutorDeRotinas(List<RotinaAgendada> rotinas, JdbcTemplate jdbc, MeterRegistry metricas, Clock relogio) {
        Map<String, RotinaAgendada> indice = new LinkedHashMap<>();
        rotinas.stream().sorted(Comparator.comparingInt((RotinaAgendada r) -> ordem(r.codigo()))).forEach(r -> {
            if (indice.put(r.codigo(), r) != null) {
                throw new IllegalStateException("Duas rotinas com o código " + r.codigo());
            }
        });
        this.rotinas = Collections.unmodifiableMap(indice);
        this.jdbc = jdbc;
        this.metricas = metricas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotinas conhecidas
     * Objetivo: Listar as rotinas registradas, em ordem de código (R1, R2, ..., R16).
     * Decisões: Somente leitura.
     */
    public Map<String, RotinaAgendada> rotinas() {
        return rotinas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Execução automática
     * Objetivo: Rodar a rotina no ciclo agendado, se não estiver pausada e se nenhuma outra instância já pegou o ciclo.
     * Decisões: Devolve vazio quando não rodou (pausada ou ciclo já tomado).
     */
    public Optional<RotinaAgendada.Resultado> executarAgendada(String codigo) {
        if (pausada(codigo)) {
            LOG.info("Rotina {} pausada: ciclo ignorado", codigo);
            return Optional.empty();
        }
        String ciclo = relogio.instant().truncatedTo(ChronoUnit.MINUTES).toString();
        return executar(codigo, ciclo, AUTOMATICO, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Execução manual
     * Objetivo: Rodar a rotina agora, a pedido do administrador, mesmo pausada.
     * Decisões: Ciclo próprio ("MANUAL-" + id), que nunca colide com o automático.
     */
    public RotinaAgendada.Resultado executarManual(String codigo, UUID quem) {
        return executar(codigo, "MANUAL-" + UUID.randomUUID(), MANUAL, quem)
                .orElseThrow(() -> new IllegalStateException("Execução manual não registrada"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina pausada
     * Objetivo: Dizer se o administrador pausou a rotina.
     * Decisões: Sem registro de estado, a rotina está ativa.
     */
    public boolean pausada(String codigo) {
        List<Boolean> estado = jdbc.queryForList("SELECT pausada FROM rotina_estado WHERE rotina = ?", Boolean.class, codigo);
        return !estado.isEmpty() && Boolean.TRUE.equals(estado.get(0));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Pausar ou retomar
     * Objetivo: Gravar o estado da rotina.
     * Decisões: Upsert, para a primeira pausa criar o estado.
     */
    public void definirPausa(String codigo, boolean pausar, UUID quem) {
        exigir(codigo);
        jdbc.update("""
                INSERT INTO rotina_estado (rotina, pausada, alterada_por, alterada_em) VALUES (?, ?, ?, ?)
                ON CONFLICT (rotina) DO UPDATE SET pausada = EXCLUDED.pausada, alterada_por = EXCLUDED.alterada_por,
                alterada_em = EXCLUDED.alterada_em""", codigo, pausar, quem, Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina pelo código
     * Objetivo: Achar a rotina registrada.
     * Decisões: Código desconhecido lança IllegalArgumentException (o painel traduz para 404).
     */
    public RotinaAgendada exigir(String codigo) {
        RotinaAgendada rotina = rotinas.get(codigo);
        if (rotina == null) {
            throw new IllegalArgumentException("Rotina desconhecida: " + codigo);
        }
        return rotina;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Executar com trava e registro
     * Objetivo: Tomar o ciclo, rodar a rotina e gravar o resultado.
     * Decisões: A gravação da trava é fora de qualquer transação da rotina; o resultado é gravado mesmo se a rotina
     *           lançar exceção (que não sobe para o agendador).
     */
    private Optional<RotinaAgendada.Resultado> executar(String codigo, String ciclo, String disparo, UUID quem) {
        RotinaAgendada rotina = exigir(codigo);
        UUID id = UUID.randomUUID();
        Instant inicio = relogio.instant();
        int tomou = jdbc.update("""
                INSERT INTO rotina_execucao (id, rotina, ciclo, disparo, disparado_por, iniciada_em, resultado)
                VALUES (?, ?, ?, ?, ?, ?, 'EM_ANDAMENTO') ON CONFLICT (rotina, ciclo) DO NOTHING""",
                id, codigo, ciclo, disparo, quem, Timestamp.from(inicio));
        if (tomou == 0) {
            LOG.debug("Rotina {} ciclo {} já tomado por outra instância", codigo, ciclo);
            return Optional.empty();
        }
        RotinaAgendada.Resultado resultado;
        try {
            resultado = rotina.executar(inicio);
        } catch (RuntimeException falha) {
            resultado = new RotinaAgendada.Resultado(0, 1, "Erro: " + falha.getClass().getSimpleName());
        }
        boolean sucesso = resultado.erros() == 0;
        String mensagem = resultado.mensagem() == null ? null
                : resultado.mensagem().substring(0, Math.min(500, resultado.mensagem().length()));
        jdbc.update("""
                UPDATE rotina_execucao SET terminada_em = ?, itens = ?, erros = ?, resultado = ?, mensagem = ?
                WHERE id = ?""", Timestamp.from(relogio.instant()), resultado.itens(), resultado.erros(),
                sucesso ? "SUCESSO" : "FALHA", mensagem, id);
        metricas.counter("orbit.rotina.execucoes", "rotina", codigo, "resultado", sucesso ? "sucesso" : "falha").increment();
        if (sucesso) {
            LOG.info("Rotina {} concluída: {} item(ns) ({})", codigo, resultado.itens(), disparo);
        } else {
            metricas.counter("orbit.rotina.falhas", "rotina", codigo).increment();
            LOG.error("Rotina falhou: {} ({} erro(s), {} item(ns)): {}", codigo, resultado.erros(), resultado.itens(),
                    mensagem);
        }
        return Optional.of(resultado);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Ordem do código
     * Objetivo: Ordenar R2 antes de R10.
     * Decisões: Número depois do "R"; códigos fora do padrão vão para o fim.
     */
    static int ordem(String codigo) {
        try {
            return Integer.parseInt(codigo.substring(1));
        } catch (RuntimeException fora) {
            return Integer.MAX_VALUE;
        }
    }
}
