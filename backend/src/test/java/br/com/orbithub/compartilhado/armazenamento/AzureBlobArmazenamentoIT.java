package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Mesmo contrato do disco local, contra o Azurite (emulador oficial do Azure Blob Storage). */
@Testcontainers
class AzureBlobArmazenamentoIT extends ContratoArmazenamento {

    @Container
    static final AzuriteContainer AZURITE =
            new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.35.0")
                    .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--skipApiVersionCheck", "--loose");

    private ArmazenamentoArquivos armazenamento;

    @BeforeEach
    void criar() {
        var azure = new ArmazenamentoProperties.Azure(null, "testes", AZURITE.getConnectionString());
        armazenamento = new ArmazenamentoConfig()
                .armazenamentoArquivos(new ArmazenamentoProperties(ArmazenamentoProperties.Tipo.AZURE_BLOB, null, azure));
    }

    @Override
    ArmazenamentoArquivos armazenamento() {
        return armazenamento;
    }

    @Test
    void configuracaoLocalCriaAdapterDeDisco(@org.junit.jupiter.api.io.TempDir Path pasta) {
        var props = new ArmazenamentoProperties(
                ArmazenamentoProperties.Tipo.LOCAL, new ArmazenamentoProperties.Local(pasta.toString()), null);

        assertThat(new ArmazenamentoConfig().armazenamentoArquivos(props)).isInstanceOf(DiscoLocalArmazenamento.class);
    }

    @Test
    void configuracaoAzureCriaAdapterDeBlob() {
        assertThat(armazenamento).isInstanceOf(AzureBlobArmazenamento.class);
    }
}
