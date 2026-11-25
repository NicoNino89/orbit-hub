package br.com.orbithub.conexoes.repository;

import br.com.orbithub.conexoes.domain.SituacaoDaRodada;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Histórico da rodada
 * Objetivo: Gravar e listar as mudanças de situação da rodada (e, na S7, as trocas de líder).
 * Decisões: Registro só de inclusão, por JDBC (não precisa de entidade). Autor nulo quando for rotina.
 */
@Repository
public class HistoricoDaRodada {

    private final JdbcTemplate jdbc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criação do repositório
     * Objetivo: Receber o acesso ao banco.
     * Decisões: Injeção por construtor.
     */
    public HistoricoDaRodada(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Registrar
     * Objetivo: Gravar uma mudança de situação.
     * Decisões: Motivo opcional.
     */
    public void registrar(UUID rodadaId, SituacaoDaRodada de, SituacaoDaRodada para, UUID por, String motivo, Instant em) {
        jdbc.update("INSERT INTO rodada_historico (id, rodada_id, de, para, por, motivo, em) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), rodadaId, de.name(), para.name(), por, motivo, Timestamp.from(em));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Listar
     * Objetivo: Devolver as mudanças da rodada, da mais recente para a mais antiga.
     * Decisões: Sem o autor (dado interno da equipe).
     */
    public List<Mudanca> listar(UUID rodadaId) {
        return jdbc.query("SELECT de, para, motivo, em FROM rodada_historico WHERE rodada_id = ? ORDER BY em DESC",
                (rs, i) -> new Mudanca(SituacaoDaRodada.valueOf(rs.getString("de")), SituacaoDaRodada.valueOf(rs.getString("para")),
                        rs.getString("motivo"), rs.getTimestamp("em").toInstant()), rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Mudança
     * Objetivo: Levar uma mudança de situação da rodada.
     * Decisões: De, para, motivo e data.
     */
    public record Mudanca(SituacaoDaRodada de, SituacaoDaRodada para, String motivo, Instant em) {
    }
}
