package br.com.orbithub.identidade.domain;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Código de uso único baseado em tempo (TOTP)
 * Objetivo: Gerar segredos e conferir os códigos de 6 dígitos dos aplicativos autenticadores (Google
 *           Authenticator, Microsoft Authenticator, Authy), o segundo fator das contas internas.
 * Decisões: Implementação própria da RFC 6238 (HMAC-SHA1, passo de 30 s, 6 dígitos), o padrão que todos os
 *           aplicativos aceitam; são poucas linhas e evitam uma dependência só para isso. Aceita o passo anterior
 *           e o seguinte, para tolerar relógio de celular um pouco adiantado ou atrasado. Comparação em tempo
 *           constante. Segredo de 160 bits, como recomenda a RFC 4226.
 */
public final class Totp {

    static final int PASSO_SEGUNDOS = 30;
    static final int DIGITOS = 6;
    static final int JANELA = 1;
    static final int BYTES_DO_SEGREDO = 20;
    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só funções.
     */
    private Totp() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Gerar segredo
     * Objetivo: Criar um segredo novo, em Base32 (o formato que os aplicativos leem).
     * Decisões: SecureRandom, 20 bytes.
     */
    public static String gerarSegredo() {
        byte[] bytes = new byte[BYTES_DO_SEGREDO];
        ALEATORIO.nextBytes(bytes);
        return base32(bytes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Passo de tempo
     * Objetivo: Calcular o número do intervalo de 30 segundos de um instante.
     * Decisões: Contado a partir de 1970 (época Unix), como na RFC.
     */
    public static long passo(Instant instante) {
        return Math.floorDiv(instante.getEpochSecond(), PASSO_SEGUNDOS);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Código de um passo
     * Objetivo: Calcular o código de 6 dígitos de um segredo num passo de tempo.
     * Decisões: Truncamento dinâmico da RFC 4226; zeros à esquerda preservados.
     */
    public static String codigo(String segredoBase32, long passo) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(deBase32(segredoBase32), "HmacSHA1"));
            byte[] resumo = hmac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(passo).array());
            int inicio = resumo[resumo.length - 1] & 0x0f;
            int binario = ((resumo[inicio] & 0x7f) << 24) | ((resumo[inicio + 1] & 0xff) << 16)
                    | ((resumo[inicio + 2] & 0xff) << 8) | (resumo[inicio + 3] & 0xff);
            int valor = binario % (int) Math.pow(10, DIGITOS);
            return String.format(Locale.ROOT, "%0" + DIGITOS + "d", valor);
        } catch (GeneralSecurityException impossivel) {
            throw new IllegalStateException("HmacSHA1 indisponível", impossivel);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Conferir código
     * Objetivo: Dizer em qual passo (atual, anterior ou seguinte) o código informado vale.
     * Decisões: Devolve o passo, para quem chama impedir o reuso; vazio quando o código não vale. Ignora
     *           espaços que o aplicativo mostra no meio do código.
     */
    public static OptionalLong conferir(String segredoBase32, String informado, Instant agora) {
        if (informado == null) {
            return OptionalLong.empty();
        }
        String limpo = informado.replace(" ", "").strip();
        if (!limpo.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        long atual = passo(agora);
        for (long p = atual - JANELA; p <= atual + JANELA; p++) {
            if (MessageDigest.isEqual(codigo(segredoBase32, p).getBytes(StandardCharsets.US_ASCII),
                    limpo.getBytes(StandardCharsets.US_ASCII))) {
                return OptionalLong.of(p);
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Endereço para o aplicativo
     * Objetivo: Montar o endereço otpauth:// que o aplicativo autenticador lê (por QR Code ou link).
     * Decisões: Emissor "Orbit Hub" e a conta pelo e-mail, para a pessoa reconhecer a entrada no aplicativo.
     */
    public static String endereco(String email, String segredoBase32) {
        String emissor = "Orbit Hub";
        String rotulo = URLEncoder.encode(emissor + ":" + email, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + rotulo + "?secret=" + segredoBase32
                + "&issuer=" + URLEncoder.encode(emissor, StandardCharsets.UTF_8).replace("+", "%20")
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + PASSO_SEGUNDOS;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Codificar em Base32
     * Objetivo: Converter bytes no alfabeto Base32 da RFC 4648.
     * Decisões: Sem preenchimento "=", como os aplicativos esperam.
     */
    static String base32(byte[] dados) {
        StringBuilder saida = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : dados) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                saida.append(ALFABETO_BASE32.charAt((buffer >> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            saida.append(ALFABETO_BASE32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return saida.toString();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Decodificar Base32
     * Objetivo: Converter o texto Base32 de volta em bytes.
     * Decisões: Aceita minúsculas, espaços e "="; caractere fora do alfabeto é erro.
     */
    static byte[] deBase32(String texto) {
        String limpo = texto.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : limpo.toCharArray()) {
            int valor = ALFABETO_BASE32.indexOf(c);
            if (valor < 0) {
                throw new IllegalArgumentException("Caractere inválido em Base32");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                saida.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return saida.toByteArray();
    }
}
