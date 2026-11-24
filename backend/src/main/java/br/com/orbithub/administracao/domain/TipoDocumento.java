package br.com.orbithub.administracao.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Tipos de documento legal e de aviso
 * Objetivo: Enumerar os textos versionados da plataforma e dizer quem precisa aceitá-los e quando.
 * Decisões: Termos de Uso e Política de Privacidade são aceitos por todos, no cadastro e a cada versão
 *           nova (no próximo acesso). O Termo de Confidencialidade é aceito pelo investidor e cobrado
 *           antes do próximo acesso ao Nível 2 (P14). Política de Moderação, Política de Publicidade
 *           (F2) e avisos são informativos: publicados e consultáveis, sem aceite.
 */
public enum TipoDocumento {
    TERMOS_DE_USO(Aceite.TODOS_NO_ACESSO),
    POLITICA_DE_PRIVACIDADE(Aceite.TODOS_NO_ACESSO),
    TERMO_DE_CONFIDENCIALIDADE(Aceite.INVESTIDOR_ANTES_DO_NIVEL_2),
    POLITICA_DE_MODERACAO(Aceite.NENHUM),
    POLITICA_DE_PUBLICIDADE(Aceite.NENHUM),
    AVISO_NAO_COMPROMISSO(Aceite.NENHUM),
    AVISO_DADOS_DECLARADOS(Aceite.NENHUM);

    private final Aceite aceite;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação do tipo
     * Objetivo: Associar a regra de aceite ao tipo.
     * Decisões: Regra fixa por tipo; mudar quem aceita é decisão jurídica e entra por código revisado.
     */
    TipoDocumento(Aceite aceite) {
        this.aceite = aceite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Regra de aceite
     * Objetivo: Informar quem aceita o documento e em que momento.
     * Decisões: Usado pelo serviço de aceites e exposto na API.
     */
    public Aceite aceite() {
        return aceite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Documento com aceite
     * Objetivo: Dizer se o documento é aceito por alguém.
     * Decisões: Avisos e políticas informativas não têm aceite.
     */
    public boolean exigeAceite() {
        return aceite != Aceite.NENHUM;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Regras de aceite
     * Objetivo: Enumerar quem aceita e quando.
     * Decisões: TODOS_NO_ACESSO bloqueia o uso até o aceite; INVESTIDOR_ANTES_DO_NIVEL_2 só bloqueia o
     *           acesso a informação confidencial.
     */
    public enum Aceite {
        TODOS_NO_ACESSO,
        INVESTIDOR_ANTES_DO_NIVEL_2,
        NENHUM
    }
}
