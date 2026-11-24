package br.com.orbithub.compartilhado.erro;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Recurso não encontrado
 * Objetivo: Sinalizar que o item pedido não existe (ou não pode ser visto por quem pediu).
 * Decisões: Vira 404 no tratador global; a mensagem é segura para mostrar ao usuário.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação da exceção
     * Objetivo: Guardar a mensagem exibida ao usuário.
     * Decisões: Sem stack trace: fluxo esperado.
     */
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem, null, false, false);
    }
}
