package br.com.orbithub.empreendimentos.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Papel na equipe da startup
 * Objetivo: Enumerar o que cada pessoa pode fazer na startup.
 * Decisões: Fundador administrador gerencia a equipe, publica, abre rodadas e libera o Nível 2; fundador edita e acompanha;
 *           colaborador edita só as seções autorizadas (KAN-65).
 */
public enum PapelNaStartup {
    FUNDADOR_ADMINISTRADOR,
    FUNDADOR,
    COLABORADOR
}
