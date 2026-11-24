package br.com.orbithub.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:35
 * Cartão Jira: KAN-75
 * Título: Testes do TOTP
 * Objetivo: Garantir que os códigos batem com os vetores oficiais da RFC 6238 (os mesmos que os aplicativos
 *           autenticadores usam), a janela de tolerância e o formato do endereço otpauth://.
 * Decisões: Segredo de teste da RFC ("12345678901234567890" em ASCII); os vetores da RFC têm 8 dígitos, e os
 *           6 últimos são o código de 6 dígitos.
 */
class TotpTest {

    static final String SEGREDO_RFC = Totp.base32("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Vetores da RFC 6238
     * Objetivo: Conferir o código em instantes da tabela da RFC.
     * Decisões: HMAC-SHA1, passo de 30 s.
     */
    @Test
    void geraOsCodigosDaRfc6238() {
        assertThat(SEGREDO_RFC).isEqualTo("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(Totp.codigo(SEGREDO_RFC, Totp.passo(Instant.ofEpochSecond(59)))).isEqualTo("287082");
        assertThat(Totp.codigo(SEGREDO_RFC, Totp.passo(Instant.ofEpochSecond(1111111109)))).isEqualTo("081804");
        assertThat(Totp.codigo(SEGREDO_RFC, Totp.passo(Instant.ofEpochSecond(1234567890)))).isEqualTo("005924");
        assertThat(Totp.codigo(SEGREDO_RFC, Totp.passo(Instant.ofEpochSecond(2000000000)))).isEqualTo("279037");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Janela de tolerância
     * Objetivo: Aceitar o código do passo anterior e do seguinte, e recusar os mais distantes e os malformados.
     * Decisões: Devolve o passo em que o código vale, para impedir reuso.
     */
    @Test
    void aceitaUmPassoDeDiferencaERecusaOResto() {
        Instant agora = Instant.ofEpochSecond(1_800_000_000L);
        long atual = Totp.passo(agora);
        assertThat(Totp.conferir(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, atual), agora)).hasValue(atual);
        assertThat(Totp.conferir(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, atual - 1), agora)).hasValue(atual - 1);
        assertThat(Totp.conferir(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, atual + 1), agora)).hasValue(atual + 1);
        assertThat(Totp.conferir(SEGREDO_RFC, Totp.codigo(SEGREDO_RFC, atual - 3), agora)).isEmpty();
        String comEspaco = Totp.codigo(SEGREDO_RFC, atual).substring(0, 3) + " " + Totp.codigo(SEGREDO_RFC, atual).substring(3);
        assertThat(Totp.conferir(SEGREDO_RFC, comEspaco, agora)).hasValue(atual);
        assertThat(Totp.conferir(SEGREDO_RFC, "12a456", agora)).isEmpty();
        assertThat(Totp.conferir(SEGREDO_RFC, "12345", agora)).isEmpty();
        assertThat(Totp.conferir(SEGREDO_RFC, null, agora)).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Segredo e Base32
     * Objetivo: Conferir o tamanho do segredo gerado, a ida e volta do Base32 e a recusa de caracteres inválidos.
     * Decisões: 20 bytes viram 32 caracteres Base32.
     */
    @Test
    void geraSegredosEConverteBase32() {
        String segredo = Totp.gerarSegredo();
        assertThat(segredo).hasSize(32).matches("[A-Z2-7]+");
        assertThat(Totp.gerarSegredo()).isNotEqualTo(segredo);
        assertThat(Totp.deBase32("gezd gnbv gy3t qojq gezd gnbv gy3t qojq"))
                .isEqualTo("12345678901234567890".getBytes(StandardCharsets.US_ASCII));
        assertThat(Totp.base32(new byte[] {1})).isEqualTo("AE");
        assertThatThrownBy(() -> Totp.deBase32("ABC1")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Endereço para o aplicativo
     * Objetivo: Conferir o endereço otpauth:// com emissor, conta e parâmetros.
     * Decisões: Espaços codificados como %20.
     */
    @Test
    void montaOEnderecoOtpauth() {
        assertThat(Totp.endereco("bia@orbithub.com", "ABC"))
                .isEqualTo("otpauth://totp/Orbit%20Hub%3Abia%40orbithub.com?secret=ABC&issuer=Orbit%20Hub"
                        + "&algorithm=SHA1&digits=6&period=30");
    }
}
