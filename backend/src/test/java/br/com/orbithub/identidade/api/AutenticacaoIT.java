package br.com.orbithub.identidade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Teste de integração da sessão com JWT e refresh token
 * Objetivo: Verificar, com a API e o PostgreSQL reais, o cookie seguro, a rotação, a detecção de reuso e
 *           o encerramento imediato do access token.
 * Decisões: Tolerância de reuso zerada neste teste, para provar a revogação sem esperar 10 segundos. A
 *           sessão é aberta pelo serviço, porque o login pela API chega com o KAN-13.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "orbit.seguranca.sessao.tolerancia-reuso=PT0S")
class AutenticacaoIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    private Usuario ana;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conta de teste
     * Objetivo: Gravar uma conta ativa nova para cada teste.
     * Decisões: E-mail aleatório, para os testes não colidirem no índice único.
     */
    @BeforeEach
    void criarConta() {
        ana = Usuario.novo("Ana", "ana-" + UUID.randomUUID() + "@exemplo.com", "{noop}x",
                Set.of(Papel.INVESTIDOR), Instant.now());
        ana.confirmarEmail(Instant.now());
        usuarios.save(ana);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Renovar sem cookie
     * Objetivo: Garantir 401 e a ordem de apagar o cookie quando não há sessão.
     * Decisões: Confere o Max-Age=0 do cookie de remoção.
     */
    @Test
    void renovarSemCookieResponde401EApagaOCookie() throws Exception {
        mvc.perform(post("/api/v1/auth/renovar"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Renovação com cookie seguro
     * Objetivo: Garantir que renovar devolve access token no corpo e um refresh token novo em cookie
     *           HttpOnly, Secure, SameSite=Strict e restrito às rotas de autenticação.
     * Decisões: O refresh token não pode aparecer no corpo.
     */
    @Test
    void renovarDevolveAccessTokenECookieSeguro() throws Exception {
        var sessao = sessoes.abrir(ana);

        MvcResult resposta = mvc.perform(post("/api/v1/auth/renovar").cookie(cookie(sessao.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcesso").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(ana.getEmail()))
                .andExpect(jsonPath("$.usuario.papeis[0]").value("INVESTIDOR"))
                .andReturn();

        String setCookie = resposta.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("orbit_sessao=", "HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth");
        assertThat(setCookie).containsPattern("Max-Age=(1799|1800);");
        assertThat(resposta.getResponse().getContentAsString()).doesNotContain(sessao.refreshToken());
        assertThat(valorDoCookie(setCookie)).isNotEqualTo(sessao.refreshToken());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Access token protege a API
     * Objetivo: Garantir que rota protegida exige token e que um token válido passa pela autenticação.
     * Decisões: Usa uma rota inexistente: sem token é 401; com token a autenticação passa e o 404 mostra
     *           que a requisição chegou ao roteamento.
     */
    @Test
    void accessTokenValidoPassaPelaAutenticacao() throws Exception {
        var sessao = sessoes.abrir(ana);

        mvc.perform(get("/api/v1/rota-de-teste")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, "Bearer " + sessao.tokenAcesso()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, "Bearer adulterado"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Encerrar sessão derruba o access token
     * Objetivo: Garantir que, depois do logout, o access token já emitido deixa de valer na hora.
     * Decisões: Sem esperar os 15 minutos de validade do token.
     */
    @Test
    void encerrarSessaoDerrubaAccessTokenNaHora() throws Exception {
        var sessao = sessoes.abrir(ana);
        sessoes.encerrar(sessao.refreshToken());

        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, "Bearer " + sessao.tokenAcesso()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/renovar").cookie(cookie(sessao.refreshToken())))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Reuso de refresh token
     * Objetivo: Garantir que reapresentar um token já trocado revoga também o token novo.
     * Decisões: Simula um atacante usando o cookie antigo depois que o usuário já renovou.
     */
    @Test
    void reusoDeTokenTrocadoRevogaASessao() throws Exception {
        var sessao = sessoes.abrir(ana);
        MvcResult renovacao = mvc.perform(post("/api/v1/auth/renovar").cookie(cookie(sessao.refreshToken())))
                .andExpect(status().isOk()).andReturn();
        String novoRefresh = valorDoCookie(renovacao.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        String novoAcesso = JsonPath.read(renovacao.getResponse().getContentAsString(), "$.tokenAcesso");

        mvc.perform(post("/api/v1/auth/renovar").cookie(cookie(sessao.refreshToken())))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/renovar").cookie(cookie(novoRefresh)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, "Bearer " + novoAcesso))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Cookie de apoio
     * Objetivo: Montar o cookie de sessão com o valor informado.
     * Decisões: Mesmo nome usado pela API.
     */
    private static Cookie cookie(String valor) {
        return new Cookie(CookieDeSessao.NOME, valor);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Valor do Set-Cookie
     * Objetivo: Extrair o valor do cookie de um cabeçalho Set-Cookie.
     * Decisões: O valor vai até o primeiro ponto e vírgula.
     */
    private static String valorDoCookie(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }
}
