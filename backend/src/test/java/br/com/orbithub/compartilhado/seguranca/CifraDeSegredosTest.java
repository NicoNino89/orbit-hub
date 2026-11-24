package br.com.orbithub.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:35
 * Cartão Jira: KAN-75
 * Título: Testes da cifra de segredos
 * Objetivo: Garantir ida e volta, vetor novo a cada cifra e recusa de texto adulterado ou de outra chave.
 * Decisões: Chaves fixas de teste.
 */
class CifraDeSegredosTest {

    static final CifraDeSegredos CIFRA = new CifraDeSegredos(
            new SecretKeySpec("chave-de-teste-com-32-bytes-0001".getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Ida e volta
     * Objetivo: Conferir que o texto decifrado é o original e que o cifrado não revela o texto.
     * Decisões: Dois cifrados do mesmo texto são diferentes (vetor aleatório).
     */
    @Test
    void cifraEDecifra() {
        String cifrado = CIFRA.cifrar("GEZDGNBVGY3TQOJQ");
        assertThat(cifrado).startsWith("v1:").doesNotContain("GEZDGNBVGY3TQOJQ");
        assertThat(CIFRA.cifrar("GEZDGNBVGY3TQOJQ")).isNotEqualTo(cifrado);
        assertThat(CIFRA.decifrar(cifrado)).isEqualTo("GEZDGNBVGY3TQOJQ");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:35
     * Cartão Jira: KAN-75
     * Título: Recusas
     * Objetivo: Recusar texto adulterado, de outra chave, sem prefixo ou nulo.
     * Decisões: A marca do GCM detecta qualquer alteração.
     */
    @Test
    void recusaAdulteradoOutraChaveEFormatoDesconhecido() {
        String cifrado = CIFRA.cifrar("segredo");
        byte[] bytes = Base64.getDecoder().decode(cifrado.substring(3));
        bytes[bytes.length - 1] ^= 0x01;
        String adulterado = "v1:" + Base64.getEncoder().encodeToString(bytes);
        assertThatThrownBy(() -> CIFRA.decifrar(adulterado)).isInstanceOf(IllegalStateException.class);

        CifraDeSegredos outra = new CifraDeSegredos(
                new SecretKeySpec("outra-chave-de-teste-32-bytes-02".getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));
        assertThatThrownBy(() -> outra.decifrar(cifrado)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CIFRA.decifrar("segredo")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CIFRA.decifrar(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> CIFRA.decifrar("v1:***")).isInstanceOf(IllegalStateException.class);
    }
}
