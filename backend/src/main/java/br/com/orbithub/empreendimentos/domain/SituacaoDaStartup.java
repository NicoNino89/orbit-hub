package br.com.orbithub.empreendimentos.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Situação da startup
 * Objetivo: Enumerar o ciclo de vida da startup na plataforma.
 * Decisões: Rascunho → em revisão → publicada (ou devolvida para ajustes) → despublicada. As transições são do KAN-27 e
 *           da curadoria (KAN-66); aqui a startup nasce em rascunho.
 */
public enum SituacaoDaStartup {
    RASCUNHO,
    EM_REVISAO,
    PUBLICADA,
    DEVOLVIDA,
    DESPUBLICADA
}
