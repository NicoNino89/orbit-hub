package br.com.orbithub.verificacao.service;

import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.tempo.DiasUteis;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.repository.DocumentoDeVerificacaoRepository;
import br.com.orbithub.verificacao.repository.PedidoDeVerificacaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Fila de verificação
 * Objetivo: Organizar os pedidos em análise por ordem de chegada, com o prazo do time, e deixar o analista assumir,
 *           ler os documentos e decidir: verificado, pendente de complemento ou recusado, sempre com motivo da lista.
 * Decisões: Só decide quem assumiu o pedido (ou o administrador). O motivo precisa ser do grupo da decisão (VERIFICADO,
 *           COMPLEMENTO, RECUSA ou FRAUDE); FRAUDE recusa e encaminha à administração até existir a moderação (KAN-71).
 *           Complemento exige dizer o que falta. Toda leitura de documento e toda decisão vão para a trilha de auditoria.
 *           A distribuição automática (KAN-76) e o recurso (história de recursos) chegam depois; aqui o analista assume.
 */
@Service
public class FilaDeVerificacao {

    static final String SLA = "verificacao.sla_dias_uteis";
    static final String VALIDADE = "verificacao.validade_meses";
    static final String PRAZO_COMPLEMENTO = "verificacao.prazo_complemento_dias";
    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final Map<SituacaoDoPedido, List<String>> GRUPOS = Map.of(
            SituacaoDoPedido.VERIFICADO, List.of("VERIFICADO"),
            SituacaoDoPedido.PENDENTE_COMPLEMENTO, List.of("COMPLEMENTO"),
            SituacaoDoPedido.RECUSADO, List.of("RECUSA", "FRAUDE"));

    private final PedidoDeVerificacaoRepository pedidos;
    private final DocumentoDeVerificacaoRepository documentos;
    private final ListasService listas;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final ConsultaDeContas contas;
    private final AvisosDaVerificacao avisos;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, listas, armazenamento, parâmetros, contas, avisos e relógio.
     * Decisões: Injeção por construtor.
     */
    public FilaDeVerificacao(PedidoDeVerificacaoRepository pedidos, DocumentoDeVerificacaoRepository documentos,
                             ListasService listas, ArmazenamentoArquivos arquivos, LeituraDeParametros parametros,
                             ConsultaDeContas contas, AvisosDaVerificacao avisos, Clock relogio) {
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.listas = listas;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.contas = contas;
        this.avisos = avisos;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fila
     * Objetivo: Listar os pedidos em análise por ordem de chegada, com o sujeito, o prazo e o responsável.
     * Decisões: O prazo é verificacao.sla_dias_uteis a partir do envio; "atrasado" quando já passou.
     */
    @Transactional(readOnly = true)
    public List<ItemDaFila> fila() {
        Instant agora = relogio.instant();
        long sla = parametros.inteiro(SLA);
        return pedidos.findBySituacaoOrderByEnviadoEmAsc(SituacaoDoPedido.EM_ANALISE).stream().map(p -> {
            Instant prazo = DiasUteis.somar(p.getEnviadoEm(), sla);
            return new ItemDaFila(p, avisos.nomeDoSujeito(p), prazo, agora.isAfter(prazo),
                    Optional.ofNullable(p.getResponsavelId()).flatMap(contas::nome).orElse(null));
        }).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Detalhe do pedido
     * Objetivo: Devolver o pedido, o sujeito, os documentos, o histórico do sujeito e os motivos possíveis.
     * Decisões: Os motivos vêm agrupados pela decisão a que pertencem.
     */
    @Transactional(readOnly = true)
    public Detalhe detalhe(UUID pedidoId) {
        PedidoDeVerificacao pedido = pedido(pedidoId);
        List<PedidoDeVerificacao> historico = pedidos.findBySujeitoTipoAndSujeitoIdOrderByCriadoEmDesc(pedido.getSujeitoTipo(),
                pedido.getSujeitoId()).stream().filter(p -> !p.getId().equals(pedidoId)).toList();
        return new Detalhe(pedido, avisos.nomeDoSujeito(pedido), documentos.findByPedidoIdOrderByEnviadoEm(pedidoId), historico,
                listas.ativos(Lista.MOTIVO_DECISAO_VERIFICACAO),
                Optional.ofNullable(pedido.getResponsavelId()).flatMap(contas::nome).orElse(null));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Assumir
     * Objetivo: Tornar o analista responsável pelo pedido em análise.
     * Decisões: Já assumido por outra pessoa: só o administrador redistribui.
     */
    @Transactional
    public PedidoDeVerificacao assumir(UUID pedidoId, UUID analista, boolean administrador) {
        PedidoDeVerificacao pedido = pedido(pedidoId);
        if (pedido.getSituacao() != SituacaoDoPedido.EM_ANALISE) {
            throw new RegraDeNegocioException("pedido_fora_da_fila", "Este pedido não está mais na fila de análise.");
        }
        if (pedido.getResponsavelId() != null && !pedido.getResponsavelId().equals(analista) && !administrador) {
            throw new RegraDeNegocioException("pedido_ja_assumido", "Outra pessoa da equipe já assumiu este pedido.");
        }
        pedido.assumir(analista, relogio.instant());
        ContextoDeAuditoria.descrever("VERIFICACAO_ASSUMIDA", "pedido_verificacao", pedidoId, null);
        return pedido;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Decidir
     * Objetivo: Registrar a decisão do analista com o motivo da lista e avisar quem responde pelo pedido.
     * Decisões: Verificado ganha a validade de verificacao.validade_meses; complemento ganha o prazo de
     *           verificacao.prazo_complemento_dias e exige a observação do que falta; motivo do grupo FRAUDE recusa e
     *           encaminha à administração.
     */
    @Transactional
    public PedidoDeVerificacao decidir(UUID pedidoId, UUID analista, boolean administrador, SituacaoDoPedido resultado,
                                       String motivo, String observacao) {
        PedidoDeVerificacao pedido = pedido(pedidoId);
        if (pedido.getSituacao() != SituacaoDoPedido.EM_ANALISE) {
            throw new RegraDeNegocioException("pedido_fora_da_fila", "Este pedido não está mais na fila de análise.");
        }
        if (!administrador && !analista.equals(pedido.getResponsavelId())) {
            throw new AccessDeniedException("Assuma o pedido antes de decidir.");
        }
        List<String> grupos = GRUPOS.get(resultado);
        if (grupos == null) {
            throw new RegraDeNegocioException("decisao_invalida", "Decida por verificado, pendente de complemento ou recusado.");
        }
        ItemDeLista item = listas.exigirAtivo(Lista.MOTIVO_DECISAO_VERIFICACAO, motivo);
        if (!grupos.contains(item.getGrupo())) {
            throw new RegraDeNegocioException("motivo_incompativel", "Este motivo não combina com a decisão escolhida.");
        }
        String nota = observacao == null || observacao.isBlank() ? null : observacao.strip();
        if (nota != null && nota.length() > 1000) {
            throw new RegraDeNegocioException("observacao_longa", "A observação pode ter até 1.000 caracteres.");
        }
        Instant agora = relogio.instant();
        switch (resultado) {
            case VERIFICADO -> {
                Instant validade = agora.atZone(BRASILIA).plusMonths(parametros.inteiro(VALIDADE)).toInstant();
                pedido.decidir(SituacaoDoPedido.VERIFICADO, analista, item.getCodigo(), nota, validade, agora);
                avisos.avisarSujeito(pedido, "Verificação aprovada",
                        "A verificação foi aprovada e o selo de verificado já aparece no perfil.");
            }
            case PENDENTE_COMPLEMENTO -> {
                if (nota == null) {
                    throw new RegraDeNegocioException("complemento_sem_observacao", "Diga ao usuário o que falta enviar.");
                }
                Instant prazo = agora.plus(Duration.ofDays(parametros.inteiro(PRAZO_COMPLEMENTO)));
                pedido.pedirComplemento(analista, item.getCodigo(), nota, prazo, agora);
                avisos.avisarSujeito(pedido, "A verificação precisa de complemento",
                        item.getRotulo() + ". " + nota + " Envie até " + DATA.format(prazo.atZone(BRASILIA)) + ".");
            }
            default -> {
                pedido.decidir(SituacaoDoPedido.RECUSADO, analista, item.getCodigo(), nota, null, agora);
                avisos.avisarSujeito(pedido, "Verificação recusada", item.getRotulo() + (nota == null ? "." : ". " + nota));
                if ("FRAUDE".equals(item.getGrupo())) {
                    pedido.encaminharModeracao();
                    avisos.avisarAdministracao(pedido, item.getRotulo());
                }
            }
        }
        ContextoDeAuditoria.descrever("VERIFICACAO_DECIDIDA", "pedido_verificacao", pedidoId,
                resultado + " (" + item.getCodigo() + ")");
        return pedido;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Ler documento
     * Objetivo: Devolver o arquivo de um documento do pedido para o analista conferir.
     * Decisões: Fica na trilha de auditoria; documento de outro pedido: 404; já apagado pela guarda: 404.
     */
    @Transactional(readOnly = true)
    public VerificacoesService.Arquivo lerDocumento(UUID pedidoId, UUID documentoId) {
        DocumentoDeVerificacao documento = documentos.findById(documentoId).filter(d -> d.getPedidoId().equals(pedidoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado."));
        ContextoDeAuditoria.descrever("DOCUMENTO_VERIFICACAO_LIDO", "pedido_verificacao", pedidoId, documento.getTipo());
        return new VerificacoesService.Arquivo(arquivos.ler(documento.getChaveArquivo()), documento.getTipoConteudo(),
                documento.getNomeOriginal());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedido
     * Objetivo: Ler um pedido pelo identificador.
     * Decisões: 404 se não existir.
     */
    private PedidoDeVerificacao pedido(UUID id) {
        return pedidos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Item da fila
     * Objetivo: Levar o pedido, o nome do sujeito, o prazo, se está atrasado e o responsável.
     * Decisões: Responsável nulo quando ninguém assumiu.
     */
    public record ItemDaFila(PedidoDeVerificacao pedido, String sujeito, Instant prazo, boolean atrasado, String responsavel) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Detalhe
     * Objetivo: Levar o pedido, o sujeito, os documentos, o histórico, os motivos e o responsável.
     * Decisões: Motivos com o grupo, para a tela filtrar pela decisão.
     */
    public record Detalhe(PedidoDeVerificacao pedido, String sujeito, List<DocumentoDeVerificacao> documentos,
                          List<PedidoDeVerificacao> historico, List<ItemDeLista> motivos, String responsavel) {
    }
}
