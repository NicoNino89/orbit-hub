package br.com.orbithub.conexoes.service;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-176
 * Título: Mensagens da conexão
 * Objetivo: Deixar a startup e o investidor conversarem dentro da conexão a partir do Match, com contador de não lidas
 *           e aviso pela central de notificações.
 * Decisões: Escreve só com a conexão aberta (Match, em análise, em negociação); depois fica só leitura. Investiu também
 *           lê. Documentos confidenciais não vão na conversa (Data Room). A equipe só acessa dentro de um caso de
 *           moderação (denúncia da conexão).
 */
@Service
public class MensagensDaConexao {

    private static final Set<Conexao.Situacao> ABERTAS = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO);
    private static final Set<Conexao.Situacao> LEGIVEIS = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO,
            Conexao.Situacao.INVESTIU, Conexao.Situacao.ENCERRADA);

    private final JdbcTemplate jdbc;
    private final ConexoesService conexoes;
    private final StartupsService startups;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    public MensagensDaConexao(JdbcTemplate jdbc, ConexoesService conexoes, StartupsService startups, ConsultaDeContas contas,
                              NotificacoesService notificacoes, Clock relogio) {
        this.jdbc = jdbc;
        this.conexoes = conexoes;
        this.startups = startups;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Conversa
     * Objetivo: Devolver as mensagens e marcar a leitura de quem abre.
     * Decisões: Só as partes; antes do Match não há conversa.
     */
    @Transactional
    public Conversa conversa(UUID eu, String organizacao, UUID conexaoId) {
        Conexao c = conexoes.ler(conexaoId);
        conexoes.ladoDe(eu, organizacao, c);
        if (!LEGIVEIS.contains(c.getSituacao())) {
            throw new RegraDeNegocioException("sem_conversa", "A conversa começa no Match.");
        }
        List<Mensagem> mensagens = jdbc.query("SELECT autor_id, lado, texto, em FROM conexao_mensagem WHERE conexao_id = ? ORDER BY em", (rs, i) -> {
            UUID autor = rs.getObject(1, UUID.class);
            return new Mensagem(contas.nome(autor).orElse(null), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant(), eu.equals(autor));
        }, conexaoId);
        jdbc.update("INSERT INTO conexao_leitura (conexao_id, usuario_id, lido_em) VALUES (?, ?, ?) ON CONFLICT (conexao_id, usuario_id) DO UPDATE "
                + "SET lido_em = EXCLUDED.lido_em", conexaoId, eu, Timestamp.from(relogio.instant()));
        return new Conversa(conexaoId, ABERTAS.contains(c.getSituacao()), mensagens);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Enviar
     * Objetivo: Gravar a mensagem e avisar o outro lado.
     * Decisões: Até 2.000 caracteres; só com a conexão aberta.
     */
    @Transactional
    public Conversa enviar(UUID eu, String organizacao, UUID conexaoId, String texto) {
        Conexao c = conexoes.ler(conexaoId);
        Conexao.Lado lado = conexoes.ladoDe(eu, organizacao, c);
        if (!ABERTAS.contains(c.getSituacao())) {
            throw new RegraDeNegocioException("conversa_encerrada", "A conversa só aceita mensagens com a conexão aberta.");
        }
        String t = texto == null ? "" : texto.strip();
        if (t.isEmpty() || t.length() > 2000) {
            throw new RegraDeNegocioException("mensagem_invalida", "Escreva a mensagem com até 2.000 caracteres.");
        }
        Instant agora = relogio.instant();
        jdbc.update("INSERT INTO conexao_mensagem (id, conexao_id, autor_id, lado, texto, em) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), conexaoId, eu,
                lado.name(), t, Timestamp.from(agora));
        String startup = startups.ler(c.getStartupId()).getNome();
        List<UUID> outros = lado == Conexao.Lado.STARTUP ? List.of(c.getPessoaId())
                : startups.fundadores(c.getStartupId(), false).stream().filter(u -> !u.equals(eu)).toList();
        notificacoes.notificarTodos(outros, TipoDeNotificacao.MENSAGEM, "Nova mensagem", (lado == Conexao.Lado.STARTUP ? startup : contas.nome(eu).orElse("O investidor"))
                + " enviou uma mensagem na conexão.", "/conexoes");
        return conversa(eu, organizacao, conexaoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Não lidas
     * Objetivo: Contar as mensagens de outras pessoas depois da última leitura, nas conexões da pessoa.
     * Decisões: Conta as conexões em que ela é o investidor (pessoa) ou fundadora da startup.
     */
    @Transactional(readOnly = true)
    public int naoLidas(UUID eu) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM conexao_mensagem m JOIN conexao c ON c.id = m.conexao_id LEFT JOIN conexao_leitura l "
                + "ON l.conexao_id = m.conexao_id AND l.usuario_id = ? WHERE m.autor_id <> ? AND (l.lido_em IS NULL OR m.em > l.lido_em) AND (c.pessoa_id = ? "
                + "OR c.startup_id IN (SELECT startup_id FROM startup_membro WHERE usuario_id = ? AND saiu_em IS NULL AND papel <> 'COLABORADOR'))",
                Integer.class, eu, eu, eu, eu);
        return n == null ? 0 : n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Dados da conversa
     * Objetivo: Tipar a conversa e a mensagem.
     * Decisões: "minha" marca as mensagens de quem vê.
     */
    public record Mensagem(String autor, String lado, String texto, Instant em, boolean minha) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: Conversa
     * Objetivo: Rotina auxiliar Conversa.
     * Decisões: Ver a documentação da classe.
     */
    public record Conversa(UUID conexaoId, boolean podeEscrever, List<Mensagem> mensagens) {
    }
}
