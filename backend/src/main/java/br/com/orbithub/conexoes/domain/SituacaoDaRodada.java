package br.com.orbithub.conexoes.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Situação da rodada
 * Objetivo: Enumerar o ciclo da rodada de investimento.
 * Decisões: Rascunho → aberta → em negociação → encerrada; o resultado (concluída ou cancelada) fica à parte.
 */
public enum SituacaoDaRodada {
    RASCUNHO,
    ABERTA,
    EM_NEGOCIACAO,
    ENCERRADA
}
