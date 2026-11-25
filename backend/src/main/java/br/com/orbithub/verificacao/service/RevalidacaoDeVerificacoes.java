package br.com.orbithub.verificacao.service;

import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.repository.DocumentoDeVerificacaoRepository;
import br.com.orbithub.verificacao.repository.PedidoDeVerificacaoRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: R9 – Revalidação de verificação
 * Objetivo: Encerrar pedidos sem complemento no prazo, avisar antes do vencimento, vencer verificações e apagar os
 *           documentos depois da guarda.
 * Decisões: Diária às 6h. Idempotente: cada pedido muda de situação uma vez e o aviso é marcado. Os documentos saem
 *           do armazenamento verificacao.retencao_documentos_meses depois da decisão (o pedido e a decisão ficam). Falha
 *           ao apagar um arquivo conta como erro e o pedido tenta de novo no dia seguinte.
 */
@Component
class RevalidacaoDeVerificacoes extends RotinaBase {

    private static final Logger LOG = LoggerFactory.getLogger(RevalidacaoDeVerificacoes.class);
    static final String AVISO = "verificacao.aviso_revalidacao_dias";
    static final String RETENCAO = "verificacao.retencao_documentos_meses";
    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final PedidoDeVerificacaoRepository pedidos;
    private final DocumentoDeVerificacaoRepository documentos;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final AvisosDaVerificacao avisos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber repositórios, armazenamento, parâmetros e avisos.
     * Decisões: Injeção por construtor.
     */
    RevalidacaoDeVerificacoes(PedidoDeVerificacaoRepository pedidos, DocumentoDeVerificacaoRepository documentos,
                              ArmazenamentoArquivos arquivos, LeituraDeParametros parametros, AvisosDaVerificacao avisos) {
        super("R9", "Encerra complementos vencidos, avisa e vence verificações e apaga documentos guardados", "0 0 6 * * *",
                "verificacao", List.of(AVISO, RETENCAO, FilaDeVerificacao.PRAZO_COMPLEMENTO, FilaDeVerificacao.VALIDADE));
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.avisos = avisos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Executar
     * Objetivo: Fazer os quatro passos da revalidação e contar o que foi feito.
     * Decisões: Um item por pedido tratado em cada passo.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        int encerrados = 0;
        for (PedidoDeVerificacao p : pedidos.findBySituacaoAndPrazoComplementoAteBefore(SituacaoDoPedido.PENDENTE_COMPLEMENTO, agora)) {
            p.encerrarPorFaltaDeComplemento(agora);
            avisos.avisarSujeito(p, "Pedido de verificação encerrado",
                    "O complemento pedido não chegou no prazo e o pedido foi encerrado. Você pode começar um novo quando quiser.");
            encerrados++;
        }
        int avisados = 0;
        Instant limiteAviso = agora.plus(Duration.ofDays(parametros.inteiro(AVISO)));
        for (PedidoDeVerificacao p : pedidos.findBySituacaoAndAvisoRevalidacaoEmIsNullAndValidoAteBefore(SituacaoDoPedido.VERIFICADO,
                limiteAviso)) {
            if (p.getValidoAte().isAfter(agora)) {
                p.marcarAvisoRevalidacao(agora);
                avisos.avisarSujeito(p, "Sua verificação vai vencer",
                        "A verificação vence em " + FilaDeVerificacao.DATA.format(p.getValidoAte().atZone(BRASILIA))
                                + ". Envie documentos atualizados para manter o selo de verificado.");
                avisados++;
            }
        }
        int vencidos = 0;
        for (PedidoDeVerificacao p : pedidos.findBySituacaoAndValidoAteBefore(SituacaoDoPedido.VERIFICADO, agora)) {
            p.vencer();
            avisos.avisarSujeito(p, "Verificação vencida",
                    "A verificação venceu e o selo saiu do perfil. Envie documentos atualizados para verificar de novo.");
            vencidos++;
        }
        int limpos = 0;
        int erros = 0;
        Instant limiteGuarda = agora.atZone(BRASILIA).minusMonths(parametros.inteiro(RETENCAO)).toInstant();
        for (PedidoDeVerificacao p : pedidos.findByDocumentosApagadosEmIsNullAndDecididoEmBeforeAndSituacaoIn(limiteGuarda,
                EnumSet.of(SituacaoDoPedido.VERIFICADO, SituacaoDoPedido.RECUSADO, SituacaoDoPedido.ENCERRADO, SituacaoDoPedido.VENCIDO))) {
            try {
                for (DocumentoDeVerificacao d : documentos.findByPedidoIdOrderByEnviadoEm(p.getId())) {
                    arquivos.remover(d.getChaveArquivo());
                    documentos.delete(d);
                }
                p.marcarDocumentosApagados(agora);
                limpos++;
            } catch (RuntimeException falha) {
                LOG.error("Não foi possível apagar os documentos do pedido {}: {}", p.getId(), falha.getClass().getSimpleName());
                erros++;
            }
        }
        return new Resultado(encerrados + avisados + vencidos + limpos, erros, encerrados + " encerrado(s), " + avisados
                + " avisado(s), " + vencidos + " vencido(s), " + limpos + " com documentos apagados");
    }
}
