package br.com.orbithub.compartilhado.convites;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-65
 * Título: Testes dos tokens e e-mails de convite
 * Objetivo: Conferir a conferência de e-mail sem expressão regular e que token e hash têm o formato esperado.
 * Decisões: Casos de borda do e-mail (sem arroba, duas arrobas, domínio sem ponto, ponto nas pontas, espaço).
 */
class TokensDeConviteTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: E-mails aceitos
     * Objetivo: Conferir formatos comuns de e-mail.
     * Decisões: Subdomínio e sinal de mais.
     */
    @ParameterizedTest
    @ValueSource(strings = {"ana@exemplo.com", "ana.souza+orbit@mail.exemplo.com.br"})
    void aceita(String email) {
        assertThat(TokensDeConvite.emailValido(email)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: E-mails recusados
     * Objetivo: Conferir que formatos inválidos são recusados.
     * Decisões: Inclui nulo e vazio.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"sem-arroba", "@exemplo.com", "a@@exemplo.com", "a@b@c.com", "a@exemplo", "a@.com", "a@exemplo.",
        "a b@exemplo.com"})
    void recusa(String email) {
        assertThat(TokensDeConvite.emailValido(email)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Token e hash
     * Objetivo: Conferir o tamanho do token, que dois tokens diferem e que o hash é SHA-256 em hexadecimal.
     * Decisões: Nulo tem o mesmo hash do vazio.
     */
    @Test
    void tokenEHash() {
        String token = TokensDeConvite.novoToken();
        assertThat(token).hasSize(43).isNotEqualTo(TokensDeConvite.novoToken());
        assertThat(TokensDeConvite.hash(token)).hasSize(64).matches("[0-9a-f]+");
        assertThat(TokensDeConvite.hash(null)).isEqualTo(TokensDeConvite.hash(""));
    }
}
