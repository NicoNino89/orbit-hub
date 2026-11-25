package br.com.orbithub.catalogo.repository;

import br.com.orbithub.catalogo.domain.MudancaNaEdicao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Repositório do histórico das edições
 * Objetivo: Listar as mudanças de uma edição.
 * Decisões: Mais recentes primeiro.
 */
public interface MudancaNaEdicaoRepository extends JpaRepository<MudancaNaEdicao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Histórico da edição
     * Objetivo: Listar as mudanças, da mais recente para a mais antiga.
     * Decisões: Por data.
     */
    List<MudancaNaEdicao> findByEdicaoIdOrderByEmDesc(UUID edicaoId);
}
