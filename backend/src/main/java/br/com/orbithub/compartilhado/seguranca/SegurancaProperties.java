package br.com.orbithub.compartilhado.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Parâmetros de segurança
 * Objetivo: Reunir chave e prazos do JWT e da sessão, lidos de orbit.seguranca.
 * Decisões: Prazos configuráveis por variável de ambiente, sem valor fixo no código. Quando o painel de
 *           parâmetros chegar (KAN-75), os prazos de sessão passam a ser editáveis por ele.
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
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Parâmetros da sessão
     * Objetivo: Janela de inatividade, duração máxima e tolerância de reuso do refresh token.
     * Decisões: A inatividade é a validade do refresh token (KAN-15); a duração máxima limita a sessão
     *           mesmo em uso contínuo; a tolerância cobre duas abas renovando ao mesmo tempo.
     */
    public record Sessao(Duration inatividade, Duration duracaoMaxima, Duration toleranciaReuso) {
    }
}
