package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:51
 * Cartão Jira: KAN-52
 * Título: Teste da seleção do adapter
 * Objetivo: Verificar que a configuração LOCAL cria o adapter de disco.
 * Decisões: Teste de unidade, sem contexto Spring; o caso AZURE_BLOB é coberto contra o Azurite no
 *           {@link AzureBlobArmazenamentoIT}.
 */
class ArmazenamentoConfigTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:51
     * Cartão Jira: KAN-52
     * Título: Configuração local cria adapter de disco
     * Objetivo: Garantir que o tipo LOCAL resulte em {@link DiscoLocalArmazenamento}.
     * Decisões: Pasta temporária do JUnit, apagada ao fim do teste.
     */
    @Test
    void configuracaoLocalCriaAdapterDeDisco(@TempDir Path pasta) {
        var props = new ArmazenamentoProperties(
                ArmazenamentoProperties.Tipo.LOCAL, new ArmazenamentoProperties.Local(pasta.toString()), null);

        assertThat(new ArmazenamentoConfig().armazenamentoArquivos(props)).isInstanceOf(DiscoLocalArmazenamento.class);
    }
}
