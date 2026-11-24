package br.com.orbithub.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Testes de domínio do cadastro
 * Objetivo: Verificar a política de senha, a fábrica de perfis e o token de uso único.
 * Decisões: Testes puros, sem Spring.
 */
class CadastroDominioTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T04:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Senha aceita
     * Objetivo: Garantir que uma frase longa e incomum passa.
     * Decisões: Sem exigir maiúscula ou símbolo (NIST).
     */
    @Test
    void senhaLongaEIncomumPassa() {
        assertThat(PoliticaDeSenha.problema("cavalo correto bateria", "ana@exemplo.com")).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Senhas recusadas
     * Objetivo: Garantir a recusa de senha curta, longa demais, comum, repetitiva e com o e-mail.
     * Decisões: Confere a mensagem de cada caso, que é a mostrada no formulário.
     */
    @Test
    void senhasFracasSaoRecusadas() {
        assertThat(PoliticaDeSenha.problema("curta", "a@b.com")).contains("A senha precisa ter pelo menos 10 caracteres.");
        assertThat(PoliticaDeSenha.problema(null, "a@b.com")).isPresent();
        assertThat(PoliticaDeSenha.problema("x".repeat(129), "a@b.com")).contains("A senha pode ter no máximo 128 caracteres.");
        assertThat(PoliticaDeSenha.problema("Senha12345", "a@b.com")).contains("Essa senha é muito comum. Escolha outra.");
        assertThat(PoliticaDeSenha.problema("abababababab", "a@b.com")).contains("Essa senha é muito comum. Escolha outra.");
        assertThat(PoliticaDeSenha.problema("anasouza-2026!", "anasouza@exemplo.com"))
                .contains("A senha não pode conter o seu e-mail.");
        assertThat(PoliticaDeSenha.problema("bia-e-uma-senha-boa", "bia@exemplo.com")).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Fábrica de perfis
     * Objetivo: Garantir o perfil, o onboarding e a exigência de confidencialidade de cada papel.
     * Decisões: ADMIN não tem perfil de cadastro.
     */
    @Test
    void fabricaDePerfis() {
        PerfilDePapel investidor = PerfilDePapel.para(Papel.INVESTIDOR);
        PerfilDePapel empreendedor = PerfilDePapel.para(Papel.EMPREENDEDOR);

        assertThat(investidor.papel()).isEqualTo(Papel.INVESTIDOR);
        assertThat(investidor.rotaOnboarding()).isEqualTo("/investidor/onboarding");
        assertThat(investidor.exigeTermoDeConfidencialidade()).isTrue();
        assertThat(empreendedor.papel()).isEqualTo(Papel.EMPREENDEDOR);
        assertThat(empreendedor.rotaOnboarding()).isEqualTo("/empreendedor/onboarding");
        assertThat(empreendedor.exigeTermoDeConfidencialidade()).isFalse();
        assertThatThrownBy(() -> PerfilDePapel.para(Papel.ADMIN)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Token de uso único
     * Objetivo: Garantir validade, finalidade e uso único do token de conta.
     * Decisões: Validade de 24 h; marcar usado duas vezes mantém a primeira data.
     */
    @Test
    void tokenDeContaDeUsoUnico() {
        UUID usuario = UUID.randomUUID();
        TokenDeConta token = TokenDeConta.emitir(usuario, TokenDeConta.Finalidade.TROCA_EMAIL, "h", "novo@exemplo.com",
                AGORA, Duration.ofHours(24));

        assertThat(token.getUsuarioId()).isEqualTo(usuario);
        assertThat(token.getDado()).isEqualTo("novo@exemplo.com");
        assertThat(token.getFinalidade()).isEqualTo(TokenDeConta.Finalidade.TROCA_EMAIL);
        assertThat(token.getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofHours(24)));
        assertThat(token.utilizavel(TokenDeConta.Finalidade.TROCA_EMAIL, AGORA)).isTrue();
        assertThat(token.utilizavel(TokenDeConta.Finalidade.REDEFINICAO_SENHA, AGORA)).isFalse();
        assertThat(token.utilizavel(TokenDeConta.Finalidade.TROCA_EMAIL, AGORA.plus(Duration.ofHours(24)))).isFalse();

        token.marcarUsado(AGORA);
        token.marcarUsado(AGORA.plusSeconds(5));
        assertThat(token.utilizavel(TokenDeConta.Finalidade.TROCA_EMAIL, AGORA)).isFalse();
    }
}
