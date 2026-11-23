package br.com.orbithub.compartilhado.armazenamento;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Erro de armazenamento de arquivos
 * Objetivo: Representar falhas de gravação, leitura ou remoção, qualquer que seja o adapter.
 * Decisões: Exceção não verificada, para os módulos não dependerem de IOException nem de exceções do
 *           SDK da Azure.
 */
public class ArmazenamentoException extends RuntimeException {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Erro de armazenamento sem causa técnica
     * Objetivo: Criar o erro só com a mensagem.
     * Decisões: Usado quando a falha é de regra (ex.: arquivo não encontrado), sem exceção de origem.
     */
    public ArmazenamentoException(String mensagem) {
        super(mensagem);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Erro de armazenamento com causa técnica
     * Objetivo: Criar o erro preservando a exceção original.
     * Decisões: A causa é mantida para o log estruturado mostrar a origem da falha.
     */
    public ArmazenamentoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
