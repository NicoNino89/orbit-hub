package br.com.orbithub.conexoes.repository;

import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.domain.SituacaoDaRodada;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Repositório das rodadas
 * Objetivo: Listar as rodadas da startup e encontrar a que está em andamento.
 * Decisões: O banco garante uma só rodada não encerrada por startup.
 */
public interface RodadaRepository extends JpaRepository<Rodada, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodadas da startup
     * Objetivo: Listar as rodadas, da mais recente para a mais antiga.
     * Decisões: Pela criação.
     */
    List<Rodada> findByStartupIdOrderByCriadaEmDesc(UUID startupId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodada em andamento
     * Objetivo: Encontrar a rodada não encerrada da startup.
     * Decisões: No máximo uma.
     */
    Optional<Rodada> findFirstByStartupIdAndSituacaoNot(UUID startupId, SituacaoDaRodada situacao);
}
