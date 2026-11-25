package br.com.orbithub.empreendimentos.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Seções do cadastro da startup
 * Objetivo: Enumerar as partes do cadastro, para autorizar o colaborador a editar só algumas.
 * Decisões: Na ordem das etapas do editor. Cada história do cadastro (KAN-22 a KAN-26) preenche a sua seção.
 */
public enum SecaoDaStartup {
    CADASTRO("Dados cadastrais"),
    PROBLEMA_E_SOLUCAO("Problema e solução"),
    PRODUTO("Produto ou serviço"),
    MERCADO("Mercado"),
    MODELO_DE_NEGOCIO("Modelo de negócio"),
    TRACAO("Tração e resultados"),
    EQUIPE_E_ESTRUTURA("Equipe e estrutura"),
    PLANEJAMENTO("Planejamento"),
    NECESSIDADE_DE_INVESTIMENTO("Necessidade de investimento"),
    MATURIDADE("Estágio do negócio");

    private final String rotulo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Criação da seção
     * Objetivo: Guardar o nome da seção para a tela.
     * Decisões: Em português.
     */
    SecaoDaStartup(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Rótulo
     * Objetivo: Devolver o nome da seção.
     * Decisões: Somente leitura.
     */
    public String getRotulo() {
        return rotulo;
    }
}
