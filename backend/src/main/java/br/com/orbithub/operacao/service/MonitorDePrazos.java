package br.com.orbithub.operacao.service;

import br.com.orbithub.administracao.service.TimesService;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-108, KAN-76
 * Título: R14 – Monitor de prazos
 * Objetivo: De hora em hora, em todas as filas internas, alertar uma vez o responsável (ou o time) e o líder quando o
 *           caso consome operacao.alerta_sla_percentual do prazo, e alertar o líder e os administradores quando vence.
 * Decisões: Cada alerta é marcado no caso para não repetir. Sem líder definido, os administradores lideram. Os prazos
 *           em dias úteis usam o calendário único de atendimento (provisório).
 */
@Component
class MonitorDePrazos extends RotinaBase {

    static final String PERCENTUAL = "operacao.alerta_sla_percentual";

    private final FilasDeOperacao filas;
    private final TimesService times;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final JdbcTemplate jdbc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-108
     * Título: Criação
     * Objetivo: Registrar a rotina com os parâmetros de prazo das filas.
     * Decisões: Minuto 10 de cada hora.
     */
    @SuppressWarnings("java:S107")
    MonitorDePrazos(FilasDeOperacao filas, TimesService times, ConsultaDeContas contas, NotificacoesService notificacoes, LeituraDeParametros parametros,
                    JdbcTemplate jdbc) {
        super("R14", "Alerta os casos das filas internas perto do prazo e os vencidos", "0 10 * * * *", "operacao",
                List.of(PERCENTUAL, "verificacao.sla_dias_uteis", "curadoria.sla_dias_uteis", "moderacao.sla_critica_horas", "moderacao.sla_alta_horas",
                        "moderacao.sla_normal_dias_uteis", "recurso.sla_dias_uteis", "suporte.sla_primeira_resposta_horas", "suporte.sla_n2_dias_uteis",
                        "disputa.sla_dias_uteis", "lgpd.prazo_resposta_dias"));
        this.filas = filas;
        this.times = times;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.jdbc = jdbc;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-108
     * Título: Executar
     * Objetivo: Percorrer as filas e mandar os alertas.
     * Decisões: Vencido tem prioridade sobre o perto do prazo (marca os dois).
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        long percentual = parametros.inteiro(PERCENTUAL);
        Timestamp t = Timestamp.from(agora);
        int alertas = 0;
        for (FilasDeOperacao.Fila fila : FilasDeOperacao.Fila.values()) {
            if (!fila.alerta()) {
                continue;
            }
            List<UUID> lideranca = fila.time() == null ? contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)) : times.lideranca(fila.time());
            for (FilasDeOperacao.Caso c : filas.abertos(fila)) {
                if (c.vencido(agora) && !c.vencidoAlertado()) {
                    Set<UUID> quem = new LinkedHashSet<>(lideranca);
                    quem.addAll(contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)));
                    notificacoes.notificarTodos(List.copyOf(quem), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, fila.rotulo() + " com prazo vencido",
                            "Um caso da fila venceu o prazo. Redistribua ou priorize.", fila.link());
                    jdbc.update("UPDATE " + fila.tabela() + " SET vencido_alertado_em = ?, alertado_em = COALESCE(alertado_em, ?) WHERE id = ?", t, t, c.id());
                    alertas++;
                } else if (!c.alertado() && c.pertoDoPrazo(agora, percentual)) {
                    Set<UUID> quem = new LinkedHashSet<>(c.responsavel() != null ? List.of(c.responsavel())
                            : fila.time() == null ? List.of() : contas.equipeAtivaComPapeis(fila.time().papeis()));
                    quem.addAll(lideranca);
                    notificacoes.notificarTodos(List.copyOf(quem), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, fila.rotulo() + " perto do prazo",
                            "Um caso já consumiu " + percentual + "% do prazo.", fila.link());
                    jdbc.update("UPDATE " + fila.tabela() + " SET alertado_em = ? WHERE id = ?", t, c.id());
                    alertas++;
                }
            }
        }
        return new Resultado(alertas, 0, alertas + " alerta(s) de prazo");
    }
}
