package br.com.orbithub.compartilhado.armazenamento;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Metadados de arquivo armazenado
 * Objetivo: Devolver chave, tamanho e tipo de um arquivo gravado pelo {@link ArmazenamentoArquivos}.
 * Decisões: Record imutável; o tamanho é o efetivamente gravado sempre que o adapter consegue medir.
 */
public record ArquivoArmazenado(String chave, long tamanho, String tipoConteudo) {
}
