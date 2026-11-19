package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Contrato de armazenamento contra o Azurite
 * Objetivo: Rodar o mesmo contrato do disco local contra o Azurite, emulador oficial do Azure Blob Storage.
 * Decisões: O SDK do Azure costuma usar uma versão da API mais nova que a do Azurite, por isso o
 *           emulador sobe com {@code --skipApiVersionCheck}. A conta e a chave são as de
 *           desenvolvimento, públicas e fixas do Azurite (não são segredo).
 */
@Testcontainers
class AzureBlobArmazenamentoIT extends ContratoArmazenamento {

    private static final int PORTA_BLOB = 10000;

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Adapter de Blob apontado para o Azurite
     * Objetivo: Criar o adapter pela mesma configuração usada em produção, trocando só a conexão.
     * Decisões: Passa pela {@link ArmazenamentoConfig}, para testar também a criação do container.
     */
    @BeforeEach
    void criar() {
        String connectionString = "DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://%s:%d/%s;"
                .formatted(CONTA, CHAVE, AZURITE.getHost(), AZURITE.getMappedPort(PORTA_BLOB), CONTA);
        var azure = new ArmazenamentoProperties.Azure(null, "testes", connectionString);
        armazenamento = new ArmazenamentoConfig()
                .armazenamentoArquivos(new ArmazenamentoProperties(ArmazenamentoProperties.Tipo.AZURE_BLOB, null, azure));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Adapter sob teste no contrato
     * Objetivo: Entregar ao contrato o adapter de Blob.
     * Decisões: Gancho exigido pela classe abstrata {@link ContratoArmazenamento}.
     */
    @Override
    ArmazenamentoArquivos armazenamento() {
        return armazenamento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Configuração Azure cria adapter de Blob
     * Objetivo: Garantir que o tipo AZURE_BLOB resulte em {@link AzureBlobArmazenamento}.
     * Decisões: Complementa o teste de unidade do caso LOCAL.
     */
    @Test
    void configuracaoAzureCriaAdapterDeBlob() {
        assertThat(armazenamento).isInstanceOf(AzureBlobArmazenamento.class);
    }
}
