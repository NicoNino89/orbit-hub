package br.com.orbithub.identidade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Teste de integração do login e do logout
 * Objetivo: Verificar, com API e banco reais, o login, as recusas genéricas, o bloqueio e o logout imediato.
 * Decisões: Cada teste usa um e-mail novo, para os bloqueios não se misturarem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LoginIT {

    private static final String SENHA = "uma-senha-bem-longa";

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder senhas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Login e logout
     * Objetivo: Garantir que o login devolve token e cookie e que o logout derruba os dois na hora.
     * Decisões: Depois do logout, nem o cookie renova nem o access token passa.
     */
    @Test
    void entraESai() throws Exception {
        String email = contaAtiva();
        MvcResult login = mvc.perform(entrar(email, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcesso").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.sessaoExpiraEm").isNotEmpty())
                .andReturn();
        String setCookie = login.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly", "SameSite=Strict");
        String refresh = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        String bearer = "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.tokenAcesso");

        MvcResult saida = mvc.perform(post("/api/v1/auth/sair").cookie(new Cookie(CookieDeSessao.NOME, refresh)))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(saida.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        mvc.perform(post("/api/v1/auth/renovar").cookie(new Cookie(CookieDeSessao.NOME, refresh)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Recusa genérica
     * Objetivo: Garantir a mesma resposta para senha errada e e-mail inexistente.
     * Decisões: Compara status, código e mensagem.
     */
    @Test
    void recusaGenerica() throws Exception {
        String email = contaAtiva();
        String senhaErrada = mvc.perform(entrar(email, "senha-errada-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("credenciais_invalidas"))
                .andReturn().getResponse().getContentAsString();
        String inexistente = mvc.perform(entrar("ninguem-" + UUID.randomUUID() + "@exemplo.com", "senha-errada-123"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(senhaErrada).isEqualTo(inexistente);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Bloqueio depois de 5 falhas
     * Objetivo: Garantir que, depois de 5 falhas seguidas, nem a senha certa entra (429).
     * Decisões: Limite padrão da aplicação.
     */
    @Test
    void bloqueiaDepoisDeCincoFalhas() throws Exception {
        String email = contaAtiva();
        for (int i = 0; i < 4; i++) {
            mvc.perform(entrar(email, "errada-" + i + "-xxxxxx")).andExpect(status().isUnauthorized());
        }
        mvc.perform(entrar(email, "errada-final-xxxx")).andExpect(status().isTooManyRequests());
        mvc.perform(entrar(email, SENHA))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("login_bloqueado"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Conta não confirmada
     * Objetivo: Garantir 403 "conta_pendente" com a senha certa de conta não confirmada.
     * Decisões: Conta gravada sem confirmar o e-mail.
     */
    @Test
    void contaNaoConfirmada() throws Exception {
        String email = "p-" + UUID.randomUUID() + "@exemplo.com";
        usuarios.save(Usuario.novo("Pessoa", email, senhas.encode(SENHA), Set.of(Papel.EMPREENDEDOR), Instant.now()));
        mvc.perform(entrar(email, SENHA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("conta_pendente"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Pedido de login
     * Objetivo: Montar o POST de login.
     * Decisões: JSON simples.
     */
    private static org.springframework.test.web.servlet.RequestBuilder entrar(String email, String senha) {
        return post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Conta ativa de apoio
     * Objetivo: Gravar uma conta ativa com a senha de teste e devolver o e-mail.
     * Decisões: Senha codificada pelo codificador real da aplicação.
     */
    private String contaAtiva() {
        String email = "a-" + UUID.randomUUID() + "@exemplo.com";
        Usuario usuario = Usuario.novo("Ana", email, senhas.encode(SENHA), Set.of(Papel.INVESTIDOR), Instant.now());
        usuario.confirmarEmail(Instant.now());
        usuarios.save(usuario);
        return email;
    }
}
