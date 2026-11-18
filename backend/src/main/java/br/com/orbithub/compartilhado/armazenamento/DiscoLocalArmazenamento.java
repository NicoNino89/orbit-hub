package br.com.orbithub.compartilhado.armazenamento;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Adapter de armazenamento em disco local, usado em desenvolvimento e no Docker Compose. */
public class DiscoLocalArmazenamento implements ArmazenamentoArquivos {

    private final Path raiz;

    public DiscoLocalArmazenamento(Path raiz) {
        this.raiz = raiz.toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.raiz);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de armazenamento " + this.raiz, e);
        }
    }

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

    @Override
    public boolean existe(String chave) {
        return Files.isRegularFile(resolver(chave));
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException e) {
            throw new ArmazenamentoException("Falha ao remover " + chave, e);
        }
    }

    private Path resolver(String chave) {
        Path caminho = raiz.resolve(ChaveArquivo.validar(chave)).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new IllegalArgumentException("Chave de arquivo inválida: " + chave);
        }
        return caminho;
    }
}
