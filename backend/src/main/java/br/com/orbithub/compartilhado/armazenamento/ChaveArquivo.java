package br.com.orbithub.compartilhado.armazenamento;

import java.util.regex.Pattern;

/**
 * Validação das chaves de arquivo, comum a todos os adapters.
 *
 * <p>Aceita só segmentos com letras, números, ponto, hífen e sublinhado, separados por "/". Isso
 * impede que uma chave saia da pasta raiz ({@code ../}) ou aponte para um caminho absoluto.
 */
final class ChaveArquivo {

    private static final Pattern SEGMENTO = Pattern.compile("[A-Za-z0-9._-]+");
    private static final int TAMANHO_MAXIMO = 512;

    private ChaveArquivo() {
    }

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
