package br.com.orbithub.moderacao.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86
 * Título: Recursos
 * Objetivo: Receber o recurso (uma vez) contra a recusa de verificação, a recusa da curadoria ou a sanção da
 *           moderação, dentro de recurso.prazo_dias; distribuir ao mesmo time; julgar por outra pessoa, dentro de
 *           recurso.sla_dias_uteis; e aplicar o resultado.
 * Decisões: Durante o recurso a decisão original vale. Julga quem tem o papel do time (analista, curador ou moderador)
 *           ou o administrador, nunca quem decidiu antes. Deferido: a sanção é revogada; a verificação volta para a
 *           fila de análise; a startup recusada volta para rascunho e pode ser reenviada. A decisão é final.
 */
@Service
public class RecursosService {

    static final String PRAZO = "recurso.prazo_dias";
    static final String SLA = "recurso.sla_dias_uteis";

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Origem do recurso
     * Objetivo: Enumerar as decisões que admitem recurso e o papel que julga cada uma.
     * Decisões: O administrador julga qualquer uma.
     */
    public enum Origem {
        VERIFICACAO(Papel.ANALISTA_VERIFICACAO), CURADORIA(Papel.CURADOR), SANCAO(Papel.MODERADOR);

        private final Papel papel;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Origem
     * Objetivo: Rotina auxiliar Origem.
     * Decisões: Ver a documentação da classe.
     */
        Origem(Papel papel) {
            this.papel = papel;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-86
         * Título: Papel que julga
         * Objetivo: Devolver o papel do time que julga o recurso.
         * Decisões: Mesmo time da decisão original.
         */
        public Papel papel() {
            return papel;
        }
    }

    private final JdbcTemplate jdbc;
    private final ModeracaoService moderacao;
    private final StartupsService startups;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    public RecursosService(JdbcTemplate jdbc, ModeracaoService moderacao, StartupsService startups, ConsultaDeContas contas,
                           NotificacoesService notificacoes, LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.moderacao = moderacao;
        this.startups = startups;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Recorrer
     * Objetivo: Registrar o recurso com os argumentos e os anexos, conferindo que a decisão é da pessoa, que admite
     *           recurso e que o prazo não venceu.
     * Decisões: Um recurso por decisão. O time recebe o aviso.
     */
    @Transactional
    public Recurso recorrer(UUID eu, NovoRecurso n) {
        if (n == null || n.origemTipo() == null || n.origemId() == null) {
            throw new RegraDeNegocioException("origem_invalida", "Indique a decisão de que você quer recorrer.");
        }
        String argumentos = n.argumentos() == null ? "" : n.argumentos().strip();
        if (argumentos.length() < 20 || argumentos.length() > 4000) {
            throw new RegraDeNegocioException("argumentos_invalidos", "Explique os seus argumentos com 20 a 4.000 caracteres.");
        }
        Decisao d = decisao(eu, n.origemTipo(), n.origemId());
        Instant agora = relogio.instant();
        if (agora.isAfter(d.em().plus(Duration.ofDays(parametros.inteiro(PRAZO))))) {
            throw new RegraDeNegocioException("prazo_vencido", "O prazo de " + parametros.inteiro(PRAZO) + " dias para recorrer desta decisão terminou.");
        }
        Integer ja = jdbc.queryForObject("SELECT count(*) FROM recurso WHERE origem_tipo = ? AND origem_id = ?", Integer.class, n.origemTipo().name(),
                n.origemId());
        if (ja != null && ja > 0) {
            throw new RegraDeNegocioException("recurso_ja_apresentado", "Já existe um recurso contra esta decisão.");
        }
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO recurso (id, origem_tipo, origem_id, autor_id, decidido_antes, argumentos, anexos, situacao, prazo, criado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDENTE', ?, ?)", id, n.origemTipo().name(), n.origemId(), eu, d.por(), argumentos,
                n.anexos() == null || n.anexos().isBlank() ? null : n.anexos().strip(), Timestamp.from(DiasUteis.somar(agora, parametros.inteiro(SLA))),
                Timestamp.from(agora));
        notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(n.origemTipo().papel())), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO,
                "Novo recurso para julgar", "Recurso contra " + nome(n.origemTipo()) + ".", "/admin/recursos");
        notificacoes.notificar(eu, TipoDeNotificacao.MODERACAO, "Recurso recebido", "Recebemos o seu recurso. A resposta sai em até "
                + parametros.inteiro(SLA) + " dias úteis; até lá, a decisão original continua valendo.", "/ajuda/recursos");
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Meus recursos
     * Objetivo: Listar os recursos da pessoa, com o resultado e o motivo.
     * Decisões: Mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<Recurso> meus(UUID eu) {
        return jdbc.queryForList("SELECT id FROM recurso WHERE autor_id = ? ORDER BY criado_em DESC", UUID.class, eu).stream().map(this::ler).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Fila de recursos
     * Objetivo: Listar os recursos pendentes que a pessoa pode julgar, pelo prazo.
     * Decisões: Ficam de fora os que contestam decisão dela.
     */
    @Transactional(readOnly = true)
    public List<Recurso> fila(UUID eu, Set<Papel> papeis) {
        return jdbc.queryForList("SELECT id, origem_tipo FROM recurso WHERE situacao = 'PENDENTE' AND (decidido_antes IS NULL OR decidido_antes <> ?) "
                + "ORDER BY prazo", eu).stream().filter(r -> pode(papeis, Origem.valueOf((String) r.get("origem_tipo"))))
                .map(r -> ler((UUID) r.get("id"))).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Julgar
     * Objetivo: Deferir ou indeferir o recurso com o motivo, aplicar o resultado e avisar a pessoa.
     * Decisões: Quem decidiu antes não julga. A decisão do recurso é final na plataforma.
     */
    @Transactional
    public Recurso julgar(UUID eu, Set<Papel> papeis, UUID id, boolean deferir, String motivo) {
        Map<String, Object> r = linha(id);
        Origem origem = Origem.valueOf((String) r.get("origem_tipo"));
        if (!pode(papeis, origem)) {
            throw new AccessDeniedException("Recurso de outro time");
        }
        if (eu.equals(r.get("decidido_antes"))) {
            throw new RegraDeNegocioException("julgador_impedido", "Quem tomou a decisão original não julga o recurso. Ele fica com outra pessoa do time.");
        }
        if (!"PENDENTE".equals(r.get("situacao"))) {
            throw new RegraDeNegocioException("recurso_julgado", "Este recurso já foi julgado.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("motivo_obrigatorio", "Explique o motivo da decisão do recurso.");
        }
        UUID origemId = (UUID) r.get("origem_id");
        if (deferir) {
            switch (origem) {
                case SANCAO -> moderacao.revogar(origemId);
                case VERIFICACAO -> reabrirVerificacao(origemId);
                case CURADORIA -> reabrirCuradoria(origemId);
            }
        }
        Instant agora = relogio.instant();
        jdbc.update("UPDATE recurso SET situacao = ?, julgado_por = ?, julgado_em = ?, motivo = ? WHERE id = ?", deferir ? "DEFERIDO" : "INDEFERIDO", eu,
                Timestamp.from(agora), motivo.strip(), id);
        notificacoes.notificar((UUID) r.get("autor_id"), TipoDeNotificacao.MODERACAO, deferir ? "Recurso aceito" : "Recurso negado",
                (deferir ? "O seu recurso contra " + nome(origem) + " foi aceito. " : "O seu recurso contra " + nome(origem) + " foi negado. ")
                        + "Motivo: " + motivo.strip(), "/ajuda/recursos");
        return ler(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: reabrirVerificacao
     * Objetivo: Rotina auxiliar reabrirVerificacao.
     * Decisões: Ver a documentação da classe.
     */
    private void reabrirVerificacao(UUID pedido) {
        Map<String, Object> p = jdbc.queryForMap("SELECT sujeito_tipo, sujeito_id FROM pedido_verificacao WHERE id = ?", pedido);
        Integer abertos = jdbc.queryForObject("SELECT count(*) FROM pedido_verificacao WHERE sujeito_tipo = ? AND sujeito_id = ? AND situacao IN "
                + "('RASCUNHO', 'EM_ANALISE', 'PENDENTE_COMPLEMENTO')", Integer.class, p.get("sujeito_tipo"), p.get("sujeito_id"));
        if (abertos != null && abertos > 0) {
            throw new RegraDeNegocioException("pedido_em_aberto", "Já existe outro pedido de verificação em aberto para este cadastro.");
        }
        jdbc.update("UPDATE pedido_verificacao SET situacao = 'EM_ANALISE', responsavel_id = NULL, decidido_por = NULL, decidido_em = NULL, motivo = NULL, "
                + "versao = versao + 1 WHERE id = ?", pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: reabrirCuradoria
     * Objetivo: Rotina auxiliar reabrirCuradoria.
     * Decisões: Ver a documentação da classe.
     */
    private void reabrirCuradoria(UUID revisao) {
        UUID startup = jdbc.queryForObject("SELECT startup_id FROM revisao_curadoria WHERE id = ?", UUID.class, revisao);
        jdbc.update("UPDATE startup SET situacao = 'RASCUNHO', versao = versao + 1 WHERE id = ? AND situacao = 'RECUSADA'", startup);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: decisao
     * Objetivo: Rotina auxiliar decisao.
     * Decisões: Ver a documentação da classe.
     */
    private Decisao decisao(UUID eu, Origem origem, UUID id) {
        switch (origem) {
            case VERIFICACAO -> {
                Map<String, Object> p = umaLinha("SELECT criado_por, situacao, decidido_por, decidido_em FROM pedido_verificacao WHERE id = ?", id);
                if (!eu.equals(p.get("criado_por"))) {
                    throw new AccessDeniedException("Pedido de outra pessoa");
                }
                exigir("RECUSADO".equals(p.get("situacao")), "Só a recusa da verificação admite recurso.");
                return new Decisao((UUID) p.get("decidido_por"), instante(p.get("decidido_em")));
            }
            case CURADORIA -> {
                Map<String, Object> r = umaLinha("SELECT startup_id, situacao, decidida_por, decidida_em FROM revisao_curadoria WHERE id = ?", id);
                if (!startups.fundadores((UUID) r.get("startup_id"), true).contains(eu)) {
                    throw new AccessDeniedException("Startup de outra pessoa");
                }
                exigir("RECUSADA".equals(r.get("situacao")), "Só a recusa da curadoria admite recurso.");
                return new Decisao((UUID) r.get("decidida_por"), instante(r.get("decidida_em")));
            }
            default -> {
                Map<String, Object> s = moderacao.sancaoParaRecurso(eu, id);
                exigir(!((String) s.get("tipo")).startsWith("PREVENTIVA") && "ATIVA".equals(s.get("situacao")),
                        "Só a sanção em vigor admite recurso (medidas preventivas não).");
                return new Decisao((UUID) s.get("aplicada_por"), instante(s.get("inicio")));
            }
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: ler
     * Objetivo: Rotina auxiliar ler.
     * Decisões: Ver a documentação da classe.
     */
    private Recurso ler(UUID id) {
        Map<String, Object> r = linha(id);
        UUID julgador = (UUID) r.get("julgado_por");
        UUID autor = (UUID) r.get("autor_id");
        return new Recurso(id, Origem.valueOf((String) r.get("origem_tipo")), (UUID) r.get("origem_id"), contas.nome(autor).orElse(null),
                (String) r.get("argumentos"), (String) r.get("anexos"), (String) r.get("situacao"), instante(r.get("prazo")), instante(r.get("criado_em")),
                julgador == null ? null : contas.nome(julgador).orElse(null), r.get("julgado_em") == null ? null : instante(r.get("julgado_em")),
                (String) r.get("motivo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: linha
     * Objetivo: Rotina auxiliar linha.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> linha(UUID id) {
        return umaLinha("SELECT * FROM recurso WHERE id = ?", id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: umaLinha
     * Objetivo: Rotina auxiliar umaLinha.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> umaLinha(String sql, UUID id) {
        List<Map<String, Object>> l = jdbc.queryForList(sql, id);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Não encontrado.");
        }
        return l.get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: exigir
     * Objetivo: Rotina auxiliar exigir.
     * Decisões: Ver a documentação da classe.
     */
    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraDeNegocioException("decisao_sem_recurso", mensagem);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: pode
     * Objetivo: Rotina auxiliar pode.
     * Decisões: Ver a documentação da classe.
     */
    private static boolean pode(Set<Papel> papeis, Origem origem) {
        return papeis.contains(Papel.ADMIN) || papeis.contains(origem.papel());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: nome
     * Objetivo: Rotina auxiliar nome.
     * Decisões: Ver a documentação da classe.
     */
    private static String nome(Origem origem) {
        return switch (origem) {
            case VERIFICACAO -> "a recusa da verificação";
            case CURADORIA -> "a recusa da curadoria";
            case SANCAO -> "a sanção da moderação";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: instante
     * Objetivo: Rotina auxiliar instante.
     * Decisões: Ver a documentação da classe.
     */
    private static Instant instante(Object o) {
        return o instanceof Timestamp t ? t.toInstant() : o instanceof java.time.OffsetDateTime d ? d.toInstant() : (Instant) o;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Decisao
     * Objetivo: Rotina auxiliar Decisao.
     * Decisões: Ver a documentação da classe.
     */
    private record Decisao(UUID por, Instant em) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Dados do recurso
     * Objetivo: Tipar o pedido e a resposta do recurso.
     * Decisões: Anexos como texto (links ou nomes dos documentos enviados à verificação).
     */
    public record NovoRecurso(Origem origemTipo, UUID origemId, String argumentos, String anexos) {
    }

    public record Recurso(UUID id, Origem origemTipo, UUID origemId, String autor, String argumentos, String anexos, String situacao, Instant prazo,
                          Instant criadoEm, String julgadoPor, Instant julgadoEm, String motivo) {
    }
}
