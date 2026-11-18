package br.com.orbithub.compartilhado.armazenamento;

import java.io.InputStream;

/**
 * Porta de armazenamento de arquivos (logotipos, documentos do Data Room).
 *
 * <p>Os módulos dependem só desta interface. A implementação é escolhida por configuração
 * ({@code orbit.armazenamento.tipo}): disco local em desenvolvimento e Azure Blob Storage na nuvem
 * (padrão Adapter, docs/arquitetura.md).
 */
public interface ArmazenamentoArquivos {

    /**
     * Grava o conteúdo na chave informada, substituindo o que já existir.
     *
     * @param chave caminho lógico, por exemplo {@code empreendimentos/42/logo.png}
     * @param conteudo fluxo com os bytes do arquivo; quem chama é responsável por fechá-lo
     * @param tamanho tamanho em bytes
     * @param tipoConteudo tipo MIME, por exemplo {@code image/png}
     */
    ArquivoArmazenado gravar(String chave, InputStream conteudo, long tamanho, String tipoConteudo);

    /**
     * Abre o arquivo para leitura. Quem chama deve fechar o fluxo.
     *
     * @throws ArquivoNaoEncontradoException se a chave não existir
     */
    InputStream ler(String chave);

    boolean existe(String chave);

    /** Remove o arquivo. Não falha se ele já não existir. */
    void remover(String chave);
}
