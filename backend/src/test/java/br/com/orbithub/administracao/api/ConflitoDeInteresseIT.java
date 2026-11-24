package br.com.orbithub.administracao.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.CnpjDeTeste;
import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Testes de integração do conflito de interesse
 * Objetivo: Conferir, com PostgreSQL real, a declaração de vínculos, o bloqueio de assumir e de decidir caso com
 *           vínculo (pessoa e organização), o impedimento declarado e a visão do administrador na revisão de acessos.
 * Decisões: Pedidos de verificação reais, pela API do investidor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConflitoDeInteresseIT {

    static final byte[] PDF = "%PDF-1.4 documento".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Declarar vínculos
     * Objetivo: Conferir as recusas (CNPJ, e-mail, descrição), a lista própria, a remoção só do próprio e a lista da
     *           equipe só para o administrador, com o nome do membro.
     * Decisões: Analista e administradora.
     */
    @Test
    void declararVinculos() throws Exception {
        Usuario lia = interna(Papel.ANALISTA_VERIFICACAO, "Lia");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        vincular(lia, "ORGANIZACAO", "12.345.678/0001-00", "Sócia").andExpect(jsonPath("$.codigo").value("vinculo_cnpj"));
        vincular(lia, "PESSOA", "sem-arroba", "Irmã").andExpect(jsonPath("$.codigo").value("vinculo_email"));
        vincular(lia, "STARTUP", CnpjDeTeste.novo(), " ").andExpect(jsonPath("$.codigo").value("vinculo_descricao"));
        String id = JsonPath.read(vincular(lia, "STARTUP", CnpjDeTeste.novo(), "Investi na rodada seed").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/v1/admin/vinculos/meus").header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].alvo").value(containsString("/0001-")));
        mvc.perform(get("/api/v1/admin/vinculos").header(HttpHeaders.AUTHORIZATION, bearer(lia))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/vinculos").header(HttpHeaders.AUTHORIZATION, bearer(chefe)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + id + "')].membro").value("Lia"));
        mvc.perform(delete("/api/v1/admin/vinculos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(chefe))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/admin/vinculos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(lia))).andExpect(status().isNoContent());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Bloqueio por vínculo com pessoa e organização
     * Objetivo: Conferir que quem tem vínculo vê o conflito na fila e não assume; que o vínculo pelo CNPJ da organização
     *           também bloqueia; que outra analista assume; e que o administrador com vínculo não decide.
     * Decisões: Vínculo de pessoa pelo e-mail da investidora.
     */
    @Test
    void bloqueioPorVinculo() throws Exception {
        Usuario ana = investidora();
        Usuario lia = interna(Papel.ANALISTA_VERIFICACAO, "Lia");
        Usuario rui = interna(Papel.ANALISTA_VERIFICACAO, "Rui");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        vincular(lia, "PESSOA", ana.getEmail().toUpperCase(), "Prima").andExpect(status().isCreated());
        vincular(chefe, "PESSOA", ana.getEmail(), "Ex-sócia").andExpect(status().isCreated());
        String pedido = pedirVerificacao(ana, null);

        mvc.perform(get("/api/v1/admin/verificacoes").header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(jsonPath("$[?(@.id == '" + pedido + "')].conflitoComigo").value(true));
        mvc.perform(get("/api/v1/admin/verificacoes").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$[?(@.id == '" + pedido + "')].conflitoComigo").value(false));
        assumir(lia, pedido).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("conflito_de_interesse"));
        decidir(chefe, pedido).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("conflito_de_interesse"));
        assumir(rui, pedido).andExpect(status().isOk());

        String cnpj = CnpjDeTeste.novo();
        Usuario bia = investidora();
        String org = JsonPath.read(mvc.perform(post("/api/v1/organizacoes").header(HttpHeaders.AUTHORIZATION, bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rede Conflito\",\"tipo\":\"REDE_DE_ANJOS\",\"cnpj\":\"" + cnpj + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        vincular(rui, "ORGANIZACAO", cnpj, "Sou membro da rede").andExpect(status().isCreated());
        String pedidoOrg = pedirVerificacao(bia, org);
        assumir(rui, pedidoOrg).andExpect(jsonPath("$.codigo").value("conflito_de_interesse"));
        assumir(lia, pedidoOrg).andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Impedimento
     * Objetivo: Conferir que o impedimento exige motivo, devolve o pedido à fila e impede assumir de novo.
     * Decisões: Analista sem vínculos.
     */
    @Test
    void impedimento() throws Exception {
        Usuario ana = investidora();
        Usuario rui = interna(Papel.ANALISTA_VERIFICACAO, "Rui");
        String pedido = pedirVerificacao(ana, null);
        assumir(rui, pedido).andExpect(status().isOk());
        impedir(rui, pedido, " ").andExpect(status().isBadRequest());
        impedir(rui, pedido, "x".repeat(501)).andExpect(jsonPath("$.codigo").value("impedimento_motivo"));
        impedir(rui, pedido, "Conheço a investidora de outro trabalho.").andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/verificacoes/" + pedido).header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.responsavel").doesNotExist());
        impedir(rui, pedido, "De novo.").andExpect(status().isOk());
        assumir(rui, pedido).andExpect(jsonPath("$.codigo").value("conflito_de_interesse"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Declarar vínculo pela API
     * Objetivo: Enviar a declaração de um vínculo.
     * Decisões: Alvo é CNPJ ou e-mail.
     */
    private ResultActions vincular(Usuario quem, String tipo, String alvo, String descricao) throws Exception {
        return mvc.perform(post("/api/v1/admin/vinculos").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"" + tipo + "\",\"alvo\":\"" + alvo + "\",\"descricao\":\"" + descricao + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Assumir pela API
     * Objetivo: Pedir para assumir um pedido.
     * Decisões: Quem pede é informado.
     */
    private ResultActions assumir(Usuario quem, String pedido) throws Exception {
        return mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/assumir").header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Decidir pela API
     * Objetivo: Aprovar um pedido.
     * Decisões: Motivo de aprovação.
     */
    private ResultActions decidir(Usuario quem, String pedido) throws Exception {
        return mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/decisao").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"VERIFICADO\",\"motivo\":\"DOCUMENTOS_CONFEREM\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Impedir pela API
     * Objetivo: Declarar impedimento num pedido.
     * Decisões: Motivo informado.
     */
    private ResultActions impedir(Usuario quem, String pedido, String motivo) throws Exception {
        return mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/impedimento").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + motivo + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Pedir verificação
     * Objetivo: Enviar todos os documentos exigidos (pessoa ou organização) e mandar para análise.
     * Decisões: Lê os exigidos da própria API.
     */
    private String pedirVerificacao(Usuario quem, String org) throws Exception {
        var consulta = get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(quem));
        if (org != null) {
            consulta.header("X-Orbit-Organizacao", org);
        }
        List<String> exigidos = JsonPath.read(mvc.perform(consulta).andReturn().getResponse().getContentAsString(),
                "$.exigidos[*].codigo");
        for (String tipo : exigidos) {
            var envio = multipart("/api/v1/verificacoes/documentos")
                    .file(new MockMultipartFile("arquivo", tipo + ".pdf", MediaType.APPLICATION_OCTET_STREAM_VALUE, PDF))
                    .param("tipo", tipo).header(HttpHeaders.AUTHORIZATION, bearer(quem));
            if (org != null) {
                envio.header("X-Orbit-Organizacao", org);
            }
            mvc.perform(envio).andExpect(status().isCreated());
        }
        var envio = post("/api/v1/verificacoes/envio").header(HttpHeaders.AUTHORIZATION, bearer(quem));
        if (org != null) {
            envio.header("X-Orbit-Organizacao", org);
        }
        return JsonPath.read(mvc.perform(envio).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Investidora de teste
     * Objetivo: Criar uma investidora ativa com os termos aceitos.
     * Decisões: E-mail único.
     */
    private Usuario investidora() {
        Usuario u = Usuario.novo("Investidora", "c-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(Papel.INVESTIDOR),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Conta da equipe
     * Objetivo: Criar uma conta interna ativa com o papel e o nome informados.
     * Decisões: Convite já aceito.
     */
    private Usuario interna(Papel papel, String nome) {
        Usuario u = Usuario.novoInterno(nome, "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
