package br.com.orbithub.moderacao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ConflitosDeInteresse;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.identidade.service.SuspensaoDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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
 * Cartão Jira: KAN-99, KAN-100, KAN-101, KAN-102
 * Título: Moderação
 * Objetivo: Receber denúncias de perfis, organizações, startups, conexões e reuniões; agrupá-las num caso por objeto,
 *           com prioridade e prazo pela categoria; mostrar a fila e o histórico do denunciado ao moderador; aplicar
 *           medidas preventivas (suspender a conta, despublicar a startup ou tirar o Nível 2); registrar o parecer do
 *           jurídico; e decidir com sanções progressivas (advertência, suspensão de 7, 30 ou 90 dias e banimento, este com
 *           segunda aprovação), avisando o denunciado com o prazo para recorrer.
 * Decisões: O denunciante nunca aparece para o denunciado e só fica sabendo que o caso foi analisado. Prioridade pelo
 *           grupo da categoria (CRITICA, ALTA, NORMAL); denúncia nova de prioridade maior sobe o caso e encurta o prazo.
 *           Caso crítico avisa os administradores (líder do time) na hora. Na escala automática conta cada sanção
 *           não revogada do alvo. Medidas preventivas são desfeitas na decisão, salvo quando a sanção as mantém. O
 *           moderador com vínculo declarado com o denunciado não assume nem decide o caso (KAN-87).
 */
@Service
public class ModeracaoService {

    static final String SLA_CRITICA = "moderacao.sla_critica_horas";
    static final String SLA_ALTA = "moderacao.sla_alta_horas";
    static final String SLA_NORMAL = "moderacao.sla_normal_dias_uteis";
    static final String SUSPENSOES = "moderacao.suspensao_dias";
    static final String PRAZO_RECURSO = "recurso.prazo_dias";
    static final String CASO = "CASO_MODERACAO";
    private static final String DECIDIDO = "DECIDIDO";
    private static final List<String> PRIORIDADES = List.of("CRITICA", "ALTA", "NORMAL");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99, KAN-101
     * Título: Tipos da moderação
     * Objetivo: Enumerar o que pode ser denunciado, as decisões, as medidas preventivas e os alvos das sanções.
     * Decisões: ESCALA deixa o sistema escolher o próximo degrau.
     */
    public enum Objeto { PERFIL, ORGANIZACAO, STARTUP, CONEXAO, REUNIAO }

    public enum Decisao { SEM_ACAO, REMOCAO_CONTEUDO, ADVERTENCIA, SUSPENSAO, BANIMENTO, ESCALA }

    public enum Preventiva { SUSPENDER_CONTA, DESPUBLICAR, SUSPENDER_NIVEL2 }

    public enum Alvo { USUARIO, STARTUP }

    private final JdbcTemplate jdbc;
    private final ListasService listas;
    private final LeituraDeParametros parametros;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;
    private final StartupsService startups;
    private final SuspensaoDeContas suspensao;
    private final ConflitosDeInteresse conflitos;
    private final SegundaAprovacaoService aprovacoes;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public ModeracaoService(JdbcTemplate jdbc, ListasService listas, LeituraDeParametros parametros, NotificacoesService notificacoes,
                            ConsultaDeContas contas, StartupsService startups, SuspensaoDeContas suspensao, ConflitosDeInteresse conflitos,
                            SegundaAprovacaoService aprovacoes, Clock relogio) {
        this.jdbc = jdbc;
        this.listas = listas;
        this.parametros = parametros;
        this.notificacoes = notificacoes;
        this.contas = contas;
        this.startups = startups;
        this.suspensao = suspensao;
        this.conflitos = conflitos;
        this.aprovacoes = aprovacoes;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99, KAN-100, KAN-102
     * Título: Denunciar
     * Objetivo: Registrar a denúncia, juntá-la ao caso aberto do mesmo objeto (ou abrir um) e confirmar o recebimento.
     * Decisões: Não se denuncia a si mesmo nem duas vezes o mesmo caso aberto. Violação de confidencialidade entra
     *           como prioridade alta pela categoria.
     */
    @Transactional
    public Recebida denunciar(UUID eu, NovaDenuncia d) {
        if (d == null || d.objetoTipo() == null || d.objetoId() == null) {
            throw new RegraDeNegocioException("objeto_invalido", "Indique o que você está denunciando.");
        }
        if (d.objetoTipo() == Objeto.PERFIL && d.objetoId().equals(eu)) {
            throw new RegraDeNegocioException("objeto_invalido", "Você não pode denunciar o próprio perfil.");
        }
        conferirObjeto(d.objetoTipo(), d.objetoId());
        ItemDeLista categoria = listas.exigirAtivo(Lista.CATEGORIA_DENUNCIA, d.categoria());
        String descricao = d.descricao() == null ? "" : d.descricao().strip();
        if (descricao.length() < 10 || descricao.length() > 4000) {
            throw new RegraDeNegocioException("descricao_invalida", "Descreva o caso com 10 a 4.000 caracteres.");
        }
        String prioridade = categoria.getGrupo() != null && PRIORIDADES.contains(categoria.getGrupo()) ? categoria.getGrupo() : "NORMAL";
        Instant agora = relogio.instant();
        UUID caso = jdbc.query("SELECT id FROM caso_moderacao WHERE objeto_tipo = ? AND objeto_id = ? AND situacao <> 'DECIDIDO'",
                (rs, i) -> rs.getObject(1, UUID.class), d.objetoTipo().name(), d.objetoId()).stream().findFirst().orElse(null);
        boolean novo = caso == null;
        if (novo) {
            caso = UUID.randomUUID();
            jdbc.update("INSERT INTO caso_moderacao (id, objeto_tipo, objeto_id, prioridade, categoria, origem, situacao, prazo, criado_em, atualizado_em) "
                    + "VALUES (?, ?, ?, ?, ?, 'DENUNCIA', 'ABERTO', ?, ?, ?)", caso, d.objetoTipo().name(), d.objetoId(), prioridade, categoria.getCodigo(),
                    Timestamp.from(prazo(prioridade, agora)), Timestamp.from(agora), Timestamp.from(agora));
        } else {
            Integer repetida = jdbc.queryForObject("SELECT count(*) FROM denuncia WHERE caso_id = ? AND denunciante_id = ?", Integer.class, caso, eu);
            if (repetida != null && repetida > 0) {
                throw new RegraDeNegocioException("denuncia_repetida", "Você já denunciou este caso. Ele está em análise.");
            }
            String atual = jdbc.queryForObject("SELECT prioridade FROM caso_moderacao WHERE id = ?", String.class, caso);
            if (PRIORIDADES.indexOf(prioridade) < PRIORIDADES.indexOf(atual)) {
                jdbc.update("UPDATE caso_moderacao SET prioridade = ?, categoria = ?, prazo = LEAST(prazo, ?), atualizado_em = ? WHERE id = ?", prioridade,
                        categoria.getCodigo(), Timestamp.from(prazo(prioridade, agora)), Timestamp.from(agora), caso);
            } else {
                prioridade = atual;
            }
        }
        UUID protocolo = UUID.randomUUID();
        jdbc.update("INSERT INTO denuncia (id, caso_id, denunciante_id, categoria, descricao, evidencias, criada_em) VALUES (?, ?, ?, ?, ?, ?, ?)", protocolo,
                caso, eu, categoria.getCodigo(), descricao, limitar(d.evidencias(), 2000), Timestamp.from(agora));
        notificacoes.notificar(eu, TipoDeNotificacao.MODERACAO, "Denúncia recebida", "Recebemos a sua denúncia (protocolo "
                + protocolo.toString().substring(0, 8) + "). Avisaremos quando o caso for analisado.", "/ajuda/denuncias");
        if (novo || "CRITICA".equals(prioridade)) {
            Set<Papel> quem = "CRITICA".equals(prioridade) ? Set.of(Papel.ADMIN, Papel.MODERADOR) : Set.of(Papel.MODERADOR);
            notificacoes.notificarTodos(contas.equipeAtivaComPapeis(quem), TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO,
                    "CRITICA".equals(prioridade) ? "Denúncia crítica na fila" : "Nova denúncia na fila",
                    "Caso de prioridade " + prioridade.toLowerCase() + ": " + categoria.getRotulo() + ".", "/admin/moderacao");
        }
        return new Recebida(protocolo, agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99
     * Título: Minhas denúncias
     * Objetivo: Listar as denúncias feitas pela pessoa, com a informação de que o caso foi ou não analisado.
     * Decisões: Sem a decisão nem a sanção: o denunciante só sabe que foi analisado.
     */
    @Transactional(readOnly = true)
    public List<MinhaDenuncia> minhas(UUID eu) {
        return jdbc.query("SELECT d.id, c.objeto_tipo, d.categoria, d.criada_em, c.situacao FROM denuncia d JOIN caso_moderacao c ON c.id = d.caso_id "
                + "WHERE d.denunciante_id = ? ORDER BY d.criada_em DESC", (rs, i) -> new MinhaDenuncia(rs.getObject(1, UUID.class),
                Objeto.valueOf(rs.getString(2)), listas.rotulo(Lista.CATEGORIA_DENUNCIA, rs.getString(3)), rs.getTimestamp(4).toInstant(),
                DECIDIDO.equals(rs.getString(5))), eu);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100
     * Título: Fila de moderação
     * Objetivo: Listar os casos em aberto, dos críticos para os normais e, em cada prioridade, pelo prazo.
     * Decisões: Mostra se o prazo venceu e quem assumiu.
     */
    @Transactional(readOnly = true)
    public List<ItemDaFila> fila() {
        Instant agora = relogio.instant();
        return jdbc.query("SELECT c.id, c.objeto_tipo, c.objeto_id, c.prioridade, c.categoria, c.situacao, c.prazo, c.responsavel_id, "
                + "(SELECT count(*) FROM denuncia d WHERE d.caso_id = c.id), c.criado_em FROM caso_moderacao c WHERE c.situacao <> 'DECIDIDO' "
                + "ORDER BY CASE c.prioridade WHEN 'CRITICA' THEN 0 WHEN 'ALTA' THEN 1 ELSE 2 END, c.prazo", (rs, i) -> {
                    UUID resp = rs.getObject(8, UUID.class);
                    Instant prazo = rs.getTimestamp(7).toInstant();
                    return new ItemDaFila(rs.getObject(1, UUID.class), Objeto.valueOf(rs.getString(2)), rotuloDoObjeto(Objeto.valueOf(rs.getString(2)),
                            rs.getObject(3, UUID.class)), rs.getString(4), listas.rotulo(Lista.CATEGORIA_DENUNCIA, rs.getString(5)), rs.getString(6), prazo,
                            prazo.isBefore(agora), resp == null ? null : contas.nome(resp).orElse(null), rs.getInt(9), rs.getTimestamp(10).toInstant());
                });
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100, KAN-102
     * Título: Detalhe do caso
     * Objetivo: Mostrar as denúncias, o histórico do denunciado (sanções e casos anteriores), as medidas, o parecer e
     *           a decisão.
     * Decisões: Para cada denunciante, quantas denúncias dele terminaram sem ação (sinal de abuso).
     */
    @Transactional(readOnly = true)
    public Caso caso(UUID id) {
        Map<String, Object> c = linha(id);
        Objeto objeto = Objeto.valueOf((String) c.get("objeto_tipo"));
        UUID objetoId = (UUID) c.get("objeto_id");
        List<Denuncia> denuncias = jdbc.query("SELECT d.id, d.denunciante_id, d.categoria, d.descricao, d.evidencias, d.criada_em, "
                + "(SELECT count(*) FROM denuncia x JOIN caso_moderacao y ON y.id = x.caso_id WHERE x.denunciante_id = d.denunciante_id "
                + "AND y.situacao = 'DECIDIDO' AND y.decisao = 'SEM_ACAO') FROM denuncia d WHERE d.caso_id = ? ORDER BY d.criada_em", (rs, i) -> {
                    UUID quem = rs.getObject(2, UUID.class);
                    return new Denuncia(rs.getObject(1, UUID.class), quem == null ? null : contas.nome(quem).orElse(null),
                            listas.rotulo(Lista.CATEGORIA_DENUNCIA, rs.getString(3)), rs.getString(4), rs.getString(5), rs.getTimestamp(6).toInstant(), rs.getInt(7));
                }, id);
        Alvo alvoPadrao = alvoPadrao(objeto);
        List<Sancao> historico = alvoPadrao == null ? List.of() : sancoesDoAlvo(alvoPadrao, objetoId);
        List<CasoAnterior> anteriores = jdbc.query("SELECT id, categoria, decisao, decidido_em FROM caso_moderacao WHERE objeto_tipo = ? AND objeto_id = ? "
                + "AND id <> ? AND situacao = 'DECIDIDO' ORDER BY decidido_em DESC", (rs, i) -> new CasoAnterior(rs.getObject(1, UUID.class),
                listas.rotulo(Lista.CATEGORIA_DENUNCIA, rs.getString(2)), rs.getString(3), rs.getTimestamp(4).toInstant()), objeto.name(), objetoId, id);
        List<Sancao> medidas = jdbc.query("SELECT id, alvo_tipo, alvo_id, tipo, dias, inicio, fim, motivo, situacao FROM sancao WHERE caso_id = ? ORDER BY inicio",
                (rs, i) -> sancao(rs), id);
        UUID resp = (UUID) c.get("responsavel_id");
        return new Caso(id, objeto, objetoId, rotuloDoObjeto(objeto, objetoId), (String) c.get("prioridade"),
                listas.rotulo(Lista.CATEGORIA_DENUNCIA, (String) c.get("categoria")), (String) c.get("situacao"), instante(c.get("prazo")),
                resp == null ? null : contas.nome(resp).orElse(null), denuncias, historico, anteriores, medidas, (String) c.get("parecer_juridico"),
                (String) c.get("decisao"), (String) c.get("motivo"), (String) c.get("observacao"), alvoPadrao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100, KAN-87
     * Título: Assumir o caso
     * Objetivo: Pôr o caso em análise com o moderador, conferindo conflito de interesse.
     * Decisões: Outro moderador pode assumir no lugar (redistribuição).
     */
    @Transactional
    public Caso assumir(UUID eu, UUID id) {
        Map<String, Object> c = aberto(id);
        semConflito(eu, id, c);
        jdbc.update("UPDATE caso_moderacao SET responsavel_id = ?, situacao = CASE WHEN situacao = 'ABERTO' THEN 'EM_ANALISE' ELSE situacao END, "
                + "atualizado_em = ? WHERE id = ?", eu, Timestamp.from(relogio.instant()), id);
        return caso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101, KAN-102
     * Título: Medida preventiva
     * Objetivo: Suspender a conta, despublicar a startup ou tirar o Nível 2 de um investidor antes da decisão final.
     * Decisões: Suspensão da conta e despublicação só em caso crítico; tirar o Nível 2 em caso crítico ou de violação
     *           de confidencialidade. Motivo obrigatório. A medida fica registrada como sanção preventiva do caso.
     */
    @Transactional
    public Caso preventiva(UUID eu, UUID id, Preventiva tipo, UUID alvoId, String motivo) {
        Map<String, Object> c = aberto(id);
        semConflito(eu, id, c);
        if (motivo == null || motivo.isBlank()) {
            throw new RegraDeNegocioException("motivo_obrigatorio", "Registre o motivo da medida preventiva.");
        }
        boolean critico = "CRITICA".equals(c.get("prioridade"));
        boolean vazamento = "VAZAMENTO_CONFIDENCIAL".equals(c.get("categoria"));
        if (tipo == null || (!critico && !(tipo == Preventiva.SUSPENDER_NIVEL2 && vazamento))) {
            throw new RegraDeNegocioException("medida_indisponivel",
                    "Medidas preventivas só em casos críticos (ou tirar o Nível 2 em violação de confidencialidade).");
        }
        Objeto objeto = Objeto.valueOf((String) c.get("objeto_tipo"));
        UUID objetoId = (UUID) c.get("objeto_id");
        Alvo alvo = tipo == Preventiva.DESPUBLICAR ? Alvo.STARTUP : Alvo.USUARIO;
        UUID quem = alvoId != null ? alvoId : (alvoPadrao(objeto) == alvo ? objetoId : null);
        if (quem == null) {
            throw new RegraDeNegocioException("alvo_obrigatorio", "Indique a conta ou a startup que recebe a medida.");
        }
        conferirAlvo(alvo, quem);
        switch (tipo) {
            case SUSPENDER_CONTA -> suspensao.suspender(quem, Usuario.BANIDA);
            case DESPUBLICAR -> startups.despublicarPorModeracao(quem);
            case SUSPENDER_NIVEL2 -> suspensao.suspenderNivel2(quem);
        }
        registrarSancao(id, alvo, quem, "PREVENTIVA_" + tipo.name(), null, null, motivo.strip(), eu, null);
        ContextoDeAuditoria.descrever("MEDIDA_PREVENTIVA", CASO, id, tipo.name());
        ContextoDeAuditoria.justificar(motivo);
        jdbc.update("UPDATE caso_moderacao SET situacao = 'EM_ANALISE', responsavel_id = COALESCE(responsavel_id, ?), atualizado_em = ? WHERE id = ?", eu,
                Timestamp.from(relogio.instant()), id);
        return caso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102
     * Título: Acessos ao Nível 2
     * Objetivo: Levantar quem acessou as informações confidenciais da startup denunciada e quando.
     * Decisões: Só em caso sobre startup; justificativa obrigatória, registrada na auditoria (KAN-80).
     */
    @Transactional(readOnly = true)
    public List<AcessoConfidencial> acessosNivel2(UUID id, String justificativa) {
        Map<String, Object> c = linha(id);
        if (!Objeto.STARTUP.name().equals(c.get("objeto_tipo"))) {
            throw new RegraDeNegocioException("objeto_invalido", "O levantamento de acessos é feito em casos sobre uma startup.");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("justificativa_obrigatoria", "Registre por que você precisa consultar os acessos.");
        }
        ContextoDeAuditoria.descrever("CONSULTA_ACESSOS_NIVEL2", CASO, id, "Levantamento de acessos ao Nível 2");
        ContextoDeAuditoria.justificar(justificativa);
        return jdbc.query("SELECT c.pessoa_id, a.liberado_em, a.valido_ate, a.encerrado_em, a.encerrado_motivo, "
                + "(SELECT count(*) FROM dataroom_link l WHERE l.acesso_id = a.id) FROM acesso_nivel2 a JOIN conexao c ON c.id = a.conexao_id "
                + "WHERE a.startup_id = ? ORDER BY a.liberado_em", (rs, i) -> {
                    UUID pessoa = rs.getObject(1, UUID.class);
                    Timestamp fim = rs.getTimestamp(4);
                    return new AcessoConfidencial(pessoa, contas.nome(pessoa).orElse(null), rs.getTimestamp(2).toInstant(), rs.getTimestamp(3).toInstant(),
                            fim == null ? null : fim.toInstant(), rs.getString(5), rs.getInt(6));
                }, c.get("objeto_id"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102
     * Título: Parecer jurídico
     * Objetivo: Guardar a avaliação do jurídico no caso, com base no termo de confidencialidade ou no NDA.
     * Decisões: Texto de até 4.000 caracteres; substitui o anterior.
     */
    @Transactional
    public Caso parecer(UUID id, String texto) {
        aberto(id);
        if (texto == null || texto.isBlank() || texto.length() > 4000) {
            throw new RegraDeNegocioException("parecer_invalido", "Escreva o parecer com até 4.000 caracteres.");
        }
        jdbc.update("UPDATE caso_moderacao SET parecer_juridico = ?, atualizado_em = ? WHERE id = ?", texto.strip(), Timestamp.from(relogio.instant()), id);
        return caso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Decidir o caso
     * Objetivo: Registrar a decisão com motivo, aplicar a sanção e seus efeitos, desfazer as medidas preventivas e avisar
     *           o denunciado e os denunciantes.
     * Decisões: Banimento vai para a segunda aprovação e o caso fica aguardando. Suspensão só com uma das durações de
     *           moderacao.suspensao_dias. Na ESCALA: advertência, depois as suspensões em ordem, depois banimento.
     */
    @Transactional
    public Caso decidir(UUID eu, UUID id, PedidoDeDecisao p) {
        Map<String, Object> c = aberto(id);
        if ("AGUARDANDO_APROVACAO".equals(c.get("situacao"))) {
            throw new RegraDeNegocioException("aguardando_aprovacao", "O banimento deste caso aguarda a segunda aprovação.");
        }
        semConflito(eu, id, c);
        if (p == null || p.decisao() == null) {
            throw new RegraDeNegocioException("decisao_invalida", "Escolha a decisão.");
        }
        listas.exigirAtivo(Lista.MOTIVO_DECISAO_MODERACAO, p.motivo());
        Objeto objeto = Objeto.valueOf((String) c.get("objeto_tipo"));
        Decisao decisao = p.decisao();
        Alvo alvo = null;
        UUID alvoId = null;
        if (decisao != Decisao.SEM_ACAO) {
            alvo = p.alvoTipo() != null ? p.alvoTipo() : alvoPadrao(objeto);
            alvoId = p.alvoId() != null ? p.alvoId() : (alvo == alvoPadrao(objeto) ? (UUID) c.get("objeto_id") : null);
            if (alvo == null || alvoId == null) {
                throw new RegraDeNegocioException("alvo_obrigatorio", "Indique a conta ou a startup que recebe a decisão.");
            }
            conferirAlvo(alvo, alvoId);
        }
        Integer dias = p.dias();
        if (decisao == Decisao.ESCALA) {
            List<Long> degraus = parametros.listaDeInteiros(SUSPENSOES);
            int anteriores = (int) sancoesDoAlvo(alvo, alvoId).stream().filter(s -> !s.tipo().startsWith("PREVENTIVA") && !"REVOGADA".equals(s.situacao())
                    && !"REMOCAO_CONTEUDO".equals(s.tipo())).count();
            if (anteriores == 0) {
                decisao = Decisao.ADVERTENCIA;
            } else if (anteriores <= Math.min(2, degraus.size())) {
                decisao = Decisao.SUSPENSAO;
                dias = degraus.get(anteriores - 1).intValue();
            } else {
                decisao = Decisao.BANIMENTO;
            }
        }
        if (decisao == Decisao.SUSPENSAO && (dias == null || !parametros.listaDeInteiros(SUSPENSOES).contains(dias.longValue()))) {
            throw new RegraDeNegocioException("duracao_invalida", "Escolha uma das durações de suspensão: " + parametros.listaDeInteiros(SUSPENSOES) + " dias.");
        }
        Instant agora = relogio.instant();
        jdbc.update("UPDATE caso_moderacao SET motivo = ?, observacao = ?, responsavel_id = COALESCE(responsavel_id, ?), atualizado_em = ? WHERE id = ?",
                p.motivo(), limitar(p.observacao(), 2000), eu, Timestamp.from(agora), id);
        if (decisao == Decisao.BANIMENTO) {
            var proposta = aprovacoes.propor(AcaoGrave.BANIMENTO_DEFINITIVO, CASO, id.toString(), "Banimento: " + rotuloDoAlvo(alvo, alvoId),
                    Map.of("alvoTipo", alvo.name(), "alvoId", alvoId.toString(), "motivo", p.motivo()), eu,
                    p.observacao() == null || p.observacao().isBlank() ? listas.rotulo(Lista.MOTIVO_DECISAO_MODERACAO, p.motivo()) : p.observacao());
            jdbc.update("UPDATE caso_moderacao SET situacao = 'AGUARDANDO_APROVACAO', decisao = 'BANIMENTO' WHERE id = ?", id);
            registrarSancao(id, alvo, alvoId, "BANIMENTO", null, null, p.motivo(), eu, proposta.getId(), "PENDENTE");
            return caso(id);
        }
        concluir(id, decisao, alvo, alvoId, dias, p.motivo(), eu, null);
        return caso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101, KAN-81
     * Título: Concluir o banimento aprovado
     * Objetivo: Aplicar o banimento depois da segunda aprovação e fechar o caso.
     * Decisões: Chamado pelo executor da ação grave.
     */
    @Transactional
    public void concluirBanimento(UUID casoId, UUID propostaId, UUID aprovador) {
        Map<String, Object> s = jdbc.queryForMap("SELECT id, alvo_tipo, alvo_id, motivo, aplicada_por FROM sancao WHERE proposta_id = ?", propostaId);
        jdbc.update("DELETE FROM sancao WHERE id = ?", s.get("id"));
        concluir(casoId, Decisao.BANIMENTO, Alvo.valueOf((String) s.get("alvo_tipo")), (UUID) s.get("alvo_id"), null, (String) s.get("motivo"),
                (UUID) s.get("aplicada_por"), propostaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101, KAN-81
     * Título: Banimento não aprovado
     * Objetivo: Devolver o caso para análise quando a segunda pessoa rejeita ou o proponente cancela o banimento.
     * Decisões: O moderador decide de novo (por exemplo, uma suspensão).
     */
    @Transactional
    public void desistirDoBanimento(UUID casoId, UUID propostaId) {
        jdbc.update("DELETE FROM sancao WHERE proposta_id = ?", propostaId);
        jdbc.update("UPDATE caso_moderacao SET situacao = 'EM_ANALISE', decisao = NULL, atualizado_em = ? WHERE id = ?", Timestamp.from(relogio.instant()), casoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Sanções da pessoa
     * Objetivo: Listar as sanções recebidas pela pessoa e pelas startups que ela administra, com o prazo para recorrer.
     * Decisões: Medidas preventivas aparecem, mas não admitem recurso (a decisão final, sim).
     */
    @Transactional(readOnly = true)
    public List<MinhaSancao> minhasSancoes(UUID eu) {
        long prazo = parametros.inteiro(PRAZO_RECURSO);
        Instant agora = relogio.instant();
        return jdbc.query("SELECT s.id, s.alvo_tipo, s.alvo_id, s.tipo, s.dias, s.inicio, s.fim, s.motivo, s.situacao, "
                + "(SELECT count(*) FROM recurso r WHERE r.origem_tipo = 'SANCAO' AND r.origem_id = s.id) FROM sancao s WHERE s.situacao <> 'PENDENTE' AND "
                + "((s.alvo_tipo = 'USUARIO' AND s.alvo_id = ?) OR (s.alvo_tipo = 'STARTUP' AND s.alvo_id IN (SELECT m.startup_id FROM startup_membro m "
                + "WHERE m.usuario_id = ? AND m.papel = 'FUNDADOR_ADMINISTRADOR' AND m.saiu_em IS NULL))) ORDER BY s.inicio DESC", (rs, i) -> {
                    Sancao s = sancao(rs);
                    Instant limite = s.inicio().plus(Duration.ofDays(prazo));
                    boolean pode = !s.tipo().startsWith("PREVENTIVA") && "ATIVA".equals(s.situacao()) && rs.getInt(10) == 0 && agora.isBefore(limite);
                    return new MinhaSancao(s, listas.rotulo(Lista.MOTIVO_DECISAO_MODERACAO, s.motivo()), limite, pode);
                }, eu, eu);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: concluir
     * Objetivo: Rotina auxiliar concluir.
     * Decisões: Ver a documentação da classe.
     */
    private void concluir(UUID id, Decisao decisao, Alvo alvo, UUID alvoId, Integer dias, String motivo, UUID eu, UUID propostaId) {
        Instant agora = relogio.instant();
        Set<String> mantidas = new HashSet<>();
        switch (decisao) {
            case SUSPENSAO -> {
                Instant fim = agora.plus(Duration.ofDays(dias));
                registrarSancao(id, alvo, alvoId, "SUSPENSAO", dias, fim, motivo, eu, propostaId);
                if (alvo == Alvo.USUARIO) {
                    suspensao.suspender(alvoId, fim);
                    mantidas.add("PREVENTIVA_SUSPENDER_CONTA");
                } else {
                    startups.despublicarPorModeracao(alvoId);
                    avisarRodada(alvoId);
                    mantidas.add("PREVENTIVA_DESPUBLICAR");
                }
            }
            case BANIMENTO -> {
                registrarSancao(id, alvo, alvoId, "BANIMENTO", null, null, motivo, eu, propostaId);
                if (alvo == Alvo.USUARIO) {
                    suspensao.suspender(alvoId, Usuario.BANIDA);
                    mantidas.add("PREVENTIVA_SUSPENDER_CONTA");
                } else {
                    startups.despublicarPorModeracao(alvoId);
                    avisarRodada(alvoId);
                    mantidas.add("PREVENTIVA_DESPUBLICAR");
                }
            }
            case ADVERTENCIA, REMOCAO_CONTEUDO -> registrarSancao(id, alvo, alvoId, decisao.name(), null, null, motivo, eu, propostaId);
            default -> { }
        }
        desfazerPreventivas(id, mantidas);
        jdbc.update("UPDATE caso_moderacao SET situacao = 'DECIDIDO', decisao = ?, decidido_por = ?, decidido_em = ?, atualizado_em = ? WHERE id = ?",
                decisao.name(), eu, Timestamp.from(agora), Timestamp.from(agora), id);
        jdbc.queryForList("SELECT DISTINCT denunciante_id FROM denuncia WHERE caso_id = ? AND denunciante_id IS NOT NULL", UUID.class, id)
                .forEach(u -> notificacoes.notificar(u, TipoDeNotificacao.MODERACAO, "Denúncia analisada",
                        "O caso que você denunciou foi analisado pela moderação. Obrigado por ajudar a manter o ecossistema confiável.", "/ajuda/denuncias"));
        if (decisao != Decisao.SEM_ACAO) {
            String texto = switch (decisao) {
                case SUSPENSAO -> "Suspensão de " + dias + " dias";
                case BANIMENTO -> "Banimento";
                case ADVERTENCIA -> "Advertência";
                default -> "Remoção ou correção de conteúdo";
            };
            String mensagem = texto + " por: " + listas.rotulo(Lista.MOTIVO_DECISAO_MODERACAO, motivo) + ". Você pode recorrer em até "
                    + parametros.inteiro(PRAZO_RECURSO) + " dias pela central de ajuda.";
            avisados(alvo, alvoId).forEach(u -> notificacoes.notificar(u, TipoDeNotificacao.MODERACAO, "Decisão da moderação", mensagem, "/ajuda/sancoes"));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: desfazerPreventivas
     * Objetivo: Rotina auxiliar desfazerPreventivas.
     * Decisões: Ver a documentação da classe.
     */
    private void desfazerPreventivas(UUID id, Set<String> mantidas) {
        Instant agora = relogio.instant();
        for (Map<String, Object> s : jdbc.queryForList("SELECT id, tipo, alvo_id FROM sancao WHERE caso_id = ? AND tipo LIKE 'PREVENTIVA%' AND situacao = 'ATIVA'", id)) {
            jdbc.update("UPDATE sancao SET situacao = 'ENCERRADA', fim = ? WHERE id = ?", Timestamp.from(agora), s.get("id"));
            String tipo = (String) s.get("tipo");
            if (!mantidas.contains(tipo)) {
                if ("PREVENTIVA_SUSPENDER_CONTA".equals(tipo)) {
                    suspensao.levantar((UUID) s.get("alvo_id"));
                } else if ("PREVENTIVA_DESPUBLICAR".equals(tipo)) {
                    startups.republicarPorModeracao((UUID) s.get("alvo_id"));
                }
            }
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Revogar sanção
     * Objetivo: Desfazer a sanção quando o recurso é deferido: devolver o acesso à conta ou pôr a startup de volta no catálogo.
     * Decisões: Acessos confidenciais e conexões encerrados não voltam sozinhos.
     */
    @Transactional
    public void revogar(UUID sancaoId) {
        Map<String, Object> s = jdbc.queryForMap("SELECT alvo_tipo, alvo_id, tipo FROM sancao WHERE id = ?", sancaoId);
        jdbc.update("UPDATE sancao SET situacao = 'REVOGADA', revogada_em = ? WHERE id = ?", Timestamp.from(relogio.instant()), sancaoId);
        String tipo = (String) s.get("tipo");
        if ("SUSPENSAO".equals(tipo) || "BANIMENTO".equals(tipo)) {
            if (Alvo.USUARIO.name().equals(s.get("alvo_tipo"))) {
                suspensao.levantar((UUID) s.get("alvo_id"));
            } else {
                startups.republicarPorModeracao((UUID) s.get("alvo_id"));
            }
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Sanção para recurso
     * Objetivo: Ler a sanção de que a pessoa quer recorrer, conferindo que ela é a sancionada (ou administra a startup).
     * Decisões: Devolve o alvo, o tipo, o início e quem aplicou.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> sancaoParaRecurso(UUID eu, UUID sancaoId) {
        List<Map<String, Object>> l = jdbc.queryForList("SELECT alvo_tipo, alvo_id, tipo, inicio, aplicada_por, situacao FROM sancao WHERE id = ?", sancaoId);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Sanção não encontrada.");
        }
        Map<String, Object> s = l.get(0);
        UUID alvo = (UUID) s.get("alvo_id");
        boolean minha = Alvo.USUARIO.name().equals(s.get("alvo_tipo")) ? alvo.equals(eu) : startups.fundadores(alvo, true).contains(eu);
        if (!minha) {
            throw new AccessDeniedException("Sanção de outra pessoa");
        }
        return s;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: registrarSancao
     * Objetivo: Rotina auxiliar registrarSancao.
     * Decisões: Ver a documentação da classe.
     */
    private void registrarSancao(UUID caso, Alvo alvo, UUID alvoId, String tipo, Integer dias, Instant fim, String motivo, UUID eu, UUID proposta) {
        registrarSancao(caso, alvo, alvoId, tipo, dias, fim, motivo, eu, proposta, "ATIVA");
    }

    @SuppressWarnings("java:S107")
    private void registrarSancao(UUID caso, Alvo alvo, UUID alvoId, String tipo, Integer dias, Instant fim, String motivo, UUID eu, UUID proposta,
                                 String situacao) {
        jdbc.update("INSERT INTO sancao (id, caso_id, alvo_tipo, alvo_id, tipo, dias, inicio, fim, motivo, aplicada_por, proposta_id, situacao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), caso, alvo.name(), alvoId, tipo, dias, Timestamp.from(relogio.instant()),
                fim == null ? null : Timestamp.from(fim), limitar(motivo, 60), eu, proposta, situacao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: avisarRodada
     * Objetivo: Rotina auxiliar avisarRodada.
     * Decisões: Ver a documentação da classe.
     */
    private void avisarRodada(UUID startupId) {
        List<UUID> participantes = jdbc.queryForList("SELECT DISTINCT p.pessoa_id FROM rodada_participante p JOIN rodada r ON r.id = p.rodada_id "
                + "WHERE r.startup_id = ? AND r.situacao IN ('ABERTA', 'EM_NEGOCIACAO', 'AGUARDANDO_RESULTADO') AND p.situacao <> 'SAIU'", UUID.class, startupId);
        notificacoes.notificarTodos(participantes, TipoDeNotificacao.RODADA_AVISO, "Rodada suspensa",
                "A " + startups.ler(startupId).getNome() + " saiu do catálogo por decisão da moderação e a rodada está suspensa.", "/conexoes");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: avisados
     * Objetivo: Rotina auxiliar avisados.
     * Decisões: Ver a documentação da classe.
     */
    private List<UUID> avisados(Alvo alvo, UUID alvoId) {
        return alvo == Alvo.USUARIO ? List.of(alvoId) : startups.fundadores(alvoId, true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: sancoesDoAlvo
     * Objetivo: Rotina auxiliar sancoesDoAlvo.
     * Decisões: Ver a documentação da classe.
     */
    private List<Sancao> sancoesDoAlvo(Alvo alvo, UUID alvoId) {
        return jdbc.query("SELECT id, alvo_tipo, alvo_id, tipo, dias, inicio, fim, motivo, situacao FROM sancao WHERE alvo_tipo = ? AND alvo_id = ? "
                + "AND situacao <> 'PENDENTE' ORDER BY inicio DESC", (rs, i) -> sancao(rs), alvo.name(), alvoId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: sancao
     * Objetivo: Rotina auxiliar sancao.
     * Decisões: Ver a documentação da classe.
     */
    private static Sancao sancao(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp fim = rs.getTimestamp(7);
        Object dias = rs.getObject(5);
        return new Sancao(rs.getObject(1, UUID.class), Alvo.valueOf(rs.getString(2)), rs.getObject(3, UUID.class), rs.getString(4),
                dias == null ? null : ((Number) dias).intValue(), rs.getTimestamp(6).toInstant(), fim == null ? null : fim.toInstant(), rs.getString(8),
                rs.getString(9));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: semConflito
     * Objetivo: Rotina auxiliar semConflito.
     * Decisões: Ver a documentação da classe.
     */
    private void semConflito(UUID eu, UUID id, Map<String, Object> c) {
        Set<UUID> envolvidos = new HashSet<>();
        Objeto objeto = Objeto.valueOf((String) c.get("objeto_tipo"));
        UUID objetoId = (UUID) c.get("objeto_id");
        if (objeto == Objeto.PERFIL) {
            envolvidos.add(objetoId);
        } else if (objeto == Objeto.STARTUP) {
            envolvidos.addAll(startups.fundadores(objetoId, false));
        }
        conflitos.exigirSemConflito(eu, CASO, id, new ConflitosDeInteresse.Envolvidos(Set.of(), envolvidos));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: conferirObjeto
     * Objetivo: Rotina auxiliar conferirObjeto.
     * Decisões: Ver a documentação da classe.
     */
    private void conferirObjeto(Objeto tipo, UUID id) {
        String sql = switch (tipo) {
            case PERFIL -> "SELECT count(*) FROM usuario WHERE id = ? AND NOT interna";
            case ORGANIZACAO -> "SELECT count(*) FROM organizacao WHERE id = ?";
            case STARTUP -> "SELECT count(*) FROM startup WHERE id = ?";
            case CONEXAO -> "SELECT count(*) FROM conexao WHERE id = ?";
            case REUNIAO -> "SELECT count(*) FROM reuniao WHERE id = ?";
        };
        Integer n = jdbc.queryForObject(sql, Integer.class, id);
        if (n == null || n == 0) {
            throw new RecursoNaoEncontradoException("Não encontramos o que você quer denunciar.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: conferirAlvo
     * Objetivo: Rotina auxiliar conferirAlvo.
     * Decisões: Ver a documentação da classe.
     */
    private void conferirAlvo(Alvo alvo, UUID id) {
        if (alvo == Alvo.USUARIO) {
            conferirObjeto(Objeto.PERFIL, id);
        } else {
            conferirObjeto(Objeto.STARTUP, id);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: alvoPadrao
     * Objetivo: Rotina auxiliar alvoPadrao.
     * Decisões: Ver a documentação da classe.
     */
    private static Alvo alvoPadrao(Objeto objeto) {
        return switch (objeto) {
            case PERFIL -> Alvo.USUARIO;
            case STARTUP -> Alvo.STARTUP;
            default -> null;
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: rotuloDoAlvo
     * Objetivo: Rotina auxiliar rotuloDoAlvo.
     * Decisões: Ver a documentação da classe.
     */
    private String rotuloDoAlvo(Alvo alvo, UUID id) {
        return alvo == Alvo.USUARIO ? contas.nome(id).orElse("conta") : startups.ler(id).getNome();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: rotuloDoObjeto
     * Objetivo: Rotina auxiliar rotuloDoObjeto.
     * Decisões: Ver a documentação da classe.
     */
    private String rotuloDoObjeto(Objeto objeto, UUID id) {
        String sql = switch (objeto) {
            case PERFIL -> "SELECT nome FROM usuario WHERE id = ?";
            case ORGANIZACAO -> "SELECT nome FROM organizacao WHERE id = ?";
            case STARTUP -> "SELECT nome FROM startup WHERE id = ?";
            case CONEXAO -> "SELECT 'Conexão com ' || s.nome FROM conexao c JOIN startup s ON s.id = c.startup_id WHERE c.id = ?";
            case REUNIAO -> "SELECT 'Reunião: ' || r.pauta FROM reuniao r WHERE r.id = ?";
        };
        return jdbc.query(sql, (rs, i) -> rs.getString(1), id).stream().findFirst().orElse("(removido)");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: prazo
     * Objetivo: Rotina auxiliar prazo.
     * Decisões: Ver a documentação da classe.
     */
    private Instant prazo(String prioridade, Instant agora) {
        return switch (prioridade) {
            case "CRITICA" -> agora.plus(Duration.ofHours(parametros.inteiro(SLA_CRITICA)));
            case "ALTA" -> agora.plus(Duration.ofHours(parametros.inteiro(SLA_ALTA)));
            default -> DiasUteis.somar(agora, parametros.inteiro(SLA_NORMAL));
        };
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
        List<Map<String, Object>> l = jdbc.queryForList("SELECT * FROM caso_moderacao WHERE id = ?", id);
        if (l.isEmpty()) {
            throw new RecursoNaoEncontradoException("Caso não encontrado.");
        }
        return l.get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: aberto
     * Objetivo: Rotina auxiliar aberto.
     * Decisões: Ver a documentação da classe.
     */
    private Map<String, Object> aberto(UUID id) {
        Map<String, Object> c = linha(id);
        if (DECIDIDO.equals(c.get("situacao"))) {
            throw new RegraDeNegocioException("caso_decidido", "Este caso já foi decidido.");
        }
        return c;
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
     * Título: limitar
     * Objetivo: Rotina auxiliar limitar.
     * Decisões: Ver a documentação da classe.
     */
    private static String limitar(String s, int max) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.strip();
        return t.length() > max ? t.substring(0, max) : t;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99, KAN-100, KAN-101, KAN-102
     * Título: Dados da moderação
     * Objetivo: Tipar os pedidos e as respostas da moderação.
     * Decisões: O nome do denunciante só aparece para a equipe.
     */
    public record NovaDenuncia(Objeto objetoTipo, UUID objetoId, String categoria, String descricao, String evidencias) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Recebida
     * Objetivo: Rotina auxiliar Recebida.
     * Decisões: Ver a documentação da classe.
     */
    public record Recebida(UUID protocolo, Instant recebidaEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: MinhaDenuncia
     * Objetivo: Rotina auxiliar MinhaDenuncia.
     * Decisões: Ver a documentação da classe.
     */
    public record MinhaDenuncia(UUID protocolo, Objeto objetoTipo, String categoria, Instant criadaEm, boolean analisada) {
    }

    public record ItemDaFila(UUID id, Objeto objetoTipo, String objeto, String prioridade, String categoria, String situacao, Instant prazo,
                             boolean vencido, String responsavel, int denuncias, Instant criadoEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Denuncia
     * Objetivo: Rotina auxiliar Denuncia.
     * Decisões: Ver a documentação da classe.
     */
    public record Denuncia(UUID id, String denunciante, String categoria, String descricao, String evidencias, Instant criadaEm, int semAcaoAntes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: Sancao
     * Objetivo: Rotina auxiliar Sancao.
     * Decisões: Ver a documentação da classe.
     */
    public record Sancao(UUID id, Alvo alvoTipo, UUID alvoId, String tipo, Integer dias, Instant inicio, Instant fim, String motivo, String situacao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: CasoAnterior
     * Objetivo: Rotina auxiliar CasoAnterior.
     * Decisões: Ver a documentação da classe.
     */
    public record CasoAnterior(UUID id, String categoria, String decisao, Instant decididoEm) {
    }

    public record Caso(UUID id, Objeto objetoTipo, UUID objetoId, String objeto, String prioridade, String categoria, String situacao, Instant prazo,
                       String responsavel, List<Denuncia> denuncias, List<Sancao> historico, List<CasoAnterior> anteriores, List<Sancao> medidas,
                       String parecerJuridico, String decisao, String motivo, String observacao, Alvo alvoPadrao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: PedidoDeDecisao
     * Objetivo: Rotina auxiliar PedidoDeDecisao.
     * Decisões: Ver a documentação da classe.
     */
    public record PedidoDeDecisao(Decisao decisao, String motivo, String observacao, Alvo alvoTipo, UUID alvoId, Integer dias) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: AcessoConfidencial
     * Objetivo: Rotina auxiliar AcessoConfidencial.
     * Decisões: Ver a documentação da classe.
     */
    public record AcessoConfidencial(UUID pessoaId, String pessoa, Instant liberadoEm, Instant validoAte, Instant encerradoEm, String motivo, int links) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
     * Título: MinhaSancao
     * Objetivo: Rotina auxiliar MinhaSancao.
     * Decisões: Ver a documentação da classe.
     */
    public record MinhaSancao(Sancao sancao, String motivo, Instant recorrerAte, boolean podeRecorrer) {
    }
}
