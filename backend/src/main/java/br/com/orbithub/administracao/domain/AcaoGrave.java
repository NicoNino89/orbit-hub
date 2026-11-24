package br.com.orbithub.administracao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Ações que exigem segunda aprovação
 * Objetivo: Enumerar as ações graves que só acontecem depois que uma segunda pessoa aprova (seção 1.3 de
 *           processos-e-operacao.md).
 * Decisões: Cada ação tem um texto para a tela. Quem executa cada uma é o módulo dono, que registra um executor
 *           quando a funcionalidade existir: parâmetros sensíveis entram com a KAN-78; as demais, com moderação,
 *           titularidade e LGPD (S10 e S11).
 */
public enum AcaoGrave {
    BANIMENTO_DEFINITIVO("Banimento definitivo de usuário ou organização"),
    REMOCAO_STARTUP_FRAUDE("Remoção de startup publicada por fraude"),
    TRANSFERENCIA_TITULARIDADE("Transferência de titularidade de startup ou organização"),
    PARAMETRO_SENSIVEL("Alteração de parâmetro sensível"),
    DADOS_PESSOAIS_FORA_DO_FLUXO("Exportação ou exclusão de dados pessoais fora do fluxo normal");

    private final String descricao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Criação da ação
     * Objetivo: Guardar o texto da ação.
     * Decisões: Texto em português, para a tela.
     */
    AcaoGrave(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Texto da ação
     * Objetivo: Devolver a descrição da ação para a tela.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }
}
