package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Comportamento que todo adapter de {@link ArmazenamentoArquivos} precisa cumprir. */
abstract class ContratoArmazenamento {

    abstract ArmazenamentoArquivos armazenamento();

    @Test
    void gravaELeOMesmoConteudo() throws IOException {
        byte[] bytes = "NovaSafra".getBytes(StandardCharsets.UTF_8);

        ArquivoArmazenado gravado = armazenamento()
                .gravar("empreendimentos/1/pitch.txt", new ByteArrayInputStream(bytes), bytes.length, "text/plain");

        assertThat(gravado.chave()).isEqualTo("empreendimentos/1/pitch.txt");
        assertThat(gravado.tamanho()).isEqualTo(bytes.length);
        try (InputStream lido = armazenamento().ler("empreendimentos/1/pitch.txt")) {
            assertThat(lido.readAllBytes()).isEqualTo(bytes);
        }
    }

    @Test
    void gravarDeNovoSubstituiOConteudo() throws IOException {
        gravarTexto("doc.txt", "versão 1");
        gravarTexto("doc.txt", "versão 2");

        try (InputStream lido = armazenamento().ler("doc.txt")) {
            assertThat(new String(lido.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("versão 2");
        }
    }

    @Test
    void existeERemover() {
        gravarTexto("temporario.txt", "x");
        assertThat(armazenamento().existe("temporario.txt")).isTrue();

        armazenamento().remover("temporario.txt");

        assertThat(armazenamento().existe("temporario.txt")).isFalse();
    }

    @Test
    void removerArquivoInexistenteNaoFalha() {
        armazenamento().remover("nunca-existiu.txt");
        assertThat(armazenamento().existe("nunca-existiu.txt")).isFalse();
    }

    @Test
    void lerArquivoInexistenteLancaExcecaoPropria() {
        assertThatThrownBy(() -> armazenamento().ler("nao-existe.pdf").close())
                .isInstanceOf(ArquivoNaoEncontradoException.class)
                .hasMessageContaining("nao-existe.pdf");
    }

    @Test
    void recusaChaveQueTentaSairDaRaiz() {
        assertThatThrownBy(() -> gravarTexto("../fora.txt", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    private void gravarTexto(String chave, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        armazenamento().gravar(chave, new ByteArrayInputStream(bytes), bytes.length, "text/plain");
    }
}
