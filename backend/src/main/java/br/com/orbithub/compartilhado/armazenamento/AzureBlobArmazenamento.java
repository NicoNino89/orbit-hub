package br.com.orbithub.compartilhado.armazenamento;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import java.io.InputStream;

/** Adapter de armazenamento no Azure Blob Storage, usado em homologação e produção. */
public class AzureBlobArmazenamento implements ArmazenamentoArquivos {

    private final BlobContainerClient container;

    public AzureBlobArmazenamento(BlobContainerClient container) {
        this.container = container;
    }

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

    @Override
    public boolean existe(String chave) {
        return Boolean.TRUE.equals(blob(chave).exists());
    }

    @Override
    public void remover(String chave) {
        try {
            blob(chave).deleteIfExists();
        } catch (BlobStorageException e) {
            throw new ArmazenamentoException("Falha ao remover " + chave, e);
        }
    }

    private BlobClient blob(String chave) {
        return container.getBlobClient(ChaveArquivo.validar(chave));
    }
}
