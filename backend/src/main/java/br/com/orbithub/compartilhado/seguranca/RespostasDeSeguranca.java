package br.com.orbithub.compartilhado.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Respostas de acesso recusado
 * Objetivo: Responder 401 (sem login válido) e 403 (sem permissão) em Problem Details e registrar as
 *           tentativas não autorizadas.
 * Decisões: Todo 403 é registrado (log e banco). O 401 é registrado quando havia um token, mas inválido; sem
 *           token nenhum é só log de depuração, para robôs varrendo a API não encherem a tabela. O cabeçalho
 *           WWW-Authenticate do padrão Bearer é mantido.
 */
public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RespostasDeSeguranca.class);

    private final ObjectProvider<AcessoNegadoRepository> registros;
    private final Clock relogio;
    private final BearerTokenAuthenticationEntryPoint padraoBearer = new BearerTokenAuthenticationEntryPoint();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Criação do componente
     * Objetivo: Receber o repositório de registros e o relógio.
     * Decisões: Repositório opcional: nos testes só da camada web ele não existe e o registro fica só no log.
     */
    public RespostasDeSeguranca(ObjectProvider<AcessoNegadoRepository> registros, Clock relogio) {
        this.registros = registros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Sem autenticação válida
     * Objetivo: Responder 401 e registrar quando o token apresentado era inválido.
     * Decisões: Reaproveita o ponto de entrada Bearer para o cabeçalho WWW-Authenticate.
     */
    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta, AuthenticationException erro)
            throws IOException {
        padraoBearer.commence(requisicao, resposta, erro);
        boolean comToken = requisicao.getHeader("Authorization") != null;
        if (comToken) {
            registrar(null, requisicao, HttpStatus.UNAUTHORIZED);
        } else {
            LOG.debug("Requisição sem token em rota protegida: {} {}", requisicao.getMethod(), requisicao.getRequestURI());
        }
        escrever(resposta, HttpStatus.UNAUTHORIZED, "Não autenticado", "Entre na sua conta para continuar.", "nao_autenticado");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Sem permissão
     * Objetivo: Responder 403 e registrar a tentativa com a conta que tentou.
     * Decisões: Mensagem genérica: não diz qual papel faltou.
     */
    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta, AccessDeniedException erro)
            throws IOException {
        registrar(contaAtual(), requisicao, HttpStatus.FORBIDDEN);
        escrever(resposta, HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para fazer isso.", "acesso_negado");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Registrar tentativa
     * Objetivo: Gravar a tentativa no banco e no log estruturado.
     * Decisões: Falha ao gravar não muda a resposta (o log continua).
     */
    private void registrar(UUID conta, HttpServletRequest requisicao, HttpStatus status) {
        LOG.warn("Acesso negado ({}): {} {} (usuario={})", status.value(), requisicao.getMethod(),
                requisicao.getRequestURI(), conta);
        AcessoNegadoRepository repositorio = registros.getIfAvailable();
        if (repositorio == null) {
            return;
        }
        try {
            repositorio.save(AcessoNegado.registrar(conta, requisicao.getMethod(), requisicao.getRequestURI(),
                    status.value(), relogio.instant()));
        } catch (RuntimeException falha) {
            LOG.error("Não foi possível registrar o acesso negado: {}", falha.getClass().getSimpleName());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Conta atual
     * Objetivo: Ler o id da conta autenticada, se houver.
     * Decisões: Nulo fora de uma autenticação por JWT.
     */
    private static UUID contaAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken token ? Autenticado.id(token.getToken()) : null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Escrever Problem Details
     * Objetivo: Escrever o corpo JSON do erro.
     * Decisões: Texto fixo, sem dados da requisição, para não refletir conteúdo do atacante.
     */
    private static void escrever(HttpServletResponse resposta, HttpStatus status, String titulo, String detalhe,
                                 String codigo) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        resposta.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detalhe + "\",\"codigo\":\"" + codigo + "\"}");
    }
}
