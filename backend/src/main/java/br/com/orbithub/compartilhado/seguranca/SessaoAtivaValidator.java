package br.com.orbithub.compartilhado.seguranca;

import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Validador de sessão do access token
 * Objetivo: Recusar access tokens cuja sessão (claim "sid") foi encerrada ou expirou.
 * Decisões: Sem essa checagem, um access token continuaria valendo até 15 minutos depois do logout ou da
 *           troca de senha; o custo é uma consulta indexada por requisição autenticada.
 */
final class SessaoAtivaValidator implements OAuth2TokenValidator<Jwt> {

    static final String CLAIM_SESSAO = "sid";
    private static final OAuth2Error SESSAO_ENCERRADA =
            new OAuth2Error("invalid_token", "Sessão encerrada", null);

    private final VerificadorDeSessao verificador;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação do validador
     * Objetivo: Receber quem sabe se a sessão está ativa.
     * Decisões: Injeção pela interface de compartilhado.
     */
    SessaoAtivaValidator(VerificadorDeSessao verificador) {
        this.verificador = verificador;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Validação do token
     * Objetivo: Aprovar o token só se a sessão dele continuar ativa.
     * Decisões: Token sem "sid" ou com "sid" malformado é recusado, nunca aceito por omissão.
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String sessao = token.getClaimAsString(CLAIM_SESSAO);
        if (sessao == null) {
            return OAuth2TokenValidatorResult.failure(SESSAO_ENCERRADA);
        }
        try {
            return verificador.sessaoAtiva(UUID.fromString(sessao))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(SESSAO_ENCERRADA);
        } catch (IllegalArgumentException malformado) {
            return OAuth2TokenValidatorResult.failure(SESSAO_ENCERRADA);
        }
    }
}
