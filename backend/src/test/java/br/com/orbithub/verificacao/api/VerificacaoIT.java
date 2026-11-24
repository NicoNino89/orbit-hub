package br.com.orbithub.verificacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import br.com.orbithub.investidores.domain.MembroDaOrganizacao;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.repository.MembroDaOrganizacaoRepository;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SituacaoDoPedido;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.repository.PedidoDeVerificacaoRepository;
import br.com.orbithub.verificacao.service.VerificacoesService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Testes de integração da verificação
 * Objetivo: Conferir, com PostgreSQL real e armazenamento em disco, o envio de documentos, as recusas, o envio para
 *           análise, o selo, o acesso só ao que é próprio e as regras da organização.
 * Decisões: Arquivos mínimos com a assinatura de PDF e PNG. A decisão da análise (KAN-83) é simulada direto no pedido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VerificacaoIT {

    static final byte[] PDF = "%PDF-1.4 documento de teste".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    MembroDaOrganizacaoRepository membros;

    @Autowired
    PedidoDeVerificacaoRepository pedidos;

    @Autowired
    VerificacoesService verificacoes;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Pessoa física
     * Objetivo: Conferir os exigidos, as recusas (formato, tipo não exigido, faltando), a troca de documento, o download
     *           próprio (e o 404 para outra pessoa), o envio para análise e o bloqueio de alteração em análise.
     * Decisões: Documento com foto em PDF e CPF em PNG.
     */
    @Test
    void pessoaFisica() throws Exception {
        Usuario ana = conta();
        Usuario bia = conta();
        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.selo").value("NAO_VERIFICADO"))
                .andExpect(jsonPath("$.sujeito").value("PESSOA")).andExpect(jsonPath("$.exigidos.length()").value(2))
                .andExpect(jsonPath("$.pedido").doesNotExist());

        enviar(ana, null, "PF_DOCUMENTO_COM_FOTO", "foto.txt", "texto qualquer".getBytes(StandardCharsets.UTF_8))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("documento_formato"));
        enviar(ana, null, "ORG_CNPJ", "cnpj.pdf", PDF).andExpect(jsonPath("$.codigo").value("documento_nao_exigido"));
        String primeiro = JsonPath.read(enviar(ana, null, "PF_DOCUMENTO_COM_FOTO", "rg.pdf", PDF).andExpect(status().isCreated())
                .andExpect(jsonPath("$.formato").value("application/pdf")).andExpect(jsonPath("$.chaveArquivo").doesNotExist())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/v1/verificacoes/envio").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("documentos_faltando"));

        String segundo = JsonPath.read(enviar(ana, null, "PF_DOCUMENTO_COM_FOTO", "cnh.png", PNG).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(segundo).isNotEqualTo(primeiro);
        mvc.perform(get("/api/v1/verificacoes/documentos/" + primeiro + "/arquivo").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/verificacoes/documentos/" + segundo + "/arquivo").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        mvc.perform(get("/api/v1/verificacoes/documentos/" + segundo + "/arquivo").header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(status().isNotFound());

        String cpf = JsonPath.read(enviar(ana, null, "PF_CPF", "cpf.pdf", PDF).andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/v1/verificacoes/documentos/" + cpf).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNoContent());
        enviar(ana, null, "PF_CPF", "cpf.pdf", PDF).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/verificacoes/envio").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("EM_ANALISE"));
        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.selo").value("EM_ANALISE")).andExpect(jsonPath("$.documentos.length()").value(2));
        enviar(ana, null, "PF_CPF", "cpf.pdf", PDF).andExpect(jsonPath("$.codigo").value("pedido_em_analise"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Organização
     * Objetivo: Conferir que fundo de VC exige a autorização da CVM e CVC não, e que o membro vê sem poder enviar.
     * Decisões: Duas organizações da mesma administradora.
     */
    @Test
    void organizacao() throws Exception {
        Usuario ana = conta();
        Usuario bia = conta();
        String fundo = organizacao(ana, "FUNDO_VC");
        String cvc = organizacao(ana, "CVC");
        membros.save(MembroDaOrganizacao.novo(UUID.fromString(fundo), bia.getId(), PapelNaOrganizacao.MEMBRO, Instant.now()));

        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(ana)).header("X-Orbit-Organizacao", fundo))
                .andExpect(jsonPath("$.sujeito").value("ORGANIZACAO")).andExpect(jsonPath("$.exigidos.length()").value(4))
                .andExpect(jsonPath("$.exigidos[?(@.codigo == 'ORG_AUTORIZACAO_CVM')]").isNotEmpty());
        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(ana)).header("X-Orbit-Organizacao", cvc))
                .andExpect(jsonPath("$.exigidos.length()").value(3));
        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(bia)).header("X-Orbit-Organizacao", fundo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.podeEnviar").value(false));
        enviar(bia, fundo, "ORG_CNPJ", "cnpj.pdf", PDF).andExpect(status().isForbidden());
        enviar(ana, fundo, "ORG_CNPJ", "cnpj.pdf", PDF).andExpect(status().isCreated());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Selo verificado e vencido
     * Objetivo: Conferir o selo verificado dentro da validade e não verificado depois dela.
     * Decisões: Decisão simulada no pedido (a fila é do KAN-83).
     */
    @Test
    void seloVerificadoEVencido() {
        UUID pessoa = UUID.randomUUID();
        Usuario ana = conta();
        Instant agora = Instant.now();
        PedidoDeVerificacao valido = PedidoDeVerificacao.rascunho(SujeitoDaVerificacao.PESSOA, ana.getId(), ana.getId(), agora);
        valido.enviar(agora);
        valido.decidir(SituacaoDoPedido.VERIFICADO, ana.getId(), "DOCUMENTOS_CONFEREM", null, agora.plus(Duration.ofDays(365)), agora);
        pedidos.save(valido);
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, ana.getId())).isEqualTo(Selo.VERIFICADO);

        Usuario bia = conta();
        PedidoDeVerificacao vencido = PedidoDeVerificacao.rascunho(SujeitoDaVerificacao.PESSOA, bia.getId(), bia.getId(), agora);
        vencido.enviar(agora);
        vencido.decidir(SituacaoDoPedido.VERIFICADO, bia.getId(), "DOCUMENTOS_CONFEREM", null, agora.minusSeconds(1), agora);
        pedidos.save(vencido);
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, bia.getId())).isEqualTo(Selo.NAO_VERIFICADO);
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, pessoa)).isEqualTo(Selo.NAO_VERIFICADO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Enviar documento pela API
     * Objetivo: Mandar um arquivo de um tipo, com ou sem a organização da atuação.
     * Decisões: Multipart com os campos "tipo" e "arquivo".
     */
    private ResultActions enviar(Usuario quem, String org, String tipo, String nome, byte[] conteudo) throws Exception {
        var pedido = multipart("/api/v1/verificacoes/documentos")
                .file(new MockMultipartFile("arquivo", nome, MediaType.APPLICATION_OCTET_STREAM_VALUE, conteudo))
                .param("tipo", tipo).header(HttpHeaders.AUTHORIZATION, bearer(quem));
        if (org != null) {
            pedido.header("X-Orbit-Organizacao", org);
        }
        return mvc.perform(pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Organização de teste
     * Objetivo: Criar uma organização do tipo informado pela API.
     * Decisões: CNPJ gerado.
     */
    private String organizacao(Usuario admin, String tipo) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/v1/organizacoes").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org " + tipo + "\",\"tipo\":\"" + tipo + "\",\"cnpj\":\"" + CnpjDeTeste.novo() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Conta de teste
     * Objetivo: Criar uma investidora ativa com os termos aceitos.
     * Decisões: E-mail único.
     */
    private Usuario conta() {
        Usuario u = Usuario.novo("Pessoa", "v-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(Papel.INVESTIDOR),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
