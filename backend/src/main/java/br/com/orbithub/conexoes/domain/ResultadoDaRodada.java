package br.com.orbithub.conexoes.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Resultado da rodada
 * Objetivo: Dizer como a rodada encerrada terminou.
 * Decisões: O registro detalhado do investimento (valores captados, investidores) fica com o KAN-64. Sem resultado
 *           (KAN-106): a R6 encerrou sem registro no prazo; não conta como investimento.
 */
public enum ResultadoDaRodada {
    CONCLUIDA,
    CANCELADA,
    SEM_RESULTADO
}
