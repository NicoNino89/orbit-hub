package br.com.orbithub.empreendimentos.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-63
 * Título: Regra de confidencialidade da startup
 * Objetivo: Enumerar o que a startup exige antes de liberar o Nível 2: o termo da plataforma (aceito no cadastro) ou o
 *           NDA bilateral (documento da startup confirmado pelas duas partes).
 * Decisões: Em qualquer caso, a liberação explícita do fundador administrador continua obrigatória (KAN-30).
 */
public enum RegraDeConfidencialidade {
    TERMO_PLATAFORMA,
    NDA_BILATERAL
}
