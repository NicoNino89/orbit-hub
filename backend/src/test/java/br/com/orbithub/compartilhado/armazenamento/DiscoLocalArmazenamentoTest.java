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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Teste do adapter de disco local
 * Objetivo: Rodar o contrato de armazenamento no disco local e cobrir os erros próprios do sistema de
 *           arquivos.
 * Decisões: Pasta temporária do JUnit por teste, para os casos não interferirem entre si.
 */
class DiscoLocalArmazenamentoTest extends ContratoArmazenamento {

    @TempDir
    Path pasta;

    private DiscoLocalArmazenamento armazenamento;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Adapter numa pasta ainda inexistente
     * Objetivo: Criar o adapter apontando para uma subpasta que ainda não existe.
     * Decisões: Assim todo teste também exercita a criação da raiz.
     */
    @BeforeEach
    void criar() {
        armazenamento = new DiscoLocalArmazenamento(pasta.resolve("raiz"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Adapter sob teste no contrato
     * Objetivo: Entregar ao contrato o adapter de disco.
     * Decisões: Gancho exigido pela classe abstrata {@link ContratoArmazenamento}.
     */
    @Override
    ArmazenamentoArquivos armazenamento() {
        return armazenamento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Cria a pasta raiz quando não existe
     * Objetivo: Garantir que o adapter crie a raiz ao ser instanciado.
     * Decisões: Verifica direto no sistema de arquivos, sem passar pelo adapter.
     */
    @Test
    void criaAPastaRaizQuandoNaoExiste() {
        assertThat(Files.isDirectory(pasta.resolve("raiz"))).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha quando a raiz não pode ser criada
     * Objetivo: Garantir que a aplicação não suba com uma raiz inválida.
     * Decisões: Usa um arquivo no lugar da pasta-mãe, erro reproduzível em qualquer sistema operacional.
     */
    @Test
    void falhaQuandoARaizNaoPodeSerCriada() throws Exception {
        Path arquivo = Files.writeString(pasta.resolve("sou-arquivo"), "x");
        assertThatThrownBy(() -> new DiscoLocalArmazenamento(arquivo.resolve("raiz")))
                .isInstanceOf(UncheckedIOException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha ao gravar com caminho ocupado
     * Objetivo: Garantir {@link ArmazenamentoException} quando uma subpasta necessária já é um arquivo.
     * Decisões: Mesma técnica de arquivo no lugar de pasta, portável entre sistemas.
     */
    @Test
    void falhaAoGravarQuandoOCaminhoEstaOcupadoPorUmArquivo() throws Exception {
        Files.writeString(pasta.resolve("raiz/ocupado"), "x");
        assertThatThrownBy(() -> armazenamento.gravar("ocupado/novo.txt", new ByteArrayInputStream(new byte[] {1}), 1, "text/plain"))
                .isInstanceOf(ArmazenamentoException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha ao remover pasta com conteúdo
     * Objetivo: Garantir {@link ArmazenamentoException} quando a chave aponta para uma pasta não vazia.
     * Decisões: O adapter nunca remove pastas recursivamente, para não apagar arquivos de outra chave.
     */
    @Test
    void falhaAoRemoverPastaComConteudo() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/cheia"));
        Files.writeString(pasta.resolve("raiz/cheia/x.txt"), "x");
        assertThatThrownBy(() -> armazenamento.remover("cheia")).isInstanceOf(ArmazenamentoException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:58
     * Cartão Jira: KAN-52
     * Título: Falha ao ler uma pasta
     * Objetivo: Garantir que ler uma chave que é pasta resulte em erro.
     * Decisões: Aceita qualquer exceção porque o momento do erro (abrir ou ler) varia entre Windows e Linux.
     */
    @Test
    void falhaAoLerUmaPasta() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/pasta"));
        assertThatThrownBy(() -> armazenamento.ler("pasta").readAllBytes()).isInstanceOf(Exception.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Pasta com o mesmo nome não conta como arquivo
     * Objetivo: Garantir que {@code existe} responda falso para uma pasta.
     * Decisões: Mantém o mesmo comportamento do Blob, onde pastas não existem de fato.
     */
    @Test
    void diretorioComOMesmoNomeNaoContaComoArquivo() throws Exception {
        Files.createDirectories(pasta.resolve("raiz/so-pasta"));
        assertThat(armazenamento.existe("so-pasta")).isFalse();
    }
}
