package br.com.orbithub.verificacao.service;

import java.util.Optional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Formato do documento
 * Objetivo: Reconhecer PDF, JPEG e PNG pelos primeiros bytes do arquivo.
 * Decisões: Não confia na extensão nem no tipo informado pelo navegador; qualquer outro formato é recusado.
 */
final class FormatoDeDocumento {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Sem instâncias
     * Objetivo: Impedir a criação de objetos desta classe utilitária.
     * Decisões: Construtor privado.
     */
    private FormatoDeDocumento() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Detectar
     * Objetivo: Devolver o tipo de conteúdo pelo começo do arquivo.
     * Decisões: %PDF, FF D8 FF (JPEG) e 89 50 4E 47 (PNG).
     */
    static Optional<String> detectar(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') {
            return Optional.of("application/pdf");
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("image/png");
        }
        return Optional.empty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Extensão
     * Objetivo: Escolher a extensão do arquivo guardado.
     * Decisões: Pelo tipo detectado.
     */
    static String extensao(String tipo) {
        return switch (tipo) {
            case "application/pdf" -> "pdf";
            case "image/png" -> "png";
            default -> "jpg";
        };
    }
}
