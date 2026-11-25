package br.com.orbithub.conexoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.CnpjDeTeste;
import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.conexoes.service.RodadasService;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Testes de integração das rodadas
 * Objetivo: Conferir, com PostgreSQL real, a exigência de CNPJ e de fundador administrador, a validação dos termos
 *           comuns e dos específicos do instrumento, uma rodada em andamento por startup e o ciclo com o histórico.
 * Decisões: Prazos calculados a partir de hoje, para o teste não envelhecer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RodadasIT {


    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    RodadasService servico;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Abertura e ciclo
     * Objetivo: Conferir CNPJ, permissão, uma rodada em andamento, validações, abertura com termos completos, negociação, encerramento e histórico.
     * Decisões: Participação societária; Rui é fundador.
     */
    @Test
    void aberturaECiclo() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rui = conta(Papel.EMPREENDEDOR);
        Usuario caio = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        String prazo = LocalDate.now().plusMonths(3).toString();
        criar(eva, id, "{\"estagio\":\"SEED\"}").andExpect(jsonPath("$.codigo").value("startup_sem_cnpj"));
        mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(eva)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Equipe Viva\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"cnpj\":\"" + CnpjDeTeste.novo() + "\"}"))
                .andExpect(status().isOk());
        aceitar(rui, convidar(eva, id, rui.getEmail(), "FUNDADOR", "")).andExpect(status().isOk());
        criar(rui, id, "{\"estagio\":\"SEED\"}").andExpect(status().isForbidden());
        criar(eva, id, "{\"estagio\":\"ESPACIAL\"}").andExpect(jsonPath("$.codigo").value("opcao_indisponivel"));

        String rodada = JsonPath.read(criar(eva, id, "{\"estagio\":\"SEED\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("RASCUNHO")).andExpect(jsonPath("$.semLider").value(true))
                .andReturn().getResponse().getContentAsString(), "$.id");
        criar(eva, id, "{\"estagio\":\"SEED\"}").andExpect(jsonPath("$.codigo").value("rodada_em_andamento"));

        editar(eva, rodada, "{\"instrumento\":\"PARTICIPACAO_SOCIETARIA\",\"especificos\":{\"VALUATION_CAP\":\"1000\"}}")
                .andExpect(jsonPath("$.codigo").value("termo_de_outro_instrumento"));
        editar(eva, rodada, "{\"valorAlvo\":100000,\"valorMinimo\":200000}").andExpect(jsonPath("$.codigo").value("valores_fora_de_ordem"));
        editar(eva, rodada, "{\"valorAlvo\":100000,\"valorMaximo\":50000}").andExpect(jsonPath("$.codigo").value("valores_fora_de_ordem"));
        editar(eva, rodada, "{\"valorAlvo\":0}").andExpect(jsonPath("$.codigo").value("valor_invalido"));
        editar(eva, rodada, "{\"prazo\":\"" + LocalDate.now().minusDays(1) + "\"}").andExpect(jsonPath("$.codigo").value("prazo_invalido"));
        editar(eva, rodada, "{\"prazo\":\"" + LocalDate.now().plusYears(3) + "\"}").andExpect(jsonPath("$.codigo").value("prazo_invalido"));
        editar(eva, rodada, "{\"instrumento\":\"PARTICIPACAO_SOCIETARIA\",\"especificos\":{\"PARTICIPACAO_OFERECIDA\":\"101\"}}")
                .andExpect(jsonPath("$.codigo").value("numero_invalido"));
        editar(eva, rodada, "{\"instrumento\":\"PARTICIPACAO_SOCIETARIA\",\"especificos\":{\"VALUATION_PRE_MONEY\":\"muito\"}}")
                .andExpect(jsonPath("$.codigo").value("numero_invalido"));
        acao(eva, rodada, "abertura").andExpect(jsonPath("$.codigo").value("termos_incompletos"));

        String completa = "{\"estagio\":\"SEED\",\"instrumento\":\"PARTICIPACAO_SOCIETARIA\",\"valorAlvo\":1000000,\"valorMinimo\":600000,"
                + "\"valorMaximo\":1500000,\"prazo\":\"" + prazo + "\",\"especificos\":{\"VALUATION_PRE_MONEY\":\"8000000,00\",\"PARTICIPACAO_OFERECIDA\":\"12,5\"}}";
        editar(eva, rodada, completa).andExpect(status().isOk()).andExpect(jsonPath("$.especificos.PARTICIPACAO_OFERECIDA").value("12.5"))
                .andExpect(jsonPath("$.especificos.VALUATION_PRE_MONEY").value("8000000"));
        assertThat(servico.rodadaAberta(UUID.fromString(id))).isEmpty();
        acao(rui, rodada, "abertura").andExpect(status().isForbidden());
        acao(eva, rodada, "abertura").andExpect(jsonPath("$.situacao").value("ABERTA"));
        assertThat(servico.rodadaAberta(UUID.fromString(id))).isPresent();
        editar(eva, rodada, completa.replace("1000000,", "1200000,")).andExpect(jsonPath("$.valorAlvo").value(1200000));
        editar(eva, rodada, "{\"estagio\":\"SEED\"}").andExpect(jsonPath("$.codigo").value("termos_incompletos"));
        acao(eva, rodada, "negociacao").andExpect(jsonPath("$.situacao").value("EM_NEGOCIACAO"));
        editar(eva, rodada, completa).andExpect(jsonPath("$.codigo").value("situacao_invalida"));
        encerrar(eva, rodada, "{}").andExpect(jsonPath("$.codigo").value("resultado_obrigatorio"));
        encerrar(eva, rodada, "{\"resultado\":\"CONCLUIDA\",\"motivo\":\"Captados R$ 1,2 mi\"}").andExpect(jsonPath("$.situacao").value("ENCERRADA"))
                .andExpect(jsonPath("$.resultado").value("CONCLUIDA"));
        mvc.perform(get("/api/v1/rodadas/" + rodada + "/historico").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.length()").value(4)).andExpect(jsonPath("$[0].motivo").value("CONCLUIDA: Captados R$ 1,2 mi"));
        mvc.perform(get("/api/v1/rodadas/" + rodada + "/historico").header(HttpHeaders.AUTHORIZATION, bearer(caio))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/startups/" + id + "/rodadas").header(HttpHeaders.AUTHORIZATION, bearer(caio))).andExpect(status().isNotFound());

        String segunda = JsonPath.read(criar(eva, id, "{\"instrumento\":\"OUTROS_LC_182\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        editar(eva, segunda, "{\"instrumento\":\"OUTROS_LC_182\",\"especificos\":{\"OUTROS_TERMOS\":\"" + "a".repeat(1001) + "\"}}")
                .andExpect(jsonPath("$.codigo").value("texto_longo"));
        encerrar(eva, segunda, "{\"resultado\":\"CONCLUIDA\"}").andExpect(jsonPath("$.codigo").value("rodada_nao_aberta"));
        encerrar(eva, segunda, "{\"resultado\":\"CANCELADA\"}").andExpect(jsonPath("$.resultado").value("CANCELADA"));
        mvc.perform(get("/api/v1/startups/" + id + "/rodadas").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/v1/rodadas/" + UUID.randomUUID() + "/abertura").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termos por instrumento
     * Objetivo: Conferir o catálogo dos termos e os meses do mútuo conversível.
     * Decisões: Mútuo com vencimento fora da faixa.
     */
    @Test
    void termosPorInstrumento() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        mvc.perform(get("/api/v1/rodadas/termos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].termo").value("VALUATION_PRE_MONEY")).andExpect(jsonPath("$[0].instrumento").value("PARTICIPACAO_SOCIETARIA"))
                .andExpect(jsonPath("$[?(@.termo == 'OUTROS_TERMOS')].instrumento").value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
        String id = startup(eva);
        mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(eva)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Mutuo\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"cnpj\":\"" + CnpjDeTeste.novo() + "\"}"))
                .andExpect(status().isOk());
        criar(eva, id, "{\"instrumento\":\"MUTUO_CONVERSIVEL\",\"especificos\":{\"VENCIMENTO_MUTUO_MESES\":\"241\"}}")
                .andExpect(jsonPath("$.codigo").value("numero_invalido"));
        criar(eva, id, "{\"instrumento\":\"MUTUO_CONVERSIVEL\",\"especificos\":{\"VENCIMENTO_MUTUO_MESES\":\"24\",\"TAXA_JUROS\":\"6\"}}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.especificos.VENCIMENTO_MUTUO_MESES").value("24"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criar pela API
     * Objetivo: Enviar a criação de uma rodada.
     * Decisões: Corpo em JSON.
     */
    private ResultActions criar(Usuario quem, String startup, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/startups/" + startup + "/rodadas").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Editar pela API
     * Objetivo: Enviar os termos da rodada.
     * Decisões: Corpo em JSON.
     */
    private ResultActions editar(Usuario quem, String rodada, String corpo) throws Exception {
        return mvc.perform(put("/api/v1/rodadas/" + rodada).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Ação pela API
     * Objetivo: Enviar uma ação sem corpo na rodada.
     * Decisões: Caminho relativo à rodada.
     */
    private ResultActions acao(Usuario quem, String rodada, String caminho) throws Exception {
        return mvc.perform(post("/api/v1/rodadas/" + rodada + "/" + caminho).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Encerrar pela API
     * Objetivo: Enviar o encerramento da rodada.
     * Decisões: Corpo em JSON.
     */
    private ResultActions encerrar(Usuario quem, String rodada, String corpo) throws Exception {
        return mvc.perform(post("/api/v1/rodadas/" + rodada + "/encerramento").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
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
     * Hora: 23:51
     * Cartão Jira: KAN-59
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
     * Hora: 23:51
     * Cartão Jira: KAN-59
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
     * Hora: 23:51
     * Cartão Jira: KAN-59
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
     * Hora: 23:51
     * Cartão Jira: KAN-59
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
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
