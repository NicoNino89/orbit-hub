package br.com.orbithub.administracao.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.identidade.api.CookieDeSessao;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Teste de integração de termos, políticas e aceites
 * Objetivo: Verificar, com API e banco reais, a leitura pública, o bloqueio até o aceite, o registro do
 *           aceite e a publicação de versão nova pelo administrador.
 * Decisões: Cada teste cria as próprias contas; a publicação de versão nova roda num tipo informativo
 *           (Política de Moderação) para não afetar os outros testes que dependem dos Termos de Uso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TermosIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Leitura pública
     * Objetivo: Garantir que qualquer visitante lê os documentos vigentes e o histórico.
     * Decisões: Os seis textos provisórios da migração estão em vigor.
     */
    @Test
    void visitanteLeDocumentosVigentesEHistorico() throws Exception {
        mvc.perform(get("/api/v1/documentos-legais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[0].tipo").value("TERMOS_DE_USO"))
                .andExpect(jsonPath("$[0].aceite").value("TODOS_NO_ACESSO"));
        mvc.perform(get("/api/v1/documentos-legais/POLITICA_DE_PRIVACIDADE/versoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versao").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Bloqueio até o aceite
     * Objetivo: Garantir que, sem aceite, a conta é barrada; depois de aceitar e renovar, segue normalmente.
     * Decisões: A rota de histórico de aceites é liberada; uma rota protegida qualquer é barrada com 403.
     */
    @Test
    void contaSemAceiteFicaBloqueadaAteAceitar() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR);
        var sessao = sessoes.abrir(ana);
        String bearer = "Bearer " + sessao.tokenAcesso();

        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("aceite_pendente"));

        String pendentes = mvc.perform(get("/api/v1/conta/aceites/pendentes").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(pendentes, "$[*].id");

        mvc.perform(post("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentos\":[\"" + String.join("\",\"", ids) + "\"]}"))
                .andExpect(status().isNoContent());

        String renovada = mvc.perform(post("/api/v1/auth/renovar")
                        .cookie(new Cookie(CookieDeSessao.NOME, sessao.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.termosPendentes").value(false))
                .andReturn().getResponse().getContentAsString();
        String novoBearer = "Bearer " + JsonPath.read(renovada, "$.tokenAcesso");

        mvc.perform(get("/api/v1/rota-de-teste").header(HttpHeaders.AUTHORIZATION, novoBearer))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, novoBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Aceite de versão inválida
     * Objetivo: Garantir 422 para id que não é versão vigente e 400 para pedido vazio.
     * Decisões: Id aleatório.
     */
    @Test
    void aceiteInvalidoEhRecusado() throws Exception {
        String bearer = "Bearer " + sessoes.abrir(conta(Papel.EMPREENDEDOR)).tokenAcesso();
        mvc.perform(post("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentos\":[\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("versao_nao_vigente"));
        mvc.perform(post("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"documentos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documentos").exists());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Publicação só pelo administrador
     * Objetivo: Garantir que só ADMIN publica e que a versão nova aparece como vigente.
     * Decisões: Usuário comum recebe 403; o administrador publica a próxima versão da Política de Moderação.
     */
    @Test
    void soAdministradorPublicaVersaoNova() throws Exception {
        String corpo = "{\"tipo\":\"POLITICA_DE_MODERACAO\",\"titulo\":\"Política de Moderação\","
                + "\"conteudo\":\"Texto revisado.\"}";
        String comum = "Bearer " + sessoes.abrir(conta(Papel.EMPREENDEDOR)).tokenAcesso();
        mvc.perform(post("/api/v1/admin/documentos-legais").header(HttpHeaders.AUTHORIZATION, comum)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        Usuario admin = conta(Papel.ADMIN);
        String bearerAdmin = "Bearer " + sessoes.abrir(admin).tokenAcesso();
        mvc.perform(post("/api/v1/conta/aceites").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(aceitarVigentes()));
        bearerAdmin = "Bearer " + sessoes.abrir(admin).tokenAcesso();

        mvc.perform(post("/api/v1/admin/documentos-legais").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versao").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        mvc.perform(get("/api/v1/documentos-legais/POLITICA_DE_MODERACAO/versoes"))
                .andExpect(jsonPath("$[0].conteudo").value("Texto revisado."))
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Corpo para aceitar os vigentes
     * Objetivo: Montar o pedido de aceite dos Termos de Uso e da Política de Privacidade vigentes.
     * Decisões: Lê os ids pela própria API pública.
     */
    private String aceitarVigentes() throws Exception {
        String vigentes = mvc.perform(get("/api/v1/documentos-legais")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(vigentes, "$[?(@.aceite == 'TODOS_NO_ACESSO')].id");
        return "{\"documentos\":[\"" + String.join("\",\"", ids) + "\"]}";
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Conta de apoio
     * Objetivo: Gravar uma conta ativa com o papel informado.
     * Decisões: E-mail aleatório.
     */
    private Usuario conta(Papel papel) {
        Usuario usuario = Usuario.novo("Pessoa", "p-" + UUID.randomUUID() + "@exemplo.com", "{noop}x",
                Set.of(papel), Instant.now());
        usuario.confirmarEmail(Instant.now());
        return usuarios.save(usuario);
    }
}
