package br.com.orbithub.compartilhado.seguranca;

import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Verificação de sessão ativa
 * Objetivo: Dizer se a sessão de um access token continua aberta.
 * Decisões: Interface em compartilhado e implementação no módulo de identidade, para a configuração de
 *           segurança não depender do módulo. É o que faz o logout e a troca de senha valerem na hora,
 *           sem esperar o access token expirar.
 */
public interface VerificadorDeSessao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão ativa
     * Objetivo: Responder se a sessão informada ainda vale.
     * Decisões: Chamado a cada requisição autenticada; a implementação deve ser uma consulta indexada.
     */
    boolean sessaoAtiva(UUID sessaoId);
}
