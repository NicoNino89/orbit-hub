package br.com.orbithub.compartilhado.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Bloqueio por aceite pendente
 * Objetivo: Impedir o uso da plataforma enquanto houver Termos de Uso ou Política de Privacidade
 *           vigentes sem aceite ("aceita a nova versão no próximo acesso").
 * Decisões: A pendência vem no próprio access token (claim "termos_pendentes"), sem consulta ao banco por
 *           requisição. Depois de aceitar, o front-end renova a sessão e recebe um token sem a marca.
 *           Ficam liberadas só as rotas de autenticação, de aceite, de leitura dos documentos e as notificações (o
 *           aviso de termos atualizados precisa aparecer, KAN-146). Resposta
 *           403 com o código "aceite_pendente", para a tela abrir o aceite.
 */
public class AceitePendenteInterceptor implements HandlerInterceptor {

    static final String CLAIM = "termos_pendentes";
    static final List<String> LIBERADAS = List.of(
            "/api/v1/auth/", "/api/v1/conta/aceites", "/api/v1/documentos-legais", "/api/v1/sobre",
            "/api/v1/notificacoes");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Conferência antes do controller
     * Objetivo: Recusar a requisição de quem tem aceite pendente, fora das rotas liberadas.
     * Decisões: Requisições sem token (rotas públicas) passam direto.
     */
    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler)
            throws IOException {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token)
                || !Boolean.TRUE.equals(token.getToken().getClaimAsBoolean(CLAIM))) {
            return true;
        }
        String caminho = requisicao.getRequestURI();
        if (LIBERADAS.stream().anyMatch(caminho::startsWith)) {
            return true;
        }
        resposta.setStatus(HttpStatus.FORBIDDEN.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Aceite pendente\",\"status\":403,"
                + "\"detail\":\"Leia e aceite a versão atual dos Termos de Uso e da Política de Privacidade.\","
                + "\"codigo\":\"aceite_pendente\"}");
        return false;
    }
}
