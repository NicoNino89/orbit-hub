package br.com.orbithub.catalogo.repository;

import br.com.orbithub.catalogo.domain.Edicao;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Repositório das edições
 * Objetivo: Listar as edições por situação e por início.
 * Decisões: Consultas derivadas.
 */
public interface EdicaoRepository extends JpaRepository<Edicao, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Por situação
     * Objetivo: Listar as edições nas situações pedidas, pelo início mais recente.
     * Decisões: Usado pela rotina e pelo catálogo.
     */
    List<Edicao> findBySituacaoInOrderByInicioDesc(Collection<Edicao.Situacao> situacoes);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Todas
     * Objetivo: Listar todas as edições, pelo início mais recente.
     * Decisões: Para a área do curador.
     */
    List<Edicao> findAllByOrderByInicioDesc();
}
