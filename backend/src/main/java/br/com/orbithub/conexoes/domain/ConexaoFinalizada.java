package br.com.orbithub.conexoes.domain;

import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-49, KAN-30
 * Título: Evento de conexão finalizada
 * Objetivo: Avisar os outros módulos de que a conexão chegou a um estado final, para encerrar o acesso ao Nível 2.
 * Decisões: Evento do Spring, ouvido na mesma transação.
 */
public record ConexaoFinalizada(UUID conexaoId) {
}
