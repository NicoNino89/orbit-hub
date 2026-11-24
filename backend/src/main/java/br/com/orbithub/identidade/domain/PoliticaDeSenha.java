package br.com.orbithub.identidade.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Política mínima de senha
 * Objetivo: Recusar senhas fracas no cadastro, na troca e na redefinição de senha.
 * Decisões: Segue a recomendação do NIST SP 800-63B: comprimento mínimo de 10, máximo de 128, sem regras de
 *           composição obrigatória (que levam a senhas previsíveis), recusando senhas comuns e senhas que
 *           contêm o próprio e-mail. A lista de senhas comuns é curta no MVP; pode virar consulta a uma base
 *           de senhas vazadas no futuro.
 */
public final class PoliticaDeSenha {

    public static final int MINIMO = 10;
    public static final int MAXIMO = 128;

    private static final Set<String> COMUNS = Set.of(
            "1234567890", "12345678910", "0123456789", "1234567890a", "qwertyuiop", "asdfghjkl1",
            "senha12345", "senhasenha", "senha123456", "password12", "password123", "passw0rd12",
            "abcdefghij", "abc1234567", "iloveyou12", "brasil1234", "flamengo123", "corinthians",
            "palmeiras1", "orbithub123", "orbithub2026", "investidor1", "1q2w3e4r5t", "aaaaaaaaaa",
            "1111111111", "0000000000", "admin12345", "mudar12345", "trocar1234", "welcome123");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Classe utilitária
     * Objetivo: Impedir instâncias.
     * Decisões: Só métodos estáticos.
     */
    private PoliticaDeSenha() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Avaliar senha
     * Objetivo: Devolver o motivo da recusa, ou vazio se a senha for aceita.
     * Decisões: A mensagem é a mesma que aparece no formulário; o e-mail é comparado pela parte antes do @.
     */
    public static Optional<String> problema(String senha, String email) {
        if (senha == null || senha.length() < MINIMO) {
            return Optional.of("A senha precisa ter pelo menos " + MINIMO + " caracteres.");
        }
        if (senha.length() > MAXIMO) {
            return Optional.of("A senha pode ter no máximo " + MAXIMO + " caracteres.");
        }
        String minuscula = senha.toLowerCase(Locale.ROOT);
        if (COMUNS.contains(minuscula) || minuscula.chars().distinct().count() <= 2) {
            return Optional.of("Essa senha é muito comum. Escolha outra.");
        }
        if (email != null && email.contains("@")) {
            String usuario = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
            if (usuario.length() >= 4 && minuscula.contains(usuario)) {
                return Optional.of("A senha não pode conter o seu e-mail.");
            }
        }
        return Optional.empty();
    }
}
