package br.com.orbithub.auditoria.api;

import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria.Descricao;
import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:15
 * Cartão Jira: KAN-80
 * Título: Registro automático das ações da equipe
 * Objetivo: Gravar na trilha toda requisição de usuário interno às rotas do backoffice, com o resultado.
 * Decisões: Interceptor do Spring MVC, ligado a /api/v1/admin/** (todo o backoffice fica nessas rotas, regra
 *           documentada no README). Grava em afterCompletion, quando o status final já é conhecido, inclusive
 *           nas recusas por regra de negócio. Leituras também entram, porque consultar dados é uma ação. Se o
 *           serviço descreveu a ação (ContextoDeAuditoria), usa a descrição; senão, o método e a rota. Uma
 *           falha ao gravar não derruba a resposta, mas vai para o log como erro, para o alerta de erros
 *           (KAN-56) disparar.
 */
class AuditoriaInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaInterceptor.class);

    private final ObjectProvider<TrilhaDeAuditoria> trilha;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Criação do interceptor
     * Objetivo: Receber a trilha onde as ações são gravadas.
     * Decisões: Instanciado pela configuração web do módulo; a trilha é buscada na hora de gravar, porque
     *           os testes de fatia web não a carregam.
     */
    AuditoriaInterceptor(ObjectProvider<TrilhaDeAuditoria> trilha) {
        this.trilha = trilha;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Gravar a ação ao fim da requisição
     * Objetivo: Montar e gravar o registro da requisição que acabou.
     * Decisões: Sem conta autenticada não há o que registrar (o acesso negado já tem registro próprio, KAN-34);
     *           sem trilha no contexto (testes de fatia web), também não.
     *           Ações já gravadas pelo próprio serviço não são gravadas de novo.
     */
    @Override
    public void afterCompletion(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler,
                                Exception erro) {
        UUID ator = contaAtual();
        TrilhaDeAuditoria destino = trilha.getIfAvailable();
        if (ator == null || destino == null) {
            return;
        }
        Descricao descricao = ContextoDeAuditoria.atual().orElse(null);
        if (descricao != null && descricao.registrada()) {
            return;
        }
        int status = resposta.getStatus();
        Resultado resultado = erro == null && status < 400 ? Resultado.SUCESSO : Resultado.FALHA;
        Acao acao = montar(requisicao, descricao, ator, resultado, status);
        try {
            destino.registrar(acao);
        } catch (RuntimeException falha) {
            LOG.error("Falha ao gravar a trilha de auditoria ({} {}): {}", requisicao.getMethod(),
                    requisicao.getRequestURI(), falha.getClass().getSimpleName());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Montar o registro
     * Objetivo: Juntar a descrição do serviço com os dados da requisição.
     * Decisões: Sem descrição, a ação vira "MÉTODO rota" (com o padrão da rota, sem ids), e o objeto vem da
     *           primeira variável do caminho, quando houver.
     */
    static Acao montar(HttpServletRequest requisicao, Descricao descricao, UUID ator, Resultado resultado,
                       int status) {
        String rota = rota(requisicao);
        String acao = descricao != null && descricao.acao() != null
                ? descricao.acao()
                : requisicao.getMethod() + " " + rota;
        String objetoTipo = descricao != null ? descricao.objetoTipo() : null;
        String objetoId = descricao != null ? descricao.objetoId() : null;
        if (objetoId == null) {
            objetoId = primeiraVariavel(requisicao);
        }
        String detalhe = descricao != null && descricao.detalhe() != null
                ? descricao.detalhe()
                : requisicao.getMethod() + " " + rota;
        String justificativa = descricao != null ? descricao.justificativa() : null;
        return new Acao(ator, null, acao, objetoTipo, objetoId, resultado, status, detalhe, justificativa);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Rota da requisição
     * Objetivo: Devolver o padrão da rota atendida (por exemplo, /api/v1/admin/documentos-legais/{tipo}/versoes).
     * Decisões: Sem padrão (rota inexistente), usa o caminho sem a query string.
     */
    private static String rota(HttpServletRequest requisicao) {
        Object padrao = requisicao.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return padrao != null ? padrao.toString() : requisicao.getRequestURI();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Objeto do caminho
     * Objetivo: Usar a primeira variável do caminho como identificador do objeto.
     * Decisões: Nulo quando a rota não tem variáveis.
     */
    @SuppressWarnings("unchecked")
    private static String primeiraVariavel(HttpServletRequest requisicao) {
        Object variaveis = requisicao.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variaveis instanceof Map<?, ?> mapa && !mapa.isEmpty()) {
            return ((Map<String, String>) mapa).values().iterator().next();
        }
        return null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:15
     * Cartão Jira: KAN-80
     * Título: Conta autenticada
     * Objetivo: Ler o id da conta do token da requisição.
     * Decisões: Nulo quando não há token válido.
     */
    private static UUID contaAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken token ? Autenticado.id(token.getToken()) : null;
    }
}
