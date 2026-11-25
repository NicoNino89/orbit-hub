package br.com.orbithub.atendimento.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89, KAN-100, KAN-86
 * Título: Rotinas do atendimento e da moderação
 * Objetivo: Agrupar a R14 (monitor de prazos) e a R20 (fechamento dos chamados parados).
 * Decisões: Uma classe por rotina, aninhadas aqui.
 */
final class RotinasDoAtendimento {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: RotinasDoAtendimento
     * Objetivo: Rotina auxiliar RotinasDoAtendimento.
     * Decisões: Ver a documentação da classe.
     */
    private RotinasDoAtendimento() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100, KAN-86, KAN-89
     * Título: R14 – Monitor de prazos
     * Objetivo: Alertar, uma vez, os responsáveis pelos casos de moderação, recursos e chamados que vencem nas próximas
     *           2 horas ou já venceram.
     * Decisões: Caso com responsável alerta só ele; sem responsável, o time inteiro. Verificação e curadoria têm os
     *           próprios avisos; LGPD entra com o fluxo do titular.
     */
    @Component
    static class MonitorDePrazos extends RotinaBase {

        private final JdbcTemplate jdbc;
        private final NotificacoesService notificacoes;
        private final ConsultaDeContas contas;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-100
         * Título: Criação
         * Objetivo: Registrar a rotina de hora em hora.
         * Decisões: Usa os prazos já gravados em cada caso.
         */
        MonitorDePrazos(JdbcTemplate jdbc, NotificacoesService notificacoes, ConsultaDeContas contas) {
            super("R14", "Alerta os casos de moderação, recursos e chamados perto do prazo", "0 10 * * * *", "atendimento",
                    List.of("moderacao.sla_critica_horas", "moderacao.sla_alta_horas", "moderacao.sla_normal_dias_uteis", "recurso.sla_dias_uteis",
                            AtendimentoService.SLA_N1, AtendimentoService.SLA_N2, AtendimentoService.SLA_DISPUTA));
            this.jdbc = jdbc;
            this.notificacoes = notificacoes;
            this.contas = contas;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-100
         * Título: Executar
         * Objetivo: Alertar os três tipos de caso e marcar o alerta.
         * Decisões: Na mesma transação.
         */
        @Override
        @Transactional
        public Resultado executar(Instant agora) {
            Timestamp limite = Timestamp.from(agora.plus(Duration.ofHours(2)));
            int n = alertar("caso_moderacao", "situacao <> 'DECIDIDO'", Set.of(Papel.MODERADOR), "Denúncia perto do prazo", "/admin/moderacao", limite, agora);
            n += alertar("recurso", "situacao = 'PENDENTE'", Set.of(Papel.ADMIN), "Recurso perto do prazo", "/admin/recursos", limite, agora);
            n += alertar("chamado", "situacao IN ('ABERTO', 'EM_ATENDIMENTO')", Set.of(Papel.SUPORTE_N1, Papel.SUPORTE_N2), "Chamado perto do prazo",
                    "/admin/atendimento", limite, agora);
            return new Resultado(n, 0, n + " alerta(s) de prazo");
        }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: alertar
     * Objetivo: Rotina auxiliar alertar.
     * Decisões: Ver a documentação da classe.
     */
        private int alertar(String tabela, String filtro, Set<Papel> time, String titulo, String link, Timestamp limite, Instant agora) {
            String prazo = "chamado".equals(tabela) ? "prazo_primeira_resposta" : "prazo";
            String responsavel = "recurso".equals(tabela) ? "NULL::uuid" : "responsavel_id";
            List<java.util.Map<String, Object>> casos = jdbc.queryForList("SELECT id, " + responsavel + " AS responsavel FROM " + tabela + " WHERE " + filtro
                    + " AND alertado_em IS NULL AND " + prazo + " < ?", limite);
            for (java.util.Map<String, Object> c : casos) {
                UUID resp = (UUID) c.get("responsavel");
                List<UUID> quem = resp != null ? List.of(resp) : contas.equipeAtivaComPapeis(time);
                notificacoes.notificarTodos(quem, TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, titulo, "Um caso vence nas próximas horas ou já venceu.", link);
                jdbc.update("UPDATE " + tabela + " SET alertado_em = ? WHERE id = ?", Timestamp.from(agora), c.get("id"));
            }
            return casos.size();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: R20 – Chamados parados
     * Objetivo: Fechar os chamados que esperam o usuário ou foram resolvidos há mais de suporte.prazo_aguardando_dias.
     * Decisões: Diária, às 4h.
     */
    @Component
    static class FechamentoDeChamados extends RotinaBase {

        private final AtendimentoService atendimento;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-89
         * Título: Criação
         * Objetivo: Registrar a rotina.
         * Decisões: Parâmetro suporte.prazo_aguardando_dias.
         */
        FechamentoDeChamados(AtendimentoService atendimento) {
            super("R20", "Fecha os chamados parados à espera do usuário", "0 0 4 * * *", "atendimento", List.of(AtendimentoService.PRAZO_AGUARDANDO));
            this.atendimento = atendimento;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-89
         * Título: Executar
         * Objetivo: Fechar os chamados parados.
         * Decisões: Ver AtendimentoService.fecharParados.
         */
        @Override
        public Resultado executar(Instant agora) {
            int n = atendimento.fecharParados();
            return new Resultado(n, 0, n + " chamado(s) fechado(s)");
        }
    }
}
