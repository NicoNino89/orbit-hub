package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Mesmo contrato do disco local, contra o Azurite (emulador oficial do Azure Blob Storage).
 *
 * <p>O SDK do Azure costuma usar uma versão da API mais nova que a do Azurite, por isso o
 * emulador sobe com {@code --skipApiVersionCheck}.
 */
@Testcontainers
class AzureBlobArmazenamentoIT extends ContratoArmazenamento {

    private static final int PORTA_BLOB = 10000;

    /** Conta de desenvolvimento pública e fixa do Azurite (não é segredo). */
    private static final String CONTA = "devstoreaccount1";
    private static final String CHAVE =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    @Container
    static final GenericContainer<?> AZURITE =
            new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:3.35.0")
                    .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--blobPort", String.valueOf(PORTA_BLOB),
                            "--skipApiVersionCheck", "--loose")
                    .withExposedPorts(PORTA_BLOB)
                    .waitingFor(Wait.forListeningPort());

    private ArmazenamentoArquivos armazenamento;

    @BeforeEach
    void criar() {
        String connectionString = "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://%s:%d/%s;"
                .formatted(CONTA, CHAVE, AZURITE.getHost(), AZURITE.getMappedPort(PORTA_BLOB), CONTA);
        var azure = new ArmazenamentoProperties.Azure(null, "testes", connectionString);
        armazenamento = new ArmazenamentoConfig()
                .armazenamentoArquivos(new ArmazenamentoProperties(ArmazenamentoProperties.Tipo.AZURE_BLOB, null, azure));
    }

    @Override
    ArmazenamentoArquivos armazenamento() {
        return armazenamento;
    }

    @Test
    void configuracaoAzureCriaAdapterDeBlob() {
        assertThat(armazenamento).isInstanceOf(AzureBlobArmazenamento.class);
    }
}
