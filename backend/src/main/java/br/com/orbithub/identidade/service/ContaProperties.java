package br.com.orbithub.identidade.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Configuração da conta
 * Objetivo: Reunir o endereço da web (para os links) e se o cadastro exige confirmar o e-mail.
 * Decisões: confirmarEmail pode ser desligado só enquanto a nuvem não tiver provedor de e-mail, para a
 *           homologação permitir cadastros. Os prazos dos links viraram parâmetros editáveis (KAN-78).
 */
@ConfigurationProperties("orbit.conta")
public record ContaProperties(String webUrl, boolean confirmarEmail) {
}
