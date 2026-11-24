package br.com.orbithub.verificacao.repository;

import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Repositório dos pedidos de verificação
 * Objetivo: Buscar o pedido em aberto e o histórico de um sujeito.
 * Decisões: No máximo um em aberto por sujeito (índice único parcial).
 */
public interface PedidoDeVerificacaoRepository extends JpaRepository<PedidoDeVerificacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Pedido em aberto
     * Objetivo: Buscar o pedido do sujeito que está numa das situações informadas.
     * Decisões: Chamado com as situações "em aberto".
     */
    Optional<PedidoDeVerificacao> findFirstBySujeitoTipoAndSujeitoIdAndSituacaoIn(SujeitoDaVerificacao tipo, UUID sujeito,
                                                                                   Collection<SituacaoDoPedido> situacoes);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Histórico do sujeito
     * Objetivo: Listar os pedidos do sujeito, do mais novo ao mais antigo.
     * Decisões: Para o selo e para a tela.
     */
    List<PedidoDeVerificacao> findBySujeitoTipoAndSujeitoIdOrderByCriadoEmDesc(SujeitoDaVerificacao tipo, UUID sujeito);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fila
     * Objetivo: Listar os pedidos numa situação, por ordem de chegada.
     * Decisões: Para a fila do time de verificação.
     */
    List<PedidoDeVerificacao> findBySituacaoOrderByEnviadoEmAsc(SituacaoDoPedido situacao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedidos numa situação até um instante
     * Objetivo: Listar os pedidos de uma situação cujo prazo do complemento já passou.
     * Decisões: Para a rotina R9.
     */
    List<PedidoDeVerificacao> findBySituacaoAndPrazoComplementoAteBefore(SituacaoDoPedido situacao, Instant limite);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Verificações vencidas
     * Objetivo: Listar as verificações aprovadas com validade antes do instante.
     * Decisões: Para a rotina R9.
     */
    List<PedidoDeVerificacao> findBySituacaoAndValidoAteBefore(SituacaoDoPedido situacao, Instant limite);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Verificações a avisar
     * Objetivo: Listar as verificações aprovadas que vencem antes do limite e ainda não foram avisadas.
     * Decisões: Para a rotina R9.
     */
    List<PedidoDeVerificacao> findBySituacaoAndAvisoRevalidacaoEmIsNullAndValidoAteBefore(SituacaoDoPedido situacao, Instant limite);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Documentos a apagar
     * Objetivo: Listar os pedidos decididos antes do limite cujos documentos ainda estão guardados.
     * Decisões: Para a rotina R9.
     */
    List<PedidoDeVerificacao> findByDocumentosApagadosEmIsNullAndDecididoEmBeforeAndSituacaoIn(Instant limite,
                                                                                              Collection<SituacaoDoPedido> situacoes);
}
