package br.com.orbithub.investidores.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Testes de integração do perfil de investimento
 * Objetivo: Conferir, com PostgreSQL real, o perfil pessoal com as validações e o perfil da organização, com o tipo
 *           da própria organização e a edição só pelo administrador.
 * Decisões: Vínculos da organização criados direto pelo repositório, para focar no perfil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerfilDeInvestimentoIT {

    static final String VALIDO = """
            {"tipo":"ANJO","ticketMinimo":50000,"ticketMaximo":250000.50,"lideranca":"SO_COINVESTE","followOn":true,
             "participacaoNaGestao":"ACEITA_OBSERVADOR","instrumentos":["MUTUO_CONVERSIVEL","CONTRATO_INVESTIMENTO_ANJO"]}""";

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

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Perfil pessoal
     * Objetivo: Conferir as recusas (tipo de organização, "outros" sem descrição, ticket invertido, sem instrumento,
     *           instrumento inexistente) e o perfil salvo, que marca a tese como cadastrada na área do investidor.
     * Decisões: Investidora sem organização.
     */
    @Test
    void perfilPessoal() throws Exception {
        Usuario ana = conta();
        mvc.perform(get("/api/v1/investidores/perfil").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil").doesNotExist())
                .andExpect(jsonPath("$.podeEditar").value(true)).andExpect(jsonPath("$.daOrganizacao").value(false));
        salvar(ana, null, VALIDO.replace("\"ANJO\"", "\"FUNDO_VC\"")).andExpect(jsonPath("$.codigo").value("tipo_de_pessoa"));
        salvar(ana, null, VALIDO.replace("\"ANJO\"", "\"OUTROS\"")).andExpect(jsonPath("$.codigo").value("tipo_outro"));
        salvar(ana, null, VALIDO.replace("\"ticketMaximo\":250000.50", "\"ticketMaximo\":1000"))
                .andExpect(jsonPath("$.codigo").value("ticket_invalido"));
        salvar(ana, null, VALIDO.replace("\"ticketMinimo\":50000", "\"ticketMinimo\":0"))
                .andExpect(jsonPath("$.codigo").value("ticket_invalido"));
        salvar(ana, null, VALIDO.replace("[\"MUTUO_CONVERSIVEL\",\"CONTRATO_INVESTIMENTO_ANJO\"]", "[]"))
                .andExpect(jsonPath("$.codigo").value("sem_instrumento"));
        salvar(ana, null, VALIDO.replace("\"MUTUO_CONVERSIVEL\"", "\"CRIPTO\"")).andExpect(jsonPath("$.codigo").value("opcao_indisponivel"));
        salvar(ana, null, VALIDO.replace("\"lideranca\":\"SO_COINVESTE\",", "")).andExpect(jsonPath("$.codigo").value("campos_obrigatorios"));

        salvar(ana, null, VALIDO.replace("\"ANJO\"", "\"OUTROS\",\"tipoOutro\":\"  Clube de investimento  \""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfil.tipo").value("OUTROS"))
                .andExpect(jsonPath("$.perfil.tipoOutro").value("Clube de investimento"))
                .andExpect(jsonPath("$.perfil.ticketMaximo").value(250000.50))
                .andExpect(jsonPath("$.perfil.instrumentos.length()").value(2))
                .andExpect(jsonPath("$.atualizadoPor").value("Pessoa"));
        salvar(ana, null, VALIDO).andExpect(status().isOk()).andExpect(jsonPath("$.perfil.tipo").value("ANJO"))
                .andExpect(jsonPath("$.perfil.tipoOutro").doesNotExist());
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.teseCadastrada").value(true)).andExpect(jsonPath("$.proximoPasso").value("/investidor"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Perfil da organização
     * Objetivo: Conferir que o administrador salva com o tipo da organização (o enviado é ignorado), que o membro vê
     *           sem poder editar, que membro e observador recebem 403 ao salvar e que o perfil pessoal é separado.
     * Decisões: Organização do tipo fundo de VC.
     */
    @Test
    void perfilDaOrganizacao() throws Exception {
        Usuario ana = conta();
        Usuario bia = conta();
        Usuario caio = conta();
        String org = JsonPath.read(mvc.perform(post("/api/v1/organizacoes").header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Fundo Perfil\",\"tipo\":\"FUNDO_VC\",\"cnpj\":\"" + OrganizacoesIT.novoCnpj() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        membros.save(MembroDaOrganizacao.novo(UUID.fromString(org), bia.getId(), PapelNaOrganizacao.MEMBRO, Instant.now()));
        membros.save(MembroDaOrganizacao.novo(UUID.fromString(org), caio.getId(), PapelNaOrganizacao.OBSERVADOR, Instant.now()));

        salvar(ana, org, VALIDO).andExpect(status().isOk()).andExpect(jsonPath("$.daOrganizacao").value(true))
                .andExpect(jsonPath("$.tipoFixo").value("FUNDO_VC")).andExpect(jsonPath("$.perfil.tipo").value("FUNDO_VC"));
        mvc.perform(get("/api/v1/investidores/perfil").header(HttpHeaders.AUTHORIZATION, bearer(bia)).header("X-Orbit-Organizacao", org))
                .andExpect(status().isOk()).andExpect(jsonPath("$.podeEditar").value(false))
                .andExpect(jsonPath("$.perfil.tipo").value("FUNDO_VC"));
        salvar(bia, org, VALIDO).andExpect(status().isForbidden());
        salvar(caio, org, VALIDO).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/investidores/perfil").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.perfil").doesNotExist());
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(bia)).header("X-Orbit-Organizacao", org))
                .andExpect(jsonPath("$.teseCadastrada").value(true));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Salvar pela API
     * Objetivo: Enviar o perfil, com ou sem a organização da atuação.
     * Decisões: Organização nula = pessoal.
     */
    private ResultActions salvar(Usuario quem, String org, String corpo) throws Exception {
        var pedido = put("/api/v1/investidores/perfil").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo);
        if (org != null) {
            pedido = pedido.header("X-Orbit-Organizacao", org);
        }
        return mvc.perform(pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Conta de teste
     * Objetivo: Criar uma investidora ativa com os termos aceitos.
     * Decisões: Nome fixo "Pessoa", para conferir o autor da alteração.
     */
    private Usuario conta() {
        Usuario u = Usuario.novo("Pessoa", "p-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(Papel.INVESTIDOR),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
