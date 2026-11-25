package br.com.orbithub.empreendimentos.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.CnpjDeTeste;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Testes de integração do cadastro da startup
 * Objetivo: Conferir, com PostgreSQL real, a criação com as recusas, a startup sem CNPJ, a lista e o detalhe só para a
 *           equipe, a edição, o logotipo e a área do empreendedor.
 * Decisões: CNPJs gerados com dígitos verificadores corretos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StartupsIT {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criar, listar e editar
     * Objetivo: Conferir as recusas, a criação sem CNPJ (empresa não constituída), o papel de fundador administrador, o
     *           CNPJ repetido, a lista, o 404 para quem é de fora e a edição com redes e fundadores.
     * Decisões: Duas empreendedoras e uma investidora.
     */
    @Test
    void criarListarEEditar() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rita = conta(Papel.EMPREENDEDOR);
        Usuario ana = conta(Papel.INVESTIDOR);
        criar(eva, "{\"nome\":\" \",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}").andExpect(jsonPath("$.codigo").value("nome_obrigatorio"));
        criar(eva, "{\"nome\":\"NovaSafra\",\"segmento\":\"ESPACIAL\",\"localizacao\":\"SP\"}").andExpect(jsonPath("$.codigo").value("opcao_indisponivel"));
        criar(eva, "{\"nome\":\"NovaSafra\",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"SP\",\"cnpj\":\"11.111.111/1111-11\"}")
                .andExpect(jsonPath("$.codigo").value("cnpj_invalido"));
        criar(eva, "{\"nome\":\"NovaSafra\",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"SP\",\"site\":\"novasafra.com\"}")
                .andExpect(jsonPath("$.codigo").value("site_invalido"));
        criar(eva, "{\"nome\":\"NovaSafra\",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"SP\",\"fundadores\":["
                + "{\"nome\":\"Eva\",\"participacao\":60},{\"nome\":\"Rui\",\"participacao\":50}]}")
                .andExpect(jsonPath("$.codigo").value("participacao_acima"));
        criar(ana, "{\"nome\":\"X\",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"SP\"}").andExpect(status().isForbidden());

        String id = JsonPath.read(criar(eva, "{\"nome\":\" NovaSafra \",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"MT\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("NovaSafra"))
                .andExpect(jsonPath("$.constituida").value(false)).andExpect(jsonPath("$.situacao").value("RASCUNHO"))
                .andExpect(jsonPath("$.meuPapel").value("FUNDADOR_ADMINISTRADOR"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/v1/startups/minhas").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].nome").value("NovaSafra")).andExpect(jsonPath("$[0].constituida").value(false));
        mvc.perform(get("/api/v1/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(rita))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/empreendimentos/meus").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.startups[0]").value(id)).andExpect(jsonPath("$.proximoPasso").value("/empreendedor"));

        String cnpj = CnpjDeTeste.novo();
        mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"NovaSafra\",\"nomeFantasia\":\"Nova Safra\",\"razaoSocial\":\"Nova Safra Tecnologia Ltda\","
                                + "\"cnpj\":\"" + cnpj + "\",\"segmento\":\"AGRONEGOCIO\",\"localizacao\":\"MT\",\"site\":\"https://novasafra.com\","
                                + "\"redes\":{\"LINKEDIN\":\"https://linkedin.com/company/novasafra\",\"INSTAGRAM\":\" \"},"
                                + "\"fundadores\":[{\"nome\":\"Eva\",\"cargo\":\"CEO\",\"participacao\":60.5},{\"nome\":\"Rui\",\"cargo\":\"CTO\",\"participacao\":39.5}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.constituida").value(true))
                .andExpect(jsonPath("$.cnpj").value(containsString("/0001-")))
                .andExpect(jsonPath("$.redes.LINKEDIN").value("https://linkedin.com/company/novasafra"))
                .andExpect(jsonPath("$.redes.INSTAGRAM").doesNotExist())
                .andExpect(jsonPath("$.fundadores[1].participacao").value(39.5));
        criar(rita, "{\"nome\":\"Outra\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"cnpj\":\"" + cnpj + "\"}")
                .andExpect(jsonPath("$.codigo").value("cnpj_em_uso"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Logotipo
     * Objetivo: Conferir o envio em PNG, a recusa de outro formato, a leitura pela equipe e o 404 para quem é de fora.
     * Decisões: PNG mínimo.
     */
    @Test
    void logotipo() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rita = conta(Papel.EMPREENDEDOR);
        String id = JsonPath.read(criar(eva, "{\"nome\":\"Logo\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/v1/startups/" + id + "/logotipo").header(HttpHeaders.AUTHORIZATION, bearer(eva))).andExpect(status().isNotFound());
        logotipo(eva, id, "GIF89a".getBytes()).andExpect(jsonPath("$.codigo").value("logotipo_formato"));
        logotipo(eva, id, PNG).andExpect(status().isOk()).andExpect(jsonPath("$.temLogotipo").value(true));
        logotipo(eva, id, PNG).andExpect(status().isOk());
        mvc.perform(get("/api/v1/startups/" + id + "/logotipo").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        mvc.perform(get("/api/v1/startups/" + id + "/logotipo").header(HttpHeaders.AUTHORIZATION, bearer(rita))).andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criar pela API
     * Objetivo: Enviar o cadastro de uma startup.
     * Decisões: Corpo em JSON.
     */
    private ResultActions criar(Usuario quem, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Logotipo pela API
     * Objetivo: Enviar um arquivo de logotipo.
     * Decisões: Multipart com PUT.
     */
    private ResultActions logotipo(Usuario quem, String id, byte[] conteudo) throws Exception {
        return mvc.perform(multipart(HttpMethod.PUT, "/api/v1/startups/" + id + "/logotipo")
                .file(new MockMultipartFile("arquivo", "logo", MediaType.APPLICATION_OCTET_STREAM_VALUE, conteudo))
                .header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Conta de teste
     * Objetivo: Criar uma conta ativa com o papel informado e os termos aceitos.
     * Decisões: E-mail único.
     */
    private Usuario conta(Papel papel) {
        Usuario u = Usuario.novo("Pessoa", "s-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
