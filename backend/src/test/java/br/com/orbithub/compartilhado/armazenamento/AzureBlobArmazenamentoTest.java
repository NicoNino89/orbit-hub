package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Falhas do Azure viram as exceções do próprio módulo, sem vazar tipos do SDK para quem chama. */
class AzureBlobArmazenamentoTest {

    private final BlobContainerClient container = mock(BlobContainerClient.class);
    private final BlobClient blob = mock(BlobClient.class);
    private AzureBlobArmazenamento armazenamento;

    @BeforeEach
    void criar() {
        when(container.getBlobClient(anyString())).thenReturn(blob);
        armazenamento = new AzureBlobArmazenamento(container);
    }

    @Test
    void falhaAoGravar() {
        BlobStorageException falha = erro(BlobErrorCode.INTERNAL_ERROR);
        when(blob.uploadWithResponse(any(), any(), any())).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.gravar("a.txt", new ByteArrayInputStream(new byte[0]), 0, "text/plain"))
                .isInstanceOf(ArmazenamentoException.class)
                .hasMessageContaining("gravar a.txt");
    }

    @Test
    void falhaAoLerQueNaoEhArquivoInexistente() {
        BlobStorageException falha = erro(BlobErrorCode.AUTHORIZATION_FAILURE);
        when(blob.openInputStream()).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.ler("a.txt"))
                .isInstanceOf(ArmazenamentoException.class)
                .isNotInstanceOf(ArquivoNaoEncontradoException.class);
    }

    @Test
    void falhaAoRemover() {
        BlobStorageException falha = erro(BlobErrorCode.INTERNAL_ERROR);
        when(blob.deleteIfExists()).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.remover("a.txt"))
                .isInstanceOf(ArmazenamentoException.class)
                .hasMessageContaining("remover a.txt");
    }

    private static BlobStorageException erro(BlobErrorCode codigo) {
        BlobStorageException e = mock(BlobStorageException.class);
        when(e.getErrorCode()).thenReturn(codigo);
        return e;
    }
}
