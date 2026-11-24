package br.com.orbithub.identidade.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Parâmetros da conta
 * Objetivo: Reunir o endereço da web (para os links) e os prazos dos links enviados por e-mail.
 * Decisões: confirmarEmail pode ser desligado só enquanto a nuvem não tiver provedor de e-mail, para a
 *           homologação permitir cadastros; os prazos seguem docs/requisitos (cadastro.link_confirmacao_horas)
 *           e vão para o painel de parâmetros no KAN-75.
 */
@ConfigurationProperties("orbit.conta")
public record ContaProperties(String webUrl, boolean confirmarEmail, Duration linkConfirmacao,
                              Duration linkRedefinicao, Duration linkTrocaEmail) {
}
