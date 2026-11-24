package br.com.orbithub.compartilhado.escalas;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Maturidade do negócio
 * Objetivo: Enumerar a escala de 7 níveis que descreve em que ponto está o negócio da startup.
 * Decisões: Escala fixa (não é lista gerenciável), porque a startup (KAN-26) e a tese do investidor (KAN-18) precisam
 *           comparar níveis pela ordem. É independente do estágio da rodada (pré-seed, seed...), que descreve a captação.
 */
public enum MaturidadeDoNegocio {
    IDEACAO("Ideação"),
    VALIDACAO("Validação"),
    MVP("MVP"),
    OPERACAO_INICIAL("Operação inicial"),
    TRACAO("Tração"),
    CRESCIMENTO("Crescimento"),
    EXPANSAO("Expansão");

    private final String rotulo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Criação do nível
     * Objetivo: Guardar o nome do nível em português.
     * Decisões: A ordem da declaração é a ordem da escala.
     */
    MaturidadeDoNegocio(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Rótulo
     * Objetivo: Devolver o nome do nível para a tela.
     * Decisões: Somente leitura.
     */
    public String getRotulo() {
        return rotulo;
    }
}
