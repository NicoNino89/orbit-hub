package br.com.orbithub.compartilhado.armazenamento;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Arquivo não encontrado
 * Objetivo: Sinalizar leitura de uma chave inexistente.
 * Decisões: Subclasse de {@link ArmazenamentoException}, para a API poder mapear só este caso para 404.
 */
public class ArquivoNaoEncontradoException extends ArmazenamentoException {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Erro de arquivo não encontrado
     * Objetivo: Criar o erro com a chave procurada na mensagem.
     * Decisões: A mensagem traz só a chave lógica, nunca o caminho físico ou a URL da conta.
     */
    public ArquivoNaoEncontradoException(String chave) {
        super("Arquivo não encontrado: " + chave);
    }
}
