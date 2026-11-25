package br.com.orbithub.privacidade.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
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
 * Hora: 20:54
 * Cartão Jira: KAN-91
 * Título: Incidentes com dados pessoais
 * Objetivo: Registrar a suspeita de incidente (qualquer pessoa da equipe), avisar na hora o encarregado e a equipe
 *           técnica (administradores), documentar dados e titulares afetados, contenção, risco e decisões, registrar a
 *           comunicação à ANPD e aos titulares no prazo e encerrar só com lições aprendidas e ações preventivas.
 * Decisões: Prazo de comunicação = lgpd.prazo_comunicacao_dias_uteis a partir da detecção (provisório, validar com o
 *           jurídico a regra da ANPD). Risco ALTO exige as duas comunicações para encerrar. Cada mudança fica no histórico.
 *           Roteiro em docs/operacao/roteiro-de-incidentes.md.
 */
@Service
public class IncidentesService {

    static final String PRAZO = "lgpd.prazo_comunicacao_dias_uteis";
    private static final Set<String> RISCOS = Set.of("BAIXO", "MEDIO", "ALTO");

    private final JdbcTemplate jdbc;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    public IncidentesService(JdbcTemplate jdbc, NotificacoesService notificacoes, ConsultaDeContas contas, LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.notificacoes = notificacoes;
        this.contas = contas;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Registrar suspeita
     * Objetivo: Abrir o incidente e avisar o encarregado e a equipe técnica.
     * Decisões: Detecção no futuro é recusada; sem data, vale agora.
     */
    @Transactional
    public Incidente registrar(UUID eu, NovoIncidente n) {
        Instant agora = relogio.instant();
        if (n == null || n.titulo() == null || n.titulo().isBlank() || n.titulo().length() > 160) {
            throw new RegraDeNegocioException("titulo_invalido", "Dê um título com até 160 caracteres.");
        }
        if (n.descricao() == null || n.descricao().isBlank()) {
            throw new RegraDeNegocioException("descricao_invalida", "Descreva o que aconteceu.");
        }
        Instant detectado = n.detectadoEm() == null ? agora : n.detectadoEm();
        if (detectado.isAfter(agora)) {
            throw new RegraDeNegocioException("data_invalida", "A detecção não pode estar no futuro.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO incidente (id, titulo, descricao, dados_afetados, titulares_afetados, detectado_em, registrado_por, prazo_comunicacao, situacao, "
                + "atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ABERTO', ?)", id, n.titulo().strip(), n.descricao().strip(), vazio(n.dadosAfetados()),
                vazio(n.titularesAfetados()), Timestamp.from(detectado), eu, Timestamp.from(DiasUteis.somar(detectado, parametros.inteiro(PRAZO))),
                Timestamp.from(agora));
        historico(id, eu, "REGISTRO", n.titulo().strip());
        notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(Papel.ENCARREGADO_DADOS, Papel.ADMIN)), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO,
                "Incidente de segurança registrado", n.titulo().strip(), "/admin/privacidade");
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Listar incidentes
     * Objetivo: Listar os incidentes, abertos primeiro.
     * Decisões: Pela data de detecção, mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<Incidente> listar() {
        return jdbc.queryForList("SELECT id FROM incidente ORDER BY CASE situacao WHEN 'ENCERRADO' THEN 1 ELSE 0 END, detectado_em DESC", UUID.class)
                .stream().map(this::ler).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Avaliar
     * Objetivo: Registrar dados e titulares afetados, contenção, risco e decisões.
     * Decisões: Com a contenção registrada, o incidente passa a CONTIDO. Campos vazios não apagam o que havia.
     */
    @Transactional
    public Incidente avaliar(UUID eu, UUID id, Avaliacao a) {
        Map<String, Object> i = aberto(id);
        if (a.risco() != null && !RISCOS.contains(a.risco())) {
            throw new RegraDeNegocioException("risco_invalido", "Risco: BAIXO, MEDIO ou ALTO.");
        }
        String contencao = vazio(a.contencao()) != null ? a.contencao().strip() : (String) i.get("contencao");
        jdbc.update("UPDATE incidente SET dados_afetados = COALESCE(?, dados_afetados), titulares_afetados = COALESCE(?, titulares_afetados), contencao = ?, "
                + "risco = COALESCE(?, risco), decisoes = COALESCE(?, decisoes), situacao = ?, atualizado_em = ? WHERE id = ?", vazio(a.dadosAfetados()),
                vazio(a.titularesAfetados()), contencao, a.risco(), vazio(a.decisoes()), contencao == null ? "ABERTO" : "CONTIDO",
                Timestamp.from(relogio.instant()), id);
        historico(id, eu, "AVALIACAO", a.risco() == null ? null : "Risco " + a.risco());
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Registrar comunicação
     * Objetivo: Marcar a comunicação feita à ANPD ou aos titulares.
     * Decisões: A comunicação é feita fora do sistema; aqui fica a data.
     */
    @Transactional
    public Incidente comunicar(UUID eu, UUID id, String destino) {
        aberto(id);
        String coluna = switch (destino == null ? "" : destino) {
            case "ANPD" -> "comunicado_anpd_em";
            case "TITULARES" -> "comunicado_titulares_em";
            default -> throw new RegraDeNegocioException("destino_invalido", "Comunicação: ANPD ou TITULARES.");
        };
        Timestamp agora = Timestamp.from(relogio.instant());
        jdbc.update("UPDATE incidente SET " + coluna + " = COALESCE(" + coluna + ", ?), atualizado_em = ? WHERE id = ?", agora, agora, id);
        historico(id, eu, "COMUNICACAO_" + destino, null);
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Encerrar
     * Objetivo: Encerrar o incidente com as lições aprendidas e as ações preventivas.
     * Decisões: Exige contenção e risco avaliado; risco ALTO exige as duas comunicações.
     */
    @Transactional
    public Incidente encerrar(UUID eu, UUID id, String licoes, String acoes) {
        Map<String, Object> i = aberto(id);
        if (vazio(licoes) == null || vazio(acoes) == null) {
            throw new RegraDeNegocioException("licoes_obrigatorias", "Registre as lições aprendidas e as ações preventivas.");
        }
        if (i.get("contencao") == null || i.get("risco") == null) {
            throw new RegraDeNegocioException("avaliacao_pendente", "Registre a contenção e o risco antes de encerrar.");
        }
        if ("ALTO".equals(i.get("risco")) && (i.get("comunicado_anpd_em") == null || i.get("comunicado_titulares_em") == null)) {
            throw new RegraDeNegocioException("comunicacao_pendente", "Com risco alto, registre a comunicação à ANPD e aos titulares antes de encerrar.");
        }
        Timestamp agora = Timestamp.from(relogio.instant());
        jdbc.update("UPDATE incidente SET licoes = ?, acoes_preventivas = ?, situacao = 'ENCERRADO', encerrado_em = ?, atualizado_em = ? WHERE id = ?",
                licoes.strip(), acoes.strip(), agora, agora, id);
        historico(id, eu, "ENCERRAMENTO", null);
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: aberto
     * Objetivo: Rotina auxiliar aberto.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> aberto(UUID id) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM incidente WHERE id = ?", id);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Incidente não encontrado.");
        }
        if ("ENCERRADO".equals(l.get(0).get("situacao"))) {
            throw new RegraDeNegocioException("incidente_encerrado", "Este incidente já foi encerrado.");
        }
        return l.get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: historico
     * Objetivo: Rotina auxiliar historico.
     * Decisões: Ver a documentação da classe.
     */
    private void historico(UUID id, UUID eu, String acao, String detalhe) {
        jdbc.update("INSERT INTO incidente_historico (id, incidente_id, autor_id, acao, detalhe, em) VALUES (?, ?, ?, ?, ?, ?)", UUID.randomUUID(), id, eu, acao,
                detalhe, Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: ler
     * Objetivo: Rotina auxiliar ler.
     * Decisões: Ver a documentação da classe.
     */
    private Incidente ler(UUID id) {
        Map<String, Object> i = jdbc.queryForMap("SELECT * FROM incidente WHERE id = ?", id);
        List<Evento> eventos = jdbc.query("SELECT autor_id, acao, detalhe, em FROM incidente_historico WHERE incidente_id = ? ORDER BY em", (rs, n) ->
                new Evento(contas.nome(rs.getObject(1, UUID.class)).orElse(null), rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant()), id);
        Instant prazo = instante(i.get("prazo_comunicacao"));
        boolean pendente = "ALTO".equals(i.get("risco")) && (i.get("comunicado_anpd_em") == null || i.get("comunicado_titulares_em") == null);
        return new Incidente(id, (String) i.get("titulo"), (String) i.get("descricao"), (String) i.get("dados_afetados"), (String) i.get("titulares_afetados"),
                instante(i.get("detectado_em")), (String) i.get("contencao"), (String) i.get("risco"), (String) i.get("decisoes"), prazo,
                pendente && prazo.isBefore(relogio.instant()), instante(i.get("comunicado_anpd_em")), instante(i.get("comunicado_titulares_em")),
                (String) i.get("licoes"), (String) i.get("acoes_preventivas"), (String) i.get("situacao"), eventos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: instante
     * Objetivo: Rotina auxiliar instante.
     * Decisões: Ver a documentação da classe.
     */
    private static Instant instante(Object o) {
        return o instanceof Timestamp t ? t.toInstant() : null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: vazio
     * Objetivo: Rotina auxiliar vazio.
     * Decisões: Ver a documentação da classe.
     */
    private static String vazio(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Dados do incidente
     * Objetivo: Tipar os pedidos e as respostas dos incidentes.
     * Decisões: comunicacaoAtrasada só com risco alto e prazo vencido.
     */
    public record NovoIncidente(String titulo, String descricao, String dadosAfetados, String titularesAfetados, Instant detectadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Avaliacao
     * Objetivo: Rotina auxiliar Avaliacao.
     * Decisões: Ver a documentação da classe.
     */
    public record Avaliacao(String dadosAfetados, String titularesAfetados, String contencao, String risco, String decisoes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: Evento
     * Objetivo: Rotina auxiliar Evento.
     * Decisões: Ver a documentação da classe.
     */
    public record Evento(String autor, String acao, String detalhe, Instant em) {
    }

    public record Incidente(UUID id, String titulo, String descricao, String dadosAfetados, String titularesAfetados, Instant detectadoEm, String contencao,
                            String risco, String decisoes, Instant prazoComunicacao, boolean comunicacaoAtrasada, Instant comunicadoAnpdEm,
                            Instant comunicadoTitularesEm, String licoes, String acoesPreventivas, String situacao, List<Evento> historico) {
    }
}
