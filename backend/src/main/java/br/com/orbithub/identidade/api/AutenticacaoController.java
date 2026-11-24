package br.com.orbithub.identidade.api;

import br.com.orbithub.identidade.service.LoginService;
import br.com.orbithub.identidade.service.RenovacaoConcorrenteException;
import br.com.orbithub.identidade.service.SessaoInvalidaException;
import br.com.orbithub.identidade.service.SessaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Rotas de autenticação
 * Objetivo: Expor login, logout (KAN-13) e a renovação de sessão pelo refresh token em cookie.
 * Decisões: As respostas de erro são genéricas, sem dizer por que a sessão ou o login foram recusados.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login, renovação e encerramento de sessão")
class AutenticacaoController {

    private final SessaoService sessoes;
    private final LoginService login;
    private final CookieDeSessao cookie;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Criação do controller
     * Objetivo: Receber os serviços de sessão e de login e o montador do cookie.
     * Decisões: Injeção pelo construtor.
     */
    AutenticacaoController(SessaoService sessoes, LoginService login, CookieDeSessao cookie) {
        this.sessoes = sessoes;
        this.login = login;
        this.cookie = cookie;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Entrar
     * Objetivo: Conferir e-mail e senha, abrir a sessão e gravar o refresh token no cookie.
     * Decisões: O access token vai no corpo e o refresh token só no cookie HttpOnly.
     */
    @PostMapping("/entrar")
    @Operation(summary = "Entra com e-mail e senha")
    ResponseEntity<SessaoResponse> entrar(@Valid @RequestBody LoginRequest pedido) {
        var sessao = login.entrar(pedido.email(), pedido.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.criar(sessao.refreshToken(), sessao.refreshExpiraEm()))
                .body(SessaoResponse.de(sessao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Sair
     * Objetivo: Encerrar a sessão do cookie e apagar o cookie.
     * Decisões: 204 sempre, mesmo sem cookie ou com sessão já encerrada.
     */
    @PostMapping("/sair")
    @Operation(summary = "Encerra a sessão atual")
    ResponseEntity<Void> sair(@CookieValue(name = CookieDeSessao.NOME, required = false) String refresh) {
        login.sair(refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.apagar()).build();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Credenciais inválidas
     * Objetivo: Responder 401 com a mensagem genérica.
     * Decisões: Não diz se o e-mail existe.
     */
    @ExceptionHandler(LoginService.CredenciaisInvalidasException.class)
    ProblemDetail credenciaisInvalidas(LoginService.CredenciaisInvalidasException erro) {
        return problema(HttpStatus.UNAUTHORIZED, "Não foi possível entrar", erro.getMessage(), "credenciais_invalidas");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Login bloqueado
     * Objetivo: Responder 429 durante o bloqueio temporário.
     * Decisões: Código "login_bloqueado" para a tela.
     */
    @ExceptionHandler(LoginService.LoginBloqueadoException.class)
    ProblemDetail loginBloqueado(LoginService.LoginBloqueadoException erro) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "Login bloqueado temporariamente", erro.getMessage(), "login_bloqueado");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Conta pendente
     * Objetivo: Responder 403 quando falta confirmar o e-mail.
     * Decisões: Código "conta_pendente" para a tela oferecer o reenvio.
     */
    @ExceptionHandler(LoginService.ContaPendenteException.class)
    ProblemDetail contaPendente(LoginService.ContaPendenteException erro) {
        return problema(HttpStatus.FORBIDDEN, "Conta não confirmada", erro.getMessage(), "conta_pendente");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Montar Problem Details
     * Objetivo: Criar a resposta de erro com título, mensagem e código.
     * Decisões: Mesmo formato do tratador global.
     */
    private static ProblemDetail problema(HttpStatus status, String titulo, String mensagem, String codigo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        return problema;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Pedido de login
     * Objetivo: E-mail e senha informados.
     * Decisões: Só obrigatoriedade; formato e tamanho não são conferidos aqui para não dar pistas.
     */
    record LoginRequest(@NotBlank(message = "Informe seu e-mail.") String email,
                        @NotBlank(message = "Informe sua senha.") String senha) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:10
         * Cartão Jira: KAN-13
         * Título: Representação textual segura
         * Objetivo: Evitar que a senha apareça em logs.
         * Decisões: Não mostra nenhum campo.
         */
        @Override
        public String toString() {
            return "LoginRequest[]";
        }
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
