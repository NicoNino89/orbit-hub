package br.com.orbithub.verificacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.verificacao.domain.DocumentoDeVerificacao;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Testes do formato do documento e do ciclo do pedido
 * Objetivo: Conferir a detecção de PDF, JPEG e PNG pelos bytes, as extensões, e as mudanças de situação permitidas e
 *           recusadas do pedido.
 * Decisões: Sem banco.
 */
class FormatoEPedidoTest {

    static final Instant AGORA = Instant.parse("2026-11-24T20:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Formatos
     * Objetivo: Reconhecer os três formatos e recusar o resto, inclusive arquivo curto.
     * Decisões: Assinaturas mínimas.
     */
    @Test
    void formatos() {
        assertThat(FormatoDeDocumento.detectar("%PDF-1.7".getBytes())).contains("application/pdf");
        assertThat(FormatoDeDocumento.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).contains("image/jpeg");
        assertThat(FormatoDeDocumento.detectar(new byte[] {(byte) 0x89, 'P', 'N', 'G'})).contains("image/png");
        assertThat(FormatoDeDocumento.detectar("GIF89a".getBytes())).isEmpty();
        assertThat(FormatoDeDocumento.detectar(new byte[] {1})).isEmpty();
        assertThat(FormatoDeDocumento.extensao("application/pdf")).isEqualTo("pdf");
        assertThat(FormatoDeDocumento.extensao("image/png")).isEqualTo("png");
        assertThat(FormatoDeDocumento.extensao("image/jpeg")).isEqualTo("jpg");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Ciclo do pedido
     * Objetivo: Conferir rascunho → análise → decisão, e as recusas de enviar em análise e decidir fora da análise.
     * Decisões: Situação final não aceita documentos.
     */
    @Test
    void cicloDoPedido() {
        UUID ana = UUID.randomUUID();
        PedidoDeVerificacao p = PedidoDeVerificacao.rascunho(SujeitoDaVerificacao.PESSOA, ana, ana, AGORA);
        assertThat(p.aceitaDocumentos()).isTrue();
        assertThat(p.getSituacao().emAberto()).isTrue();
        assertThatThrownBy(() -> p.decidir(SituacaoDoPedido.VERIFICADO, ana, "M", null, AGORA, AGORA))
                .isInstanceOf(IllegalStateException.class);
        p.enviar(AGORA);
        assertThat(p.aceitaDocumentos()).isFalse();
        assertThat(p.getEnviadoEm()).isEqualTo(AGORA);
        assertThatThrownBy(() -> p.enviar(AGORA)).isInstanceOf(IllegalStateException.class);
        p.decidir(SituacaoDoPedido.RECUSADO, ana, "DOCUMENTO_ILEGIVEL", "Foto sem nitidez", null, AGORA);
        assertThat(p.getSituacao().emAberto()).isFalse();
        assertThat(p.getMotivo()).isEqualTo("DOCUMENTO_ILEGIVEL");
        assertThat(p.getObservacao()).isEqualTo("Foto sem nitidez");
        assertThat(p.verificadoEm(AGORA)).isFalse();
        assertThat(SujeitoDaVerificacao.FUNDADOR.getGrupoDeDocumentos()).isEqualTo("FUNDADOR");

        DocumentoDeVerificacao d = DocumentoDeVerificacao.novo(UUID.randomUUID(), p.getId(), "PF_CPF", "k", " ", "image/png", 3, AGORA);
        assertThat(d.getNomeOriginal()).isEqualTo("documento");
        assertThat(DocumentoDeVerificacao.novo(UUID.randomUUID(), p.getId(), "PF_CPF", "k", "a".repeat(300), "image/png", 3, AGORA)
                .getNomeOriginal()).hasSize(200);
    }
}
