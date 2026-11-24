package br.com.orbithub.identidade.api;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Cookie do refresh token
 * Objetivo: Montar e apagar o cookie que leva o refresh token.
 * Decisões: HttpOnly (fora do alcance do JavaScript), Secure (só HTTPS; navegadores aceitam em
 *           http://localhost), SameSite=Strict (não vai em requisições de outro site) e Path restrito às
 *           rotas de autenticação, para o cookie não acompanhar as demais chamadas da API.
 */
@Component
public class CookieDeSessao {

    public static final String NOME = "orbit_sessao";
    static final String CAMINHO = "/api/v1/auth";

    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação do montador de cookie
     * Objetivo: Receber o relógio para calcular o Max-Age.
     * Decisões: Relógio injetado para os testes.
     */
    public CookieDeSessao(Clock relogio) {
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Cookie com o refresh token
     * Objetivo: Montar o cabeçalho Set-Cookie com o token e a validade dele.
     * Decisões: Max-Age igual à validade do token: o navegador descarta o cookie quando a sessão expira por
     *           inatividade.
     */
    public String criar(String refreshToken, Instant expiraEm) {
        long segundos = Math.max(0, Duration.between(relogio.instant(), expiraEm).toSeconds());
        return base(refreshToken).maxAge(segundos).build().toString();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Cookie de remoção
     * Objetivo: Montar o cabeçalho Set-Cookie que apaga o cookie no navegador.
     * Decisões: Mesmo nome, caminho e atributos, com Max-Age zero.
     */
    public String apagar() {
        return base("").maxAge(0).build().toString();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Atributos comuns do cookie
     * Objetivo: Concentrar os atributos de segurança num só lugar.
     * Decisões: Criação e remoção precisam dos mesmos atributos para o navegador tratar como o mesmo cookie.
     */
    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(CAMINHO);
    }
}
