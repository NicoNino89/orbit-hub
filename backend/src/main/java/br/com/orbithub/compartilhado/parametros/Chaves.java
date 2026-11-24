package br.com.orbithub.compartilhado.parametros;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Chaves dos parâmetros usados pelo código
 * Objetivo: Reunir num lugar as chaves que o código lê, para ver de relance quais regras já são configuráveis e
 *           evitar chave digitada errada.
 * Decisões: Só as chaves já usadas; cada história nova acrescenta as suas. O catálogo completo (valor, limites,
 *           sensível) está na migração V11__parametros.sql.
 */
public final class Chaves {

    public static final String CADASTRO_LINK_CONFIRMACAO = "cadastro.link_confirmacao_horas";
    public static final String CONTA_LINK_REDEFINICAO = "conta.link_redefinicao_minutos";
    public static final String CONTA_LINK_TROCA_EMAIL = "conta.link_troca_email_horas";
    public static final String LOGIN_MAX_TENTATIVAS = "login.max_tentativas";
    public static final String LOGIN_BLOQUEIO = "login.bloqueio_minutos";
    public static final String SESSAO_INATIVIDADE = "sessao.inatividade_minutos";
    public static final String SESSAO_DURACAO_MAXIMA = "sessao.duracao_maxima_horas";
    public static final String CONVITE_VALIDADE = "convite.validade_dias";
    public static final String ACESSO_REVISAO = "acesso.revisao_meses";
    public static final String APROVACAO_PRAZO = "aprovacao.prazo_horas";

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Classe de constantes
     * Objetivo: Impedir instâncias.
     * Decisões: Só constantes.
     */
    private Chaves() {
    }
}
