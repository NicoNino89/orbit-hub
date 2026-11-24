package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Repositório de documentos legais
 * Objetivo: Ler as versões publicadas e descobrir a vigente de cada tipo.
 * Decisões: A vigente é a maior versão com vigência já iniciada; o índice único (tipo, versão) atende as
 *           buscas.
 */
public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Versão vigente de um tipo
     * Objetivo: Buscar a maior versão já em vigor no instante informado.
     * Decisões: Versões com vigência futura ficam de fora até a data chegar.
     */
    Optional<DocumentoLegal> findFirstByTipoAndVigenteDesdeLessThanEqualOrderByVersaoDesc(TipoDocumento tipo,
                                                                                         Instant agora);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Histórico de um tipo
     * Objetivo: Listar as versões de um tipo da mais nova para a mais antiga.
     * Decisões: Inclui versões futuras; quem filtra é o serviço.
     */
    List<DocumentoLegal> findByTipoOrderByVersaoDesc(TipoDocumento tipo);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Maior versão de um tipo
     * Objetivo: Saber o número da próxima versão a publicar.
     * Decisões: Nulo quando o tipo ainda não tem versão.
     */
    @Query("select max(d.versao) from DocumentoLegal d where d.tipo = :tipo")
    Integer maiorVersao(TipoDocumento tipo);
}
