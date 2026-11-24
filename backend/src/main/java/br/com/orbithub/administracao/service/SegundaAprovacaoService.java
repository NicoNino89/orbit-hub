package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao.Situacao;
import br.com.orbithub.administracao.repository.PropostaDeAprovacaoRepository;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Ações com segunda aprovação
 * Objetivo: Receber propostas de ações graves, deixar uma segunda pessoa aprovar ou rejeitar, e só então executar
 *           a ação.
 * Decisões: Os módulos não aplicam ações graves direto: chamam {@link #propor}, e o executor registrado para a ação
 *           só roda na aprovação. Quem propõe nunca decide; quem decide precisa de um dos papéis aceitos pelo
 *           executor. Proposta parada há mais de aprovacao.prazo_horas aparece como vencida e entra no alerta ao
 *           administrador (rotina da KAN-104). Cada passo recebe nome na trilha de auditoria.
 */
@Service
public class SegundaAprovacaoService {

    static final String ACAO_PROPOSTA = "APROVACAO_PROPOSTA";
    static final String ACAO_APROVADA = "APROVACAO_APROVADA";
    static final String ACAO_REJEITADA = "APROVACAO_REJEITADA";
    static final String ACAO_CANCELADA = "APROVACAO_CANCELADA";
    static final String OBJETO = "proposta_aprovacao";

    private final PropostaDeAprovacaoRepository propostas;
    private final Map<AcaoGrave, ExecutorDeAcaoGrave> executores;
    private final LeituraDeParametros parametros;
    private final NotificacoesService notificacoes;
    private final ConsultaDeContas contas;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Criação do serviço
     * Objetivo: Indexar os executores por ação e receber o prazo e o relógio.
     * Decisões: Dois executores para a mesma ação é erro de configuração e falha na inicialização.
     */
    public SegundaAprovacaoService(PropostaDeAprovacaoRepository propostas, List<ExecutorDeAcaoGrave> executores,
                                   LeituraDeParametros parametros, NotificacoesService notificacoes,
                                   ConsultaDeContas contas, Clock relogio) {
        this.notificacoes = notificacoes;
        this.contas = contas;
        this.propostas = propostas;
        Map<AcaoGrave, ExecutorDeAcaoGrave> indice = new EnumMap<>(AcaoGrave.class);
        for (ExecutorDeAcaoGrave executor : executores) {
            if (indice.put(executor.acao(), executor) != null) {
                throw new IllegalStateException("Dois executores para " + executor.acao());
            }
        }
        this.executores = Collections.unmodifiableMap(indice);
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Propor ação grave
     * Objetivo: Registrar a proposta, que fica pendente até a segunda pessoa decidir.
     * Decisões: Sem executor, a ação não existe ainda; proposta igual pendente é recusada; o executor confere os
     *           dados já aqui, para não aprovar algo que não poderia ser aplicado.
     */
    @Transactional
    public PropostaDeAprovacao propor(AcaoGrave acao, String objetoTipo, String objetoId, String resumo,
                                      Map<String, String> dados, UUID proponente, String justificativa) {
        ExecutorDeAcaoGrave executor = executor(acao);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("justificativa_obrigatoria", "Explique por que esta ação é necessária.");
        }
        if (propostas.existsByTipoAndObjetoTipoAndObjetoIdAndSituacao(acao, objetoTipo, objetoId, Situacao.PENDENTE)) {
            throw new RegraDeNegocioException("proposta_pendente",
                    "Já existe uma proposta pendente para esta ação. Aguarde a decisão ou cancele a anterior.");
        }
        executor.conferir(dados);
        PropostaDeAprovacao proposta = propostas.save(PropostaDeAprovacao.propor(acao, objetoTipo, objetoId, resumo,
                dados, proponente, justificativa, relogio.instant()));
        ContextoDeAuditoria.descrever(ACAO_PROPOSTA, OBJETO, proposta.getId(), acao + ": " + resumo);
        ContextoDeAuditoria.justificar(justificativa);
        avisarQuemAprova(proposta, "Nova ação aguardando a sua aprovação");
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Aprovar
     * Objetivo: Registrar a aprovação e executar a ação.
     * Decisões: A ação roda na mesma transação; se o executor recusar (por exemplo, o valor deixou de ser aceito),
     *           nada muda e a proposta continua pendente.
     */
    @Transactional
    public PropostaDeAprovacao aprovar(UUID propostaId, UUID aprovador, Set<Papel> papeis, String justificativa) {
        PropostaDeAprovacao proposta = decidivel(propostaId, aprovador, papeis, justificativa, ACAO_APROVADA);
        executor(proposta.getTipo()).executar(proposta, aprovador);
        proposta.aprovar(aprovador, justificativa, relogio.instant());
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Rejeitar
     * Objetivo: Registrar a rejeição; a ação não é executada.
     * Decisões: Mesmas regras da aprovação.
     */
    @Transactional
    public PropostaDeAprovacao rejeitar(UUID propostaId, UUID aprovador, Set<Papel> papeis, String justificativa) {
        PropostaDeAprovacao proposta = decidivel(propostaId, aprovador, papeis, justificativa, ACAO_REJEITADA);
        proposta.rejeitar(aprovador, justificativa, relogio.instant());
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Cancelar
     * Objetivo: Deixar quem propôs desistir da proposta pendente.
     * Decisões: Só quem propôs.
     */
    @Transactional
    public PropostaDeAprovacao cancelar(UUID propostaId, UUID quem) {
        PropostaDeAprovacao proposta = buscar(propostaId);
        ContextoDeAuditoria.descrever(ACAO_CANCELADA, OBJETO, propostaId, proposta.getResumo());
        if (!proposta.getPropostoPor().equals(quem)) {
            throw new RegraDeNegocioException("cancelamento_negado", "Só quem fez a proposta pode cancelá-la.");
        }
        exigirPendente(proposta);
        proposta.cancelar(quem, relogio.instant());
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Fila de propostas
     * Objetivo: Listar as pendentes (mais antigas primeiro) e as últimas decididas.
     * Decisões: Duas listas numa só, para a tela mostrar fila e histórico.
     */
    @Transactional(readOnly = true)
    public List<PropostaDeAprovacao> listar() {
        List<PropostaDeAprovacao> todas = new ArrayList<>(propostas.findBySituacaoOrderByPropostoEmAsc(Situacao.PENDENTE));
        todas.addAll(propostas.findTop50BySituacaoNotOrderByDecididoEmDesc(Situacao.PENDENTE));
        return todas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Pode decidir
     * Objetivo: Dizer se a pessoa pode aprovar ou rejeitar a proposta (para a tela mostrar os botões).
     * Decisões: Pendente, não é dela e ela tem um papel aceito pelo executor.
     */
    public boolean podeDecidir(PropostaDeAprovacao proposta, UUID pessoa, Set<Papel> papeis) {
        ExecutorDeAcaoGrave executor = executores.get(proposta.getTipo());
        return proposta.getSituacao() == Situacao.PENDENTE && !proposta.getPropostoPor().equals(pessoa)
                && executor != null && executor.papeisQueAprovam().stream().anyMatch(papeis::contains);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Proposta vencida
     * Objetivo: Dizer se a proposta passou do prazo de análise.
     * Decisões: Prazo aprovacao.prazo_horas.
     */
    public boolean vencida(PropostaDeAprovacao proposta) {
        return proposta.vencida(relogio.instant(), parametros.duracao(Chaves.APROVACAO_PRAZO));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Vencidas para alertar
     * Objetivo: Devolver as pendentes paradas além do prazo que ainda não geraram alerta, e marcá-las.
     * Decisões: Marca na mesma transação, para a rotina (KAN-104) não alertar duas vezes a mesma proposta.
     */
    @Transactional
    public List<PropostaDeAprovacao> vencidasParaAlertar() {
        Instant agora = relogio.instant();
        List<PropostaDeAprovacao> vencidas = propostas.pendentesVencidasSemAlerta(
                agora.minus(parametros.duracao(Chaves.APROVACAO_PRAZO)));
        vencidas.forEach(p -> p.marcarAlertada(agora));
        return vencidas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Ações disponíveis
     * Objetivo: Dizer quais ações graves já têm executor (podem ser propostas).
     * Decisões: As demais aparecem na tela como "em breve".
     */
    public Set<AcaoGrave> acoesDisponiveis() {
        return executores.keySet();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta que a pessoa pode decidir
     * Objetivo: Conferir as regras comuns de aprovação e rejeição.
     * Decisões: Recusas com mensagens de negócio; a ação é descrita antes, para a recusa também entrar na trilha.
     */
    private PropostaDeAprovacao decidivel(UUID propostaId, UUID aprovador, Set<Papel> papeis, String justificativa,
                                          String acaoNaTrilha) {
        PropostaDeAprovacao proposta = buscar(propostaId);
        ContextoDeAuditoria.descrever(acaoNaTrilha, OBJETO, propostaId,
                proposta.getTipo() + ": " + proposta.getResumo());
        ContextoDeAuditoria.justificar(justificativa);
        exigirPendente(proposta);
        if (proposta.getPropostoPor().equals(aprovador)) {
            throw new RegraDeNegocioException("autoaprovacao",
                    "Quem propõe não pode decidir a própria proposta. Outra pessoa precisa analisar.");
        }
        if (executor(proposta.getTipo()).papeisQueAprovam().stream().noneMatch(papeis::contains)) {
            throw new RegraDeNegocioException("papel_insuficiente",
                    "Seu papel não permite decidir este tipo de ação.");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("justificativa_obrigatoria", "Explique a sua decisão.");
        }
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Buscar proposta
     * Objetivo: Ler a proposta pelo id.
     * Decisões: Inexistente: 404.
     */
    private PropostaDeAprovacao buscar(UUID propostaId) {
        return propostas.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Exigir pendente
     * Objetivo: Recusar decisão sobre proposta já decidida.
     * Decisões: Mensagem de negócio.
     */
    private static void exigirPendente(PropostaDeAprovacao proposta) {
        if (proposta.getSituacao() != Situacao.PENDENTE) {
            throw new RegraDeNegocioException("proposta_decidida", "Esta proposta já foi decidida.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Executor da ação
     * Objetivo: Achar o executor da ação grave.
     * Decisões: Sem executor: a funcionalidade ainda não existe.
     */
    private ExecutorDeAcaoGrave executor(AcaoGrave acao) {
        ExecutorDeAcaoGrave executor = executores.get(acao);
        if (executor == null) {
            throw new RegraDeNegocioException("acao_indisponivel",
                    "Esta ação ainda não está disponível na plataforma.");
        }
        return executor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Avisar quem aprova
     * Objetivo: Notificar os membros ativos com papel para decidir a proposta, menos quem propôs.
     * Decisões: Aviso obrigatório para a equipe; usado na proposta nova e no alerta de proposta parada (R15).
     */
    public void avisarQuemAprova(PropostaDeAprovacao proposta, String titulo) {
        ExecutorDeAcaoGrave executor = executores.get(proposta.getTipo());
        if (executor == null) {
            return;
        }
        List<UUID> destinatarios = contas.equipeAtivaComPapeis(executor.papeisQueAprovam()).stream()
                .filter(id -> !id.equals(proposta.getPropostoPor())).toList();
        notificacoes.notificarTodos(destinatarios, TipoDeNotificacao.EQUIPE_APROVACAO, titulo,
                proposta.getTipo().getDescricao() + ": " + proposta.getResumo() + ". Justificativa: "
                        + proposta.getJustificativa(), "/admin/aprovacoes");
    }
}
