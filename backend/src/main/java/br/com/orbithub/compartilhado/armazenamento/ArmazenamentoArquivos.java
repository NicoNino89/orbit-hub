package br.com.orbithub.compartilhado.armazenamento;

import java.io.InputStream;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Porta de armazenamento de arquivos
 * Objetivo: Contrato único para gravar, ler, consultar e remover arquivos (logotipos, documentos do
 *           Data Room).
 * Decisões: Os módulos dependem só desta interface (padrão Adapter, docs/arquitetura.md). A
 *           implementação é escolhida por {@code orbit.armazenamento.tipo}: disco local em
 *           desenvolvimento e Azure Blob Storage na nuvem.
 */
public interface ArmazenamentoArquivos {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Gravar arquivo
     * Objetivo: Gravar o conteúdo na chave informada, substituindo o que já existir.
     * Decisões: Recebe fluxo em vez de byte[], para arquivos grandes não ocuparem a memória; quem chama
     *           fecha o fluxo.
     *
     * @param chave caminho lógico, por exemplo {@code empreendimentos/42/logo.png}
     * @param conteudo fluxo com os bytes do arquivo; quem chama é responsável por fechá-lo
     * @param tamanho tamanho em bytes
     * @param tipoConteudo tipo MIME, por exemplo {@code image/png}
     */
    ArquivoArmazenado gravar(String chave, InputStream conteudo, long tamanho, String tipoConteudo);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Ler arquivo
     * Objetivo: Abrir o arquivo para leitura. Quem chama deve fechar o fluxo.
     * Decisões: Arquivo ausente vira {@link ArquivoNaoEncontradoException}, igual em todos os adapters,
     *           para a API responder 404 sem conhecer o adapter.
     *
     * @throws ArquivoNaoEncontradoException se a chave não existir
     */
    InputStream ler(String chave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Verificar existência de arquivo
     * Objetivo: Informar se há um arquivo na chave.
     * Decisões: Pastas com o mesmo nome não contam como arquivo.
     */
    boolean existe(String chave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Remover arquivo
     * Objetivo: Apagar o arquivo da chave.
     * Decisões: Idempotente: não falha se o arquivo já não existir.
     */
    void remover(String chave);
}
