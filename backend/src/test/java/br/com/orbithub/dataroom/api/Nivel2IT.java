package br.com.orbithub.dataroom.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.rotinas.ExecutorDeRotinas;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-145, KAN-105
 * Título: Testes de integração do Nível 2 e do Data Room
 * Objetivo: Percorrer Data Room (envio, formato, versão), liberação com validade, leitura com histórico, link temporário,
 *           nova versão do termo, revogação, fim da conexão, bloqueio, NDA bilateral e as rotinas R3 e R4.
 * Decisões: Startups publicadas, verificações e prazos ajustados direto no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class Nivel2IT {

    static final byte[] PDF = "%PDF-1.4 conteudo de teste".getBytes();

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    DocumentoLegalService documentosLegais;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ExecutorDeRotinas rotinas;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-145, KAN-63
     * Título: Liberação, leitura e revogação
     * Objetivo: Enviar documentos, liberar, ver o Nível 2, baixar pelo link, exigir novo aceite do termo, revogar e
     *           encerrar pelo fim da conexão.
     * Decisões: Investidor com o termo aceito no cadastro.
     */
    @Test
    void liberacaoLeituraERevogacao() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String startup = publicada(eva);
        String conexao = match(eva, leo, startup);

        documento(eva, startup, "FINANCEIRO", "Balanço", "x.pdf", "%PDF-1".getBytes()).andExpect(status().isCreated());
        documento(eva, startup, "FINANCEIRO", "Planilha", "x.exe", "MZ123".getBytes()).andExpect(jsonPath("$.codigo").value("documento_formato"));
        String doc = JsonPath.read(documento(eva, startup, "PITCH_DECK", "Pitch", "p.pdf", PDF).andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(multipart("/api/v1/startups/" + startup + "/dataroom/" + doc + "/substituicao")
                        .file(new MockMultipartFile("arquivo", "p2.pdf", "application/pdf", PDF)).header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.versao").value(2));
        pedir(get("/api/v1/startups/" + startup + "/dataroom"), eva).andExpect(jsonPath("$.length()").value(3));

        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2/situacao"), leo).andExpect(jsonPath("$.temAcesso").value(false))
                .andExpect(jsonPath("$.termoAceito").value(true));
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(status().isForbidden());
        pedir(get("/api/v1/startups/" + startup + "/nivel2"), eva).andExpect(jsonPath("$.conexoes[0].verificado").value(true))
                .andExpect(jsonPath("$.conexoes[0].falta").doesNotExist());

        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), leo, "{\"conexaoId\":\"" + conexao + "\"}").andExpect(status().isForbidden());
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\",\"dias\":3}")
                .andExpect(jsonPath("$.codigo").value("prazo_invalido"));
        String acesso = JsonPath.read(corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\",\"dias\":30}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\"}")
                .andExpect(jsonPath("$.codigo").value("acesso_ativo"));
        assertThat(contar(leo, "Informações confidenciais liberadas")).isEqualTo(1);

        String corpoNivel2 = pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(status().isOk())
                .andExpect(jsonPath("$.documentos.length()").value(2)).andExpect(jsonPath("$.informacoes.modo").value("NIVEL2"))
                .andReturn().getResponse().getContentAsString();
        assertThat(corpoNivel2).contains("Mensalidades").doesNotContain("Frete caro").contains("40% do capital");
        pedir(post("/api/v1/catalogo/startups/" + startup + "/nivel2/documentos/" + doc + "/link"), leo).andExpect(status().isNotFound());
        String atual = jdbc.queryForObject("SELECT id::text FROM dataroom_documento WHERE startup_id = ?::uuid AND nome = 'Pitch' AND atual",
                String.class, startup);
        String link = JsonPath.read(pedir(post("/api/v1/catalogo/startups/" + startup + "/nivel2/documentos/" + atual + "/link"), leo)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        pedir(get("/api/v1/dataroom/links/" + link), leo).andExpect(status().isOk()).andExpect(content().bytes(PDF))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"));
        Usuario outro = conta(Papel.INVESTIDOR, "Outro");
        pedir(get("/api/v1/dataroom/links/" + link), outro).andExpect(status().isNotFound());

        documentosLegais.publicar(TipoDocumento.TERMO_DE_CONFIDENCIALIDADE, "Termo de Confidencialidade da Plataforma",
                "[TEXTO PROVISÓRIO] versão de teste", null, null);
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(status().isForbidden());
        aceites.aceitar(leo.getId(), aceites.pendentes(leo.getId(), true).stream().map(DocumentoLegal::getId).toList());
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(status().isOk());

        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos/" + acesso + "/renovacao"), eva, "{\"dias\":60}").andExpect(status().isOk());
        pedir(post("/api/v1/startups/" + startup + "/nivel2/acessos/" + acesso + "/revogacao"), eva).andExpect(status().isNoContent());
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(status().isForbidden());
        pedir(get("/api/v1/startups/" + startup + "/nivel2/historico"), eva)
                .andExpect(jsonPath("$[0].tipo").value("REVOGACAO")).andExpect(jsonPath("$[?(@.tipo == 'DOWNLOAD')].documento").value("Pitch"))
                .andExpect(jsonPath("$[?(@.tipo == 'VISUALIZACAO')]").isNotEmpty()).andExpect(jsonPath("$[?(@.tipo == 'RENOVACAO')]").isNotEmpty());

        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\"}").andExpect(status().isCreated());
        corpo(post("/api/v1/conexoes/" + conexao + "/encerramento"), leo, "{\"motivo\":\"OUTRO\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM acesso_nivel2 WHERE conexao_id = ?::uuid AND encerrado_motivo = 'CONEXAO_FINAL'",
                Integer.class, conexao)).isEqualTo(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-63, KAN-30
     * Título: NDA bilateral
     * Objetivo: Exigir o NDA enviado e confirmado pelas duas partes antes de liberar.
     * Decisões: O investidor baixa o NDA antes de confirmar.
     */
    @Test
    void ndaBilateral() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String startup = publicada(eva);
        String conexao = match(eva, leo, startup);
        corpo(put("/api/v1/startups/" + startup + "/nivel2/regra"), eva, "{\"regra\":\"NDA_BILATERAL\"}").andExpect(jsonPath("$.regra").value("NDA_BILATERAL"));
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\"}")
                .andExpect(jsonPath("$.codigo").value("requisito_pendente"));
        pedir(post("/api/v1/conexoes/" + conexao + "/nda"), leo).andExpect(jsonPath("$.codigo").value("sem_nda"));
        documento(eva, startup, "NDA", "NDA padrão", "nda.pdf", PDF).andExpect(status().isCreated());
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nda"), leo).andExpect(status().isOk()).andExpect(content().bytes(PDF));
        pedir(post("/api/v1/conexoes/" + conexao + "/nda"), leo).andExpect(status().isNoContent());
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\"}")
                .andExpect(jsonPath("$.codigo").value("requisito_pendente"));
        pedir(post("/api/v1/conexoes/" + conexao + "/nda"), eva).andExpect(status().isNoContent());
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + conexao + "\"}").andExpect(status().isCreated());
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), leo).andExpect(jsonPath("$.documentos.length()").value(0));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 12:04
     * Cartão Jira: KAN-30, KAN-67, KAN-105
     * Título: Bloqueio e rotinas
     * Objetivo: Encerrar o acesso no bloqueio, avisar o vencimento (R4) e encerrar o vencido (R3).
     * Decisões: Prazos recuados no banco.
     */
    @Test
    void bloqueioERotinas() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String startup = publicada(eva);
        String c1 = match(eva, leo, startup);
        String c2 = match(eva, ana, startup);
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + c1 + "\"}").andExpect(status().isCreated());
        corpo(post("/api/v1/startups/" + startup + "/nivel2/acessos"), eva, "{\"conexaoId\":\"" + c2 + "\"}").andExpect(status().isCreated());
        corpo(post("/api/v1/startups/" + startup + "/bloqueios"), eva, "{\"tipo\":\"PESSOA\",\"identificador\":\"" + leo.getEmail() + "\"}")
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT encerrado_motivo FROM acesso_nivel2 WHERE conexao_id = ?::uuid", String.class, c1)).isEqualTo("BLOQUEIO");

        jdbc.update("UPDATE acesso_nivel2 SET valido_ate = now() + interval '3 days' WHERE conexao_id = ?::uuid", c2);
        rotinas.executarManual("R4", null);
        rotinas.executarManual("R4", null);
        assertThat(contar(ana, "Acesso confidencial perto de vencer")).isEqualTo(1);
        jdbc.update("UPDATE acesso_nivel2 SET valido_ate = now() - interval '1 minute' WHERE conexao_id = ?::uuid", c2);
        rotinas.executarManual("R3", null);
        assertThat(jdbc.queryForObject("SELECT encerrado_motivo FROM acesso_nivel2 WHERE conexao_id = ?::uuid", String.class, c2)).isEqualTo("VENCIDO");
        assertThat(contar(ana, "Acesso confidencial vencido")).isEqualTo(1);
        pedir(get("/api/v1/catalogo/startups/" + startup + "/nivel2"), ana).andExpect(status().isForbidden());
    }

    private String match(Usuario eva, Usuario investidor, String startup) throws Exception {
        verificar(investidor);
        String c = JsonPath.read(corpo(post("/api/v1/catalogo/startups/" + startup + "/interesse"), investidor, "{}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        pedir(post("/api/v1/conexoes/" + c + "/aceite"), eva).andExpect(status().isOk());
        return c;
    }

    private ResultActions documento(Usuario quem, String startup, String categoria, String nome, String arquivo, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/v1/startups/" + startup + "/dataroom").file(new MockMultipartFile("arquivo", arquivo, "application/octet-stream", bytes))
                .param("categoria", categoria).param("nome", nome).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String publicada(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sigilosa\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"fundadores\":[{\"nome\":\"Eva\",\"participacao\":40}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");
        mvc.perform(put("/api/v1/startups/" + id + "/secoes/PROBLEMA_E_SOLUCAO").header(HttpHeaders.AUTHORIZATION, bearer(dono))
                .contentType(MediaType.APPLICATION_JSON).content("{\"PROBLEMA\":\"Frete caro\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/startups/" + id + "/secoes/MODELO_DE_NEGOCIO").header(HttpHeaders.AUTHORIZATION, bearer(dono))
                .contentType(MediaType.APPLICATION_JSON).content("{\"FONTES_FATURAMENTO\":\"Mensalidades\"}")).andExpect(status().isOk());
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), nome_aprovado = nome, segmento_aprovado = segmento, "
                + "estagio_aprovado = 'MVP' WHERE id = ?::uuid", id);
        return id;
    }

    private void verificar(Usuario u) {
        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_em, valido_ate) "
                + "VALUES (?, 'PESSOA', ?, 'VERIFICADO', ?, now(), now(), now(), now() + interval '1 year')", UUID.randomUUID(), u.getId(), u.getId());
    }

    private int contar(Usuario quem, String titulo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE ?", Integer.class, quem.getId(), titulo);
    }

    private Usuario conta(Papel papel, String nome) {
        Usuario u = Usuario.novo(nome, "d-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
