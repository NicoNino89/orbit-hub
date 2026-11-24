package br.com.orbithub.compartilhado.erro;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Regra de negócio violada
 * Objetivo: Sinalizar que o pedido é válido no formato, mas fere uma regra da plataforma.
 * Decisões: Vira 422 no tratador global, com a mensagem para o usuário e um código estável para o
 *           front-end decidir o que mostrar.
 */
public class RegraDeNegocioException extends RuntimeException {

    private final String codigo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Criação da exceção
     * Objetivo: Guardar código e mensagem.
     * Decisões: Sem stack trace: fluxo esperado.
     */
    public RegraDeNegocioException(String codigo, String mensagem) {
        super(mensagem, null, false, false);
        this.codigo = codigo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Código da regra
     * Objetivo: Expor o código estável da regra violada.
     * Decisões: Em minúsculas com sublinhado, por exemplo "email_em_uso".
     */
    public String getCodigo() {
        return codigo;
    }
}
