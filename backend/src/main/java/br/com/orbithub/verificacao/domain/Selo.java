package br.com.orbithub.verificacao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Selo de verificação
 * Objetivo: Enumerar o que o perfil mostra: verificado, em análise ou não verificado.
 * Decisões: Verificado só enquanto a verificação estiver dentro da validade; uma revalidação em análise não tira o
 *           selo antes do vencimento.
 */
public enum Selo {
    VERIFICADO,
    EM_ANALISE,
    NAO_VERIFICADO
}
