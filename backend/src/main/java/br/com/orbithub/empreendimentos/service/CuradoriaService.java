package br.com.orbithub.empreendimentos.service;

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
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import br.com.orbithub.empreendimentos.domain.MudancaDeSituacao;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.RevisaoDeCuradoria;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.MembroDaStartupRepository;
import br.com.orbithub.empreendimentos.repository.MudancaDeSituacaoRepository;
import br.com.orbithub.empreendimentos.repository.RevisaoDeCuradoriaRepository;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66, KAN-84
 * Título: Curadoria das startups
 * Objetivo: Montar a fila da curadoria com prazo, mostrar ao curador o que conferir (cadastro, selos dos fundadores e
 *           da startup, alterações), registrar o checklist e a decisão (aprovar, devolver com apontamentos ou propor a
 *           recusa por fraude), e mostrar à equipe da startup o resultado e os comentários.
 * Decisões: Prazo em dias úteis (curadoria.sla_dias_uteis). O checklist precisa marcar todos os itens ativos da lista;
 *           aprovar exige todos atendidos. Devolver exige parecer ou comentário. A recusa por fraude vira proposta de
 *           segunda aprovação (REMOCAO_STARTUP_FRAUDE) e só tira a startup depois de aprovada; o caso de moderação e o
 *           recurso entram com as histórias de moderação e recursos (S10). Quem tem vínculo com a startup não assume nem
 *           decide (KAN-87).
 */
@Service
public class CuradoriaService {

    static final String SLA = "curadoria.sla_dias_uteis";
    static final String CASO = "CURADORIA";
    static final String DADO_REVISAO = "revisao";

    private final RevisaoDeCuradoriaRepository revisoes;
    private final StartupRepository startups;
    private final MembroDaStartupRepository membros;
    private final MudancaDeSituacaoRepository historico;
    private final StartupsService cadastro;
    private final ListasService listas;
    private final LeituraDeParametros parametros;
    private final ConsultaDeContas contas;
    private final ConflitosDeInteresse conflitos;
    private final VerificacoesService verificacoes;
    private final SegundaAprovacaoService aprovacoes;
    private final NotificacoesService notificacoes;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Criação do serviço
     * Objetivo: Receber repositórios e serviços usados pela curadoria.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public CuradoriaService(RevisaoDeCuradoriaRepository revisoes, StartupRepository startups, MembroDaStartupRepository membros,
                            MudancaDeSituacaoRepository historico, StartupsService cadastro, ListasService listas,
                            LeituraDeParametros parametros, ConsultaDeContas contas, ConflitosDeInteresse conflitos,
                            VerificacoesService verificacoes, SegundaAprovacaoService aprovacoes,
                            NotificacoesService notificacoes, Clock relogio) {
        this.revisoes = revisoes;
        this.startups = startups;
        this.membros = membros;
        this.historico = historico;
        this.cadastro = cadastro;
        this.listas = listas;
        this.parametros = parametros;
        this.contas = contas;
        this.conflitos = conflitos;
        this.verificacoes = verificacoes;
        this.aprovacoes = aprovacoes;
        this.notificacoes = notificacoes;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Fila da curadoria
     * Objetivo: Listar as revisões abertas por ordem de chegada, com prazo, atraso, responsável e conflitos.
     * Decisões: "Sem curador livre" quando todos os curadores têm vínculo: o caso vai para o administrador (KAN-87).
     */
    @Transactional(readOnly = true)
    public List<ItemDaFila> fila(UUID quemVe) {
        Instant agora = relogio.instant();
        long sla = parametros.inteiro(SLA);
        List<UUID> curadores = contas.equipeAtivaComPapeis(Set.of(Papel.CURADOR));
        return revisoes.findBySituacaoInOrderByEnviadaEmAsc(RevisoesDaStartup.ABERTAS).stream().map(r -> {
            Startup s = startup(r.getStartupId());
            Instant prazo = DiasUteis.somar(r.getEnviadaEm(), sla);
            ConflitosDeInteresse.Envolvidos envolvidos = envolvidos(s);
            boolean comigo = conflitos.temConflito(quemVe, CASO, r.getId(), envolvidos);
            boolean semCurador = curadores.stream().allMatch(c -> conflitos.temConflito(c, CASO, r.getId(), envolvidos));
            return new ItemDaFila(r, s.getNome(), prazo, agora.isAfter(prazo), nome(r.getResponsavelId()), comigo, semCurador);
        }).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Detalhe da revisão
     * Objetivo: Reunir o que o curador confere: a startup, o checklist ativo, os selos, as alterações e as revisões anteriores.
     * Decisões: Fundadores são os membros com papel de fundador; o selo vem da verificação (KAN-62).
     */
    @Transactional(readOnly = true)
    public Detalhe detalhe(UUID revisaoId) {
        RevisaoDeCuradoria r = revisao(revisaoId);
        Startup s = startup(r.getStartupId());
        List<Fundador> fundadores = membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(s.getId()).stream()
                .filter(m -> m.getPapel() != PapelNaStartup.COLABORADOR)
                .map(m -> new Fundador(nome(m.getUsuarioId()), m.getPapel(), verificacoes.selo(SujeitoDaVerificacao.FUNDADOR, m.getUsuarioId())))
                .toList();
        List<Alteracao> alteracoes = new ArrayList<>();
        if (r.getTipo() == RevisaoDeCuradoria.Tipo.ALTERACAO) {
            alteracao(alteracoes, "Nome", s.getNomeAprovado(), s.getNome());
            alteracao(alteracoes, "Segmento", s.getSegmentoAprovado(), s.getSegmento());
            alteracao(alteracoes, "Estágio", s.getEstagioAprovado(), s.campos().get(CampoDaStartup.ESTAGIO));
        }
        List<RevisaoDeCuradoria> anteriores = revisoes.findByStartupIdOrderByEnviadaEmDesc(s.getId()).stream()
                .filter(x -> !x.getId().equals(revisaoId)).toList();
        return new Detalhe(r, s, nome(r.getResponsavelId()), listas.ativos(Lista.CHECKLIST_CURADORIA), fundadores,
                verificacoes.selo(SujeitoDaVerificacao.STARTUP, s.getId()), alteracoes, anteriores,
                DiasUteis.somar(r.getEnviadaEm(), parametros.inteiro(SLA)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Assumir revisão
     * Objetivo: Atribuir a revisão ao curador.
     * Decisões: Só o administrador toma de outro curador. Conflito de interesse bloqueia (KAN-87).
     */
    @Transactional
    public RevisaoDeCuradoria assumir(UUID revisaoId, UUID curador, boolean administrador) {
        RevisaoDeCuradoria r = revisao(revisaoId);
        if (r.getResponsavelId() != null && !r.getResponsavelId().equals(curador) && !administrador) {
            throw new RegraDeNegocioException("revisao_ja_assumida", "Outra pessoa da curadoria já assumiu esta revisão.");
        }
        conflitos.exigirSemConflito(curador, CASO, revisaoId, envolvidos(startup(r.getStartupId())));
        r.assumir(curador, relogio.instant());
        ContextoDeAuditoria.descrever("CURADORIA_ASSUMIDA", "revisao_curadoria", revisaoId, null);
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Declarar impedimento
     * Objetivo: Deixar o curador se declarar impedido, com motivo, e devolver a revisão à fila.
     * Decisões: Mesma regra da verificação (KAN-87).
     */
    @Transactional
    public RevisaoDeCuradoria declararImpedimento(UUID revisaoId, UUID curador, String motivo) {
        RevisaoDeCuradoria r = revisao(revisaoId);
        conflitos.declararImpedimento(CASO, revisaoId, curador, motivo);
        if (curador.equals(r.getResponsavelId())) {
            r.liberar();
        }
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66, KAN-84
     * Título: Decidir revisão
     * Objetivo: Registrar o checklist, os apontamentos e o parecer, e aprovar, devolver ou propor a recusa por fraude.
     * Decisões: Aprovar a publicação publica a startup e guarda os valores aprovados; aprovar a alteração só troca os
     *           valores aprovados. Devolver a publicação põe a startup em "devolvida"; devolver a alteração mantém a
     *           versão aprovada no catálogo. A recusa por fraude espera a segunda aprovação.
     */
    @Transactional
    public RevisaoDeCuradoria decidir(UUID revisaoId, UUID curador, boolean administrador, Decisao decisao) {
        RevisaoDeCuradoria r = revisao(revisaoId);
        if (r.getSituacao() != RevisaoDeCuradoria.Situacao.EM_ANALISE) {
            throw new RegraDeNegocioException("revisao_fora_da_fila", "Esta revisão não está mais na fila.");
        }
        if (!administrador && !curador.equals(r.getResponsavelId())) {
            throw new AccessDeniedException("Assuma a revisão antes de decidir.");
        }
        Startup s = startup(r.getStartupId());
        conflitos.exigirSemConflito(curador, CASO, revisaoId, envolvidos(s));
        String parecer = texto(decisao.parecer(), 2000, "O parecer pode ter até 2.000 caracteres.");
        List<RevisaoDeCuradoria.Item> itens = itens(decisao);
        Instant agora = relogio.instant();
        switch (decisao.resultado()) {
            case APROVADA -> {
                if (itens.stream().anyMatch(i -> RevisaoDeCuradoria.Item.CHECKLIST.equals(i.tipo()) && !Boolean.TRUE.equals(i.atendido()))) {
                    throw new RegraDeNegocioException("checklist_pendente", "Para aprovar, todos os itens do checklist precisam estar atendidos.");
                }
                r.decidir(RevisaoDeCuradoria.Situacao.APROVADA, curador, itens, parecer, null, agora);
                if (r.getTipo() == RevisaoDeCuradoria.Tipo.PUBLICACAO) {
                    mudanca(s, s.aprovarPublicacao(agora), curador, null);
                } else {
                    s.aprovarValoresAtuais();
                }
                avisarEquipe(s, "A " + s.getNome() + " foi aprovada pela curadoria",
                        r.getTipo() == RevisaoDeCuradoria.Tipo.PUBLICACAO ? "A startup já aparece no catálogo."
                                : "A alteração já aparece no catálogo.");
            }
            case DEVOLVIDA -> {
                boolean temComentario = itens.stream().anyMatch(i -> i.comentario() != null);
                if (parecer == null && !temComentario) {
                    throw new RegraDeNegocioException("devolucao_sem_comentario", "Diga ao empreendedor o que ajustar.");
                }
                r.decidir(RevisaoDeCuradoria.Situacao.DEVOLVIDA, curador, itens, parecer, null, agora);
                if (r.getTipo() == RevisaoDeCuradoria.Tipo.PUBLICACAO) {
                    mudanca(s, s.devolver(agora), curador, parecer);
                }
                avisarEquipe(s, "A curadoria pediu ajustes na " + s.getNome(),
                        (r.getTipo() == RevisaoDeCuradoria.Tipo.ALTERACAO ? "A versão aprovada continua no catálogo. " : "")
                                + "Veja os comentários na aba Publicação e envie de novo.");
            }
            case RECUSADA -> {
                if (parecer == null) {
                    throw new RegraDeNegocioException("recusa_sem_parecer", "Descreva o indício de fraude para a segunda aprovação.");
                }
                UUID proposta = aprovacoes.propor(AcaoGrave.REMOCAO_STARTUP_FRAUDE, "startup", s.getId().toString(),
                        "Recusa por fraude da startup " + s.getNome(), Map.of(DADO_REVISAO, revisaoId.toString()), curador, parecer).getId();
                r.decidir(RevisaoDeCuradoria.Situacao.AGUARDANDO_APROVACAO, curador, itens, parecer, proposta, agora);
            }
            default -> throw new RegraDeNegocioException("decisao_invalida", "Decida por aprovar, devolver ou recusar.");
        }
        ContextoDeAuditoria.descrever("CURADORIA_DECIDIDA", "revisao_curadoria", revisaoId, decisao.resultado().name());
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Concluir a recusa por fraude
     * Objetivo: Depois da segunda aprovação, recusar a startup (sai da fila e do catálogo) e avisar a equipe; rejeitada a
     *           proposta, devolver a revisão à análise.
     * Decisões: Chamado pelo executor da ação grave. Sem revisão ligada (remoção vinda da moderação), só recusa a startup.
     */
    @Transactional
    public void concluirRecusa(UUID startupId, UUID propostaId, UUID aprovador, boolean aprovada) {
        Optional<RevisaoDeCuradoria> r = revisoes.findByPropostaId(propostaId);
        r.ifPresent(x -> x.concluirRecusa(aprovada, relogio.instant()));
        if (!aprovada) {
            return;
        }
        Startup s = startup(startupId);
        mudanca(s, s.recusarPorFraude(relogio.instant()), aprovador, "Recusada por indício de fraude, com segunda aprovação");
        avisarEquipe(s, "A " + s.getNome() + " foi recusada pela curadoria",
                "A startup foi recusada por indício de fraude e saiu do catálogo. Fale com o atendimento se discordar.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Revisões da startup para a equipe
     * Objetivo: Mostrar à equipe as revisões da startup, com o resultado, o parecer, os itens não atendidos e os
     *           apontamentos por seção.
     * Decisões: Qualquer membro da equipe vê. O nome do curador não aparece. A recusa aguardando aprovação aparece como
     *           "em análise" para a equipe.
     */
    @Transactional(readOnly = true)
    public List<RevisaoParaEquipe> daStartup(UUID eu, UUID startupId) {
        cadastro.papel(eu, startupId);
        long sla = parametros.inteiro(SLA);
        return revisoes.findByStartupIdOrderByEnviadaEmDesc(startupId).stream()
                .filter(r -> r.getSituacao() != RevisaoDeCuradoria.Situacao.CANCELADA)
                .map(r -> {
                    boolean emAnalise = r.aberta();
                    List<RevisaoDeCuradoria.Item> apontamentos = emAnalise ? List.of() : r.getItens().stream()
                            .filter(i -> RevisaoDeCuradoria.Item.SECAO.equals(i.tipo())
                                    || (!Boolean.TRUE.equals(i.atendido()) && i.comentario() != null))
                            .toList();
                    return new RevisaoParaEquipe(r.getId(), r.getTipo(), emAnalise ? RevisaoDeCuradoria.Situacao.EM_ANALISE : r.getSituacao(),
                            r.getEnviadaEm(), emAnalise ? null : r.getDecididaEm(), emAnalise ? null : r.getParecer(),
                            emAnalise ? DiasUteis.somar(r.getEnviadaEm(), sla) : null, apontamentos.stream()
                            .map(i -> new Apontamento(i.tipo(), i.codigo(), rotuloDoItem(i), i.comentario())).toList());
                }).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Montar itens da decisão
     * Objetivo: Conferir o checklist (todos os itens ativos, sem repetição) e os apontamentos por seção.
     * Decisões: Comentário até 1.000 caracteres.
     */
    private List<RevisaoDeCuradoria.Item> itens(Decisao decisao) {
        Set<String> ativos = listas.ativos(Lista.CHECKLIST_CURADORIA).stream().map(ItemDeLista::getCodigo)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<RevisaoDeCuradoria.Item> itens = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Marcacao m : decisao.checklist() == null ? List.<Marcacao>of() : decisao.checklist()) {
            if (!ativos.contains(m.codigo()) || !vistos.add(m.codigo())) {
                throw new RegraDeNegocioException("checklist_invalido", "Item do checklist desconhecido ou repetido: " + m.codigo() + ".");
            }
            itens.add(new RevisaoDeCuradoria.Item(RevisaoDeCuradoria.Item.CHECKLIST, m.codigo(), m.atendido(),
                    texto(m.comentario(), 1000, "O comentário pode ter até 1.000 caracteres.")));
        }
        if (!vistos.containsAll(ativos)) {
            throw new RegraDeNegocioException("checklist_incompleto", "Marque todos os itens do checklist.");
        }
        Set<SecaoDaStartup> secoes = EnumSet.noneOf(SecaoDaStartup.class);
        for (ApontamentoNaSecao a : decisao.apontamentos() == null ? List.<ApontamentoNaSecao>of() : decisao.apontamentos()) {
            String comentario = texto(a.comentario(), 1000, "O comentário pode ter até 1.000 caracteres.");
            if (a.secao() == null || comentario == null || !secoes.add(a.secao())) {
                throw new RegraDeNegocioException("apontamento_invalido", "Cada apontamento precisa de uma seção (sem repetir) e do comentário.");
            }
            itens.add(new RevisaoDeCuradoria.Item(RevisaoDeCuradoria.Item.SECAO, a.secao().name(), null, comentario));
        }
        return itens;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Rótulo do item
     * Objetivo: Traduzir o código do item do checklist ou da seção para o texto da tela.
     * Decisões: Item removido da lista mostra o código.
     */
    private String rotuloDoItem(RevisaoDeCuradoria.Item i) {
        if (RevisaoDeCuradoria.Item.SECAO.equals(i.tipo())) {
            return SecaoDaStartup.valueOf(i.codigo()).getRotulo();
        }
        return listas.rotulo(Lista.CHECKLIST_CURADORIA, i.codigo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Registrar mudança de situação
     * Objetivo: Gravar no histórico da startup a mudança feita pela curadoria.
     * Decisões: Mesma tabela do KAN-27.
     */
    private void mudanca(Startup s, SituacaoDaStartup anterior, UUID por, String motivo) {
        historico.save(MudancaDeSituacao.nova(s.getId(), anterior, s.getSituacao(), por, motivo, relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Avisar a equipe
     * Objetivo: Notificar os membros ativos da startup sobre o resultado da curadoria.
     * Decisões: Tipo CURADORIA_RESULTADO; link para a startup.
     */
    private void avisarEquipe(Startup s, String titulo, String texto) {
        List<UUID> equipe = membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(s.getId()).stream()
                .map(MembroDaStartup::getUsuarioId).toList();
        notificacoes.notificarTodos(equipe, TipoDeNotificacao.CURADORIA_RESULTADO, titulo, texto, "/empreendedor/startups/" + s.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-87
     * Título: Envolvidos
     * Objetivo: Montar o CNPJ e as contas da equipe para o bloqueio de conflito de interesse.
     * Decisões: Inclui colaboradores.
     */
    private ConflitosDeInteresse.Envolvidos envolvidos(Startup s) {
        Set<UUID> equipe = membros.findByStartupIdAndSaiuEmIsNullOrderByEntrouEm(s.getId()).stream()
                .map(MembroDaStartup::getUsuarioId).collect(Collectors.toSet());
        return new ConflitosDeInteresse.Envolvidos(s.getCnpj() == null ? Set.of() : Set.of(s.getCnpj()), equipe);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Alteração
     * Objetivo: Acrescentar um campo alterado, com o valor aprovado e o novo.
     * Decisões: Só quando diferem.
     */
    private static void alteracao(List<Alteracao> lista, String campo, String aprovado, String atual) {
        if (!Objects.equals(aprovado, atual)) {
            lista.add(new Alteracao(campo, aprovado, atual));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Texto opcional
     * Objetivo: Limpar o texto e conferir o limite.
     * Decisões: Vazio vira nulo.
     */
    private static String texto(String valor, int limite, String erro) {
        String t = valor == null || valor.isBlank() ? null : valor.strip();
        if (t != null && t.length() > limite) {
            throw new RegraDeNegocioException("texto_longo", erro);
        }
        return t;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Nome da conta
     * Objetivo: Devolver o nome de uma conta da equipe interna.
     * Decisões: Nulo para conta nula.
     */
    private String nome(UUID conta) {
        return Optional.ofNullable(conta).flatMap(contas::nome).orElse(null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Revisão
     * Objetivo: Ler a revisão pelo identificador.
     * Decisões: 404 se não existir.
     */
    private RevisaoDeCuradoria revisao(UUID id) {
        return revisoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Revisão não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Decisão do curador
     * Objetivo: Levar o resultado, o checklist marcado, os apontamentos por seção e o parecer.
     * Decisões: Resultado APROVADA, DEVOLVIDA ou RECUSADA (por fraude).
     */
    public record Decisao(RevisaoDeCuradoria.Situacao resultado, List<Marcacao> checklist, List<ApontamentoNaSecao> apontamentos,
                          String parecer) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Marcação do checklist
     * Objetivo: Levar um item do checklist marcado.
     * Decisões: Comentário opcional.
     */
    public record Marcacao(String codigo, boolean atendido, String comentario) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Apontamento na seção
     * Objetivo: Levar o comentário do curador para uma seção do cadastro.
     * Decisões: Aparece para o empreendedor junto da seção.
     */
    public record ApontamentoNaSecao(SecaoDaStartup secao, String comentario) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Item da fila
     * Objetivo: Levar a revisão com prazo, atraso, responsável e conflitos.
     * Decisões: Nome do responsável, não o identificador.
     */
    public record ItemDaFila(RevisaoDeCuradoria revisao, String startup, Instant prazo, boolean atrasado, String responsavel,
                             boolean conflitoComigo, boolean semCuradorLivre) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Fundador no detalhe
     * Objetivo: Levar o nome, o papel e o selo de verificação de um fundador.
     * Decisões: Colaboradores ficam fora.
     */
    public record Fundador(String nome, PapelNaStartup papel, Selo selo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Alteração relevante
     * Objetivo: Levar o campo alterado, o valor aprovado e o novo.
     * Decisões: Valores crus (códigos); a tela traduz.
     */
    public record Alteracao(String campo, String aprovado, String novo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Detalhe da revisão
     * Objetivo: Levar tudo o que o curador confere.
     * Decisões: Inclui o prazo.
     */
    public record Detalhe(RevisaoDeCuradoria revisao, Startup startup, String responsavel, List<ItemDeLista> checklist,
                          List<Fundador> fundadores, Selo seloDaStartup, List<Alteracao> alteracoes,
                          List<RevisaoDeCuradoria> anteriores, Instant prazo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Revisão para a equipe
     * Objetivo: Levar à equipe da startup o resultado e os comentários de uma revisão.
     * Decisões: Sem o curador.
     */
    public record RevisaoParaEquipe(UUID id, RevisaoDeCuradoria.Tipo tipo, RevisaoDeCuradoria.Situacao situacao, Instant enviadaEm,
                                    Instant decididaEm, String parecer, Instant prazo, List<Apontamento> apontamentos) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Apontamento para a equipe
     * Objetivo: Levar um comentário da curadoria: item não atendido do checklist ou seção do cadastro.
     * Decisões: Tipo CHECKLIST ou SECAO; código para a tela achar a seção.
     */
    public record Apontamento(String tipo, String codigo, String rotulo, String comentario) {
    }
}
