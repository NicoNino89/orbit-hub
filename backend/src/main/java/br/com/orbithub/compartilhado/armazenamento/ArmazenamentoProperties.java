package br.com.orbithub.compartilhado.armazenamento;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Parâmetros do armazenamento de arquivos
 * Objetivo: Ler de {@code orbit.armazenamento.*} o tipo de armazenamento e os dados de cada adapter.
 * Decisões: Records imutáveis com {@code @ConfigurationProperties}: nenhum limite ou endereço fica fixo
 *           no código.
 *
 * @param tipo {@code LOCAL} ou {@code AZURE_BLOB}
 * @param local pasta raiz quando o tipo é LOCAL
 * @param azure dados da conta quando o tipo é AZURE_BLOB
 */
@ConfigurationProperties(prefix = "orbit.armazenamento")
public record ArmazenamentoProperties(Tipo tipo, Local local, Azure azure) {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Tipos de armazenamento
     * Objetivo: Enumerar os adapters disponíveis.
     * Decisões: Enum fechado, para o {@code switch} da configuração ser exaustivo.
     */
    public enum Tipo { LOCAL, AZURE_BLOB }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Parâmetros do disco local
     * Objetivo: Informar a pasta raiz dos arquivos em desenvolvimento.
     * Decisões: No Docker Compose a pasta é um volume nomeado, para os arquivos sobreviverem ao contêiner.
     *
     * @param pasta caminho da pasta raiz
     */
    public record Local(String pasta) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Parâmetros do Azure Blob Storage
     * Objetivo: Informar a conta e o container na nuvem.
     * Decisões: Na nuvem usa identidade gerenciada (sem segredo). A connection string só existe para o
     *           Azurite, o emulador local do Blob Storage.
     *
     * @param endpoint URL da conta, por exemplo {@code https://orbithubhml.blob.core.windows.net}
     * @param container nome do container
     * @param connectionString opcional, apenas para o emulador
     */
    public record Azure(String endpoint, String container, String connectionString) {
    }
}
