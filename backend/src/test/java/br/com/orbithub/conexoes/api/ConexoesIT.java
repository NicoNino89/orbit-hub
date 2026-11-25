package br.com.orbithub.conexoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.CnpjDeTeste;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46, KAN-47, KAN-48, KAN-49, KAN-67, KAN-105
 * Título: Testes de integração das conexões
 * Objetivo: Percorrer interesse, verificação exigida, Match, avanço, encerramento, recusa com nova tentativa, retirada,
 *           bloqueio de pessoa e de organização, e as rotinas R1 e R2.
 * Decisões: Startups publicadas e verificações direto no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConexoesIT {

    static final String ORG = "X-Orbit-Organizacao";

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
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-47, KAN-48, KAN-49
     * Título: Interesse até o encerramento
     * Objetivo: Exigir verificação, manifestar, ver no painel da startup, aceitar, ver contatos, avançar e encerrar.
     * Decisões: O motivo do encerramento só aparece para quem encerrou.
     */
    @Test
    void interesseMatchEEncerramento() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String id = publicada(eva);
        pedir(get("/api/v1/catalogo/startups/" + id + "/conexao"), leo).andExpect(jsonPath("$.podeManifestar").value(false))
                .andExpect(jsonPath("$.precisaVerificar").value(true));
        interesse(leo, id, null).andExpect(jsonPath("$.codigo").value("interesse_impedido"));
        verificar(leo);
        String conexao = JsonPath.read(interesse(leo, id, null).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        interesse(leo, id, null).andExpect(jsonPath("$.codigo").value("conexao_ativa"));
        assertThat(contar(eva, "Novo interesse%")).isEqualTo(1);
        pedir(get("/api/v1/startups/" + id + "/conexoes"), eva).andExpect(jsonPath("$[0].situacao").value("PENDENTE"))
                .andExpect(jsonPath("$[0].aguardaVoce").value(true)).andExpect(jsonPath("$[0].pedido").value("APRESENTACAO"))
                .andExpect(jsonPath("$[0].valorPreliminar").value(100000)).andExpect(jsonPath("$[0].investidor.selo").value("VERIFICADO"))
                .andExpect(jsonPath("$[0].contatos.length()").value(0));
        pedir(get("/api/v1/startups/" + id + "/conexoes").param("situacao", "MATCH"), eva).andExpect(jsonPath("$.length()").value(0));

        pedir(post("/api/v1/conexoes/" + conexao + "/aceite"), leo).andExpect(status().isNotFound());
        pedir(post("/api/v1/conexoes/" + conexao + "/aceite"), eva).andExpect(jsonPath("$.situacao").value("MATCH"));
        assertThat(contar(leo, "Match com%")).isEqualTo(1);
        pedir(get("/api/v1/conexoes"), leo).andExpect(jsonPath("$[0].contatos[0].nome").value("Eva"))
                .andExpect(jsonPath("$[0].aguardaVoce").value(true));
        corpo(post("/api/v1/conexoes/" + conexao + "/avanco"), leo, "{\"para\":\"EM_ANALISE\"}").andExpect(jsonPath("$.situacao").value("EM_ANALISE"));
        corpo(post("/api/v1/conexoes/" + conexao + "/avanco"), eva, "{\"para\":\"INVESTIU\"}").andExpect(jsonPath("$.codigo").value("transicao_invalida"));
        corpo(post("/api/v1/conexoes/" + conexao + "/avanco"), eva, "{\"para\":\"EM_NEGOCIACAO\"}").andExpect(jsonPath("$.situacao").value("EM_NEGOCIACAO"));
        corpo(post("/api/v1/conexoes/" + conexao + "/encerramento"), leo, "{\"motivo\":\"MOMENTO_INADEQUADO\"}")
                .andExpect(jsonPath("$.situacao").value("ENCERRADA"));
        pedir(get("/api/v1/conexoes"), leo).andExpect(jsonPath("$[0].motivo").value("Momento não adequado"));
        pedir(get("/api/v1/startups/" + id + "/conexoes"), eva).andExpect(jsonPath("$[0].motivo").doesNotExist());
        pedir(get("/api/v1/conexoes/" + conexao + "/historico"), eva).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[1].lado").value("STARTUP")).andExpect(jsonPath("$[4].por").value("Leo"));
        Usuario estranho = conta(Papel.INVESTIDOR, "Zé");
        pedir(get("/api/v1/conexoes/" + conexao + "/historico"), estranho).andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48, KAN-49, KAN-45
     * Título: Recusa, nova tentativa e retirada
     * Objetivo: Recusar com mensagem neutra, impedir nova tentativa antes de 90 dias, liberar depois e retirar.
     * Decisões: A startup desliga "só verificados".
     */
    @Test
    void recusaNovaTentativaERetirada() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String id = publicada(eva);
        corpo(put("/api/v1/startups/" + id + "/apenas-verificados"), eva, "{\"apenasVerificados\":false}")
                .andExpect(jsonPath("$.apenasVerificados").value(false));
        String c1 = JsonPath.read(interesse(ana, id, null).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/conexoes/" + c1 + "/recusa"), eva, "{\"motivo\":\"TICKET_INCOMPATIVEL\"}").andExpect(jsonPath("$.situacao").value("RECUSADA"));
        assertThat(jdbc.queryForObject("SELECT mensagem FROM notificacao WHERE usuario_id = ? AND titulo = 'Resposta ao seu interesse'", String.class,
                ana.getId())).doesNotContain("Ticket");
        interesse(ana, id, null).andExpect(jsonPath("$.codigo").value("interesse_impedido"));
        jdbc.update("UPDATE conexao SET atualizada_em = now() - interval '91 days' WHERE id = ?::uuid", c1);
        String c2 = JsonPath.read(interesse(ana, id, null).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        pedir(post("/api/v1/conexoes/" + c2 + "/retirada"), eva).andExpect(status().isNotFound());
        pedir(post("/api/v1/conexoes/" + c2 + "/retirada"), ana).andExpect(jsonPath("$.situacao").value("RETIRADA"));
        assertThat(contar(eva, "Interesse retirado")).isEqualTo(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Bloqueios
     * Objetivo: Bloquear pessoa e organização: some do catálogo, da apresentação e do interesse; desbloquear devolve.
     * Decisões: A organização bloqueada só bloqueia quem atua por ela.
     */
    @Test
    void bloqueios() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String id = publicada(eva);
        corpo(post("/api/v1/startups/" + id + "/bloqueios"), eva, "{\"tipo\":\"PESSOA\",\"identificador\":\"" + leo.getEmail() + "\"}")
                .andExpect(status().isCreated());
        pedir(get("/api/v1/catalogo/startups/" + id), leo).andExpect(status().isNotFound());
        pedir(get("/api/v1/catalogo/startups").param("lista", "NOVAS"), leo).andExpect(jsonPath("$.itens[?(@.startup.id == '" + id + "')]").isEmpty());
        interesse(leo, id, null).andExpect(status().isNotFound());
        assertThat(contar(leo, "%")).isZero();
        pedir(delete("/api/v1/startups/" + id + "/bloqueios/PESSOA/" + leo.getId()), eva).andExpect(status().isNoContent());
        pedir(get("/api/v1/catalogo/startups/" + id), leo).andExpect(status().isOk());

        String cnpj = CnpjDeTeste.novo();
        String org = JsonPath.read(corpo(post("/api/v1/organizacoes"), leo, "{\"nome\":\"Fundo Leo\",\"tipo\":\"FUNDO_VC\",\"cnpj\":\"" + cnpj
                + "\",\"site\":\"https://exemplo.com\",\"descricao\":\"Teste\"}").andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/startups/" + id + "/bloqueios"), eva, "{\"tipo\":\"ORGANIZACAO\",\"identificador\":\"" + cnpj + "\"}")
                .andExpect(jsonPath("$.rotulo").value(org.isEmpty() ? "" : "Fundo Leo (" + br.com.orbithub.compartilhado.documentos.Cnpj.formatar(cnpj) + ")"));
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo)).header(ORG, org))
                .andExpect(status().isNotFound());
        pedir(get("/api/v1/catalogo/startups/" + id), leo).andExpect(status().isOk());
        corpo(post("/api/v1/startups/" + id + "/bloqueios"), eva, "{\"tipo\":\"ORGANIZACAO\",\"identificador\":\"11111111111111\"}")
                .andExpect(jsonPath("$.codigo").value("cnpj_invalido"));
        corpo(post("/api/v1/startups/" + id + "/bloqueios"), eva, "{\"tipo\":\"PESSOA\",\"identificador\":\"ninguem@exemplo.com\"}")
                .andExpect(jsonPath("$.codigo").value("alvo_nao_encontrado"));
        pedir(get("/api/v1/startups/" + id + "/bloqueios"), eva).andExpect(jsonPath("$.ativos.length()").value(1))
                .andExpect(jsonPath("$.historico.length()").value(3)).andExpect(jsonPath("$.apenasVerificados").value(true));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: Rotinas R1 e R2
     * Objetivo: Lembrar no prazo de 7 dias (uma vez só) e expirar depois de 30.
     * Decisões: Datas recuadas no banco.
     */
    @Test
    void rotinasDeExpiracaoELembrete() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        verificar(leo);
        String id = publicada(eva);
        String c = JsonPath.read(interesse(leo, id, null).andReturn().getResponse().getContentAsString(), "$.id");
        jdbc.update("UPDATE conexao SET criada_em = now() - interval '8 days' WHERE id = ?::uuid", c);
        rotinas.executarManual("R2", null);
        rotinas.executarManual("R2", null);
        assertThat(contar(eva, "Interesse aguardando resposta")).isEqualTo(1);
        jdbc.update("UPDATE conexao SET criada_em = now() - interval '31 days' WHERE id = ?::uuid", c);
        rotinas.executarManual("R1", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM conexao WHERE id = ?::uuid", String.class, c)).isEqualTo("EXPIRADA");
        assertThat(contar(leo, "Interesse expirado")).isEqualTo(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Manifestar
     * Objetivo: Enviar o interesse com valor, condições, pedido de apresentação e mensagem.
     * Decisões: Organização opcional.
     */
    private ResultActions interesse(Usuario quem, String startup, String org) throws Exception {
        MockHttpServletRequestBuilder p = post("/api/v1/catalogo/startups/" + startup + "/interesse").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":100000,\"condicoes\":\"Mútuo\",\"pedido\":\"APRESENTACAO\",\"mensagem\":\"Podemos conversar?\"}");
        return mvc.perform(org == null ? p : p.header(ORG, org));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Requisição com token
     * Objetivo: Enviar a requisição com o token.
     * Decisões: Genérica.
     */
    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Requisição com corpo
     * Objetivo: Enviar JSON com o token.
     * Decisões: Genérica.
     */
    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Startup publicada
     * Objetivo: Criar a startup e publicá-la no banco.
     * Decisões: Valores aprovados copiados.
     */
    private String publicada(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Conecta\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), nome_aprovado = nome, segmento_aprovado = segmento, "
                + "estagio_aprovado = 'MVP' WHERE id = ?::uuid", id);
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-62
     * Título: Verificar
     * Objetivo: Dar o selo de verificado à pessoa.
     * Decisões: Direto no banco.
     */
    private void verificar(Usuario u) {
        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_em, valido_ate) "
                + "VALUES (?, 'PESSOA', ?, 'VERIFICADO', ?, now(), now(), now(), now() + interval '1 year')", UUID.randomUUID(), u.getId(), u.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Contar notificações
     * Objetivo: Contar as notificações da conta com o título parecido.
     * Decisões: LIKE.
     */
    private int contar(Usuario quem, String titulo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE ?", Integer.class, quem.getId(), titulo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Conta pública
     * Objetivo: Criar uma conta confirmada com nome e termos aceitos.
     * Decisões: Direto no repositório.
     */
    private Usuario conta(Papel papel, String nome) {
        Usuario u = Usuario.novo(nome, "c-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
