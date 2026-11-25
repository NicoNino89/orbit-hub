package br.com.orbithub.empreendimentos.domain;

import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-67, KAN-30
 * Título: Evento de investidor bloqueado
 * Objetivo: Avisar os outros módulos de que a startup bloqueou uma pessoa ou organização, para encerrar o Nível 2 na hora.
 * Decisões: Evento do Spring, ouvido na mesma transação.
 */
public record InvestidorBloqueado(UUID startupId, String alvoTipo, UUID alvoId) {
}
