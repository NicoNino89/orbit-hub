package br.com.orbithub.compartilhado.armazenamento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do armazenamento de arquivos.
 *
 * @param tipo {@code LOCAL} ou {@code AZURE_BLOB}
 * @param local pasta raiz quando o tipo é LOCAL
 * @param azure dados da conta quando o tipo é AZURE_BLOB
 */
@ConfigurationProperties(prefix = "orbit.armazenamento")
public record ArmazenamentoProperties(Tipo tipo, Local local, Azure azure) {

    public enum Tipo { LOCAL, AZURE_BLOB }

    /** @param pasta caminho da pasta raiz */
    public record Local(String pasta) {
    }

    /**
     * Na nuvem usa identidade gerenciada (sem segredo). A connection string só existe para o
     * Azurite, o emulador local do Blob Storage.
     *
     * @param endpoint URL da conta, por exemplo {@code https://orbithubhml.blob.core.windows.net}
     * @param container nome do container
     * @param connectionString opcional, apenas para o emulador
     */
    public record Azure(String endpoint, String container, String connectionString) {
    }
}
