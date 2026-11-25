package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
 * Cartão Jira: KAN-107
 * Título: R11 – Contas inativas
 * Objetivo: Avisar as contas sem acesso há conta.inatividade_meses, desativar as que não voltaram em
 *           conta.aviso_inatividade_dias depois do aviso e alertar o atendimento sobre startups e organizações sem
 *           administrador ativo.
 * Decisões: Semanal (segunda, 5h). O aviso é obrigatório (segurança da conta, sino e e-mail). Entrar de novo limpa o
 *           aviso. Contas da equipe têm a revisão de acessos própria (R16) e ficam de fora.
 */
@Component
class ContasInativas extends RotinaBase {

    private final JdbcTemplate jdbc;
    private final LeituraDeParametros parametros;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-107
     * Título: Criação
     * Objetivo: Registrar a rotina com os parâmetros.
     * Decisões: Injeção por construtor.
     */
    ContasInativas(JdbcTemplate jdbc, LeituraDeParametros parametros, NotificacoesService notificacoes, ConsultaDeContas contas) {
        super("R11", "Avisa e desativa contas inativas; alerta startups e organizações sem administrador", "0 0 5 * * MON", "identidade",
                List.of("conta.inatividade_meses", "conta.aviso_inatividade_dias"));
        this.jdbc = jdbc;
        this.parametros = parametros;
        this.notificacoes = notificacoes;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-107
     * Título: Executar
     * Objetivo: Desativar os avisados que não voltaram, avisar os novos inativos e alertar os sem administrador.
     * Decisões: Desativa antes de avisar, para não desativar quem acabou de ser avisado.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        Timestamp t = Timestamp.from(agora);
        int desativadas = jdbc.update("UPDATE usuario SET status = 'DESATIVADO', atualizado_em = ?, versao = versao + 1 WHERE status = 'ATIVO' AND NOT interna "
                + "AND aviso_inatividade_em < ?", t, Timestamp.from(agora.minus(Duration.ofDays(parametros.inteiro("conta.aviso_inatividade_dias")))));
        Timestamp limite = Timestamp.from(agora.atOffset(ZoneOffset.UTC).minusMonths(parametros.inteiro("conta.inatividade_meses")).toInstant());
        List<UUID> avisar = jdbc.queryForList("SELECT id FROM usuario WHERE status = 'ATIVO' AND NOT interna AND aviso_inatividade_em IS NULL "
                + "AND COALESCE(ultimo_acesso_em, criado_em) < ?", UUID.class, limite);
        for (UUID u : avisar) {
            notificacoes.notificar(u, TipoDeNotificacao.SEGURANCA_CONTA, "Sua conta está sem uso", "Faz tempo que você não entra no Orbit Hub. Entre em até "
                    + parametros.inteiro("conta.aviso_inatividade_dias") + " dias para manter a conta ativa.", "/entrar");
            jdbc.update("UPDATE usuario SET aviso_inatividade_em = ? WHERE id = ?", t, u);
        }
        List<String> orfas = jdbc.queryForList("SELECT 'Startup ' || s.nome FROM startup s WHERE s.situacao <> 'RASCUNHO' AND NOT EXISTS (SELECT 1 FROM "
                + "startup_membro m JOIN usuario u ON u.id = m.usuario_id WHERE m.startup_id = s.id AND m.saiu_em IS NULL AND m.papel = 'FUNDADOR_ADMINISTRADOR' "
                + "AND u.status = 'ATIVO') UNION ALL SELECT 'Organização ' || g.nome FROM organizacao g WHERE EXISTS (SELECT 1 FROM organizacao_membro x "
                + "WHERE x.organizacao_id = g.id) AND NOT EXISTS (SELECT 1 FROM organizacao_membro m JOIN usuario u ON u.id = m.usuario_id WHERE "
                + "m.organizacao_id = g.id AND m.saiu_em IS NULL AND m.papel = 'ADMINISTRADOR' AND u.status = 'ATIVO')", String.class);
        if (!orfas.isEmpty()) {
            notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(Papel.SUPORTE_N2, Papel.ADMIN)), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO,
                    "Sem administrador ativo", orfas.size() + " cadastro(s) sem administrador ativo: " + String.join(", ", orfas.stream().limit(5).toList())
                            + (orfas.size() > 5 ? "…" : "") + ". Avalie a transferência de titularidade.", "/admin/atendimento");
        }
        return new Resultado(desativadas + avisar.size(), 0, avisar.size() + " aviso(s), " + desativadas + " desativada(s), " + orfas.size()
                + " sem administrador");
    }
}
