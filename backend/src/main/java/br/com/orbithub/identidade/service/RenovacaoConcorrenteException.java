package br.com.orbithub.identidade.service;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Renovação concorrente
 * Objetivo: Sinalizar que o refresh token acabou de ser trocado por outra requisição da mesma sessão.
 * Decisões: Acontece com duas abas renovando juntas; a sessão continua, e o front-end tenta de novo com o
 *           cookie novo, que o navegador já recebeu da outra resposta.
 */
public class RenovacaoConcorrenteException extends RuntimeException {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação da exceção
     * Objetivo: Indicar a corrida entre duas renovações.
     * Decisões: Sem stack trace: fluxo esperado.
     */
    public RenovacaoConcorrenteException() {
        super("Refresh token trocado por outra requisição", null, false, false);
    }
}
