package br.com.orbithub.operacao.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-82, KAN-76
 * Título: Painel de operação
 * Objetivo: Mostrar a situação de cada fila (abertos, idade média, perto do prazo, vencidos, por responsável), os
 *           indicadores dos últimos 30 dias e os indicadores do ecossistema.
 * Decisões: Só contagens e médias: nenhum dado confidencial nem valor de investimento. "Mensal" = últimos 30 dias.
 */
@Service
public class PainelDeOperacaoService {

    private static final String SEM_RESPONSAVEL = "Sem responsável";

    private final FilasDeOperacao filas;
    private final JdbcTemplate jdbc;
    private final ConsultaDeContas contas;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    public PainelDeOperacaoService(FilasDeOperacao filas, JdbcTemplate jdbc, ConsultaDeContas contas, LeituraDeParametros parametros, Clock relogio) {
        this.filas = filas;
        this.jdbc = jdbc;
        this.contas = contas;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Montar o painel
     * Objetivo: Reunir as filas, os indicadores do mês e os do ecossistema.
     * Decisões: Consulta na hora (o volume do MVP é pequeno).
     */
    @Transactional(readOnly = true)
    public Painel painel() {
        Instant agora = relogio.instant();
        long percentual = parametros.inteiro(MonitorDePrazos.PERCENTUAL);
        List<SituacaoDaFila> situacoes = new ArrayList<>();
        for (FilasDeOperacao.Fila fila : FilasDeOperacao.Fila.values()) {
            List<FilasDeOperacao.Caso> casos = filas.abertos(fila);
            var nomes = contas.nomes(casos.stream().map(FilasDeOperacao.Caso::responsavel).filter(Objects::nonNull).distinct().toList());
            Map<String, Integer> porResponsavel = new TreeMap<>();
            casos.forEach(c -> porResponsavel.merge(c.responsavel() == null ? SEM_RESPONSAVEL : nomes.getOrDefault(c.responsavel(), SEM_RESPONSAVEL), 1,
                    Integer::sum));
            double idade = casos.stream().mapToLong(c -> Duration.between(c.inicio(), agora).toHours()).average().orElse(0);
            situacoes.add(new SituacaoDaFila(fila, fila.rotulo(), casos.size(), Math.round(idade),
                    (int) casos.stream().filter(c -> c.pertoDoPrazo(agora, percentual)).count(), (int) casos.stream().filter(c -> c.vencido(agora)).count(),
                    porResponsavel));
        }
        Timestamp mes = Timestamp.from(agora.minus(Duration.ofDays(30)));
        Indicadores indicadores = new Indicadores(
                contar("SELECT (SELECT count(*) FROM caso_moderacao WHERE criado_em >= ?) + (SELECT count(*) FROM chamado WHERE criado_em >= ? "
                        + "AND situacao <> 'AGUARDANDO_CONFIRMACAO') + (SELECT count(*) FROM recurso WHERE criado_em >= ?) + (SELECT count(*) FROM pedido_titular "
                        + "WHERE criado_em >= ?) + (SELECT count(*) FROM pedido_verificacao WHERE enviado_em >= ?)", mes, mes, mes, mes, mes),
                decimal("SELECT avg(EXTRACT(EPOCH FROM (resolvido_em - criado_em)) / 3600) FROM chamado WHERE resolvido_em >= ?", mes),
                decimal("SELECT 100.0 * count(*) FILTER (WHERE COALESCE(primeira_resposta_em, resolvido_em) <= prazo_primeira_resposta) / NULLIF(count(*), 0) "
                        + "FROM chamado WHERE resolvido_em >= ?", mes),
                decimal("SELECT 100.0 * count(*) FILTER (WHERE situacao = 'DEFERIDO') / NULLIF(count(*), 0) FROM recurso WHERE julgado_em >= ?", mes),
                decimal("SELECT avg(nota) FROM chamado WHERE nota IS NOT NULL AND atualizado_em >= ?", mes));
        Ecossistema ecossistema = new Ecossistema(
                contar("SELECT count(*) FROM usuario WHERE NOT interna AND excluida_em IS NULL AND criado_em >= ?", mes),
                contar("SELECT count(*) FROM startup WHERE situacao = 'PUBLICADA'"),
                contar("SELECT count(*) FROM conexao_historico WHERE para = 'MATCH' AND em >= ?", mes),
                contar("SELECT count(DISTINCT reuniao_id) FROM reuniao_registro WHERE resultado = 'REALIZADA' AND em >= ?", mes),
                contar("SELECT count(*) FROM rodada WHERE situacao IN ('ABERTA', 'EM_NEGOCIACAO')"),
                contar("SELECT count(*) FROM rodada_investimento WHERE situacao = 'CONFIRMADO'"));
        return new Painel(situacoes, indicadores, ecossistema, agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: contar
     * Objetivo: Rotina auxiliar contar.
     * Decisões: Ver a documentação da classe.
     */
    private int contar(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: decimal
     * Objetivo: Rotina auxiliar decimal.
     * Decisões: Ver a documentação da classe.
     */
    private Double decimal(String sql, Object... args) {
        Number n = jdbc.queryForObject(sql, Number.class, args);
        return n == null ? null : Math.round(n.doubleValue() * 10) / 10.0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Dados do painel
     * Objetivo: Tipar o painel de operação.
     * Decisões: Indicadores nulos quando não há base no período.
     */
    public record SituacaoDaFila(FilasDeOperacao.Fila fila, String nome, int abertos, long idadeMediaHoras, int pertoDoPrazo, int vencidos,
                                 Map<String, Integer> porResponsavel) {
    }

    public record Indicadores(int casosNoMes, Double horasMediasParaResolver, Double percentualNoPrazo, Double percentualDeRecursosAceitos,
                              Double satisfacao) {
    }

    public record Ecossistema(int novosCadastros, int startupsPublicadas, int matches, int reunioesRealizadas, int rodadasAbertas,
                              int investimentosConcretizados) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Painel
     * Objetivo: Rotina auxiliar Painel.
     * Decisões: Ver a documentação da classe.
     */
    public record Painel(List<SituacaoDaFila> filas, Indicadores indicadores, Ecossistema ecossistema, Instant geradoEm) {
    }
}
