package br.com.orbithub.empreendimentos.service;

import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.empreendimentos.domain.InvestidorBloqueado;
import org.springframework.context.ApplicationEventPublisher;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.OrganizacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-67
 * Título: Bloqueio de investidores pela startup
 * Objetivo: Deixar o fundador administrador bloquear e desbloquear uma pessoa (pelo e-mail da conta) ou uma organização
 *           (pelo CNPJ), com histórico, e responder aos outros módulos se a startup bloqueou quem está atuando.
 * Decisões: Silencioso: ninguém é avisado. Organização bloqueada vale para os membros enquanto atuam em nome dela; a
 *           pessoa bloqueada fica bloqueada em qualquer atuação. Catálogo, edições, interesse e (S8) Nível 2 consultam
 *           este serviço. Tabelas simples por JDBC.
 */
@Service
public class BloqueiosDaStartup {

    static final String PESSOA = "PESSOA";
    static final String ORGANIZACAO = "ORGANIZACAO";

    private final JdbcTemplate jdbc;
    private final StartupsService cadastro;
    private final ConsultaDeContas contas;
    private final OrganizacoesService organizacoes;
    private final Clock relogio;
    private final ApplicationEventPublisher eventos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Criação
     * Objetivo: Receber o JDBC, o cadastro, as contas, as organizações e o relógio.
     * Decisões: Injeção por construtor.
     */
    public BloqueiosDaStartup(JdbcTemplate jdbc, StartupsService cadastro, ConsultaDeContas contas, OrganizacoesService organizacoes,
                              Clock relogio, ApplicationEventPublisher eventos) {
        this.eventos = eventos;
        this.jdbc = jdbc;
        this.cadastro = cadastro;
        this.contas = contas;
        this.organizacoes = organizacoes;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Bloquear
     * Objetivo: Bloquear uma pessoa pelo e-mail ou uma organização pelo CNPJ.
     * Decisões: Só o fundador administrador. Recusa quem não existe. Idempotente. Avisa os outros módulos, para o Nível 2
     *           terminar na hora (KAN-30, 25/11/2026).
     */
    @Transactional
    public Bloqueio bloquear(UUID eu, UUID startupId, String tipo, String identificador) {
        cadastro.exigirAdministrador(eu, startupId);
        Alvo alvo = alvo(tipo, identificador);
        Instant agora = relogio.instant();
        int novos = jdbc.update("INSERT INTO startup_bloqueio (startup_id, alvo_tipo, alvo_id, rotulo, criado_por, criado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING", startupId, alvo.tipo(), alvo.id(), alvo.rotulo(), eu, Timestamp.from(agora));
        if (novos > 0) {
            historico(startupId, "BLOQUEIO", alvo, eu, agora);
            eventos.publishEvent(new InvestidorBloqueado(startupId, alvo.tipo(), alvo.id()));
        }
        return new Bloqueio(alvo.tipo(), alvo.id(), alvo.rotulo(), agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Desbloquear
     * Objetivo: Tirar o bloqueio.
     * Decisões: Só o fundador administrador; sem erro se não havia bloqueio.
     */
    @Transactional
    public void desbloquear(UUID eu, UUID startupId, String tipo, UUID alvoId) {
        cadastro.exigirAdministrador(eu, startupId);
        List<String> rotulos = jdbc.queryForList("DELETE FROM startup_bloqueio WHERE startup_id = ? AND alvo_tipo = ? AND alvo_id = ? RETURNING rotulo",
                String.class, startupId, tipo, alvoId);
        rotulos.forEach(r -> historico(startupId, "DESBLOQUEIO", new Alvo(tipo, alvoId, r), eu, relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Listar bloqueios
     * Objetivo: Listar os bloqueios ativos e o histórico.
     * Decisões: Qualquer membro da equipe vê.
     */
    @Transactional(readOnly = true)
    public Painel painel(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        List<Bloqueio> ativos = jdbc.query("SELECT alvo_tipo, alvo_id, rotulo, criado_em FROM startup_bloqueio WHERE startup_id = ? ORDER BY criado_em DESC",
                (rs, i) -> new Bloqueio(rs.getString(1), rs.getObject(2, UUID.class), rs.getString(3), rs.getTimestamp(4).toInstant()), startupId);
        List<Mudanca> mudancas = jdbc.query("SELECT acao, alvo_tipo, rotulo, por, em FROM startup_bloqueio_historico WHERE startup_id = ? ORDER BY em DESC",
                (rs, i) -> new Mudanca(rs.getString(1), rs.getString(2), rs.getString(3), contas.nome(rs.getObject(4, UUID.class)).orElse(null),
                        rs.getTimestamp(5).toInstant()), startupId);
        return new Painel(ativos, mudancas);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Está bloqueado
     * Objetivo: Dizer se a startup bloqueou a pessoa ou a organização em nome de quem ela atua.
     * Decisões: Serviço público para interesse, catálogo e Nível 2.
     */
    @Transactional(readOnly = true)
    public boolean bloqueia(UUID startupId, UUID pessoa, UUID organizacao) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM startup_bloqueio WHERE startup_id = ? AND ((alvo_tipo = 'PESSOA' AND alvo_id = ?) "
                + "OR (alvo_tipo = 'ORGANIZACAO' AND alvo_id = ?))", Integer.class, startupId, pessoa, organizacao);
        return n != null && n > 0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Startups que bloquearam
     * Objetivo: Listar as startups que bloquearam a pessoa ou a organização da atuação, para o catálogo esconder.
     * Decisões: Uma consulta.
     */
    @Transactional(readOnly = true)
    public Set<UUID> startupsQueBloquearam(UUID pessoa, UUID organizacao) {
        return new HashSet<>(jdbc.queryForList("SELECT startup_id FROM startup_bloqueio WHERE (alvo_tipo = 'PESSOA' AND alvo_id = ?) "
                + "OR (alvo_tipo = 'ORGANIZACAO' AND alvo_id = ?)", UUID.class, pessoa, organizacao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Resolver alvo
     * Objetivo: Achar a conta pelo e-mail ou a organização pelo CNPJ.
     * Decisões: Mensagem neutra quando não acha.
     */
    private Alvo alvo(String tipo, String identificador) {
        String texto = identificador == null ? "" : identificador.strip();
        if (PESSOA.equals(tipo)) {
            UUID conta = contas.contaAtivaDoEmail(texto.toLowerCase(java.util.Locale.ROOT))
                    .orElseThrow(() -> new RegraDeNegocioException("alvo_nao_encontrado", "Não há conta ativa com este e-mail."));
            return new Alvo(PESSOA, conta, contas.nome(conta).orElse(texto) + " (" + texto + ")");
        }
        if (ORGANIZACAO.equals(tipo)) {
            String cnpj = Cnpj.validar(texto).orElseThrow(() -> new RegraDeNegocioException("cnpj_invalido", "CNPJ inválido."));
            Map.Entry<UUID, String> org = organizacoes.porCnpj(cnpj)
                    .orElseThrow(() -> new RegraDeNegocioException("alvo_nao_encontrado", "Não há organização com este CNPJ na plataforma."));
            return new Alvo(ORGANIZACAO, org.getKey(), org.getValue() + " (" + Cnpj.formatar(cnpj) + ")");
        }
        throw new RegraDeNegocioException("tipo_invalido", "Bloqueie uma pessoa ou uma organização.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Registrar no histórico
     * Objetivo: Gravar o bloqueio ou o desbloqueio.
     * Decisões: Com autor.
     */
    private void historico(UUID startupId, String acao, Alvo alvo, UUID por, Instant em) {
        jdbc.update("INSERT INTO startup_bloqueio_historico (id, startup_id, acao, alvo_tipo, rotulo, por, em) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), startupId, acao, alvo.tipo(), alvo.rotulo(), por, Timestamp.from(em));
    }

    record Alvo(String tipo, UUID id, String rotulo) {
    }

    public record Bloqueio(String tipo, UUID alvoId, String rotulo, Instant em) {
    }

    public record Mudanca(String acao, String tipo, String rotulo, String por, Instant em) {
    }

    public record Painel(List<Bloqueio> ativos, List<Mudanca> historico) {
    }
}
