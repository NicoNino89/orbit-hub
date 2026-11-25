package br.com.orbithub.verificacao.repository;

import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Repositório dos documentos de verificação
 * Objetivo: Listar e buscar os documentos de um pedido.
 * Decisões: Um por tipo em cada pedido.
 */
public interface DocumentoDeVerificacaoRepository extends JpaRepository<DocumentoDeVerificacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documentos do pedido
     * Objetivo: Listar os documentos de um pedido.
     * Decisões: Pela data de envio.
     */
    List<DocumentoDeVerificacao> findByPedidoIdOrderByEnviadoEm(UUID pedidoId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Documento de um tipo
     * Objetivo: Buscar o documento de um tipo no pedido, para substituir.
     * Decisões: No máximo um.
     */
    Optional<DocumentoDeVerificacao> findByPedidoIdAndTipo(UUID pedidoId, String tipo);
}
