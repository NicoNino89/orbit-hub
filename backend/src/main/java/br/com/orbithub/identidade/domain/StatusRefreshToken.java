package br.com.orbithub.identidade.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Situação de um refresh token
 * Objetivo: Enumerar os estados de um refresh token.
 * Decisões: USADO marca o token já trocado na rotação: reapresentá-lo é sinal de roubo e revoga as
 *           sessões do usuário. REVOGADO é o fim explícito (logout, troca de senha, reuso).
 */
public enum StatusRefreshToken {
    ATIVO,
    USADO,
    REVOGADO
}
