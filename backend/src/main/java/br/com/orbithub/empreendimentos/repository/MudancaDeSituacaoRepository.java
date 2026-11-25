package br.com.orbithub.empreendimentos.repository;

import br.com.orbithub.empreendimentos.domain.MudancaDeSituacao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Repositório do histórico de situações
 * Objetivo: Listar as mudanças de situação de uma startup.
 * Decisões: Mais recentes primeiro.
 */
public interface MudancaDeSituacaoRepository extends JpaRepository<MudancaDeSituacao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Histórico da startup
     * Objetivo: Listar as mudanças de situação, da mais recente para a mais antiga.
     * Decisões: Por data.
     */
    List<MudancaDeSituacao> findByStartupIdOrderByEmDesc(UUID startupId);
}
