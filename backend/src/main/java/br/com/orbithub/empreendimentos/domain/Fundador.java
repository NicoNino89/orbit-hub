package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Fundador e participação
 * Objetivo: Representar um fundador da startup com o cargo e a participação societária.
 * Decisões: A participação compõe o cap table, sempre confidencial (Nível 2). Ligar o fundador a uma conta da equipe é do
 *           KAN-65; aqui é o registro cadastral.
 */
@Embeddable
public record Fundador(@Column(nullable = false, length = 120) String nome, @Column(length = 80) String cargo,
                       @Column(precision = 5, scale = 2) BigDecimal participacao) {
}
