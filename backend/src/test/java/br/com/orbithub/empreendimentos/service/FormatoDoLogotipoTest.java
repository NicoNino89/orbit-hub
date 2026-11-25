package br.com.orbithub.empreendimentos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Testes do formato do logotipo
 * Objetivo: Conferir o reconhecimento de PNG, JPEG e WebP pelos bytes e a recusa do resto.
 * Decisões: Assinaturas mínimas.
 */
class FormatoDoLogotipoTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Formatos
     * Objetivo: PNG, JPEG e WebP reconhecidos; GIF e arquivo curto recusados.
     * Decisões: WebP pelo cabeçalho RIFF....WEBP.
     */
    @Test
    void formatos() {
        assertThat(StartupsService.formatoDaImagem(new byte[] {(byte) 0x89, 'P', 'N', 'G'})).contains("image/png");
        assertThat(StartupsService.formatoDaImagem(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})).contains("image/jpeg");
        assertThat(StartupsService.formatoDaImagem("RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII))).contains("image/webp");
        assertThat(StartupsService.formatoDaImagem("RIFF0000WAVE".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(StartupsService.formatoDaImagem("GIF89a".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(StartupsService.formatoDaImagem(new byte[] {1})).isEmpty();
    }
}
