package br.com.orbithub.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Testes do validador de sessão e da chave do JWT
 * Objetivo: Verificar a recusa de tokens de sessão encerrada e as regras da chave de assinatura.
 * Decisões: Tokens montados em memória (Jwt.withTokenValue), sem assinatura, porque aqui só a claim importa.
 */
class SessaoAtivaValidatorTest {

    private final UUID ativa = UUID.randomUUID();
    private final SessaoAtivaValidator validador = new SessaoAtivaValidator(ativa::equals);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão ativa aprovada
     * Objetivo: Garantir que um token com sessão ativa passa.
     * Decisões: Verificador que só conhece uma sessão.
     */
    @Test
    void aprovaSessaoAtiva() {
        assertThat(validador.validate(token(Map.of("sid", ativa.toString()))).hasErrors()).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão encerrada, ausente ou malformada
     * Objetivo: Garantir que o token é recusado em todos esses casos.
     * Decisões: Nunca aceitar por omissão.
     */
    @Test
    void recusaSessaoEncerradaAusenteOuMalformada() {
        assertThat(validador.validate(token(Map.of("sid", UUID.randomUUID().toString()))).hasErrors()).isTrue();
        assertThat(validador.validate(token(Map.of("outra", "x"))).hasErrors()).isTrue();
        assertThat(validador.validate(token(Map.of("sid", "nao-e-uuid"))).hasErrors()).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Chave obrigatória na nuvem
     * Objetivo: Garantir que, fora do ambiente local, a falta de chave impede a inicialização.
     * Decisões: Vazio e nulo.
     */
    @Test
    void chaveObrigatoriaForaDoAmbienteLocal() {
        assertThatThrownBy(() -> ChaveJwt.montar("", "homologacao")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ChaveJwt.montar(null, "producao")).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Chave curta recusada
     * Objetivo: Garantir que chaves com menos de 256 bits são recusadas.
     * Decisões: 16 bytes.
     */
    @Test
    void chaveCurtaRecusada() {
        String curta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> ChaveJwt.montar(curta, "homologacao")).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Chave informada e chave local aleatória
     * Objetivo: Garantir que a chave informada é usada e que o ambiente local gera uma aleatória.
     * Decisões: Duas chaves locais seguidas precisam ser diferentes.
     */
    @Test
    void usaChaveInformadaOuGeraAleatoriaNoLocal() {
        String informada = Base64.getEncoder().encodeToString(new byte[32]);
        assertThat(ChaveJwt.montar(informada, "homologacao").getEncoded()).hasSize(32);
        assertThat(ChaveJwt.montar("", "local").getEncoded()).hasSize(32)
                .isNotEqualTo(ChaveJwt.montar("", "local").getEncoded());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Token de apoio
     * Objetivo: Montar um Jwt com as claims informadas.
     * Decisões: Validade qualquer; o validador de sessão não olha o tempo.
     */
    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder jwt = Jwt.withTokenValue("t").header("alg", "HS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        claims.forEach(jwt::claim);
        return jwt.build();
    }
}
