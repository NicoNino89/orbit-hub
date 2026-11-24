package br.com.orbithub.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Testes do refresh token
 * Objetivo: Verificar validade deslizante, limite da família, uso, revogação e tolerância de reuso.
 * Decisões: Instantes fixos, sem depender do relógio da máquina.
 */
class RefreshTokenTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T03:00:00Z");
    private static final Duration TRINTA_MIN = Duration.ofMinutes(30);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Validade pela inatividade
     * Objetivo: Garantir que o token vale pela janela de inatividade quando a família tem folga.
     * Decisões: Família com 12 h de folga.
     */
    @Test
    void expiraPelaJanelaDeInatividade() {
        RefreshToken token = emitir(AGORA.plus(Duration.ofHours(12)));
        assertThat(token.getExpiraEm()).isEqualTo(AGORA.plus(TRINTA_MIN));
        assertThat(token.getStatus()).isEqualTo(StatusRefreshToken.ATIVO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Limite absoluto da família
     * Objetivo: Garantir que o token nunca passa do fim da família.
     * Decisões: Família terminando em 10 minutos.
     */
    @Test
    void naoPassaDoLimiteDaFamilia() {
        Instant fim = AGORA.plus(Duration.ofMinutes(10));
        assertThat(emitir(fim).getExpiraEm()).isEqualTo(fim);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Validade no tempo
     * Objetivo: Garantir que o token vale antes da expiração e não vale no instante exato dela.
     * Decisões: Confere as duas bordas.
     */
    @Test
    void valeAteAExpiracaoExclusive() {
        RefreshToken token = emitir(AGORA.plus(Duration.ofHours(12)));
        assertThat(token.valido(AGORA.plus(Duration.ofMinutes(29)))).isTrue();
        assertThat(token.valido(AGORA.plus(TRINTA_MIN))).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Token usado deixa de valer
     * Objetivo: Garantir que o token trocado não vale mais e que a tolerância de reuso é respeitada.
     * Decisões: Tolerância de 10 s, como no padrão da aplicação.
     */
    @Test
    void tokenUsadoNaoValeERespeitaTolerancia() {
        RefreshToken token = emitir(AGORA.plus(Duration.ofHours(12)));
        token.marcarUsado(AGORA);

        assertThat(token.valido(AGORA)).isFalse();
        assertThat(token.getStatus()).isEqualTo(StatusRefreshToken.USADO);
        assertThat(token.usadoHaMenosDe(Duration.ofSeconds(10), AGORA.plusSeconds(5))).isTrue();
        assertThat(token.usadoHaMenosDe(Duration.ofSeconds(10), AGORA.plusSeconds(10))).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Token ativo não conta como reuso
     * Objetivo: Garantir que a tolerância só vale para tokens usados.
     * Decisões: Token recém-emitido.
     */
    @Test
    void tokenAtivoNaoEstaNaTolerancia() {
        assertThat(emitir(AGORA.plus(Duration.ofHours(1))).usadoHaMenosDe(Duration.ofSeconds(10), AGORA)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Revogação idempotente
     * Objetivo: Garantir que revogar duas vezes mantém o primeiro motivo.
     * Decisões: Segundo motivo diferente, para provar que não sobrescreve.
     */
    @Test
    void revogacaoMantemOPrimeiroMotivo() {
        RefreshToken token = emitir(AGORA.plus(Duration.ofHours(1)));
        token.revogar(AGORA, "logout");
        token.revogar(AGORA.plusSeconds(1), "reuso_detectado");

        assertThat(token.getStatus()).isEqualTo(StatusRefreshToken.REVOGADO);
        assertThat(token.getMotivoRevogacao()).isEqualTo("logout");
        assertThat(token.valido(AGORA)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Dados de identificação
     * Objetivo: Garantir que o token guarda usuário, família e limite informados.
     * Decisões: Confere os acessores usados pelo serviço.
     */
    @Test
    void guardaUsuarioFamiliaELimite() {
        UUID usuario = UUID.randomUUID();
        UUID familia = UUID.randomUUID();
        Instant limite = AGORA.plus(Duration.ofHours(12));
        RefreshToken token = RefreshToken.emitir(usuario, familia, "a".repeat(64), AGORA, TRINTA_MIN, limite);

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUsuarioId()).isEqualTo(usuario);
        assertThat(token.getFamiliaId()).isEqualTo(familia);
        assertThat(token.getFamiliaExpiraEm()).isEqualTo(limite);
        assertThat(token.getMotivoRevogacao()).isNull();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Emissão de apoio
     * Objetivo: Criar um token de teste com o limite de família informado.
     * Decisões: Inatividade de 30 minutos.
     */
    private static RefreshToken emitir(Instant limiteFamilia) {
        return RefreshToken.emitir(UUID.randomUUID(), UUID.randomUUID(), "a".repeat(64), AGORA, TRINTA_MIN,
                limiteFamilia);
    }
}
