package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArmazenamentoConfigTest {

    @Test
    void configuracaoLocalCriaAdapterDeDisco(@TempDir Path pasta) {
        var props = new ArmazenamentoProperties(
                ArmazenamentoProperties.Tipo.LOCAL, new ArmazenamentoProperties.Local(pasta.toString()), null);

        assertThat(new ArmazenamentoConfig().armazenamentoArquivos(props)).isInstanceOf(DiscoLocalArmazenamento.class);
    }
}
