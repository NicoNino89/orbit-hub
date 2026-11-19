package br.com.orbithub.compartilhado.armazenamento;

import java.util.regex.Pattern;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Validação de chaves de arquivo
 * Objetivo: Regra única de chave válida, comum a todos os adapters.
 * Decisões: Aceita só segmentos com letras, números, ponto, hífen e sublinhado, separados por "/". Isso
 *           impede que uma chave saia da pasta raiz ({@code ../}) ou aponte para um caminho absoluto.
 */
final class ChaveArquivo {

    private static final Pattern SEGMENTO = Pattern.compile("[A-Za-z0-9._-]+");
    private static final int TAMANHO_MAXIMO = 512;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Construtor bloqueado
     * Objetivo: Impedir instâncias da classe utilitária.
     * Decisões: Classe final com construtor privado; só há métodos estáticos.
     */
    private ChaveArquivo() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Validar chave de arquivo
     * Objetivo: Recusar chave vazia, longa demais ou com segmento perigoso; devolver a chave válida.
     * Decisões: Lista de caracteres permitidos (whitelist), não de proibidos; limite de 512 caracteres,
     *           compatível com o Blob Storage; "." e ".." são recusados mesmo casando com o padrão.
     */
    static String validar(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A chave do arquivo é obrigatória");
        }
        if (chave.length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A chave do arquivo passa de " + TAMANHO_MAXIMO + " caracteres");
        }
        for (String segmento : chave.split("/", -1)) {
            if (!SEGMENTO.matcher(segmento).matches() || segmento.equals(".") || segmento.equals("..")) {
                throw new IllegalArgumentException("Chave de arquivo inválida: " + chave);
            }
        }
        return chave;
    }
}
