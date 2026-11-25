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
    IDEACAO("Ideação", "Há a ideia e o problema definido, mas ainda sem produto nem clientes."),
    VALIDACAO("Validação", "Testando se o problema existe e se as pessoas pagariam pela solução (entrevistas, protótipos)."),
    MVP("MVP", "Uma primeira versão do produto está em uso, com os primeiros usuários ou clientes de teste."),
    OPERACAO_INICIAL("Operação inicial", "Vendendo para os primeiros clientes pagantes, ainda ajustando o modelo de negócio."),
    TRACAO("Tração", "Receita recorrente e crescimento consistente, com o modelo de negócio validado."),
    CRESCIMENTO("Crescimento", "Escalando vendas, equipe e operação, em ritmo acelerado."),
    EXPANSAO("Expansão", "Consolidada no mercado principal e abrindo novos mercados, regiões ou produtos.");

    private final String rotulo;
    private final String explicacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Criação do nível
     * Objetivo: Guardar o nome do nível em português e a explicação que orienta a escolha (KAN-26).
     * Decisões: A ordem da declaração é a ordem da escala.
     */
    MaturidadeDoNegocio(String rotulo, String explicacao) {
        this.rotulo = rotulo;
        this.explicacao = explicacao;
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Explicação
     * Objetivo: Devolver a breve explicação do nível, mostrada ao lado de cada opção.
     * Decisões: Somente leitura.
     */
    public String getExplicacao() {
        return explicacao;
    }
}
