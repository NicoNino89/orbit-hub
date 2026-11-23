package br.com.orbithub.compartilhado.armazenamento;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Seleção do adapter de armazenamento
 * Objetivo: Criar o adapter de armazenamento conforme {@code orbit.armazenamento.tipo}.
 * Decisões: A escolha é por configuração, sem perfis Spring específicos, para o mesmo artefato rodar
 *           em qualquer ambiente.
 */
@Configuration(proxyBeanMethods = false)
class ArmazenamentoConfig {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Bean de armazenamento de arquivos
     * Objetivo: Devolver o adapter de disco local ou de Azure Blob.
     * Decisões: {@code switch} exaustivo sobre o enum: um tipo novo sem adapter não compila.
     */
    @Bean
    ArmazenamentoArquivos armazenamentoArquivos(ArmazenamentoProperties props) {
        return switch (props.tipo()) {
            case LOCAL -> new DiscoLocalArmazenamento(Path.of(props.local().pasta()));
            case AZURE_BLOB -> new AzureBlobArmazenamento(containerAzure(props.azure()));
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Cliente do container no Azure Blob
     * Objetivo: Conectar ao container configurado e criá-lo se ainda não existir.
     * Decisões: Na nuvem usa identidade gerenciada (DefaultAzureCredential), sem segredo; a connection
     *           string só é aceita para o Azurite, o emulador local.
     */
    static BlobContainerClient containerAzure(ArmazenamentoProperties.Azure azure) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        if (azure.connectionString() != null && !azure.connectionString().isBlank()) {
            builder.connectionString(azure.connectionString());
        } else {
            builder.endpoint(azure.endpoint()).credential(new DefaultAzureCredentialBuilder().build());
        }
        BlobContainerClient container = builder.buildClient().getBlobContainerClient(azure.container());
        container.createIfNotExists();
        return container;
    }
}
