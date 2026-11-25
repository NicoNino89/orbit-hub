package br.com.orbithub.identidade.domain;

import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-101, KAN-102
 * Título: Conta suspensa ou banida
 * Objetivo: Avisar os módulos de que a pessoa perdeu o acesso, para que tirem os acessos confidenciais, expirem os
 *           interesses pendentes, tirem a pessoa das rodadas abertas e cancelem as reuniões futuras.
 * Decisões: Publicado na mesma transação da sanção. apenasNivel2 = só a medida preventiva de violação de
 *           confidencialidade (KAN-102), que tira os acessos ao Nível 2 sem suspender a conta.
 */
public record ContaSuspensa(UUID usuarioId, boolean apenasNivel2) {
}
