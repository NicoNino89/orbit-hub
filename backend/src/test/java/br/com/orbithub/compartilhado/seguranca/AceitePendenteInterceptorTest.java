package br.com.orbithub.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Testes do bloqueio por aceite pendente e dos dados do token
 * Objetivo: Verificar quem passa e quem é barrado pelo interceptor e a leitura das claims do token.
 * Decisões: Contexto de segurança montado à mão, sem subir o Spring.
 */
class AceitePendenteInterceptorTest {

    private final AceitePendenteInterceptor interceptor = new AceitePendenteInterceptor();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Limpeza
     * Objetivo: Esvaziar o contexto de segurança depois de cada teste.
     * Decisões: O contexto é por thread e vazaria para o próximo teste.
     */
    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Sem login ou sem pendência
     * Objetivo: Garantir que requisições públicas e tokens sem pendência passam.
     * Decisões: Dois casos.
     */
    @Test
    void passaSemLoginOuSemPendencia() throws Exception {
        assertThat(interceptor.preHandle(requisicao("/api/v1/qualquer"), new MockHttpServletResponse(), null)).isTrue();

        autenticar(false);
        assertThat(interceptor.preHandle(requisicao("/api/v1/qualquer"), new MockHttpServletResponse(), null)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Pendência barra o resto da API
     * Objetivo: Garantir 403 com código aceite_pendente fora das rotas liberadas.
     * Decisões: Confere status, tipo e corpo.
     */
    @Test
    void pendenciaBarraRotasNaoLiberadas() throws Exception {
        autenticar(true);
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(requisicao("/api/v1/conta"), resposta, null)).isFalse();
        assertThat(resposta.getStatus()).isEqualTo(403);
        assertThat(resposta.getContentType()).startsWith("application/problem+json");
        assertThat(resposta.getContentAsString()).contains("\"codigo\":\"aceite_pendente\"");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Rotas liberadas com pendência
     * Objetivo: Garantir que aceite, autenticação e leitura dos termos continuam acessíveis.
     * Decisões: Uma rota de cada grupo.
     */
    @Test
    void pendenciaLiberaAceiteAutenticacaoELeitura() throws Exception {
        autenticar(true);
        for (String rota : List.of("/api/v1/conta/aceites", "/api/v1/conta/aceites/pendentes",
                "/api/v1/auth/renovar", "/api/v1/documentos-legais", "/api/v1/sobre")) {
            assertThat(interceptor.preHandle(requisicao(rota), new MockHttpServletResponse(), null)).isTrue();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Leitura das claims
     * Objetivo: Garantir id, papéis e sessão lidos do token.
     * Decisões: Token sem a claim de papéis não tem papel nenhum.
     */
    @Test
    void lerDadosDoToken() {
        UUID conta = UUID.randomUUID();
        UUID sessao = UUID.randomUUID();
        Jwt token = jwt(conta, sessao, false);
        assertThat(Autenticado.id(token)).isEqualTo(conta);
        assertThat(Autenticado.sessao(token)).isEqualTo(sessao);
        assertThat(Autenticado.temPapel(token, "INVESTIDOR")).isTrue();
        assertThat(Autenticado.temPapel(token, "ADMIN")).isFalse();
        Jwt semPapeis = Jwt.withTokenValue("t").header("alg", "HS256").subject(conta.toString()).build();
        assertThat(Autenticado.temPapel(semPapeis, "INVESTIDOR")).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Autenticar no contexto
     * Objetivo: Colocar no contexto um token com ou sem pendência.
     * Decisões: JwtAuthenticationToken, o mesmo tipo usado pelo resource server.
     */
    private static void autenticar(boolean pendente) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(UUID.randomUUID(), UUID.randomUUID(), pendente)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Token de apoio
     * Objetivo: Montar um Jwt com conta, sessão, papel investidor e pendência.
     * Decisões: Sem assinatura: aqui só as claims importam.
     */
    private static Jwt jwt(UUID conta, UUID sessao, boolean pendente) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(conta.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("sid", sessao.toString()).claim("papeis", List.of("INVESTIDOR"))
                .claim(AceitePendenteInterceptor.CLAIM, pendente).build();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Requisição de apoio
     * Objetivo: Criar uma requisição GET para a rota informada.
     * Decisões: O interceptor só olha o caminho.
     */
    private static MockHttpServletRequest requisicao(String caminho) {
        return new MockHttpServletRequest("GET", caminho);
    }
}
