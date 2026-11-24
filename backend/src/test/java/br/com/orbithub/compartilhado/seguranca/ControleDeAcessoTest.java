package br.com.orbithub.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Testes do controle de dono e das respostas de acesso recusado
 * Objetivo: Verificar a regra de dono (negar por padrão) e o registro e o formato das respostas 401 e 403.
 * Decisões: Contexto de segurança montado à mão.
 */
class ControleDeAcessoTest {

    private final UUID ana = UUID.randomUUID();
    private final UUID startupDaAna = UUID.randomUUID();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Limpeza
     * Objetivo: Esvaziar o contexto de segurança.
     * Decisões: O contexto é por thread.
     */
    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Regra de dono
     * Objetivo: Garantir que só o dono passa e que tipo desconhecido, id nulo e anônimo são negados.
     * Decisões: Regra simulada para o tipo "startup".
     */
    @Test
    void soODonoPassa() {
        ControleDeAcesso.DonoDeRecurso startups = new ControleDeAcesso.DonoDeRecurso() {
            @Override
            public String tipo() {
                return "startup";
            }

            @Override
            public boolean ehDono(UUID contaId, UUID recursoId) {
                return contaId.equals(ana) && recursoId.equals(startupDaAna);
            }
        };
        ControleDeAcesso acesso = new ControleDeAcesso(List.of(startups));

        assertThat(acesso.ehDono("startup", startupDaAna)).isFalse();
        autenticar(ana);
        assertThat(acesso.ehDono("startup", startupDaAna)).isTrue();
        assertThat(acesso.ehDono("startup", UUID.randomUUID())).isFalse();
        assertThat(acesso.ehDono("rodada", startupDaAna)).isFalse();
        assertThat(acesso.ehDono("startup", null)).isFalse();
        autenticar(UUID.randomUUID());
        assertThat(acesso.ehDono("startup", startupDaAna)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Tipo registrado duas vezes
     * Objetivo: Garantir que duas regras para o mesmo tipo falham na inicialização.
     * Decisões: Erro de configuração, não de execução.
     */
    @Test
    void tipoDuplicadoFalha() {
        ControleDeAcesso.DonoDeRecurso regra = mock(ControleDeAcesso.DonoDeRecurso.class);
        when(regra.tipo()).thenReturn("startup");
        assertThatThrownBy(() -> new ControleDeAcesso(List.of(regra, regra))).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Respostas 403 e 401
     * Objetivo: Garantir o Problem Details, o registro do 403 com a conta e do 401 com token, e o 401 sem
     *           token só em log.
     * Decisões: Repositório simulado.
     */
    @Test
    @SuppressWarnings("unchecked")
    void respondeERegistra() throws Exception {
        AcessoNegadoRepository repositorio = mock(AcessoNegadoRepository.class);
        ObjectProvider<AcessoNegadoRepository> provedor = mock(ObjectProvider.class);
        when(provedor.getIfAvailable()).thenReturn(repositorio);
        RespostasDeSeguranca respostas = new RespostasDeSeguranca(provedor,
                new RelogioAjustavel(Instant.parse("2026-11-24T04:50:00Z")));

        autenticar(ana);
        MockHttpServletResponse proibido = new MockHttpServletResponse();
        respostas.handle(new MockHttpServletRequest("GET", "/api/v1/investidores/eu"), proibido,
                new AccessDeniedException("x"));
        assertThat(proibido.getStatus()).isEqualTo(403);
        assertThat(proibido.getContentAsString()).contains("\"codigo\":\"acesso_negado\"");
        verify(repositorio).save(org.mockito.ArgumentMatchers.argThat(r ->
                ana.equals(r.getUsuarioId()) && r.getStatus() == 403 && r.getCaminho().equals("/api/v1/investidores/eu")));

        MockHttpServletResponse semToken = new MockHttpServletResponse();
        respostas.commence(new MockHttpServletRequest("GET", "/api/v1/conta"), semToken, new BadCredentialsException("x"));
        assertThat(semToken.getStatus()).isEqualTo(401);
        assertThat(semToken.getHeader("WWW-Authenticate")).startsWith("Bearer");
        assertThat(semToken.getContentAsString()).contains("\"codigo\":\"nao_autenticado\"");

        MockHttpServletRequest comToken = new MockHttpServletRequest("GET", "/api/v1/conta");
        comToken.addHeader("Authorization", "Bearer adulterado");
        respostas.commence(comToken, new MockHttpServletResponse(), new BadCredentialsException("x"));
        verify(repositorio).save(org.mockito.ArgumentMatchers.argThat(r -> r.getUsuarioId() == null && r.getStatus() == 401));

        when(provedor.getIfAvailable()).thenReturn(null);
        respostas.handle(new MockHttpServletRequest("GET", "/x"), new MockHttpServletResponse(), new AccessDeniedException("x"));
        when(provedor.getIfAvailable()).thenReturn(repositorio);
        when(repositorio.save(any())).thenThrow(new IllegalStateException("banco fora"));
        MockHttpServletResponse mesmoAssim = new MockHttpServletResponse();
        respostas.handle(new MockHttpServletRequest("GET", "/x"), mesmoAssim, new AccessDeniedException("x"));
        assertThat(mesmoAssim.getStatus()).isEqualTo(403);
        verify(provedor, org.mockito.Mockito.atLeastOnce()).getIfAvailable();
        verify(repositorio, never()).deleteAll();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Autenticar no contexto
     * Objetivo: Colocar no contexto um token da conta informada.
     * Decisões: JwtAuthenticationToken, como o resource server.
     */
    private static void autenticar(UUID conta) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject(conta.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
