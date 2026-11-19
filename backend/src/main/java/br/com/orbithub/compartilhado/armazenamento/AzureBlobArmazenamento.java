package br.com.orbithub.compartilhado.armazenamento;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import java.io.InputStream;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Adapter de armazenamento no Azure Blob Storage
 * Objetivo: Implementar {@link ArmazenamentoArquivos} sobre um container do Blob Storage, usado em
 *           homologação e produção.
 * Decisões: Recebe o {@link BlobContainerClient} pronto, para ser testado com o Azurite ou com dublês.
 */
public class AzureBlobArmazenamento implements ArmazenamentoArquivos {

    private final BlobContainerClient container;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Criação do adapter de Blob
     * Objetivo: Guardar o container onde os arquivos ficam.
     * Decisões: A criação do container fica na configuração, não aqui.
     */
    public AzureBlobArmazenamento(BlobContainerClient container) {
        this.container = container;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Gravar arquivo no Blob
     * Objetivo: Enviar o fluxo ao blob da chave, com o tipo MIME no cabeçalho.
     * Decisões: Upload paralelo do SDK, que divide arquivos grandes em blocos; o Content-Type é gravado
     *           para o navegador abrir o arquivo corretamente.
     */
    @Override
    public ArquivoArmazenado gravar(String chave, InputStream conteudo, long tamanho, String tipoConteudo) {
        BlobClient blob = blob(chave);
        try {
            BlobParallelUploadOptions opcoes = new BlobParallelUploadOptions(conteudo)
                    .setHeaders(new BlobHttpHeaders().setContentType(tipoConteudo));
            blob.uploadWithResponse(opcoes, null, null);
            return new ArquivoArmazenado(chave, tamanho, tipoConteudo);
        } catch (BlobStorageException e) {
            throw new ArmazenamentoException("Falha ao gravar " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Ler arquivo do Blob
     * Objetivo: Abrir o blob da chave como fluxo.
     * Decisões: BLOB_NOT_FOUND vira {@link ArquivoNaoEncontradoException}, como no disco local; outras
     *           falhas viram {@link ArmazenamentoException}.
     */
    @Override
    public InputStream ler(String chave) {
        try {
            return blob(chave).openInputStream();
        } catch (BlobStorageException e) {
            if (BlobErrorCode.BLOB_NOT_FOUND.equals(e.getErrorCode())) {
                throw new ArquivoNaoEncontradoException(chave);
            }
            throw new ArmazenamentoException("Falha ao ler " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Verificar existência no Blob
     * Objetivo: Informar se o blob da chave existe.
     * Decisões: {@code Boolean.TRUE.equals} protege contra o retorno nulo do SDK.
     */
    @Override
    public boolean existe(String chave) {
        return Boolean.TRUE.equals(blob(chave).exists());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Remover arquivo do Blob
     * Objetivo: Apagar o blob da chave.
     * Decisões: {@code deleteIfExists} mantém a remoção idempotente, como prevê o contrato.
     */
    @Override
    public void remover(String chave) {
        try {
            blob(chave).deleteIfExists();
        } catch (BlobStorageException e) {
            throw new ArmazenamentoException("Falha ao remover " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Cliente do blob de uma chave
     * Objetivo: Obter o cliente do blob depois de validar a chave.
     * Decisões: Toda operação passa por aqui, então nenhuma chave inválida chega ao Blob Storage.
     */
    private BlobClient blob(String chave) {
        return container.getBlobClient(ChaveArquivo.validar(chave));
    }
}
