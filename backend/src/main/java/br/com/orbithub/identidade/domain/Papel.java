package br.com.orbithub.identidade.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Papéis de uma conta
 * Objetivo: Enumerar os papéis com que um usuário atua na plataforma.
 * Decisões: Investidor e empreendedor podem coexistir na mesma conta (KAN-12). ADMIN é o papel interno
 *           da equipe, sem cadastro público; os demais papéis internos chegam com o backoffice (KAN-54).
 */
public enum Papel {
    INVESTIDOR,
    EMPREENDEDOR,
    ADMIN;

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
}
