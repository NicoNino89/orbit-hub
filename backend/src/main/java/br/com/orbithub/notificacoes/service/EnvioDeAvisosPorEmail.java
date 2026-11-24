package br.com.orbithub.notificacoes.service;

import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.ContaProperties;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: R17 – Envio dos avisos por e-mail
 * Objetivo: Enviar os e-mails da fila de avisos imediatos e tentar de novo os que falharam.
 * Decisões: A cada minuto, até 100 por vez, com FOR UPDATE SKIP LOCKED (duas instâncias nunca pegam o mesmo e-mail).
 *           Falha espera cada vez mais (1, 5, 15, 60 minutos...) e desiste depois de notificacoes.max_tentativas_email,
 *           contando como erro da rotina (o que gera alerta). Conta desativada sem e-mail: o item é descartado. O e-mail
 *           só traz o título, a mensagem e o link da tela; nada de dado confidencial.
 */
@Component
class EnvioDeAvisosPorEmail extends RotinaBase {

    static final String CHAVE_TENTATIVAS = "notificacoes.max_tentativas_email";
    static final List<Duration> ESPERAS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
            Duration.ofHours(1), Duration.ofHours(6));

    private final JdbcTemplate jdbc;
    private final EnvioDeEmail email;
    private final ConsultaDeContas contas;
    private final ContaProperties conta;
    private final LeituraDeParametros parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o banco, o e-mail, as contas, o endereço da web e os parâmetros.
     * Decisões: Injeção por construtor.
     */
    EnvioDeAvisosPorEmail(JdbcTemplate jdbc, EnvioDeEmail email, ConsultaDeContas contas, ContaProperties conta,
                          LeituraDeParametros parametros) {
        super("R17", "Envia os avisos por e-mail e tenta de novo os que falharam", "0 * * * * *", "notificacoes",
                List.of(CHAVE_TENTATIVAS));
        this.jdbc = jdbc;
        this.email = email;
        this.contas = contas;
        this.conta = conta;
        this.parametros = parametros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Enviar a fila
     * Objetivo: Enviar os e-mails vencidos da fila e reagendar os que falharam.
     * Decisões: Um item é um e-mail; erro é desistência depois do limite de tentativas.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        List<Map<String, Object>> fila = jdbc.queryForList("""
                SELECT e.id, e.usuario_id, e.tentativas, n.titulo, n.mensagem, n.link
                FROM notificacao_email e JOIN notificacao n ON n.id = e.notificacao_id
                WHERE e.situacao = 'PENDENTE' AND e.no_resumo = FALSE AND e.proxima_tentativa_em <= ?
                ORDER BY e.proxima_tentativa_em LIMIT 100 FOR UPDATE OF e SKIP LOCKED""", Timestamp.from(agora));
        long limite = parametros.inteiro(CHAVE_TENTATIVAS);
        int enviados = 0;
        int desistencias = 0;
        for (Map<String, Object> item : fila) {
            UUID id = (UUID) item.get("id");
            Optional<String> para = contas.emailParaAviso((UUID) item.get("usuario_id"));
            if (para.isEmpty()) {
                jdbc.update("UPDATE notificacao_email SET situacao = 'DESCARTADO' WHERE id = ?", id);
                continue;
            }
            String texto = item.get("mensagem") + (item.get("link") == null ? ""
                    : "\n\nVeja na plataforma: " + conta.webUrl() + item.get("link"))
                    + "\n\nVocê pode escolher como receber estes avisos em " + conta.webUrl() + "/notificacoes.";
            if (email.tentarEnviar(new EnvioDeEmail.MensagemEmail(para.get(), "Orbit Hub: " + item.get("titulo"),
                    texto, "aviso"))) {
                jdbc.update("UPDATE notificacao_email SET situacao = 'ENVIADO', enviado_em = ?, tentativas = tentativas + 1 "
                        + "WHERE id = ?", Timestamp.from(agora), id);
                enviados++;
            } else if (reagendar(id, ((Number) item.get("tentativas")).intValue() + 1, limite, agora)) {
                desistencias++;
            }
        }
        return new Resultado(enviados, desistencias, enviados + " enviado(s) de " + fila.size());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Reagendar após falha
     * Objetivo: Marcar a falha e agendar a próxima tentativa, ou desistir no limite.
     * Decisões: Devolve verdadeiro quando desistiu.
     */
    private boolean reagendar(UUID id, int tentativas, long limite, Instant agora) {
        if (tentativas >= limite) {
            jdbc.update("UPDATE notificacao_email SET situacao = 'FALHOU', tentativas = ?, ultimo_erro = ? WHERE id = ?",
                    tentativas, "Servidor de e-mail recusou o envio", id);
            return true;
        }
        Duration espera = ESPERAS.get(Math.min(tentativas - 1, ESPERAS.size() - 1));
        jdbc.update("UPDATE notificacao_email SET tentativas = ?, proxima_tentativa_em = ?, ultimo_erro = ? WHERE id = ?",
                tentativas, Timestamp.from(agora.plus(espera)), "Servidor de e-mail recusou o envio", id);
        return false;
    }
}
