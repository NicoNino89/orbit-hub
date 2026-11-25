package br.com.orbithub.investidores.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Papel na organização
 * Objetivo: Enumerar o que cada pessoa pode fazer dentro de uma organização investidora.
 * Decisões: Administrador gerencia membros e dados; membro atua em nome da organização (interesse, Match, Nível 2);
 *           observador só vê. O administrador também atua.
 */
public enum PapelNaOrganizacao {
    ADMINISTRADOR,
    MEMBRO,
    OBSERVADOR;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pode atuar
     * Objetivo: Dizer se o papel permite agir em nome da organização.
     * Decisões: Administrador e membro.
     */
    public boolean podeAtuar() {
        return this != OBSERVADOR;
    }
}
