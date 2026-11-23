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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:58
 * Cartão Jira: KAN-52
 * Título: Teste dos caminhos de erro do adapter de Blob
 * Objetivo: Verificar que falhas do Azure viram as exceções do próprio módulo, sem vazar tipos do SDK
 *           para quem chama.
 * Decisões: Dublês do Mockito, porque o Azurite não simula falhas de autorização nem erros internos.
 */
class AzureBlobArmazenamentoTest {

    private final BlobContainerClient container = mock(BlobContainerClient.class);
    private final BlobClient blob = mock(BlobClient.class);
    private AzureBlobArmazenamento armazenamento;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Adapter com container simulado
     * Objetivo: Criar o adapter sobre um container que devolve sempre o mesmo blob simulado.
     * Decisões: Qualquer chave devolve o mesmo dublê, para cada teste programar só a falha que interessa.
     */
    @BeforeEach
    void criar() {
        when(container.getBlobClient(anyString())).thenReturn(blob);
        armazenamento = new AzureBlobArmazenamento(container);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha ao gravar
     * Objetivo: Garantir que erro no upload vire {@link ArmazenamentoException} com a chave na mensagem.
     * Decisões: Erro interno genérico (INTERNAL_ERROR), o caso mais comum de instabilidade.
     */
    @Test
    void falhaAoGravar() {
        BlobStorageException falha = erro(BlobErrorCode.INTERNAL_ERROR);
        when(blob.uploadWithResponse(any(), any(), any())).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.gravar("a.txt", new ByteArrayInputStream(new byte[0]), 0, "text/plain"))
                .isInstanceOf(ArmazenamentoException.class)
                .hasMessageContaining("gravar a.txt");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha de leitura que não é arquivo ausente
     * Objetivo: Garantir que só BLOB_NOT_FOUND vire "arquivo não encontrado".
     * Decisões: Falha de autorização não pode responder 404, senão esconderia um problema de permissão.
     */
    @Test
    void falhaAoLerQueNaoEhArquivoInexistente() {
        BlobStorageException falha = erro(BlobErrorCode.AUTHORIZATION_FAILURE);
        when(blob.openInputStream()).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.ler("a.txt"))
                .isInstanceOf(ArmazenamentoException.class)
                .isNotInstanceOf(ArquivoNaoEncontradoException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha ao remover
     * Objetivo: Garantir que erro na remoção vire {@link ArmazenamentoException} com a chave na mensagem.
     * Decisões: Mesmo padrão de mensagem da gravação, para facilitar a busca no log.
     */
    @Test
    void falhaAoRemover() {
        BlobStorageException falha = erro(BlobErrorCode.INTERNAL_ERROR);
        when(blob.deleteIfExists()).thenThrow(falha);

        assertThatThrownBy(() -> armazenamento.remover("a.txt"))
                .isInstanceOf(ArmazenamentoException.class)
                .hasMessageContaining("remover a.txt");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Erro simulado do Blob Storage
     * Objetivo: Criar uma {@link BlobStorageException} com o código de erro desejado.
     * Decisões: A exceção do SDK não tem construtor público simples; o dublê evita montar uma resposta HTTP.
     */
    private static BlobStorageException erro(BlobErrorCode codigo) {
        BlobStorageException e = mock(BlobStorageException.class);
        when(e.getErrorCode()).thenReturn(codigo);
        return e;
    }
}
