package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.compartilhado.seguranca.SegurancaProperties;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Testes da emissão do access token
 * Objetivo: Verificar as claims, a duração e a assinatura do access token.
 * Decisões: Assinador e leitor reais (Nimbus) com chave de teste, para conferir o token de ponta a ponta.
 */
class TokenAcessoServiceTest {

    private static final SecretKey CHAVE = new SecretKeySpec(
            "chave-de-teste-com-mais-de-256-bits-0000".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Claims do token
     * Objetivo: Garantir emissor, assunto, sessão, papéis e validade de 15 minutos, sem dados pessoais.
     * Decisões: Lê o token com o decodificador sem validadores de tempo (relógio fixo no passado).
     */
    @Test
    void geraTokenComClaimsMinimasEValidadeCurta() {
        Instant agora = Instant.parse("2026-11-24T03:00:00Z");
        var propriedades = new SegurancaProperties(
                new SegurancaProperties.Jwt("x", "orbit-hub", Duration.ofMinutes(15)),
                new SegurancaProperties.Sessao(Duration.ofMinutes(30), Duration.ofHours(12), Duration.ofSeconds(10)));
        var servico = new TokenAcessoService(new NimbusJwtEncoder(new ImmutableSecret<>(CHAVE)), propriedades,
                new RelogioAjustavel(agora));
        Usuario ana = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR, Papel.EMPREENDEDOR), agora);
        UUID sessao = UUID.randomUUID();

        var token = servico.gerar(ana, sessao);

        NimbusJwtDecoder leitor = NimbusJwtDecoder.withSecretKey(CHAVE).macAlgorithm(MacAlgorithm.HS256).build();
        leitor.setJwtValidator(jwt -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
        Jwt jwt = leitor.decode(token.valor());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("orbit-hub");
        assertThat(jwt.getSubject()).isEqualTo(ana.getId().toString());
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(sessao.toString());
        assertThat(jwt.getClaimAsStringList("papeis")).containsExactly("EMPREENDEDOR", "INVESTIDOR");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getIssuedAt()).isEqualTo(agora);
        assertThat(jwt.getExpiresAt()).isEqualTo(agora.plus(Duration.ofMinutes(15)));
        assertThat(token.expiraEm()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "nome");
    }
}
