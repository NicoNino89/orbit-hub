package br.com.orbithub.identidade.service;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Sessão inválida
 * Objetivo: Sinalizar que o refresh token apresentado não abre nem renova sessão.
 * Decisões: O motivo fica só no log interno; para quem chamou, a resposta é sempre a mesma (401), sem
 *           revelar se o token existiu, expirou ou foi revogado.
 */
public class SessaoInvalidaException extends RuntimeException {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação da exceção
     * Objetivo: Registrar o motivo interno da recusa.
     * Decisões: Sem stack trace: é um fluxo esperado, não um erro de programação.
     */
    public SessaoInvalidaException(String motivo) {
        super(motivo, null, false, false);
    }
}
