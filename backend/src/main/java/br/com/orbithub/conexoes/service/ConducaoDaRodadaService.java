package br.com.orbithub.conexoes.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.domain.ResultadoDaRodada;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.domain.SituacaoDaRodada;
import br.com.orbithub.conexoes.repository.HistoricoDaRodada;
import br.com.orbithub.conexoes.repository.RodadaRepository;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-106, KAN-109
 * Título: Condução da rodada
 * Objetivo: Completar a rodada com participantes (intenção e condições), convites de co-investidores pelo líder com
 *           aprovação do empreendedor, convite e aceite do investidor líder, painel com excesso de demanda, política de
 *           alocação, prorrogação, pedido de encerramento, cancelamento com volta das conexões, conclusão com o registro
 *           do investimento confirmado pelas partes, e as rotinas R6 e R7.
 * Decisões: Entra quem tem conexão em Match, em análise ou em negociação; ao entrar, a conexão vai para em negociação
 *           (e volta no cancelamento). Visibilidade padrão: todos sabem quem é o líder; o co-investidor só vê o próprio
 *           valor; o líder vê os participantes e a soma; a startup vê tudo. Valores do investimento: startup e o próprio
 *           investidor. Só conta como investimento o que o investidor confirmou. Nada disso é compromisso de investimento.
 */
@Service
public class ConducaoDaRodadaService {

    static final String MAX_PRORROGACOES = "rodada.max_prorrogacoes";
    static final String PRORROGACAO_DIAS = "rodada.prorrogacao_max_dias";
    static final String PRAZO_RESULTADO = "rodada.prazo_registro_resultado_dias";
    static final String PRAZO_CONFIRMACAO = "rodada.prazo_confirmacao_dias";
    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    static final Set<SituacaoDaRodada> EM_CURSO = EnumSet.of(SituacaoDaRodada.ABERTA, SituacaoDaRodada.EM_NEGOCIACAO);
    static final Set<Conexao.Situacao> PODEM_ENTRAR = EnumSet.of(Conexao.Situacao.MATCH, Conexao.Situacao.EM_ANALISE, Conexao.Situacao.EM_NEGOCIACAO);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Política de alocação
     * Objetivo: Enumerar como o excesso de demanda é alocado, declarado ao abrir a rodada.
     * Decisões: No MVP a alocação é feita fora do sistema, seguindo a política.
     */
    public enum Politica { LIDER_PRIMEIRO, PROPORCIONAL, CRITERIO_EMPREENDEDOR }

    private final JdbcTemplate jdbc;
    private final RodadaRepository rodadas;
    private final HistoricoDaRodada historico;
    private final ConexoesService conexoes;
    private final StartupsService startups;
    private final BloqueiosDaStartup bloqueios;
    private final AtuacaoService atuacao;
    private final OrganizacoesService organizacoes;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Criação
     * Objetivo: Receber os serviços envolvidos.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public ConducaoDaRodadaService(JdbcTemplate jdbc, RodadaRepository rodadas, HistoricoDaRodada historico, ConexoesService conexoes,
                                   StartupsService startups, BloqueiosDaStartup bloqueios, AtuacaoService atuacao,
                                   OrganizacoesService organizacoes, ConsultaDeContas contas, NotificacoesService notificacoes,
                                   LeituraDeParametros parametros, Clock relogio) {
        this.jdbc = jdbc;
        this.rodadas = rodadas;
        this.historico = historico;
        this.conexoes = conexoes;
        this.startups = startups;
        this.bloqueios = bloqueios;
        this.atuacao = atuacao;
        this.organizacoes = organizacoes;
        this.contas = contas;
        this.notificacoes = notificacoes;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Painel da startup
     * Objetivo: Mostrar à equipe os participantes com intenção, os convites pendentes, o líder, o total das intenções
     *           frente ao alvo, mínimo e máximo, o excesso de demanda e os investimentos registrados.
     * Decisões: Fundadores (não colaboradores).
     */
    @Transactional(readOnly = true)
    public Painel daStartup(UUID eu, UUID rodadaId) {
        Rodada r = rodada(rodadaId);
        if (startups.papel(eu, r.getStartupId()) == PapelNaStartup.COLABORADOR) {
            throw new AccessDeniedException("Só os fundadores acompanham a rodada.");
        }
        return painel(r, participantes(rodadaId), investimentos(rodadaId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Rodada para o investidor
     * Objetivo: Mostrar ao investidor a rodada aberta da startup: o líder, a política, se pode entrar e a própria
     *           participação; ao líder, também os participantes e a soma das intenções.
     * Decisões: Co-investidor não vê os valores dos outros. Sem rodada aberta, 404.
     */
    @Transactional(readOnly = true)
    public ParaInvestidor paraInvestidor(UUID eu, String organizacao, UUID startupId) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        if (bloqueios.bloqueia(startupId, a.pessoa(), a.organizacao())) {
            throw new RecursoNaoEncontradoException("Rodada não encontrada.");
        }
        Rodada r = rodadas.findFirstByStartupIdAndSituacaoNot(startupId, SituacaoDaRodada.ENCERRADA)
                .filter(x -> x.getSituacao() != SituacaoDaRodada.RASCUNHO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rodada não encontrada."));
        UUID chave = chave(a);
        List<Participante> todos = participantes(r.getId());
        Optional<Participante> meu = todos.stream().filter(p -> p.investidorChave().equals(chave)).findFirst();
        boolean souLider = chave.equals(lider(r.getId()));
        Optional<Conexao> conexao = conexoes.ativaDe(startupId, chave);
        boolean podeEntrar = meu.isEmpty() && EM_CURSO.contains(r.getSituacao()) && conexao.map(c -> PODEM_ENTRAR.contains(c.getSituacao())).orElse(false);
        List<Investimento> meusInvestimentos = investimentos(r.getId()).stream().filter(i -> meu.map(m -> m.id().equals(i.participanteId())).orElse(false)).toList();
        return new ParaInvestidor(r.getId(), r.getSituacao(), politica(r.getId()), nomeDaChave(lider(r.getId())),
                chave.equals(liderConvidado(r.getId())), souLider, podeEntrar, meu.orElse(null),
                souLider ? todos.stream().map(p -> new ParticipanteParaLider(p.id(), p.investidor(), p.situacao())).toList() : List.of(),
                souLider ? soma(todos) : null, meusInvestimentos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Entrar na rodada
     * Objetivo: Registrar a participação do investidor, com intenção e condições, e pôr a conexão em negociação.
     * Decisões: Rodada aberta ou em negociação; conexão em Match ou depois; sem bloqueio; uma participação por investidor.
     */
    @Transactional
    public Participante entrar(UUID eu, String organizacao, UUID rodadaId, BigDecimal intencao, String condicoes) {
        Rodada r = rodada(rodadaId);
        exigirEmCurso(r);
        AtuacaoService.Atuacao a = atuacao.paraAgir(eu, organizacao);
        if (bloqueios.bloqueia(r.getStartupId(), a.pessoa(), a.organizacao())) {
            throw new RecursoNaoEncontradoException("Rodada não encontrada.");
        }
        Conexao c = conexoes.ativaDe(r.getStartupId(), chave(a)).filter(x -> PODEM_ENTRAR.contains(x.getSituacao()))
                .orElseThrow(() -> new RegraDeNegocioException("sem_match", "Para entrar na rodada, você precisa de um Match com a startup."));
        if (participantes(rodadaId).stream().anyMatch(p -> p.investidorChave().equals(chave(a)))) {
            throw new RegraDeNegocioException("ja_participa", "Você já participa desta rodada.");
        }
        conferirValor(intencao);
        Conexao.Situacao anterior = conexoes.entrarNaRodada(c, eu, Conexao.Lado.INVESTIDOR);
        UUID id = UUID.randomUUID();
        Instant agora = relogio.instant();
        jdbc.update("INSERT INTO rodada_participante (id, rodada_id, investidor_chave, pessoa_id, organizacao_id, conexao_id, situacao, intencao, "
                + "condicoes, conexao_anterior, entrou_em, atualizado_em) VALUES (?, ?, ?, ?, ?, ?, 'ATIVO', ?, ?, ?, ?, ?)", id, rodadaId, chave(a),
                eu, a.organizacao(), c.getId(), intencao, texto(condicoes), anterior.name(), Timestamp.from(agora), Timestamp.from(agora));
        avisarFundadores(r, "Novo participante na rodada", conexoes.investidor(c) + " entrou na rodada.");
        return participante(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Atualizar intenção
     * Objetivo: Trocar o valor de intenção e as condições preliminares do participante.
     * Decisões: Só com a rodada em curso.
     */
    @Transactional
    public Participante atualizarIntencao(UUID eu, String organizacao, UUID rodadaId, BigDecimal intencao, String condicoes) {
        Rodada r = rodada(rodadaId);
        exigirEmCurso(r);
        Participante p = meuParticipante(eu, organizacao, rodadaId);
        conferirValor(intencao);
        jdbc.update("UPDATE rodada_participante SET intencao = ?, condicoes = ?, atualizado_em = ? WHERE id = ?", intencao, texto(condicoes),
                Timestamp.from(relogio.instant()), p.id());
        return participante(p.id());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Convidar co-investidor
     * Objetivo: Deixar o líder convidar um investidor (pelo e-mail da conta) para a rodada, à espera da aprovação da
     *           startup.
     * Decisões: Só o líder. O convidado precisa ser investidor ativo e não estar bloqueado.
     */
    @Transactional
    public Participante convidarCoInvestidor(UUID eu, String organizacao, UUID rodadaId, String email) {
        Rodada r = rodada(rodadaId);
        exigirEmCurso(r);
        AtuacaoService.Atuacao a = atuacao.paraAgir(eu, organizacao);
        if (!chave(a).equals(lider(rodadaId))) {
            throw new AccessDeniedException("Só o investidor líder convida co-investidores.");
        }
        UUID pessoa = contas.contaAtivaDoEmail(email == null ? "" : email.strip().toLowerCase(java.util.Locale.ROOT))
                .filter(contas::investidoraAtiva)
                .orElseThrow(() -> new RegraDeNegocioException("convidado_invalido", "Não há investidor ativo com este e-mail."));
        if (bloqueios.bloqueia(r.getStartupId(), pessoa, null)) {
            throw new RegraDeNegocioException("convidado_invalido", "Este investidor não pode participar desta rodada.");
        }
        if (participantes(rodadaId).stream().anyMatch(p -> p.investidorChave().equals(pessoa))) {
            throw new RegraDeNegocioException("ja_participa", "Este investidor já está na rodada.");
        }
        UUID id = UUID.randomUUID();
        Instant agora = relogio.instant();
        jdbc.update("INSERT INTO rodada_participante (id, rodada_id, investidor_chave, pessoa_id, situacao, convidado_por, entrou_em, atualizado_em) "
                + "VALUES (?, ?, ?, ?, 'AGUARDANDO_APROVACAO', ?, ?, ?)", id, rodadaId, pessoa, pessoa, eu, Timestamp.from(agora), Timestamp.from(agora));
        avisarFundadores(r, "Co-investidor convidado pelo líder", contas.nome(pessoa).orElse("Um investidor")
                + " foi convidado pelo líder. Aprove ou recuse na aba Rodada.");
        return participante(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Decidir convidado
     * Objetivo: Aprovar (criando o Match e a participação) ou recusar o co-investidor convidado pelo líder.
     * Decisões: Só o fundador administrador.
     */
    @Transactional
    public Participante decidirConvidado(UUID eu, UUID rodadaId, UUID participanteId, boolean aprovar) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        Participante p = participante(participanteId);
        if (!p.rodadaId().equals(rodadaId) || !"AGUARDANDO_APROVACAO".equals(p.situacao())) {
            throw new RegraDeNegocioException("convite_invalido", "Este convite não aguarda aprovação.");
        }
        Instant agora = relogio.instant();
        if (!aprovar) {
            jdbc.update("UPDATE rodada_participante SET situacao = 'SAIU', atualizado_em = ? WHERE id = ?", Timestamp.from(agora), participanteId);
            return participante(participanteId);
        }
        exigirEmCurso(r);
        Conexao c = conexoes.matchPorConvite(r.getStartupId(), p.pessoaId(), null, eu);
        Conexao.Situacao anterior = conexoes.entrarNaRodada(c, eu, Conexao.Lado.STARTUP);
        jdbc.update("UPDATE rodada_participante SET situacao = 'ATIVO', conexao_id = ?, conexao_anterior = ?, atualizado_em = ? WHERE id = ?",
                c.getId(), (anterior == Conexao.Situacao.EM_NEGOCIACAO ? Conexao.Situacao.MATCH : anterior).name(), Timestamp.from(agora), participanteId);
        notificacoes.notificar(p.pessoaId(), TipoDeNotificacao.MATCH, "Você entrou numa rodada",
                "A " + startups.ler(r.getStartupId()).getNome() + " aprovou o convite do investidor líder. Registre a sua intenção.",
                "/catalogo/startups/" + r.getStartupId());
        return participante(participanteId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Convidar o líder
     * Objetivo: Convidar um participante para liderar a rodada.
     * Decisões: Só o fundador administrador; no máximo um líder. Convidar outro substitui o convite pendente.
     */
    @Transactional
    public void convidarLider(UUID eu, UUID rodadaId, UUID participanteId) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        exigirEmCurso(r);
        Participante p = participante(participanteId);
        if (!p.rodadaId().equals(rodadaId) || !"ATIVO".equals(p.situacao())) {
            throw new RegraDeNegocioException("participante_invalido", "Convide para liderar um participante ativo da rodada.");
        }
        jdbc.update("UPDATE rodada SET lider_convidado_chave = ? WHERE id = ?", p.investidorChave(), rodadaId);
        notificacoes.notificar(p.pessoaId(), TipoDeNotificacao.RODADA_AVISO, "Convite para liderar a rodada",
                "A " + startups.ler(r.getStartupId()).getNome() + " convidou você para ser o investidor líder da rodada.",
                "/catalogo/startups/" + r.getStartupId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Responder liderança
     * Objetivo: Aceitar ou recusar o convite para liderar.
     * Decisões: Aceitar troca o líder e registra no histórico.
     */
    @Transactional
    public void responderLideranca(UUID eu, String organizacao, UUID rodadaId, boolean aceitar) {
        Rodada r = rodada(rodadaId);
        UUID chave = chave(atuacao.paraAgir(eu, organizacao));
        if (!chave.equals(liderConvidado(rodadaId))) {
            throw new RegraDeNegocioException("sem_convite", "Não há convite de liderança para você nesta rodada.");
        }
        if (aceitar) {
            jdbc.update("UPDATE rodada SET lider_chave = ?, lider_convidado_chave = NULL WHERE id = ?", chave, rodadaId);
            historico.registrar(rodadaId, r.getSituacao(), r.getSituacao(), eu, "Novo investidor líder: " + nomeDaChave(chave), relogio.instant());
            avisarParticipantes(r, "Rodada com investidor líder", nomeDaChave(chave) + " é o investidor líder da rodada.");
        } else {
            jdbc.update("UPDATE rodada SET lider_convidado_chave = NULL WHERE id = ?", rodadaId);
        }
        avisarFundadores(r, aceitar ? "Líder aceitou" : "Convite de liderança recusado",
                nomeDaChave(chave) + (aceitar ? " aceitou liderar a rodada." : " recusou liderar a rodada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Definir política
     * Objetivo: Declarar a política de alocação, avisando os participantes se a rodada já estiver em curso.
     * Decisões: Só o fundador administrador.
     */
    @Transactional
    public Politica definirPolitica(UUID eu, UUID rodadaId, Politica politica) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        jdbc.update("UPDATE rodada SET politica_alocacao = ? WHERE id = ?", politica.name(), rodadaId);
        if (EM_CURSO.contains(r.getSituacao())) {
            avisarParticipantes(r, "Política de alocação alterada", "A política de alocação da rodada mudou. Confira no painel.");
        }
        return politica;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Prorrogar
     * Objetivo: Dar novo prazo à rodada, dentro dos limites de quantidade e de dias.
     * Decisões: O novo prazo conta a partir do prazo atual (ou de hoje, se já passou). Avisa os participantes.
     */
    @Transactional
    public Rodada prorrogar(UUID eu, UUID rodadaId, LocalDate novoPrazo) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        Integer feitas = jdbc.queryForObject("SELECT prorrogacoes FROM rodada WHERE id = ?", Integer.class, rodadaId);
        if (feitas != null && feitas >= parametros.inteiro(MAX_PRORROGACOES)) {
            throw new RegraDeNegocioException("prorrogacao_esgotada", "A rodada já foi prorrogada o máximo de vezes.");
        }
        LocalDate hoje = LocalDate.ofInstant(relogio.instant(), BRASILIA);
        LocalDate base = r.getPrazo() == null || r.getPrazo().isBefore(hoje) ? hoje : r.getPrazo();
        if (novoPrazo == null || !novoPrazo.isAfter(base) || ChronoUnit.DAYS.between(base, novoPrazo) > parametros.inteiro(PRORROGACAO_DIAS)) {
            throw new RegraDeNegocioException("prorrogacao_invalida", "Prorrogue por até " + parametros.inteiro(PRORROGACAO_DIAS) + " dias depois do prazo.");
        }
        SituacaoDaRodada anterior = r.prorrogar(novoPrazo, relogio.instant());
        jdbc.update("UPDATE rodada SET prorrogacoes = prorrogacoes + 1, aguardando_desde = NULL WHERE id = ?", rodadaId);
        historico.registrar(rodadaId, anterior, r.getSituacao(), eu, "Prorrogada até " + novoPrazo, relogio.instant());
        avisarParticipantes(r, "Rodada prorrogada", "O prazo da rodada foi prorrogado até " + novoPrazo + ".");
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Pedir encerramento
     * Objetivo: Deixar o líder pedir o encerramento, avisando o fundador administrador, que decide.
     * Decisões: Só o líder; fica registrado.
     */
    @Transactional
    public void pedirEncerramento(UUID eu, String organizacao, UUID rodadaId) {
        Rodada r = rodada(rodadaId);
        if (!chave(atuacao.paraAgir(eu, organizacao)).equals(lider(rodadaId))) {
            throw new AccessDeniedException("Só o investidor líder pede o encerramento.");
        }
        jdbc.update("UPDATE rodada SET encerramento_pedido_em = ? WHERE id = ?", Timestamp.from(relogio.instant()), rodadaId);
        historico.registrar(rodadaId, r.getSituacao(), r.getSituacao(), eu, "Líder pediu o encerramento", relogio.instant());
        avisarFundadores(r, "O líder pediu o encerramento da rodada", "Registre o resultado na aba Rodada quando decidir.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Cancelar
     * Objetivo: Cancelar a rodada com motivo, devolvendo as conexões dos participantes à situação de antes e avisando todos.
     * Decisões: Só o fundador administrador; motivo obrigatório.
     */
    @Transactional
    public Rodada cancelar(UUID eu, UUID rodadaId, String motivo) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        String m = texto(motivo);
        if (m == null) {
            throw new RegraDeNegocioException("motivo_obrigatorio", "Diga o motivo do cancelamento.");
        }
        SituacaoDaRodada anterior = r.encerrar(ResultadoDaRodada.CANCELADA, relogio.instant());
        historico.registrar(rodadaId, anterior, r.getSituacao(), eu, "CANCELADA: " + m, relogio.instant());
        for (Participante p : participantes(rodadaId)) {
            if ("ATIVO".equals(p.situacao()) && p.conexaoId() != null) {
                conexoes.voltarAntesDaRodada(conexoes.ler(p.conexaoId()),
                        p.conexaoAnterior() == null ? null : Conexao.Situacao.valueOf(p.conexaoAnterior()), eu);
            }
        }
        avisarParticipantes(r, "Rodada cancelada", "A rodada foi cancelada: " + m);
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-64, KAN-109
     * Título: Concluir e registrar o resultado
     * Objetivo: Encerrar a rodada como concluída, registrando valor captado, instrumento, data e os investidores (com
     *           valor opcional), que confirmam depois. Os demais participantes têm a conexão encerrada.
     * Decisões: Só o fundador administrador. Investidores indicados precisam ser participantes ativos.
     */
    @Transactional
    public Rodada concluir(UUID eu, UUID rodadaId, Resultado resultado) {
        Rodada r = rodada(rodadaId);
        startups.exigirAdministrador(eu, r.getStartupId());
        if (resultado.valorCaptado() == null || resultado.valorCaptado().signum() <= 0 || resultado.dataFechamento() == null) {
            throw new RegraDeNegocioException("resultado_incompleto", "Informe o valor captado e a data do fechamento.");
        }
        Map<UUID, Participante> ativos = participantes(rodadaId).stream().filter(p -> "ATIVO".equals(p.situacao()))
                .collect(Collectors.toMap(Participante::id, p -> p));
        List<InvestidorDoResultado> indicados = resultado.investidores() == null ? List.of() : resultado.investidores();
        for (InvestidorDoResultado i : indicados) {
            if (!ativos.containsKey(i.participanteId())) {
                throw new RegraDeNegocioException("investidor_invalido", "Indique só participantes ativos da rodada.");
            }
            if (i.valor() != null) {
                conferirValor(i.valor());
            }
        }
        Instant agora = relogio.instant();
        SituacaoDaRodada anterior = r.encerrar(ResultadoDaRodada.CONCLUIDA, agora);
        jdbc.update("UPDATE rodada SET valor_captado = ?, instrumento_utilizado = ?, data_fechamento = ? WHERE id = ?", resultado.valorCaptado(),
                texto(resultado.instrumento()), Date.valueOf(resultado.dataFechamento()), rodadaId);
        historico.registrar(rodadaId, anterior, r.getSituacao(), eu, "CONCLUIDA", agora);
        Set<UUID> investiram = indicados.stream().map(InvestidorDoResultado::participanteId).collect(Collectors.toSet());
        for (InvestidorDoResultado i : indicados) {
            jdbc.update("INSERT INTO rodada_investimento (id, rodada_id, participante_id, valor, situacao, criado_em) VALUES (?, ?, ?, ?, 'PENDENTE', ?)",
                    UUID.randomUUID(), rodadaId, i.participanteId(), i.valor(), Timestamp.from(agora));
            notificacoes.notificar(ativos.get(i.participanteId()).pessoaId(), TipoDeNotificacao.RODADA_AVISO, "Confirme o seu investimento",
                    "A " + startups.ler(r.getStartupId()).getNome() + " registrou a sua participação no fechamento da rodada. Confirme ou conteste.",
                    "/catalogo/startups/" + r.getStartupId());
        }
        for (Participante p : ativos.values()) {
            if (!investiram.contains(p.id()) && p.conexaoId() != null) {
                conexoes.encerrarPelaRodada(conexoes.ler(p.conexaoId()));
            }
        }
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-64
     * Título: Responder investimento
     * Objetivo: Deixar o investidor confirmar ou contestar a participação registrada pela startup.
     * Decisões: Confirmado conta como investimento concretizado e a conexão passa para "investiu"; contestado avisa a
     *           startup.
     */
    @Transactional
    public Investimento responderInvestimento(UUID eu, String organizacao, UUID investimentoId, boolean confirmar) {
        Investimento inv = investimentos(null, investimentoId).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Investimento não encontrado."));
        Participante p = participante(inv.participanteId());
        if (!chave(atuacao.paraAgir(eu, organizacao)).equals(p.investidorChave())) {
            throw new RecursoNaoEncontradoException("Investimento não encontrado.");
        }
        if (!"PENDENTE".equals(inv.situacao())) {
            throw new RegraDeNegocioException("ja_respondido", "Esta participação já foi respondida.");
        }
        jdbc.update("UPDATE rodada_investimento SET situacao = ?, respondido_em = ?, respondido_por = ? WHERE id = ?",
                confirmar ? "CONFIRMADA" : "CONTESTADA", Timestamp.from(relogio.instant()), eu, investimentoId);
        Rodada r = rodada(p.rodadaId());
        if (confirmar && p.conexaoId() != null) {
            conexoes.marcarInvestimento(conexoes.ler(p.conexaoId()), eu);
        }
        avisarFundadores(r, confirmar ? "Investimento confirmado" : "Investimento contestado",
                p.investidor() + (confirmar ? " confirmou a participação na rodada." : " contestou a participação registrada na rodada."));
        return investimentos(null, investimentoId).get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-106
     * Título: Rodadas vencidas (R6)
     * Objetivo: Passar as rodadas com prazo vencido para "aguardando resultado" e lembrar a startup; sem registro no
     *           prazo, encerrar "sem resultado informado".
     * Decisões: Devolve quantas mudaram.
     */
    @Transactional
    public int tratarRodadasVencidas() {
        Instant agora = relogio.instant();
        LocalDate hoje = LocalDate.ofInstant(agora, BRASILIA);
        int n = 0;
        for (Rodada r : rodadas.findBySituacaoIn(EnumSet.of(SituacaoDaRodada.ABERTA, SituacaoDaRodada.EM_NEGOCIACAO, SituacaoDaRodada.AGUARDANDO_RESULTADO))) {
            if (r.getSituacao() != SituacaoDaRodada.AGUARDANDO_RESULTADO && r.getPrazo() != null && r.getPrazo().isBefore(hoje)) {
                SituacaoDaRodada anterior = r.aguardarResultado();
                jdbc.update("UPDATE rodada SET aguardando_desde = ? WHERE id = ?", Timestamp.from(agora), r.getId());
                historico.registrar(r.getId(), anterior, r.getSituacao(), null, "Prazo vencido (R6)", agora);
                avisarFundadores(r, "Registre o resultado da rodada", "O prazo da rodada venceu. Registre o resultado ou prorrogue.");
                n++;
            } else if (r.getSituacao() == SituacaoDaRodada.AGUARDANDO_RESULTADO) {
                Timestamp desde = jdbc.queryForObject("SELECT aguardando_desde FROM rodada WHERE id = ?", Timestamp.class, r.getId());
                if (desde != null && desde.toInstant().plus(Duration.ofDays(parametros.inteiro(PRAZO_RESULTADO))).isBefore(agora)) {
                    SituacaoDaRodada anterior = r.encerrar(ResultadoDaRodada.SEM_RESULTADO, agora);
                    historico.registrar(r.getId(), anterior, r.getSituacao(), null, "Encerrada sem resultado informado (R6)", agora);
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-106
     * Título: Confirmações pendentes (R7)
     * Objetivo: Lembrar uma vez os investidores que não confirmaram a participação e marcar "não confirmada" depois do
     *           prazo.
     * Decisões: Devolve quantas trataram.
     */
    @Transactional
    public int tratarConfirmacoes() {
        Instant agora = relogio.instant();
        Instant limite = agora.minus(Duration.ofDays(parametros.inteiro(PRAZO_CONFIRMACAO)));
        int vencidas = jdbc.update("UPDATE rodada_investimento SET situacao = 'NAO_CONFIRMADA', respondido_em = ? WHERE situacao = 'PENDENTE' "
                + "AND criado_em < ?", Timestamp.from(agora), Timestamp.from(limite));
        List<Map<String, Object>> lembrar = jdbc.queryForList("SELECT i.id, p.pessoa_id FROM rodada_investimento i JOIN rodada_participante p "
                + "ON p.id = i.participante_id WHERE i.situacao = 'PENDENTE' AND i.lembrado_em IS NULL");
        for (Map<String, Object> l : lembrar) {
            notificacoes.notificar((UUID) l.get("pessoa_id"), TipoDeNotificacao.RODADA_AVISO, "Confirme o seu investimento",
                    "Ainda falta confirmar a sua participação registrada numa rodada.", "/conexoes");
            jdbc.update("UPDATE rodada_investimento SET lembrado_em = ? WHERE id = ?", Timestamp.from(agora), l.get("id"));
        }
        return vencidas + lembrar.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Montar painel
     * Objetivo: Somar as intenções, comparar com os valores da rodada e sinalizar excesso de demanda.
     * Decisões: Excesso quando a soma passa do máximo.
     */
    private Painel painel(Rodada r, List<Participante> todos, List<Investimento> investimentos) {
        BigDecimal soma = soma(todos);
        Rodada.Termos t = r.termos();
        Map<String, Object> extra = jdbc.queryForMap("SELECT politica_alocacao, prorrogacoes, encerramento_pedido_em, valor_captado, "
                + "instrumento_utilizado, data_fechamento FROM rodada WHERE id = ?", r.getId());
        return new Painel(r.getId(), r.getSituacao(), r.getResultado(), Politica.valueOf((String) extra.get("politica_alocacao")),
                nomeDaChave(lider(r.getId())), nomeDaChave(liderConvidado(r.getId())), todos, soma, t.valorAlvo(), t.valorMinimo(),
                t.valorMaximo(), t.valorMaximo() != null && soma.compareTo(t.valorMaximo()) > 0, r.getPrazo(),
                ((Number) extra.get("prorrogacoes")).intValue(), extra.get("encerramento_pedido_em") != null,
                (BigDecimal) extra.get("valor_captado"), (String) extra.get("instrumento_utilizado"),
                extra.get("data_fechamento") == null ? null : ((java.sql.Date) extra.get("data_fechamento")).toLocalDate(), investimentos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: soma
     * Objetivo: Rotina auxiliar soma.
     * Decisões: Ver a documentação da classe.
     */
    private static BigDecimal soma(List<Participante> todos) {
        return todos.stream().filter(p -> "ATIVO".equals(p.situacao()) && p.intencao() != null).map(Participante::intencao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: meuParticipante
     * Objetivo: Rotina auxiliar meuParticipante.
     * Decisões: Ver a documentação da classe.
     */
    private Participante meuParticipante(UUID eu, String organizacao, UUID rodadaId) {
        UUID chave = chave(atuacao.paraAgir(eu, organizacao));
        return participantes(rodadaId).stream().filter(p -> p.investidorChave().equals(chave) && "ATIVO".equals(p.situacao())).findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Você não participa desta rodada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: participantes
     * Objetivo: Rotina auxiliar participantes.
     * Decisões: Ver a documentação da classe.
     */
    private List<Participante> participantes(UUID rodadaId) {
        return jdbc.query(SELECT_P + " WHERE rodada_id = ? AND situacao <> 'SAIU' ORDER BY entrou_em", mapaParticipante(), rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: participante
     * Objetivo: Rotina auxiliar participante.
     * Decisões: Ver a documentação da classe.
     */
    private Participante participante(UUID id) {
        return jdbc.query(SELECT_P + " WHERE id = ?", mapaParticipante(), id).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: investimentos
     * Objetivo: Rotina auxiliar investimentos.
     * Decisões: Ver a documentação da classe.
     */
    private List<Investimento> investimentos(UUID rodadaId) {
        return investimentos(rodadaId, null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: investimentos
     * Objetivo: Rotina auxiliar investimentos.
     * Decisões: Ver a documentação da classe.
     */
    private List<Investimento> investimentos(UUID rodadaId, UUID id) {
        String filtro = rodadaId != null ? "i.rodada_id = ?" : "i.id = ?";
        return jdbc.query("SELECT i.id, i.participante_id, i.valor, i.situacao, i.criado_em, i.respondido_em FROM rodada_investimento i WHERE "
                + filtro + " ORDER BY i.criado_em", (rs, n) -> new Investimento(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                nomeDoParticipante(rs.getObject(2, UUID.class)), rs.getBigDecimal(3), rs.getString(4), rs.getTimestamp(5).toInstant(),
                rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toInstant()), rodadaId != null ? rodadaId : id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: nomeDoParticipante
     * Objetivo: Rotina auxiliar nomeDoParticipante.
     * Decisões: Ver a documentação da classe.
     */
    private String nomeDoParticipante(UUID participanteId) {
        return participante(participanteId).investidor();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: lider
     * Objetivo: Rotina auxiliar lider.
     * Decisões: Ver a documentação da classe.
     */
    private UUID lider(UUID rodadaId) {
        return jdbc.queryForObject("SELECT lider_chave FROM rodada WHERE id = ?", UUID.class, rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: liderConvidado
     * Objetivo: Rotina auxiliar liderConvidado.
     * Decisões: Ver a documentação da classe.
     */
    private UUID liderConvidado(UUID rodadaId) {
        return jdbc.queryForObject("SELECT lider_convidado_chave FROM rodada WHERE id = ?", UUID.class, rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: politica
     * Objetivo: Rotina auxiliar politica.
     * Decisões: Ver a documentação da classe.
     */
    private Politica politica(UUID rodadaId) {
        return Politica.valueOf(jdbc.queryForObject("SELECT politica_alocacao FROM rodada WHERE id = ?", String.class, rodadaId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: nomeDaChave
     * Objetivo: Rotina auxiliar nomeDaChave.
     * Decisões: Ver a documentação da classe.
     */
    private String nomeDaChave(UUID chave) {
        return chave == null ? null : organizacoes.nome(chave).or(() -> contas.nome(chave)).orElse(null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: avisarFundadores
     * Objetivo: Rotina auxiliar avisarFundadores.
     * Decisões: Ver a documentação da classe.
     */
    private void avisarFundadores(Rodada r, String titulo, String texto) {
        notificacoes.notificarTodos(startups.fundadores(r.getStartupId(), true), TipoDeNotificacao.RODADA_AVISO, titulo, texto,
                "/empreendedor/startups/" + r.getStartupId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: avisarParticipantes
     * Objetivo: Rotina auxiliar avisarParticipantes.
     * Decisões: Ver a documentação da classe.
     */
    private void avisarParticipantes(Rodada r, String titulo, String texto) {
        notificacoes.notificarTodos(participantes(r.getId()).stream().filter(p -> "ATIVO".equals(p.situacao())).map(Participante::pessoaId).toList(),
                TipoDeNotificacao.RODADA_AVISO, titulo, texto, "/catalogo/startups/" + r.getStartupId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: exigirEmCurso
     * Objetivo: Rotina auxiliar exigirEmCurso.
     * Decisões: Ver a documentação da classe.
     */
    private void exigirEmCurso(Rodada r) {
        if (!EM_CURSO.contains(r.getSituacao())) {
            throw new RegraDeNegocioException("rodada_fechada", "A rodada não está aberta para novas entradas.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: conferirValor
     * Objetivo: Rotina auxiliar conferirValor.
     * Decisões: Ver a documentação da classe.
     */
    private static void conferirValor(BigDecimal valor) {
        if (valor != null && valor.signum() <= 0) {
            throw new RegraDeNegocioException("valor_invalido", "O valor precisa ser maior que zero.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: texto
     * Objetivo: Rotina auxiliar texto.
     * Decisões: Ver a documentação da classe.
     */
    private static String texto(String valor) {
        String t = valor == null || valor.isBlank() ? null : valor.strip();
        if (t != null && t.length() > 1000) {
            throw new RegraDeNegocioException("texto_longo", "Use até 1.000 caracteres.");
        }
        return t;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: chave
     * Objetivo: Rotina auxiliar chave.
     * Decisões: Ver a documentação da classe.
     */
    private static UUID chave(AtuacaoService.Atuacao a) {
        return a.organizacao() != null ? a.organizacao() : a.pessoa();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: rodada
     * Objetivo: Rotina auxiliar rodada.
     * Decisões: Ver a documentação da classe.
     */
    private Rodada rodada(UUID id) {
        return rodadas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Rodada não encontrada."));
    }

    static final String SELECT_P = "SELECT id, rodada_id, investidor_chave, pessoa_id, organizacao_id, conexao_id, situacao, intencao, condicoes, "
            + "conexao_anterior, entrou_em FROM rodada_participante";

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: mapaParticipante
     * Objetivo: Rotina auxiliar mapaParticipante.
     * Decisões: Ver a documentação da classe.
     */
    private RowMapper<Participante> mapaParticipante() {
        return (rs, i) -> {
            UUID org = rs.getObject("organizacao_id", UUID.class);
            UUID pessoa = rs.getObject("pessoa_id", UUID.class);
            String nome = contas.nome(pessoa).orElse(null);
            return new Participante(rs.getObject("id", UUID.class), rs.getObject("rodada_id", UUID.class), rs.getObject("investidor_chave", UUID.class),
                    pessoa, org, rs.getObject("conexao_id", UUID.class),
                    org == null ? nome : organizacoes.nome(org).orElse("Organização") + " (por " + nome + ")", rs.getString("situacao"),
                    rs.getBigDecimal("intencao"), rs.getString("condicoes"), rs.getString("conexao_anterior"), rs.getTimestamp("entrou_em").toInstant());
        };
    }

    public record Participante(UUID id, UUID rodadaId, UUID investidorChave, UUID pessoaId, UUID organizacaoId, UUID conexaoId, String investidor,
                               String situacao, BigDecimal intencao, String condicoes, String conexaoAnterior, Instant entrouEm) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: ParticipanteParaLider
     * Objetivo: Rotina auxiliar ParticipanteParaLider.
     * Decisões: Ver a documentação da classe.
     */
    public record ParticipanteParaLider(UUID id, String investidor, String situacao) {
    }

    public record Investimento(UUID id, UUID participanteId, String investidor, BigDecimal valor, String situacao, Instant criadoEm,
                               Instant respondidoEm) {
    }

    public record Painel(UUID id, SituacaoDaRodada situacao, ResultadoDaRodada resultado, Politica politica, String lider, String liderConvidado,
                         List<Participante> participantes, BigDecimal somaIntencoes, BigDecimal valorAlvo, BigDecimal valorMinimo,
                         BigDecimal valorMaximo, boolean excessoDeDemanda, LocalDate prazo, int prorrogacoes, boolean encerramentoPedido,
                         BigDecimal valorCaptado, String instrumentoUtilizado, LocalDate dataFechamento, List<Investimento> investimentos) {
    }

    public record ParaInvestidor(UUID id, SituacaoDaRodada situacao, Politica politica, String lider, boolean convidadoParaLiderar,
                                 boolean souLider, boolean podeEntrar, Participante minhaParticipacao, List<ParticipanteParaLider> participantes,
                                 BigDecimal somaIntencoes, List<Investimento> meusInvestimentos) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: InvestidorDoResultado
     * Objetivo: Rotina auxiliar InvestidorDoResultado.
     * Decisões: Ver a documentação da classe.
     */
    public record InvestidorDoResultado(UUID participanteId, BigDecimal valor) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Resultado
     * Objetivo: Rotina auxiliar Resultado.
     * Decisões: Ver a documentação da classe.
     */
    public record Resultado(BigDecimal valorCaptado, String instrumento, LocalDate dataFechamento, List<InvestidorDoResultado> investidores) {
    }
}
