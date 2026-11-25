package br.com.orbithub.verificacao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Situação do pedido de verificação
 * Objetivo: Enumerar o ciclo do pedido: rascunho, em análise, pendente de complemento e as decisões finais.
 * Decisões: Os três primeiros são "em aberto" (um por sujeito). As decisões e o encerramento por falta de complemento
 *           são do KAN-83; o vencimento, da rotina R9.
 */
public enum SituacaoDoPedido {
    RASCUNHO,
    EM_ANALISE,
    PENDENTE_COMPLEMENTO,
    VERIFICADO,
    RECUSADO,
    ENCERRADO,
    VENCIDO;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Em aberto
     * Objetivo: Dizer se o pedido ainda não teve decisão final.
     * Decisões: Rascunho, em análise e pendente de complemento.
     */
    public boolean emAberto() {
        return this == RASCUNHO || this == EM_ANALISE || this == PENDENTE_COMPLEMENTO;
    }
}
