package br.com.orbithub.empreendimentos.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Testes de integração da equipe da startup
 * Objetivo: Conferir, com PostgreSQL real, o convite por e-mail, o aceite só pela conta certa e com o papel de
 *           empreendedor, as seções do colaborador, a regra do último fundador administrador e que a saída tira o acesso
 *           na hora.
 * Decisões: E-mail simulado, para capturar o link do convite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EquipeDaStartupIT {

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

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convidar e aceitar
     * Objetivo: Conferir as recusas do convite, o aviso no sino, a consulta, o aceite só pela conta do e-mail e com o
     *           papel de empreendedor, o convite usado uma vez só e o cancelamento.
     * Decisões: Eva cria; Rui é convidado como fundador; Ana é investidora; Caio é de fora.
     */
    @Test
    void convidarEAceitar() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario rui = conta(Papel.EMPREENDEDOR);
        Usuario ana = conta(Papel.INVESTIDOR);
        Usuario caio = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);

        convite(eva, id, "sem-arroba", "FUNDADOR", "").andExpect(jsonPath("$.codigo").value("email_invalido"));
        convite(eva, id, rui.getEmail(), "COLABORADOR", "").andExpect(jsonPath("$.codigo").value("colaborador_sem_secao"));
        convite(eva, id, rui.getEmail(), null, "").andExpect(jsonPath("$.codigo").value("papel_obrigatorio"));
        convite(eva, id, eva.getEmail(), "FUNDADOR", "").andExpect(jsonPath("$.codigo").value("ja_membro"));
        convite(caio, id, rui.getEmail(), "FUNDADOR", "").andExpect(status().isNotFound());

        String token = convidar(eva, id, rui.getEmail().toUpperCase(), "FUNDADOR", "");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND tipo = 'STARTUP'",
                Integer.class, rui.getId())).isEqualTo(1);
        mvc.perform(post("/api/v1/startups/convites/consulta").header(HttpHeaders.AUTHORIZATION, bearer(caio))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(jsonPath("$.startup").value("Equipe Viva")).andExpect(jsonPath("$.papel").value("FUNDADOR"));
        mvc.perform(post("/api/v1/startups/convites/consulta").header(HttpHeaders.AUTHORIZATION, bearer(caio))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"nao-existe\"}"))
                .andExpect(jsonPath("$.codigo").value("convite_invalido"));
        aceitar(caio, token).andExpect(jsonPath("$.codigo").value("convite_outro_email"));
        aceitar(rui, token).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        aceitar(rui, token).andExpect(jsonPath("$.codigo").value("convite_invalido"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND tipo = 'STARTUP'",
                Integer.class, eva.getId())).isEqualTo(1);
        mvc.perform(get("/api/v1/startups/" + id + "/equipe").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[1].papel").value("FUNDADOR"));
        mvc.perform(get("/api/v1/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.meuPapel").value("FUNDADOR"));
        convite(rui, id, caio.getEmail(), "FUNDADOR", "").andExpect(status().isForbidden());

        String daAna = convidar(eva, id, ana.getEmail(), "FUNDADOR", "");
        aceitar(ana, daAna).andExpect(jsonPath("$.codigo").value("sem_papel_empreendedor"));

        convidar(eva, id, caio.getEmail(), "FUNDADOR", "");
        String segundo = convidar(eva, id, caio.getEmail(), "FUNDADOR", "");
        String pendentes = mvc.perform(get("/api/v1/startups/" + id + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$.length()").value(2)).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(pendentes, "$[?(@.email == '" + caio.getEmail() + "')].id");
        assertThat(ids).hasSize(1);
        mvc.perform(delete("/api/v1/startups/" + id + "/convites/" + ids.get(0)).header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(status().isNoContent());
        aceitar(caio, segundo).andExpect(jsonPath("$.codigo").value("convite_invalido"));
        mvc.perform(delete("/api/v1/startups/" + id + "/convites/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Colaborador, papéis e saída
     * Objetivo: Conferir que o colaborador só edita as seções autorizadas, a troca de papel, a regra do último fundador
     *           administrador e que quem sai ou é removido perde o acesso na hora.
     * Decisões: Eva cria; Lia entra como colaboradora do cadastro; Rui entra como fundador.
     */
    @Test
    void colaboradorPapeisESaida() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario lia = conta(Papel.EMPREENDEDOR);
        Usuario rui = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        String cadastro = "{\"nome\":\"Equipe Viva\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}";

        aceitar(lia, convidar(eva, id, lia.getEmail(), "COLABORADOR", "\"MERCADO\"")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(jsonPath("$.meuPapel").value("COLABORADOR")).andExpect(jsonPath("$.minhasSecoes[0]").value("MERCADO"));
        editar(lia, id, cadastro).andExpect(status().isForbidden());

        papel(eva, id, lia.getId(), "COLABORADOR", "").andExpect(jsonPath("$.codigo").value("colaborador_sem_secao"));
        papel(eva, id, lia.getId(), "COLABORADOR", "\"CADASTRO\",\"MERCADO\"").andExpect(status().isNoContent());
        editar(lia, id, cadastro).andExpect(status().isOk()).andExpect(jsonPath("$.minhasSecoes.length()").value(2));
        papel(lia, id, lia.getId(), "FUNDADOR_ADMINISTRADOR", "").andExpect(status().isForbidden());
        papel(eva, id, UUID.randomUUID(), "FUNDADOR", "").andExpect(status().isNotFound());

        papel(eva, id, eva.getId(), "FUNDADOR", "").andExpect(jsonPath("$.codigo").value("ultimo_administrador"));
        sair(eva, id, eva.getId()).andExpect(jsonPath("$.codigo").value("ultimo_administrador"));

        aceitar(rui, convidar(eva, id, rui.getEmail(), "FUNDADOR", "")).andExpect(status().isOk());
        sair(rui, id, lia.getId()).andExpect(status().isForbidden());
        papel(eva, id, rui.getId(), "FUNDADOR_ADMINISTRADOR", "").andExpect(status().isNoContent());
        sair(eva, id, eva.getId()).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(eva))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/startups/minhas").header(HttpHeaders.AUTHORIZATION, bearer(eva))).andExpect(jsonPath("$.length()").value(0));

        sair(rui, id, lia.getId()).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/startups/" + id + "/equipe").header(HttpHeaders.AUTHORIZATION, bearer(lia))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT motivo_saida FROM startup_membro WHERE startup_id = ?::uuid AND usuario_id = ?",
                String.class, id, lia.getId())).isEqualTo("REMOVIDO");
        mvc.perform(get("/api/v1/startups/" + id + "/equipe").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].papel").value("FUNDADOR_ADMINISTRADOR"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Trocar papel pela API
     * Objetivo: Enviar a troca de papel e seções.
     * Decisões: Seções já em JSON.
     */
    private ResultActions papel(Usuario quem, String id, UUID alvo, String papel, String secoes) throws Exception {
        return mvc.perform(put("/api/v1/startups/" + id + "/equipe/" + alvo).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"papel\":\"" + papel + "\",\"secoes\":[" + secoes + "]}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Sair ou remover pela API
     * Objetivo: Encerrar o vínculo de um membro.
     * Decisões: Quem pede é informado.
     */
    private ResultActions sair(Usuario quem, String id, UUID alvo) throws Exception {
        return mvc.perform(delete("/api/v1/startups/" + id + "/equipe/" + alvo).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Editar cadastro pela API
     * Objetivo: Enviar os dados cadastrais.
     * Decisões: Corpo informado.
     */
    private ResultActions editar(Usuario quem, String id, String corpo) throws Exception {
        return mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
