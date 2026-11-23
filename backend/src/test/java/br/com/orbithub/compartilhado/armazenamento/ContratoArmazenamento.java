package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Contrato dos adapters de armazenamento
 * Objetivo: Definir o comportamento que todo adapter de {@link ArmazenamentoArquivos} precisa cumprir.
 * Decisões: Classe abstrata herdada pelos testes de cada adapter, para disco e Blob serem verificados
 *           exatamente pelos mesmos casos.
 */
abstract class ContratoArmazenamento {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Adapter sob teste
     * Objetivo: Gancho para cada subclasse entregar o seu adapter.
     * Decisões: Método abstrato em vez de campo, para a subclasse criar o adapter no {@code @BeforeEach}.
     */
    abstract ArmazenamentoArquivos armazenamento();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Grava e lê o mesmo conteúdo
     * Objetivo: Garantir que o conteúdo lido seja byte a byte o gravado, com a chave e o tamanho certos.
     * Decisões: Chave com subpastas, para cobrir a criação de pastas no disco.
     */
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Gravar de novo substitui o conteúdo
     * Objetivo: Garantir que uma segunda gravação na mesma chave substitua a primeira.
     * Decisões: Texto com acento, para conferir também a codificação UTF-8.
     */
    @Test
    void gravarDeNovoSubstituiOConteudo() throws IOException {
        gravarTexto("doc.txt", "versão 1");
        gravarTexto("doc.txt", "versão 2");

        try (InputStream lido = armazenamento().ler("doc.txt")) {
            assertThat(new String(lido.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("versão 2");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Existência e remoção
     * Objetivo: Garantir que o arquivo exista depois de gravado e deixe de existir depois de removido.
     * Decisões: Os dois comportamentos no mesmo teste, porque um verifica o outro.
     */
    @Test
    void existeERemover() {
        gravarTexto("temporario.txt", "x");
        assertThat(armazenamento().existe("temporario.txt")).isTrue();

        armazenamento().remover("temporario.txt");

        assertThat(armazenamento().existe("temporario.txt")).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Remover arquivo inexistente não falha
     * Objetivo: Garantir que a remoção seja idempotente.
     * Decisões: Permite repetir uma exclusão (ex.: nova tentativa após timeout) sem tratar erro.
     */
    @Test
    void removerArquivoInexistenteNaoFalha() {
        armazenamento().remover("nunca-existiu.txt");
        assertThat(armazenamento().existe("nunca-existiu.txt")).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Ler arquivo inexistente lança exceção própria
     * Objetivo: Garantir {@link ArquivoNaoEncontradoException} com a chave na mensagem.
     * Decisões: O {@code close()} entra no lambda porque o Blob só detecta a ausência ao abrir o fluxo.
     */
    @Test
    void lerArquivoInexistenteLancaExcecaoPropria() {
        assertThatThrownBy(() -> armazenamento().ler("nao-existe.pdf").close())
                .isInstanceOf(ArquivoNaoEncontradoException.class)
                .hasMessageContaining("nao-existe.pdf");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Chave que tenta sair da raiz é recusada
     * Objetivo: Garantir que nenhum adapter grave fora da sua área.
     * Decisões: Caso de travessia de caminho mais comum ("../").
     */
    @Test
    void recusaChaveQueTentaSairDaRaiz() {
        assertThatThrownBy(() -> gravarTexto("../fora.txt", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Gravar texto
     * Objetivo: Atalho para gravar um texto UTF-8 como text/plain.
     * Decisões: Mantém os testes curtos e focados no comportamento verificado.
     */
    private void gravarTexto(String chave, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        armazenamento().gravar(chave, new ByteArrayInputStream(bytes), bytes.length, "text/plain");
    }
}
