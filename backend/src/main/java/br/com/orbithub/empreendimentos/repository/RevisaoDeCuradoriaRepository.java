package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.RevisaoDeCuradoria;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-84
 * Título: Repositório das revisões da curadoria
 * Objetivo: Listar a fila por ordem de chegada, a revisão aberta e o histórico de uma startup.
 * Decisões: Consultas derivadas.
 */
public interface RevisaoDeCuradoriaRepository extends JpaRepository<RevisaoDeCuradoria, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Fila
     * Objetivo: Listar as revisões nas situações pedidas, da mais antiga para a mais nova.
     * Decisões: Ordem de chegada.
     */
    List<RevisaoDeCuradoria> findBySituacaoInOrderByEnviadaEmAsc(Collection<RevisaoDeCuradoria.Situacao> situacoes);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Revisão aberta da startup
     * Objetivo: Achar a revisão da startup que ainda espera decisão.
     * Decisões: No máximo uma (índice único parcial).
     */
    Optional<RevisaoDeCuradoria> findFirstByStartupIdAndSituacaoIn(UUID startupId, Collection<RevisaoDeCuradoria.Situacao> situacoes);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Histórico da startup
     * Objetivo: Listar as revisões da startup, da mais recente para a mais antiga.
     * Decisões: Inclui as canceladas.
     */
    List<RevisaoDeCuradoria> findByStartupIdOrderByEnviadaEmDesc(UUID startupId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Pela proposta
     * Objetivo: Achar a revisão ligada a uma proposta de segunda aprovação.
     * Decisões: Usado pelo executor da recusa por fraude.
     */
    Optional<RevisaoDeCuradoria> findByPropostaId(UUID propostaId);
}
