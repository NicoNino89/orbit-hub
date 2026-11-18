package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;

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
    void falhaQuandoARaizNaoPodeSerCriada() throws Exception {
        Path arquivo = Files.writeString(pasta.resolve("sou-arquivo"), "x");
        assertThatThrownBy(() -> new DiscoLocalArmazenamento(arquivo.resolve("raiz")))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void falhaAoGravarQuandoOCaminhoEstaOcupadoPorUmArquivo() throws Exception {
        Files.writeString(pasta.resolve("raiz/ocupado"), "x");
        assertThatThrownBy(() -> armazenamento.gravar("ocupado/novo.txt", new ByteArrayInputStream(new byte[] {1}), 1, "text/plain"))
                .isInstanceOf(ArmazenamentoException.class);
    }

    @Test
    void falhaAoRemoverPastaComConteudo() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/cheia"));
        Files.writeString(pasta.resolve("raiz/cheia/x.txt"), "x");
        assertThatThrownBy(() -> armazenamento.remover("cheia")).isInstanceOf(ArmazenamentoException.class);
    }

    @Test
    void falhaAoLerUmaPasta() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/pasta"));
        assertThatThrownBy(() -> armazenamento.ler("pasta").readAllBytes()).isInstanceOf(Exception.class);
    }

    @Test
    void diretorioComOMesmoNomeNaoContaComoArquivo() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/so-pasta"));
        assertThat(armazenamento.existe("so-pasta")).isFalse();
    }
}
