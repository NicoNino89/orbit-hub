package br.com.orbithub.compartilhado.seguranca;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Chave de assinatura do JWT
 * Objetivo: Montar a chave HMAC-SHA256 que assina e confere os access tokens.
 * Decisões: Na nuvem a chave vem do Key Vault (ORBIT_JWT_CHAVE) e é obrigatória, com pelo menos 256 bits.
 *           Só no ambiente local, sem chave informada, é gerada uma chave aleatória a cada início: as
 *           sessões caem ao reiniciar, o que é aceitável em desenvolvimento e evita chave fixa no código.
 */
final class ChaveJwt {

    static final int TAMANHO_MINIMO_BYTES = 32;
    private static final Logger LOG = LoggerFactory.getLogger(ChaveJwt.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só métodos estáticos.
     */
    private ChaveJwt() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Montagem da chave
     * Objetivo: Decodificar a chave em Base64 ou, no ambiente local, gerar uma aleatória.
     * Decisões: Falha na inicialização (e não no primeiro login) quando falta a chave na nuvem ou ela é
     *           curta demais, para o problema aparecer no deploy.
     */
    static SecretKey montar(String chaveBase64, String ambiente) {
        byte[] bytes;
        if (chaveBase64 == null || chaveBase64.isBlank()) {
            if (!"local".equals(ambiente)) {
                throw new IllegalStateException(
                        "Defina ORBIT_JWT_CHAVE (Base64, pelo menos 256 bits) no ambiente " + ambiente);
            }
            bytes = new byte[TAMANHO_MINIMO_BYTES];
            ALEATORIO.nextBytes(bytes);
            LOG.warn("ORBIT_JWT_CHAVE não definida: usando chave aleatória; as sessões caem a cada reinício");
        } else {
            bytes = Base64.getDecoder().decode(chaveBase64.strip());
            if (bytes.length < TAMANHO_MINIMO_BYTES) {
                throw new IllegalStateException("ORBIT_JWT_CHAVE precisa ter pelo menos 256 bits");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
