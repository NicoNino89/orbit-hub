package br.com.orbithub.identidade.service;

import java.util.Optional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Formato de imagem pelo conteúdo
 * Objetivo: Reconhecer JPEG, PNG e WebP pelos primeiros bytes do arquivo.
 * Decisões: Não confia na extensão nem no tipo enviado pelo navegador: um arquivo HTML renomeado para .png
 *           é recusado. SVG fica de fora de propósito (pode conter script).
 */
final class FormatoDeImagem {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só métodos estáticos.
     */
    private FormatoDeImagem() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Detectar formato
     * Objetivo: Devolver o tipo MIME da imagem, se for um formato aceito.
     * Decisões: Assinaturas oficiais: JPEG FF D8 FF; PNG 89 50 4E 47 0D 0A 1A 0A; WebP "RIFF" ... "WEBP".
     */
    static Optional<String> detectar(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        if (comeca(b, png, 0)) {
            return Optional.of("image/png");
        }
        if (comeca(b, new byte[] {'R', 'I', 'F', 'F'}, 0) && comeca(b, new byte[] {'W', 'E', 'B', 'P'}, 8)) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Extensão do tipo
     * Objetivo: Converter o tipo MIME na extensão usada na chave do arquivo.
     * Decisões: Só os três formatos aceitos.
     */
    static String extensao(String tipo) {
        return switch (tipo) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Tipo da extensão
     * Objetivo: Converter a extensão da chave no tipo MIME da resposta.
     * Decisões: Inverso de {@link #extensao}.
     */
    static String tipoDaExtensao(String extensao) {
        return switch (extensao) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Começa com a assinatura
     * Objetivo: Conferir se os bytes, a partir da posição, batem com a assinatura.
     * Decisões: Arquivo menor que a assinatura não bate.
     */
    private static boolean comeca(byte[] b, byte[] assinatura, int posicao) {
        if (b.length < posicao + assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if (b[posicao + i] != assinatura[i]) {
                return false;
            }
        }
        return true;
    }
}
