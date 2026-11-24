package br.com.orbithub.verificacao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Quem é verificado
 * Objetivo: Enumerar os tipos de sujeito de uma verificação e o grupo de documentos exigidos de cada um.
 * Decisões: O grupo é o da lista DOCUMENTO_VERIFICACAO (KAN-79). Startup e fundador entram com o cadastro da startup
 *           (S4); o modelo já os prevê.
 */
public enum SujeitoDaVerificacao {
    PESSOA("INVESTIDOR_PESSOA_FISICA"),
    ORGANIZACAO("ORGANIZACAO_INVESTIDORA"),
    STARTUP("STARTUP"),
    FUNDADOR("FUNDADOR");

    private final String grupoDeDocumentos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Criação do tipo
     * Objetivo: Guardar o grupo de documentos.
     * Decisões: Mesmo texto do grupo na lista.
     */
    SujeitoDaVerificacao(String grupoDeDocumentos) {
        this.grupoDeDocumentos = grupoDeDocumentos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Grupo de documentos
     * Objetivo: Devolver o grupo da lista DOCUMENTO_VERIFICACAO deste tipo de sujeito.
     * Decisões: Somente leitura.
     */
    public String getGrupoDeDocumentos() {
        return grupoDeDocumentos;
    }
}
