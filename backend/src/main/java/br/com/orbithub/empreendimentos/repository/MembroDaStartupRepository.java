package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.MembroDaStartup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Repositório da equipe das startups
 * Objetivo: Consultar os vínculos ativos entre pessoas e startups.
 * Decisões: Ativo = sem data de saída; conferido a cada requisição.
 */
public interface MembroDaStartupRepository extends JpaRepository<MembroDaStartup, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Vínculo ativo
     * Objetivo: Buscar o vínculo ativo de uma pessoa com uma startup.
     * Decisões: No máximo um (índice único parcial).
     */
    Optional<MembroDaStartup> findByStartupIdAndUsuarioIdAndSaiuEmIsNull(UUID startupId, UUID usuarioId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Startups da pessoa
     * Objetivo: Listar os vínculos ativos de uma pessoa.
     * Decisões: Mais antigos primeiro.
     */
    List<MembroDaStartup> findByUsuarioIdAndSaiuEmIsNullOrderByEntrouEm(UUID usuarioId);
}
