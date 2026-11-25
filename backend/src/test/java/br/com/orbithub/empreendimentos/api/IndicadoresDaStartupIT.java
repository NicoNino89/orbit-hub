package br.com.orbithub.empreendimentos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.rotinas.ExecutorDeRotinas;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Testes de integração dos indicadores da startup
 * Objetivo: Conferir, com PostgreSQL real, a validação dos indicadores (formato e mês de referência), o histórico, a
 *           marca de desatualizado, as permissões e o lembrete da R8, uma vez só.
 * Decisões: Meses calculados a partir de hoje em Brasília, para o teste não envelhecer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IndicadoresDaStartupIT {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

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

    @Autowired
    ExecutorDeRotinas rotinas;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Declarar e listar
     * Objetivo: Conferir as recusas de formato e de mês, o histórico, o valor atual e a marca de desatualizado.
     * Decisões: Mês atual e oito meses atrás.
     */
    @Test
    void declararEListar() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        YearMonth agora = YearMonth.now(BRASILIA);

        valor(eva, id, "CLIENTES", "120", "").andExpect(jsonPath("$.codigo").value("referencia_obrigatoria"));
        valor(eva, id, "CLIENTES", "120", "outubro").andExpect(jsonPath("$.codigo").value("referencia_obrigatoria"));
        valor(eva, id, "CLIENTES", "-1", agora.toString()).andExpect(jsonPath("$.codigo").value("valor_invalido"));
        valor(eva, id, "CLIENTES", "10.5", agora.toString()).andExpect(jsonPath("$.codigo").value("valor_invalido"));
        valor(eva, id, "RETENCAO", "101", agora.toString()).andExpect(jsonPath("$.codigo").value("valor_invalido"));
        valor(eva, id, "FATURAMENTO_MENSAL", "10.555", agora.toString()).andExpect(jsonPath("$.codigo").value("valor_invalido"));
        valor(eva, id, "CLIENTES", "120", agora.plusMonths(1).toString()).andExpect(jsonPath("$.codigo").value("referencia_futura"));
        mvc.perform(post("/api/v1/startups/" + id + "/indicadores").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":1,\"referencia\":\"" + agora + "\"}"))
                .andExpect(jsonPath("$.codigo").value("indicador_obrigatorio"));

        valor(eva, id, "CRESCIMENTO_MENSAL", "-20", agora.toString()).andExpect(status().isCreated());
        valor(eva, id, "CLIENTES", "120", agora.minusMonths(1).toString()).andExpect(status().isCreated());
        valor(eva, id, "CLIENTES", "150", agora.toString()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.atual.valor").value(150)).andExpect(jsonPath("$.historico.length()").value(2))
                .andExpect(jsonPath("$.desatualizado").value(false));
        valor(eva, id, "FATURAMENTO_MENSAL", "48000.50", agora.minusMonths(8).toString()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.desatualizado").value(true)).andExpect(jsonPath("$.atual.referencia").value(agora.minusMonths(8).toString()));

        mvc.perform(get("/api/v1/startups/" + id + "/indicadores").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.length()").value(6)).andExpect(jsonPath("$[0].indicador").value("CLIENTES"))
                .andExpect(jsonPath("$[0].historico[1].valor").value(120)).andExpect(jsonPath("$[0].formato").value("QUANTIDADE"))
                .andExpect(jsonPath("$[3].atual").doesNotExist());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Permissões
     * Objetivo: Conferir que só quem edita a tração declara e que quem é de fora não vê.
     * Decisões: Lia é colaboradora só de mercado; Caio é de fora.
     */
    @Test
    void permissoes() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario lia = conta(Papel.EMPREENDEDOR);
        Usuario caio = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        String mes = YearMonth.now(BRASILIA).toString();
        aceitar(lia, convidar(eva, id, lia.getEmail(), "COLABORADOR", "\"MERCADO\"")).andExpect(status().isOk());
        valor(lia, id, "CLIENTES", "10", mes).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/startups/" + id + "/indicadores").header(HttpHeaders.AUTHORIZATION, bearer(lia))).andExpect(status().isOk());
        valor(caio, id, "CLIENTES", "10", mes).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/startups/" + id + "/indicadores").header(HttpHeaders.AUTHORIZATION, bearer(caio))).andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Lembrete da R8
     * Objetivo: Conferir que a R8 avisa a equipe da startup com indicador desatualizado uma vez só e não avisa quem está em dia.
     * Decisões: Rodada duas vezes; Rui tem o indicador do mês.
     */
    @Test
    void lembreteDaR8() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rui = conta(Papel.EMPREENDEDOR);
        String antiga = startup(eva);
        String emDia = startup(rui);
        YearMonth agora = YearMonth.now(BRASILIA);
        valor(eva, antiga, "CLIENTES", "30", agora.minusMonths(7).toString()).andExpect(status().isCreated());
        valor(rui, emDia, "CLIENTES", "30", agora.toString()).andExpect(status().isCreated());

        rotinas.executarManual("R8", null);
        rotinas.executarManual("R8", null);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE 'Atualize os indicadores%'",
                Integer.class, eva.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE 'Atualize os indicadores%'",
                Integer.class, rui.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT lembrete_indicadores_em IS NOT NULL FROM startup WHERE id = ?::uuid", Boolean.class, antiga)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Declarar pela API
     * Objetivo: Enviar um valor de indicador.
     * Decisões: Valor como número no JSON.
     */
    private ResultActions valor(Usuario quem, String id, String indicador, String valor, String referencia) throws Exception {
        return mvc.perform(post("/api/v1/startups/" + id + "/indicadores").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"indicador\":\"" + indicador + "\",\"valor\":" + valor + ",\"referencia\":\"" + referencia + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Criar startup pela API
     * Objetivo: Criar a startup e devolver o identificador.
     * Decisões: Sem CNPJ.
     */
    private String startup(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Equipe Viva\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Convite pela API
     * Objetivo: Enviar um pedido de convite.
     * Decisões: Papel nulo vai como null no JSON; seções já em JSON.
     */
    private ResultActions convite(Usuario quem, String id, String destino, String papel, String secoes) throws Exception {
        String p = papel == null ? "null" : "\"" + papel + "\"";
        return mvc.perform(post("/api/v1/startups/" + id + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + destino + "\",\"papel\":" + p + ",\"secoes\":[" + secoes + "]}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Convidar e pegar o token
     * Objetivo: Enviar o convite e devolver o token tirado do e-mail.
     * Decisões: Último e-mail capturado.
     */
    private String convidar(Usuario quem, String id, String destino, String papel, String secoes) throws Exception {
        convite(quem, id, destino, papel, secoes).andExpect(status().isCreated());
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        List<EnvioDeEmail.MensagemEmail> enviadas = mensagem.getAllValues();
        String texto = enviadas.get(enviadas.size() - 1).texto();
        return texto.substring(texto.indexOf("token=") + 6).split("\\s")[0];
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Aceitar pela API
     * Objetivo: Enviar o aceite de um convite.
     * Decisões: Conta informada.
     */
    private ResultActions aceitar(Usuario quem, String token) throws Exception {
        return mvc.perform(post("/api/v1/startups/convites/aceite").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Conta de teste
     * Objetivo: Criar uma conta ativa com o papel informado e os termos aceitos.
     * Decisões: E-mail único.
     */
    private Usuario conta(Papel papel) {
        Usuario u = Usuario.novo("Pessoa " + papel, "s-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
