package br.com.orbithub.empreendimentos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Testes de integração da publicação da startup
 * Objetivo: Conferir, com PostgreSQL real, o progresso, o envio só com os obrigatórios e pelo fundador administrador, o
 *           ciclo de situações com o histórico e o tratamento das startups publicadas inativas pela R8.
 * Decisões: A publicação pela curadoria (KAN-66) é simulada direto no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicacaoDaStartupIT {


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
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Progresso, envio e ciclo
     * Objetivo: Conferir o progresso e as pendências, o envio recusado com o cadastro incompleto e por quem não é administrador, e o ciclo envio → cancelamento → envio → publicada → despublicada com o histórico.
     * Decisões: Rui é fundador (não administrador).
     */
    @Test
    void progressoEnvioECiclo() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rui = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        mvc.perform(get("/api/v1/startups/campos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].obrigatorio").value(true)).andExpect(jsonPath("$[1].obrigatorio").value(false));
        mvc.perform(get("/api/v1/startups/" + id + "/progresso").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.percentual").value(0)).andExpect(jsonPath("$.podeEnviar").value(false))
                .andExpect(jsonPath("$.pendencias[0]").value("ao menos um fundador nos dados cadastrais"))
                .andExpect(jsonPath("$.secoes[0].secao").value("CADASTRO")).andExpect(jsonPath("$.situacao").value("RASCUNHO"));
        acao(eva, id, "envio").andExpect(jsonPath("$.codigo").value("cadastro_incompleto"));

        mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(eva)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Equipe Viva\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"fundadores\":[{\"nome\":\"Eva\"}]}"))
                .andExpect(status().isOk());
        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"Frete caro\",\"SOLUCAO\":\"Rotas\"}");
        secao(eva, id, "PRODUTO", "{\"DESCRICAO_PRODUTO\":\"Aplicativo\"}");
        secao(eva, id, "MERCADO", "{\"PUBLICO_ALVO\":\"Cooperativas\"}");
        mvc.perform(get("/api/v1/startups/" + id + "/progresso").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.percentual").value(62)).andExpect(jsonPath("$.pendencias.length()").value(3));
        secao(eva, id, "MODELO_DE_NEGOCIO", "{\"MODELO\":\"SAAS\",\"GERACAO_RECEITA\":\"Assinatura\"}");
        secao(eva, id, "MATURIDADE", "{\"ESTAGIO\":\"MVP\"}");
        mvc.perform(get("/api/v1/startups/" + id + "/progresso").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.percentual").value(100)).andExpect(jsonPath("$.podeEnviar").value(true));

        aceitar(rui, convidar(eva, id, rui.getEmail(), "FUNDADOR", "")).andExpect(status().isOk());
        acao(rui, id, "envio").andExpect(status().isForbidden());
        acao(eva, id, "envio").andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("EM_REVISAO"));
        acao(eva, id, "envio").andExpect(jsonPath("$.codigo").value("situacao_invalida"));
        acao(eva, id, "envio/cancelamento").andExpect(jsonPath("$.situacao").value("RASCUNHO"));
        acao(eva, id, "despublicacao").andExpect(jsonPath("$.codigo").value("situacao_invalida"));
        acao(eva, id, "envio").andExpect(jsonPath("$.situacao").value("EM_REVISAO"));
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), enviada_em = NULL WHERE id = ?::uuid", id);
        acao(rui, id, "despublicacao").andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/startups/" + id + "/despublicacao").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Pausa para reestruturar\"}"))
                .andExpect(jsonPath("$.situacao").value("DESPUBLICADA"));
        acao(eva, id, "envio").andExpect(jsonPath("$.situacao").value("EM_REVISAO"));
        acao(eva, id, "envio/cancelamento").andExpect(jsonPath("$.situacao").value("DESPUBLICADA"));
        mvc.perform(get("/api/v1/startups/" + id + "/situacoes").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.length()").value(6)).andExpect(jsonPath("$[2].motivo").value("Pausa para reestruturar"))
                .andExpect(jsonPath("$[2].de").value("PUBLICADA")).andExpect(jsonPath("$[2].pelaPlataforma").value(false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Inatividade pela R8
     * Objetivo: Conferir o aviso à startup publicada parada, a despublicação 30 dias depois sem atualização e a limpeza do aviso quando a startup volta a ser atualizada.
     * Decisões: Datas ajustadas direto no banco.
     */
    @Test
    void inatividadePelaR8() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String parada = startup(eva);
        String ativa = startup(eva);
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), atualizada_em = now() - interval '10 months' WHERE id IN (?::uuid, ?::uuid)",
                parada, ativa);

        rotinas.executarManual("R8", null);
        assertThat(contar(eva, "%está sem atualização")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT aviso_inatividade_em IS NOT NULL FROM startup WHERE id = ?::uuid", Boolean.class, parada)).isTrue();

        jdbc.update("UPDATE startup SET aviso_inatividade_em = now() - interval '31 days' WHERE id IN (?::uuid, ?::uuid)", parada, ativa);
        jdbc.update("UPDATE startup SET atualizada_em = now() WHERE id = ?::uuid", ativa);
        rotinas.executarManual("R8", null);
        rotinas.executarManual("R8", null);

        assertThat(jdbc.queryForObject("SELECT situacao FROM startup WHERE id = ?::uuid", String.class, parada)).isEqualTo("DESPUBLICADA");
        assertThat(jdbc.queryForObject("SELECT situacao FROM startup WHERE id = ?::uuid", String.class, ativa)).isEqualTo("PUBLICADA");
        assertThat(jdbc.queryForObject("SELECT aviso_inatividade_em IS NULL FROM startup WHERE id = ?::uuid", Boolean.class, ativa)).isTrue();
        assertThat(contar(eva, "%foi despublicada")).isEqualTo(1);
        mvc.perform(get("/api/v1/startups/" + parada + "/situacoes").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].pelaPlataforma").value(true)).andExpect(jsonPath("$[0].para").value("DESPUBLICADA"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Ação pela API
     * Objetivo: Enviar uma ação de publicação sem corpo.
     * Decisões: Caminho relativo à startup.
     */
    private ResultActions acao(Usuario quem, String id, String caminho) throws Exception {
        return mvc.perform(post("/api/v1/startups/" + id + "/" + caminho).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Salvar seção pela API
     * Objetivo: Gravar os valores de uma seção e conferir que deu certo.
     * Decisões: Espera 200.
     */
    private void secao(Usuario quem, String id, String secao, String corpo) throws Exception {
        mvc.perform(put("/api/v1/startups/" + id + "/secoes/" + secao).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Contar avisos
     * Objetivo: Contar as notificações de uma pessoa pelo começo do título.
     * Decisões: LIKE no título.
     */
    private int contar(Usuario quem, String titulo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE ?", Integer.class, quem.getId(), titulo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
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
     * Hora: 23:40
     * Cartão Jira: KAN-27
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
     * Hora: 23:40
     * Cartão Jira: KAN-27
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
     * Hora: 23:40
     * Cartão Jira: KAN-27
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
     * Hora: 23:40
     * Cartão Jira: KAN-27
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
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
