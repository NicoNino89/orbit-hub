package br.com.orbithub.identidade.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:20
 * Cartão Jira: KAN-75
 * Título: Papéis de uma conta
 * Objetivo: Enumerar os papéis com que um usuário atua na plataforma.
 * Decisões: Investidor e empreendedor podem coexistir na mesma conta (KAN-12). Os demais são papéis da
 *           equipe interna (seção 1.2 de processos-e-operacao.md), atribuídos só pelo administrador, numa conta
 *           interna separada (KAN-75). Os papéis internos da F2 (gestor de publicidade e curador de conteúdo
 *           educacional) entram com os módulos deles.
 */
public enum Papel {
    INVESTIDOR,
    EMPREENDEDOR,
    ADMIN,
    ANALISTA_VERIFICACAO,
    CURADOR,
    MODERADOR,
    SUPORTE_N1,
    SUPORTE_N2,
    ENCARREGADO_DADOS,
    JURIDICO;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Nome da autoridade no Spring Security
     * Objetivo: Converter o papel na autoridade usada nas regras de acesso.
     * Decisões: Prefixo ROLE_, o padrão de hasRole() do Spring Security.
     */
    public String autoridade() {
        return "ROLE_" + name();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Papel de uso público
     * Objetivo: Dizer se o papel pode ser escolhido pelo próprio usuário.
     * Decisões: Só investidor e empreendedor; papéis internos são atribuídos pela equipe.
     */
    public boolean publico() {
        return this == INVESTIDOR || this == EMPREENDEDOR;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:20
     * Cartão Jira: KAN-75
     * Título: Papel da equipe interna
     * Objetivo: Dizer se o papel é da equipe interna (backoffice).
     * Decisões: Todo papel que não é público é interno.
     */
    public boolean interno() {
        return !publico();
    }
}
