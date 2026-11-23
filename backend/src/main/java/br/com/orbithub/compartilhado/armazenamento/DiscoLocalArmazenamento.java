package br.com.orbithub.compartilhado.armazenamento;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Adapter de armazenamento em disco local
 * Objetivo: Implementar {@link ArmazenamentoArquivos} numa pasta local, usada em desenvolvimento e no
 *           Docker Compose.
 * Decisões: Mesmo contrato e mesmos erros do adapter de Blob, verificados pelo mesmo teste de contrato.
 */
public class DiscoLocalArmazenamento implements ArmazenamentoArquivos {

    private final Path raiz;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Criação do adapter de disco
     * Objetivo: Normalizar a pasta raiz e criá-la se não existir.
     * Decisões: Caminho absoluto e normalizado, base da checagem contra fuga de pasta; falha na criação
     *           impede a aplicação de subir, em vez de falhar só no primeiro upload.
     */
    public DiscoLocalArmazenamento(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de armazenamento " + this.raiz, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Gravar arquivo em disco
     * Objetivo: Copiar o fluxo para o arquivo da chave, criando as subpastas.
     * Decisões: Substitui o arquivo existente (REPLACE_EXISTING); o tamanho devolvido é o de bytes
     *           realmente gravados, não o informado.
     */
    @Override
    public ArquivoArmazenado gravar(String chave, InputStream conteudo, long tamanho, String tipoConteudo) {
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            long gravados = Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
            return new ArquivoArmazenado(chave, gravados, tipoConteudo);
        } catch (IOException e) {
            throw new ArmazenamentoException("Falha ao gravar " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Ler arquivo do disco
     * Objetivo: Abrir o arquivo da chave como fluxo.
     * Decisões: NoSuchFileException vira {@link ArquivoNaoEncontradoException}, como no Blob.
     */
    @Override
    public InputStream ler(String chave) {
        try {
            return Files.newInputStream(resolver(chave));
        } catch (NoSuchFileException e) {
            throw new ArquivoNaoEncontradoException(chave);
        } catch (IOException e) {
            throw new ArmazenamentoException("Falha ao ler " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Verificar existência em disco
     * Objetivo: Informar se há um arquivo comum na chave.
     * Decisões: {@code isRegularFile}: uma pasta com o mesmo nome não conta como arquivo.
     */
    @Override
    public boolean existe(String chave) {
        return Files.isRegularFile(resolver(chave));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Remover arquivo do disco
     * Objetivo: Apagar o arquivo da chave.
     * Decisões: {@code deleteIfExists} mantém a remoção idempotente; subpastas vazias ficam, sem custo.
     */
    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException e) {
            throw new ArmazenamentoException("Falha ao remover " + chave, e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Caminho físico de uma chave
     * Objetivo: Converter a chave lógica num caminho dentro da pasta raiz.
     * Decisões: Defesa em duas camadas: valida a chave e, depois de normalizar, confere que o caminho
     *           continua dentro da raiz.
     */
    private Path resolver(String chave) {
        Path caminho = raiz.resolve(ChaveArquivo.validar(chave)).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new IllegalArgumentException("Chave de arquivo inválida: " + chave);
        }
        return caminho;
    }
}
