package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscoLocalArmazenamentoTest extends ContratoArmazenamento {

    @TempDir
    Path pasta;

    private DiscoLocalArmazenamento armazenamento;

    @BeforeEach
    void criar() {
        armazenamento = new DiscoLocalArmazenamento(pasta.resolve("raiz"));
    }

    @Override
    ArmazenamentoArquivos armazenamento() {
        return armazenamento;
    }

    @Test
    void criaAPastaRaizQuandoNaoExiste() {
        assertThat(Files.isDirectory(pasta.resolve("raiz"))).isTrue();
    }

    @Test
    void diretorioComOMesmoNomeNaoContaComoArquivo() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/so-pasta"));
        assertThat(armazenamento.existe("so-pasta")).isFalse();
    }
}
