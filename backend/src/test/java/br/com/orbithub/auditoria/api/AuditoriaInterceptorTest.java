package br.com.orbithub.auditoria.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.auditoria.service.TrilhaDeAuditoria;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:30
 * Cartão Jira: KAN-80
 * Título: Testes do registro automático
 * Objetivo: Garantir que cada requisição do backoffice vira um registro com o resultado certo, usando a
 *           descrição do serviço quando existe.
 * Decisões: Requisição e token simulados; a trilha é mock.
 */
class AuditoriaInterceptorTest {

    static final UUID ADMIN = UUID.randomUUID();

    TrilhaDeAuditoria trilha;
    ObjectProvider<TrilhaDeAuditoria> fornecedor;
    AuditoriaInterceptor interceptor;
    MockHttpServletRequest requisicao;
    MockHttpServletResponse resposta;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Preparação
     * Objetivo: Montar uma requisição de backoffice com um administrador autenticado.
     * Decisões: A requisição é publicada no RequestContextHolder, como o Spring faz.
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        trilha = mock(TrilhaDeAuditoria.class);
        fornecedor = mock(ObjectProvider.class);
        when(fornecedor.getIfAvailable()).thenReturn(trilha);
        interceptor = new AuditoriaInterceptor(fornecedor);
        requisicao = new MockHttpServletRequest("POST", "/api/v1/admin/documentos-legais/abc/versoes");
        requisicao.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                "/api/v1/admin/documentos-legais/{tipo}/versoes");
        requisicao.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("tipo", "abc"));
        resposta = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
        Jwt token = Jwt.withTokenValue("t").header("alg", "HS256").subject(ADMIN.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Limpeza
     * Objetivo: Não deixar requisição nem autenticação para o próximo teste.
     * Decisões: Limpa os dois contextos.
     */
    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Ação sem descrição
     * Objetivo: Conferir que a ação vira "MÉTODO rota" e o objeto vem da variável do caminho.
     * Decisões: Status 200 é sucesso.
     */
    @Test
    void registraMetodoERotaQuandoOServicoNaoDescreve() {
        resposta.setStatus(200);
        interceptor.afterCompletion(requisicao, resposta, null, null);

        Acao acao = capturar();
        assertThat(acao.atorId()).isEqualTo(ADMIN);
        assertThat(acao.acao()).isEqualTo("POST /api/v1/admin/documentos-legais/{tipo}/versoes");
        assertThat(acao.objetoId()).isEqualTo("abc");
        assertThat(acao.resultado()).isEqualTo(Resultado.SUCESSO);
        assertThat(acao.statusHttp()).isEqualTo(200);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Ação descrita e recusada
     * Objetivo: Conferir que a descrição do serviço é usada e que 4xx vira falha.
     * Decisões: A justificativa informada acompanha o registro.
     */
    @Test
    void usaADescricaoDoServicoERegistraFalha() {
        ContextoDeAuditoria.descrever("PARAMETRO_ALTERADO", "parametro", "conexao.expiracao_dias", "30 → 45");
        ContextoDeAuditoria.justificar("Ajuste pedido pelo time de atendimento");
        resposta.setStatus(422);
        interceptor.afterCompletion(requisicao, resposta, null, null);

        Acao acao = capturar();
        assertThat(acao.acao()).isEqualTo("PARAMETRO_ALTERADO");
        assertThat(acao.objetoTipo()).isEqualTo("parametro");
        assertThat(acao.objetoId()).isEqualTo("conexao.expiracao_dias");
        assertThat(acao.detalhe()).isEqualTo("30 → 45");
        assertThat(acao.justificativa()).isEqualTo("Ajuste pedido pelo time de atendimento");
        assertThat(acao.resultado()).isEqualTo(Resultado.FALHA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Exceção é falha
     * Objetivo: Conferir que uma exceção não tratada vira falha mesmo com status 200.
     * Decisões: O status ainda não foi trocado quando a exceção escapa.
     */
    @Test
    void excecaoNaoTratadaViraFalha() {
        resposta.setStatus(200);
        interceptor.afterCompletion(requisicao, resposta, null, new IllegalStateException("x"));
        assertThat(capturar().resultado()).isEqualTo(Resultado.FALHA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Sem conta, sem trilha ou já registrada
     * Objetivo: Não gravar sem conta autenticada, sem trilha no contexto, nem duas vezes a ação que o serviço
     *           já gravou.
     * Decisões: O acesso negado sem conta tem registro próprio (KAN-34).
     */
    @Test
    void naoGravaSemContaNemDuasVezes() {
        when(fornecedor.getIfAvailable()).thenReturn(null);
        interceptor.afterCompletion(requisicao, resposta, null, null);
        when(fornecedor.getIfAvailable()).thenReturn(trilha);

        ContextoDeAuditoria.marcarRegistrada();
        interceptor.afterCompletion(requisicao, resposta, null, null);
        verify(trilha, never()).registrar(any());

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        SecurityContextHolder.clearContext();
        interceptor.afterCompletion(requisicao, resposta, null, null);
        verifyNoInteractions(trilha);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Falha ao gravar não derruba a resposta
     * Objetivo: Conferir que um erro na gravação só vai para o log.
     * Decisões: O log de erro alimenta o alerta de erros do monitoramento.
     */
    @Test
    void falhaAoGravarNaoPropaga() {
        when(trilha.registrar(any())).thenThrow(new IllegalStateException("banco fora"));
        interceptor.afterCompletion(requisicao, resposta, null, null);
        verify(trilha).registrar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Captura da ação gravada
     * Objetivo: Ler a ação enviada à trilha.
     * Decisões: Exige exatamente uma gravação.
     */
    private Acao capturar() {
        ArgumentCaptor<Acao> acao = ArgumentCaptor.forClass(Acao.class);
        verify(trilha).registrar(acao.capture());
        return acao.getValue();
    }
}
