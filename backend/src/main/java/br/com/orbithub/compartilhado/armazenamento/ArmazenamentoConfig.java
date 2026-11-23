package br.com.orbithub.compartilhado.armazenamento;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Escolhe o adapter de armazenamento conforme {@code orbit.armazenamento.tipo}. */
@Configuration(proxyBeanMethods = false)
class ArmazenamentoConfig {

    @Bean
    ArmazenamentoArquivos armazenamentoArquivos(ArmazenamentoProperties props) {
        return switch (props.tipo()) {
            case LOCAL -> new DiscoLocalArmazenamento(Path.of(props.local().pasta()));
            case AZURE_BLOB -> new AzureBlobArmazenamento(containerAzure(props.azure()));
        };
    }

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
