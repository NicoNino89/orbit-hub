package br.com.orbithub.busca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-167, KAN-170, KAN-171
 * Título: Testes de integração da página inicial
 * Objetivo: Conferir a busca global (sem acentos e sem startups bloqueadas), os banners (vigentes e administração), a
 *           vitrine da startup (chamada e capa).
 * Decisões: Banco real (Testcontainers); imagem PNG mínima.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InicioIT {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170
     * Título: Busca e vitrine
     * Objetivo: Achar a startup pelo nome sem acento, esconder a bloqueada e manter a capa e a chamada.
     * Decisões: Colaborador não muda a vitrine.
     */
    @Test
    void buscaEVitrine() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        Usuario col = conta(Papel.EMPREENDEDOR, "Col");
        String id = startup(eva, "Água Viva " + UUID.randomUUID().toString().substring(0, 4));
        pedir(get("/api/v1/catalogo/startups/" + id + "/capa"), leo).andExpect(status().isNotFound());
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), nome_aprovado = nome, segmento_aprovado = segmento, "
                + "estagio_aprovado = 'MVP' WHERE id = ?::uuid", id);
        jdbc.update("INSERT INTO startup_membro (id, startup_id, usuario_id, papel, entrou_em) VALUES (?, ?::uuid, ?, 'COLABORADOR', now())",
                UUID.randomUUID(), id, col.getId());

        pedir(get("/api/v1/busca?q=a"), leo).andExpect(jsonPath("$.startups.length()").value(0));
        pedir(get("/api/v1/busca?q=agua viva"), leo).andExpect(jsonPath("$.startups[?(@.id == '" + id + "')].detalhe").isNotEmpty());
        pedir(get("/api/v1/busca?q=match"), leo).andExpect(jsonPath("$.artigos[0].nome").value("O que é o Match"));
        corpo(post("/api/v1/startups/" + id + "/bloqueios"), eva, "{\"tipo\":\"PESSOA\",\"identificador\":\"" + leo.getEmail() + "\"}");
        pedir(get("/api/v1/busca?q=agua viva"), leo).andExpect(jsonPath("$.startups[?(@.id == '" + id + "')]").isEmpty());

        corpo(put("/api/v1/startups/" + id + "/vitrine"), col, "{\"chamada\":\"x\"}").andExpect(status().isForbidden());
        corpo(put("/api/v1/startups/" + id + "/vitrine"), eva, "{\"chamada\":\"" + "a".repeat(121) + "\"}").andExpect(jsonPath("$.codigo").value("chamada_longa"));
        corpo(put("/api/v1/startups/" + id + "/vitrine"), eva, "{\"chamada\":\"Água limpa para o campo\"}").andExpect(jsonPath("$.chamada").value("Água limpa para o campo"));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/startups/" + id + "/capa").file(new MockMultipartFile("arquivo", "c.txt", "text/plain", "oi".getBytes()))
                .header(HttpHeaders.AUTHORIZATION, bearer(eva))).andExpect(jsonPath("$.codigo").value("capa_formato"));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/startups/" + id + "/capa").file(new MockMultipartFile("arquivo", "c.png", "image/png", PNG))
                .header(HttpHeaders.AUTHORIZATION, bearer(eva))).andExpect(jsonPath("$.temCapa").value(true));
        pedir(get("/api/v1/startups/" + id + "/vitrine"), eva).andExpect(jsonPath("$.chamada").value("Água limpa para o campo"));
        pedir(get("/api/v1/startups/" + id + "/capa"), eva).andExpect(status().isOk());
        pedir(get("/api/v1/catalogo/startups/" + id + "/capa"), eva).andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-171
     * Título: Banners
     * Objetivo: Criar, validar, enviar imagem e listar os vigentes por papel.
     * Decisões: Só administrador cadastra.
     */
    @Test
    void banners() throws Exception {
        Usuario adm = interna();
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String corpo = "{\"tipo\":\"CHAMADA\",\"titulo\":\"Só empreendedores\",\"publico\":\"EMPREENDEDOR\",\"inicio\":\"2026-01-01\",\"ordem\":9,\"link\":\"%s\"}";
        corpo(post("/api/v1/admin/banners"), leo, String.format(corpo, "/x")).andExpect(status().isForbidden());
        corpo(post("/api/v1/admin/banners"), adm, String.format(corpo, "http://x")).andExpect(jsonPath("$.codigo").value("link_invalido"));
        corpo(post("/api/v1/admin/banners"), adm, "{\"tipo\":\"X\"}").andExpect(jsonPath("$.codigo").value("banner_invalido"));
        String id = JsonPath.read(corpo(post("/api/v1/admin/banners"), adm, String.format(corpo, "/empreendedor")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/banners/" + id + "/imagem").file(new MockMultipartFile("arquivo", "b.png", "image/png", PNG))
                .header(HttpHeaders.AUTHORIZATION, bearer(adm))).andExpect(jsonPath("$.codigo").value("texto_alt_obrigatorio"));
        corpo(put("/api/v1/admin/banners/" + id), adm, String.format(corpo.replace("\"ordem\":9", "\"ordem\":9,\"textoAlt\":\"Foguete\""), "/empreendedor"))
                .andExpect(jsonPath("$.textoAlt").value("Foguete"));
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/banners/" + id + "/imagem").file(new MockMultipartFile("arquivo", "b.png", "image/png", PNG))
                .header(HttpHeaders.AUTHORIZATION, bearer(adm))).andExpect(jsonPath("$.temImagem").value(true));
        pedir(get("/api/v1/inicio/banners/" + id + "/imagem"), leo).andExpect(status().isOk());
        pedir(get("/api/v1/inicio/banners?papel=INVESTIDOR"), leo).andExpect(jsonPath("$[?(@.id == '" + id + "')]").isEmpty())
                .andExpect(jsonPath("$[?(@.tipo == 'BANNER')]").isNotEmpty());
        pedir(get("/api/v1/inicio/banners?papel=EMPREENDEDOR"), leo).andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());
        pedir(get("/api/v1/admin/banners"), adm).andExpect(jsonPath("$[?(@.id == '" + id + "')]").isNotEmpty());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: startup
     * Objetivo: Rotina auxiliar startup.
     * Decisões: Ver a documentação da classe.
     */
    private String startup(Usuario dono, String nome) throws Exception {
        return JsonPath.read(corpo(post("/api/v1/startups"), dono, "{\"nome\":\"" + nome + "\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: pedir
     * Objetivo: Rotina auxiliar pedir.
     * Decisões: Ver a documentação da classe.
     */
    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: corpo
     * Objetivo: Rotina auxiliar corpo.
     * Decisões: Ver a documentação da classe.
     */
    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: conta
     * Objetivo: Rotina auxiliar conta.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario conta(Papel papel, String nome) {
        Usuario u = Usuario.novo(nome, "s-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: interna
     * Objetivo: Rotina auxiliar interna.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario interna() {
        Usuario u = Usuario.novoInterno("Chefe", "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(Papel.ADMIN), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: bearer
     * Objetivo: Rotina auxiliar bearer.
     * Decisões: Ver a documentação da classe.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
