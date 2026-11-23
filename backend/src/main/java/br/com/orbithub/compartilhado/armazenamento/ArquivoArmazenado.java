package br.com.orbithub.compartilhado.armazenamento;

/** Metadados de um arquivo gravado pelo {@link ArmazenamentoArquivos}. */
public record ArquivoArmazenado(String chave, long tamanho, String tipoConteudo) {
}
