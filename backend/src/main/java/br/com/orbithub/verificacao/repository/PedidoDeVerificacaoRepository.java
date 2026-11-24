package br.com.orbithub.verificacao.repository;

import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
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
}
