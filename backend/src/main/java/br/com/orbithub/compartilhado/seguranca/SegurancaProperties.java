package br.com.orbithub.compartilhado.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Configuração de segurança
 * Objetivo: Reunir a chave e a duração do JWT e a tolerância técnica de reuso do refresh token.
 * Decisões: Só o que é técnico fica aqui. A inatividade e a duração máxima da sessão são regra de negócio e
 *           viraram parâmetros editáveis (sessao.inatividade_minutos, sessao.duracao_maxima_horas, KAN-78).
 */
@ConfigurationProperties("orbit.seguranca")
public record SegurancaProperties(Jwt jwt, Sessao sessao) {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Parâmetros do access token
     * Objetivo: Chave HMAC (Base64), emissor e duração do access token.
     * Decisões: Duração curta (15 min por padrão); a sessão longa fica com o refresh token.
     */
    public record Jwt(String chave, String emissor, Duration duracao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Configuração técnica da sessão
     * Objetivo: Tolerância de reuso do refresh token.
     * Decisões: A tolerância cobre duas abas renovando ao mesmo tempo. Inatividade e duração máxima são
     *           parâmetros de negócio (KAN-78).
     */
    public record Sessao(Duration toleranciaReuso) {
    }
}
