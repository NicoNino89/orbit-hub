package br.com.orbithub.operacao.service;

import br.com.orbithub.administracao.service.TimesService;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-82, KAN-108
 * Título: Filas da operação
 * Objetivo: Ler, de cada fila interna, os casos em aberto com início, prazo e responsável, para o painel e o monitor de
 *           prazos.
 * Decisões: Verificação e curadoria calculam o prazo pelos dias úteis do parâmetro (não ficam gravados); as demais
 *           filas usam o prazo gravado no caso. Segundas aprovações têm a R15. Incidentes aparecem no painel pelo prazo
 *           de comunicação, sem alerta da R14.
 */
@Component
public class FilasDeOperacao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82, KAN-108
     * Título: Filas
     * Objetivo: Descrever cada fila: nome, tabela, consulta, time e se recebe alerta.
     * Decisões: Recursos sem time próprio ficam com a liderança geral (administradores).
     */
    public enum Fila {
        VERIFICACAO("Verificação", "pedido_verificacao", TimesService.Time.VERIFICACAO, "verificacao.sla_dias_uteis",
                "SELECT id, COALESCE(enviado_em, criado_em), NULL::timestamptz, responsavel_id, alertado_em, vencido_alertado_em FROM pedido_verificacao "
                        + "WHERE situacao = 'EM_ANALISE'", "/admin/verificacao"),
        CURADORIA("Revisão de curadoria", "revisao_curadoria", TimesService.Time.CURADORIA, "curadoria.sla_dias_uteis",
                "SELECT id, enviada_em, NULL::timestamptz, responsavel_id, alertado_em, vencido_alertado_em FROM revisao_curadoria WHERE situacao = 'EM_ANALISE'",
                "/admin/curadoria"),
        MODERACAO("Denúncia", "caso_moderacao", TimesService.Time.MODERACAO, null,
                "SELECT id, criado_em, prazo, responsavel_id, alertado_em, vencido_alertado_em FROM caso_moderacao WHERE situacao <> 'DECIDIDO'",
                "/admin/moderacao"),
        RECURSOS("Recurso", "recurso", null, null,
                "SELECT id, criado_em, prazo, NULL::uuid, alertado_em, vencido_alertado_em FROM recurso WHERE situacao = 'PENDENTE'", "/admin/recursos"),
        ATENDIMENTO("Chamado", "chamado", TimesService.Time.ATENDIMENTO, null,
                "SELECT id, criado_em, prazo_primeira_resposta, responsavel_id, alertado_em, vencido_alertado_em FROM chamado "
                        + "WHERE situacao IN ('ABERTO', 'EM_ATENDIMENTO')", "/admin/atendimento"),
        PRIVACIDADE("Pedido do titular", "pedido_titular", TimesService.Time.PRIVACIDADE, null,
                "SELECT id, criado_em, prazo, responsavel_id, alertado_em, vencido_alertado_em FROM pedido_titular WHERE situacao IN ('ABERTO', 'EM_ANALISE')",
                "/admin/privacidade"),
        INCIDENTES("Incidente", "incidente", TimesService.Time.PRIVACIDADE, null,
                "SELECT id, detectado_em, prazo_comunicacao, NULL::uuid, now(), now() FROM incidente WHERE situacao <> 'ENCERRADO'", "/admin/privacidade");

        private final String rotulo;
        private final String tabela;
        private final TimesService.Time time;
        private final String sla;
        private final String consulta;
        private final String link;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Fila
     * Objetivo: Rotina auxiliar Fila.
     * Decisões: Ver a documentação da classe.
     */
        Fila(String rotulo, String tabela, TimesService.Time time, String sla, String consulta, String link) {
            this.rotulo = rotulo;
            this.tabela = tabela;
            this.time = time;
            this.sla = sla;
            this.consulta = consulta;
            this.link = link;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-82
         * Título: Rótulo
         * Objetivo: Devolver o nome do caso da fila.
         * Decisões: Singular, para os alertas.
         */
        public String rotulo() {
            return rotulo;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Tabela
         * Objetivo: Devolver a tabela da fila, para marcar os alertas.
         * Decisões: Nome fixo no código (nunca vem de fora).
         */
        public String tabela() {
            return tabela;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Time
         * Objetivo: Devolver o time da fila.
         * Decisões: Nulo para recursos.
         */
        public TimesService.Time time() {
            return time;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Link
         * Objetivo: Devolver a tela da fila.
         * Decisões: Backoffice.
         */
        public String link() {
            return link;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Recebe alerta
         * Objetivo: Dizer se a R14 alerta os casos da fila.
         * Decisões: Incidentes não (têm o próprio acompanhamento).
         */
        public boolean alerta() {
            return this != INCIDENTES;
        }
    }

    private final JdbcTemplate jdbc;
    private final LeituraDeParametros parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Criação
     * Objetivo: Receber o banco e os parâmetros.
     * Decisões: Injeção por construtor.
     */
    public FilasDeOperacao(JdbcTemplate jdbc, LeituraDeParametros parametros) {
        this.jdbc = jdbc;
        this.parametros = parametros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82, KAN-108
     * Título: Casos em aberto
     * Objetivo: Ler os casos em aberto da fila com o prazo calculado.
     * Decisões: Sem início conhecido, o caso fica de fora.
     */
    public List<Caso> abertos(Fila fila) {
        long sla = fila.sla == null ? 0 : parametros.inteiro(fila.sla);
        List<Caso> casos = new ArrayList<>();
        jdbc.query(fila.consulta, rs -> {
            Timestamp inicio = rs.getTimestamp(2);
            if (inicio == null) {
                return;
            }
            Timestamp prazo = rs.getTimestamp(3);
            casos.add(new Caso(fila, rs.getObject(1, UUID.class), inicio.toInstant(),
                    prazo != null ? prazo.toInstant() : DiasUteis.somar(inicio.toInstant(), sla), rs.getObject(4, UUID.class), rs.getTimestamp(5) != null,
                    rs.getTimestamp(6) != null));
        });
        return casos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82, KAN-108
     * Título: Caso de uma fila
     * Objetivo: Tipar o caso em aberto.
     * Decisões: Limiar do alerta pelo percentual do prazo consumido.
     */
    public record Caso(Fila fila, UUID id, Instant inicio, Instant prazo, UUID responsavel, boolean alertado, boolean vencidoAlertado) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Perto do prazo
         * Objetivo: Dizer se o caso já consumiu o percentual do prazo (e ainda não venceu).
         * Decisões: operacao.alerta_sla_percentual.
         */
        public boolean pertoDoPrazo(Instant agora, long percentual) {
            long total = prazo.toEpochMilli() - inicio.toEpochMilli();
            return !vencido(agora) && agora.toEpochMilli() >= inicio.toEpochMilli() + total * percentual / 100;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-108
         * Título: Vencido
         * Objetivo: Dizer se o prazo já passou.
         * Decisões: Prazo no instante exato conta como vencido.
         */
        public boolean vencido(Instant agora) {
            return !agora.isBefore(prazo);
        }
    }
}
