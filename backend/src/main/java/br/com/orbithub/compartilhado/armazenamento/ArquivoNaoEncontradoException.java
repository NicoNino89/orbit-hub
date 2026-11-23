package br.com.orbithub.compartilhado.armazenamento;

public class ArquivoNaoEncontradoException extends ArmazenamentoException {

    public ArquivoNaoEncontradoException(String chave) {
        super("Arquivo não encontrado: " + chave);
    }
}
