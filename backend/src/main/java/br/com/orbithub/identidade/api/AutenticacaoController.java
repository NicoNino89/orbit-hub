package br.com.orbithub.identidade.api;

import br.com.orbithub.identidade.service.RenovacaoConcorrenteException;
import br.com.orbithub.identidade.service.SessaoInvalidaException;
import br.com.orbithub.identidade.service.SessaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Rotas de autenticação
 * Objetivo: Expor a renovação de sessão pelo refresh token em cookie.
 * Decisões: Login e logout chegam com o KAN-13, na mesma rota base; todas as respostas de erro são
 *           genéricas (401), sem dizer por que a sessão foi recusada.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, renovação e encerramento de sessão")
class AutenticacaoController {

    private final SessaoService sessoes;
    private final CookieDeSessao cookie;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Criação do controller
     * Objetivo: Receber o serviço de sessão e o montador do cookie.
     * Decisões: Injeção pelo construtor.
     */
    AutenticacaoController(SessaoService sessoes, CookieDeSessao cookie) {
        this.sessoes = sessoes;
        this.cookie = cookie;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Renovar sessão
     * Objetivo: Trocar o refresh token do cookie por um novo e devolver um access token novo.
     * Decisões: O cookie é regravado a cada renovação (rotação); sem cookie, 401 direto.
     */
    @PostMapping("/renovar")
    @Operation(summary = "Renova a sessão com o refresh token do cookie")
    ResponseEntity<SessaoResponse> renovar(@CookieValue(name = CookieDeSessao.NOME, required = false) String refresh) {
        if (refresh == null || refresh.isBlank()) {
            throw new SessaoInvalidaException("sem_cookie");
        }
        var sessao = sessoes.renovar(refresh);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(sessao.refreshToken(), sessao.refreshExpiraEm()))
                .body(SessaoResponse.de(sessao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão recusada
     * Objetivo: Responder 401 e apagar o cookie quando a sessão não vale mais.
     * Decisões: Mensagem única para todos os motivos; o motivo real fica fora da resposta.
     */
    @ExceptionHandler(SessaoInvalidaException.class)
    ResponseEntity<ProblemDetail> sessaoInvalida() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Sessão expirada ou inválida. Entre novamente.");
        problema.setTitle("Sessão inválida");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookie.apagar())
                .body(problema);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Renovação concorrente
     * Objetivo: Responder 409 quando outra aba acabou de renovar a mesma sessão.
     * Decisões: O cookie não é apagado: o navegador já tem o token novo e o front-end tenta de novo.
     */
    @ExceptionHandler(RenovacaoConcorrenteException.class)
    ResponseEntity<ProblemDetail> renovacaoConcorrente() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "A sessão acabou de ser renovada. Tente novamente.");
        problema.setTitle("Renovação concorrente");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }
}
