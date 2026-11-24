package br.com.orbithub.notificacoes.service;

import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ContaProperties;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: R18 – Resumo diário dos avisos
 * Objetivo: Mandar um único e-mail por dia, às 7h, com os avisos das pessoas que escolheram o resumo.
 * Decisões: Agrupa os itens pendentes do resumo por pessoa; enviado o e-mail, todos os itens saem da fila (não vão no
 *           resumo do dia seguinte). Falha deixa os itens para o próximo dia e conta como erro da rotina.
 */
@Component
class ResumoDiarioDeAvisos extends RotinaBase {

    private final JdbcTemplate jdbc;
    private final EnvioDeEmail email;
    private final ConsultaDeContas contas;
    private final ContaProperties conta;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o banco, o e-mail, as contas e o endereço da web.
     * Decisões: Injeção por construtor.
     */
    ResumoDiarioDeAvisos(JdbcTemplate jdbc, EnvioDeEmail email, ConsultaDeContas contas, ContaProperties conta) {
        super("R18", "Envia o resumo diário dos avisos a quem escolheu", "0 0 7 * * *", "notificacoes", List.of());
        this.jdbc = jdbc;
        this.email = email;
        this.contas = contas;
        this.conta = conta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Montar e enviar os resumos
     * Objetivo: Enviar um e-mail por pessoa com os títulos dos avisos pendentes.
     * Decisões: Um item é um resumo enviado.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        List<Map<String, Object>> pendentes = jdbc.queryForList("""
                SELECT e.id, e.usuario_id, n.titulo FROM notificacao_email e JOIN notificacao n ON n.id = e.notificacao_id
                WHERE e.situacao = 'PENDENTE' AND e.no_resumo = TRUE ORDER BY e.usuario_id, n.criada_em
                FOR UPDATE OF e SKIP LOCKED""");
        Map<UUID, List<Map<String, Object>>> porPessoa = pendentes.stream()
                .collect(Collectors.groupingBy(m -> (UUID) m.get("usuario_id"), LinkedHashMap::new, Collectors.toList()));
        int enviados = 0;
        int falhas = 0;
        for (Map.Entry<UUID, List<Map<String, Object>>> pessoa : porPessoa.entrySet()) {
            Optional<String> para = contas.emailParaAviso(pessoa.getKey());
            List<UUID> ids = pessoa.getValue().stream().map(m -> (UUID) m.get("id")).toList();
            if (para.isEmpty()) {
                ids.forEach(id -> jdbc.update("UPDATE notificacao_email SET situacao = 'DESCARTADO' WHERE id = ?", id));
                continue;
            }
            String lista = pessoa.getValue().stream().map(m -> "- " + m.get("titulo")).collect(Collectors.joining("\n"));
            String texto = "Seus avisos das últimas 24 horas no Orbit Hub:\n\n" + lista + "\n\nVeja todos em "
                    + conta.webUrl() + "/notificacoes.";
            if (email.tentarEnviar(new EnvioDeEmail.MensagemEmail(para.get(), "Orbit Hub: resumo do dia", texto,
                    "resumo_avisos"))) {
                ids.forEach(id -> jdbc.update("UPDATE notificacao_email SET situacao = 'ENVIADO', enviado_em = ? WHERE id = ?",
                        Timestamp.from(agora), id));
                enviados++;
            } else {
                falhas++;
            }
        }
        return new Resultado(enviados, falhas, enviados + " resumo(s) enviado(s)");
    }
}
