package br.com.orbithub.compartilhado.convites;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-65
 * Título: Tokens e e-mails de convite
 * Objetivo: Gerar o token do link do convite, calcular o hash guardado e conferir o e-mail de destino.
 * Decisões: Um só lugar para organizações e equipes de startup. A conferência do e-mail é feita sem expressão regular
 *           (sem risco de lentidão por backtracking): uma arroba, partes não vazias e um ponto no domínio, sem espaços.
 */
public final class TokensDeConvite {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Construtor privado
     * Objetivo: Impedir instâncias da classe utilitária.
     * Decisões: Só métodos estáticos.
     */
    private TokensDeConvite() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Novo token
     * Objetivo: Gerar o token que vai no link do convite.
     * Decisões: 32 bytes aleatórios em Base64 para URL, sem preenchimento.
     */
    public static String novoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Hash do token
     * Objetivo: Calcular o SHA-256 do token, que é o que fica no banco.
     * Decisões: Hexadecimal minúsculo; token nulo vira vazio (não encontra convite).
     */
    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: E-mail de destino válido
     * Objetivo: Conferir o formato básico do e-mail convidado.
     * Decisões: A confirmação de que o e-mail existe é o próprio aceite pela conta com esse e-mail.
     */
    public static boolean emailValido(String email) {
        if (email == null || email.isEmpty() || email.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0 || arroba != email.lastIndexOf('@')) {
            return false;
        }
        String dominio = email.substring(arroba + 1);
        int ponto = dominio.lastIndexOf('.');
        return ponto > 0 && ponto < dominio.length() - 1 && !dominio.startsWith(".");
    }
}
