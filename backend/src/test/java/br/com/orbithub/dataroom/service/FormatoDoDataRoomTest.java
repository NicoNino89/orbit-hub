package br.com.orbithub.dataroom.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-145
 * Título: Testes do formato dos documentos do Data Room
 * Objetivo: Conferir a detecção pelo conteúdo: PDF, PNG, JPEG e Office (ZIP com a extensão certa).
 * Decisões: Sem arquivo real; só os primeiros bytes.
 */
class FormatoDoDataRoomTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-145
     * Título: Formatos
     * Objetivo: Reconhecer os aceitos e recusar o resto, inclusive ZIP com extensão errada.
     * Decisões: A extensão só decide entre os tipos Office.
     */
    @Test
    void reconheceFormatos() {
        assertThat(DataRoomService.formato("%PDF-1.7".getBytes(), "a.txt")).contains("application/pdf");
        assertThat(DataRoomService.formato(new byte[] {(byte) 0x89, 'P', 'N', 'G'}, "a")).contains("image/png");
        assertThat(DataRoomService.formato(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "a")).contains("image/jpeg");
        byte[] zip = {'P', 'K', 3, 4, 0};
        assertThat(DataRoomService.formato(zip, "plano.XLSX")).contains(DataRoomService.Office.XLSX.tipo);
        assertThat(DataRoomService.formato(zip, "deck.pptx")).contains(DataRoomService.Office.PPTX.tipo);
        assertThat(DataRoomService.formato(zip, "contrato.docx")).contains(DataRoomService.Office.DOCX.tipo);
        assertThat(DataRoomService.formato(zip, "arquivo.zip")).isEmpty();
        assertThat(DataRoomService.formato(zip, null)).isEmpty();
        assertThat(DataRoomService.formato("MZ".getBytes(), "a.exe")).isEmpty();
    }
}
