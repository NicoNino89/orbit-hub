package br.com.orbithub.conexoes.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Situação da rodada
 * Objetivo: Enumerar o ciclo da rodada de investimento.
 * Decisões: Rascunho → aberta → em negociação → encerrada; o resultado (concluída ou cancelada) fica à parte. Aguardando
 *           resultado (KAN-106, 25/11/2026): prazo vencido, sem novas entradas, esperando o registro (R6).
 */
public enum SituacaoDaRodada {
    RASCUNHO,
    ABERTA,
    EM_NEGOCIACAO,
    AGUARDANDO_RESULTADO,
    ENCERRADA
}
