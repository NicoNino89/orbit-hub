package br.com.orbithub.identidade.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Situação da conta
 * Objetivo: Enumerar os estados de uma conta de usuário.
 * Decisões: Só contas ATIVAS entram e renovam sessão. PENDENTE_CONFIRMACAO aguarda o e-mail (P1);
 *           DESATIVADA cobre exclusão e inatividade (KAN-17, R11), sem apagar o histórico.
 */
public enum StatusUsuario {
    PENDENTE_CONFIRMACAO,
    ATIVO,
    DESATIVADO
}
