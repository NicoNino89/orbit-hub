package br.com.orbithub.moderacao.api;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86, KAN-88, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
 * Título: Testes de integração da moderação e do atendimento
 * Objetivo: Conferir denúncia, fila, medidas preventivas, sanções progressivas, banimento com segunda aprovação,
 *           violação de confidencialidade, recursos, chamados com níveis e avaliação, chamado sem conta, disputas e as
 *           rotinas R14 e R20.
 * Decisões: Banco real (Testcontainers) e e-mail simulado; equipe criada direto no repositório.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ModeracaoEAtendimentoIT {

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

    @Autowired
    PasswordEncoder senhas;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99, KAN-100, KAN-101, KAN-86, KAN-81
     * Título: Denúncia, sanções progressivas, recurso e banimento
     * Objetivo: Denunciar um perfil, agrupar, subir a prioridade, suspender preventivamente, advertir, suspender, recorrer
     *           (impedindo quem decidiu), revogar no recurso e banir com segunda aprovação.
     * Decisões: O suspenso tenta entrar com a senha e recebe conta_suspensa.
     */
    @Test
    void denunciaSancoesERecurso() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario caio = conta(Papel.INVESTIDOR, "Caio");
        Usuario mo = interna(Papel.MODERADOR, "Mo");
        Usuario bia = interna(Papel.MODERADOR, "Bia");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        String startup = publicada(eva);
        verificar(caio);
        String interesse = JsonPath.read(corpo(post("/api/v1/catalogo/startups/" + startup + "/interesse"), caio, "{}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        corpo(post("/api/v1/denuncias"), ana, denuncia("PERFIL", ana.getId(), "SPAM")).andExpect(jsonPath("$.codigo").value("objeto_invalido"));
        corpo(post("/api/v1/denuncias"), ana, denuncia("PERFIL", caio.getId(), "SPAM")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo").isNotEmpty());
        corpo(post("/api/v1/denuncias"), ana, denuncia("PERFIL", caio.getId(), "SPAM")).andExpect(jsonPath("$.codigo").value("denuncia_repetida"));
        pedir(get("/api/v1/admin/moderacao/casos"), ana).andExpect(status().isForbidden());
        pedir(get("/api/v1/admin/moderacao/casos"), mo).andExpect(jsonPath("$[?(@.objeto == 'Caio')].prioridade").value("NORMAL"));
        corpo(post("/api/v1/denuncias"), eva, denuncia("PERFIL", caio.getId(), "FRAUDE")).andExpect(status().isCreated());
        String caso = jdbc.queryForObject("SELECT id::text FROM caso_moderacao WHERE objeto_id = ? AND situacao <> 'DECIDIDO'", String.class, caio.getId());
        pedir(get("/api/v1/admin/moderacao/casos/" + caso), mo).andExpect(jsonPath("$.prioridade").value("CRITICA"))
                .andExpect(jsonPath("$.denuncias.length()").value(2)).andExpect(jsonPath("$.denuncias[0].denunciante").value("Ana"));
        assertThat(contar(chefe, "Denúncia crítica na fila")).isPositive();

        pedir(post("/api/v1/admin/moderacao/casos/" + caso + "/assumir"), mo).andExpect(jsonPath("$.situacao").value("EM_ANALISE"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/medida-preventiva"), mo, "{\"tipo\":\"SUSPENDER_CONTA\"}")
                .andExpect(jsonPath("$.codigo").value("motivo_obrigatorio"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/medida-preventiva"), mo, "{\"tipo\":\"SUSPENDER_CONTA\",\"motivo\":\"Indício de golpe\"}")
                .andExpect(jsonPath("$.medidas[0].tipo").value("PREVENTIVA_SUSPENDER_CONTA"));
        assertThat(situacaoDaConexao(interesse)).isEqualTo("EXPIRADA");
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + caio.getEmail() + "\",\"senha\":\"Senha!123\"}"))
                .andExpect(jsonPath("$.codigo").value("conta_suspensa"));

        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/decisao"), mo, "{\"decisao\":\"ESCALA\",\"motivo\":\"VIOLACAO_LEVE\"}")
                .andExpect(jsonPath("$.situacao").value("DECIDIDO")).andExpect(jsonPath("$.decisao").value("ADVERTENCIA"));
        assertThat(suspensaAte(caio)).isNull();
        assertThat(contar(caio, "Decisão da moderação")).isEqualTo(1);
        assertThat(contar(ana, "Denúncia analisada")).isEqualTo(1);
        pedir(get("/api/v1/denuncias"), ana).andExpect(jsonPath("$[0].analisada").value(true));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/decisao"), mo, "{\"decisao\":\"SEM_ACAO\",\"motivo\":\"SEM_VIOLACAO\"}")
                .andExpect(jsonPath("$.codigo").value("caso_decidido"));

        String caso2 = novoCaso(ana, caio, "ASSEDIO");
        corpo(post("/api/v1/admin/moderacao/casos/" + caso2 + "/decisao"), mo, "{\"decisao\":\"SUSPENSAO\",\"motivo\":\"VIOLACAO_GRAVE\",\"dias\":5}")
                .andExpect(jsonPath("$.codigo").value("duracao_invalida"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso2 + "/decisao"), mo, "{\"decisao\":\"ESCALA\",\"motivo\":\"VIOLACAO_GRAVE\"}")
                .andExpect(jsonPath("$.decisao").value("SUSPENSAO"));
        assertThat(suspensaAte(caio)).isAfter(Instant.now().plusSeconds(6 * 86400));
        String sancao = JsonPath.read(pedir(get("/api/v1/sancoes"), caio).andExpect(jsonPath("$[0].sancao.tipo").value("SUSPENSAO"))
                .andExpect(jsonPath("$[0].podeRecorrer").value(true)).andReturn().getResponse().getContentAsString(), "$[0].sancao.id");

        corpo(post("/api/v1/recursos"), caio, "{\"origemTipo\":\"SANCAO\",\"origemId\":\"" + sancao + "\",\"argumentos\":\"curto\"}")
                .andExpect(jsonPath("$.codigo").value("argumentos_invalidos"));
        corpo(post("/api/v1/recursos"), ana, recurso("SANCAO", sancao)).andExpect(status().isForbidden());
        String rec = JsonPath.read(corpo(post("/api/v1/recursos"), caio, recurso("SANCAO", sancao)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("PENDENTE")).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/recursos"), caio, recurso("SANCAO", sancao)).andExpect(jsonPath("$.codigo").value("recurso_ja_apresentado"));
        pedir(get("/api/v1/admin/recursos"), mo).andExpect(jsonPath("$[?(@.id == '" + rec + "')]").isEmpty());
        corpo(post("/api/v1/admin/recursos/" + rec + "/julgamento"), mo, "{\"deferir\":true,\"motivo\":\"x\"}")
                .andExpect(jsonPath("$.codigo").value("julgador_impedido"));
        corpo(post("/api/v1/admin/recursos/" + rec + "/julgamento"), bia, "{\"deferir\":true}").andExpect(jsonPath("$.codigo").value("motivo_obrigatorio"));
        corpo(post("/api/v1/admin/recursos/" + rec + "/julgamento"), bia, "{\"deferir\":true,\"motivo\":\"Provas frágeis\"}")
                .andExpect(jsonPath("$.situacao").value("DEFERIDO"));
        assertThat(suspensaAte(caio)).isNull();
        assertThat(jdbc.queryForObject("SELECT situacao FROM sancao WHERE id = ?::uuid", String.class, sancao)).isEqualTo("REVOGADA");
        pedir(get("/api/v1/recursos"), caio).andExpect(jsonPath("$[0].motivo").value("Provas frágeis"));
        corpo(post("/api/v1/admin/recursos/" + rec + "/julgamento"), chefe, "{\"deferir\":false,\"motivo\":\"x\"}")
                .andExpect(jsonPath("$.codigo").value("recurso_julgado"));

        String caso3 = novoCaso(ana, caio, "CONTEUDO_IMPROPRIO");
        corpo(post("/api/v1/admin/moderacao/casos/" + caso3 + "/decisao"), mo, "{\"decisao\":\"BANIMENTO\",\"motivo\":\"FRAUDE_CONFIRMADA\"}")
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_APROVACAO"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso3 + "/decisao"), mo, "{\"decisao\":\"ADVERTENCIA\",\"motivo\":\"VIOLACAO_LEVE\"}")
                .andExpect(jsonPath("$.codigo").value("aguardando_aprovacao"));
        String proposta = jdbc.queryForObject("SELECT id::text FROM proposta_aprovacao WHERE objeto_id = ? AND situacao = 'PENDENTE'", String.class, caso3);
        corpo(post("/api/v1/admin/aprovacoes/" + proposta + "/rejeicao"), bia, "{\"justificativa\":\"Basta suspender\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT situacao FROM caso_moderacao WHERE id = ?::uuid", String.class, caso3)).isEqualTo("EM_ANALISE");
        corpo(post("/api/v1/admin/moderacao/casos/" + caso3 + "/decisao"), mo, "{\"decisao\":\"BANIMENTO\",\"motivo\":\"FRAUDE_CONFIRMADA\"}")
                .andExpect(status().isOk());
        String proposta2 = jdbc.queryForObject("SELECT id::text FROM proposta_aprovacao WHERE objeto_id = ? AND situacao = 'PENDENTE'", String.class, caso3);
        corpo(post("/api/v1/admin/aprovacoes/" + proposta2 + "/aprovacao"), bia, "{\"justificativa\":\"Fraude comprovada\"}").andExpect(status().isOk());
        assertThat(suspensaAte(caio)).isEqualTo(Usuario.BANIDA);
        pedir(get("/api/v1/admin/moderacao/casos/" + caso3), mo).andExpect(jsonPath("$.situacao").value("DECIDIDO"))
                .andExpect(jsonPath("$.historico[0].tipo").value("BANIMENTO"));
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + caio.getEmail() + "\",\"senha\":\"Senha!123\"}"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("banida")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102, KAN-101, KAN-86
     * Título: Violação de confidencialidade e sanção da startup
     * Objetivo: Abrir o caso de vazamento, tirar o Nível 2 do suspeito, levantar os acessos com justificativa, registrar o
     *           parecer jurídico, suspender a startup (que sai do catálogo) e devolvê-la pelo recurso.
     * Decisões: Despublicar preventivamente só em caso crítico.
     */
    @Test
    void confidencialidadeEStartup() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario caio = conta(Papel.INVESTIDOR, "Caio");
        Usuario mo = interna(Papel.MODERADOR, "Mo");
        Usuario bia = interna(Papel.MODERADOR, "Bia");
        Usuario juri = interna(Papel.JURIDICO, "Juri");
        String startup = publicada(eva);
        corpo(post("/api/v1/denuncias"), eva, denuncia("STARTUP", UUID.fromString(startup), "VAZAMENTO_CONFIDENCIAL")).andExpect(status().isCreated());
        String caso = jdbc.queryForObject("SELECT id::text FROM caso_moderacao WHERE objeto_id = ?::uuid", String.class, startup);
        pedir(get("/api/v1/admin/moderacao/casos/" + caso), mo).andExpect(jsonPath("$.prioridade").value("ALTA"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/medida-preventiva"), mo, "{\"tipo\":\"DESPUBLICAR\",\"motivo\":\"x\"}")
                .andExpect(jsonPath("$.codigo").value("medida_indisponivel"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/medida-preventiva"), mo, "{\"tipo\":\"SUSPENDER_NIVEL2\",\"motivo\":\"Suspeito\"}")
                .andExpect(jsonPath("$.codigo").value("alvo_obrigatorio"));
        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/medida-preventiva"), mo, "{\"tipo\":\"SUSPENDER_NIVEL2\",\"alvoId\":\"" + caio.getId()
                + "\",\"motivo\":\"Suspeito\"}").andExpect(jsonPath("$.medidas[0].tipo").value("PREVENTIVA_SUSPENDER_NIVEL2"));
        assertThat(suspensaAte(caio)).isNull();
        pedir(get("/api/v1/admin/moderacao/casos/" + caso + "/acessos-nivel2?justificativa="), mo).andExpect(jsonPath("$.codigo").value("justificativa_obrigatoria"));
        pedir(get("/api/v1/admin/moderacao/casos/" + caso + "/acessos-nivel2?justificativa=Levantar suspeitos"), mo).andExpect(status().isOk());
        corpo(put("/api/v1/admin/moderacao/casos/" + caso + "/parecer"), mo, "{\"texto\":\"x\"}").andExpect(status().isForbidden());
        corpo(put("/api/v1/admin/moderacao/casos/" + caso + "/parecer"), juri, "{\"texto\":\"Violação do termo, cláusula 3\"}")
                .andExpect(jsonPath("$.parecerJuridico").value("Violação do termo, cláusula 3"));

        String perfil = novoCaso(ana, caio, "SPAM");
        corpo(post("/api/v1/admin/moderacao/casos/" + perfil + "/medida-preventiva"), mo, "{\"tipo\":\"SUSPENDER_CONTA\",\"motivo\":\"x\"}")
                .andExpect(jsonPath("$.codigo").value("medida_indisponivel"));
        corpo(post("/api/v1/admin/moderacao/casos/" + perfil + "/decisao"), mo, "{\"decisao\":\"REMOCAO_CONTEUDO\",\"motivo\":\"VIOLACAO_LEVE\"}")
                .andExpect(jsonPath("$.decisao").value("REMOCAO_CONTEUDO"));

        String reuniao = UUID.randomUUID().toString();
        corpo(post("/api/v1/denuncias"), ana, denuncia("REUNIAO", UUID.fromString(reuniao), "ASSEDIO")).andExpect(status().isNotFound());

        corpo(post("/api/v1/admin/moderacao/casos/" + caso + "/decisao"), mo, "{\"decisao\":\"SUSPENSAO\",\"motivo\":\"VAZAMENTO_CONFIRMADO\",\"dias\":30}")
                .andExpect(jsonPath("$.decisao").value("SUSPENSAO"));
        assertThat(situacaoDaStartup(startup)).isEqualTo("DESPUBLICADA");
        assertThat(contar(eva, "Decisão da moderação")).isEqualTo(1);
        String sancao = JsonPath.read(pedir(get("/api/v1/sancoes"), eva).andExpect(jsonPath("$[0].sancao.alvoTipo").value("STARTUP"))
                .andReturn().getResponse().getContentAsString(), "$[0].sancao.id");
        String rec = JsonPath.read(corpo(post("/api/v1/recursos"), eva, recurso("SANCAO", sancao)).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/admin/recursos/" + rec + "/julgamento"), bia, "{\"deferir\":true,\"motivo\":\"O vazamento não partiu da startup\"}")
                .andExpect(jsonPath("$.situacao").value("DEFERIDO"));
        assertThat(situacaoDaStartup(startup)).isEqualTo("PUBLICADA");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Recursos da verificação e da curadoria
     * Objetivo: Recorrer da recusa da verificação (o pedido volta para análise) e da curadoria (indeferido), conferir o
     *           prazo vencido, o time que julga e quem pode recorrer.
     * Decisões: Pedidos e revisões criados direto no banco.
     */
    @Test
    void recursosDaVerificacaoEDaCuradoria() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario analista = interna(Papel.ANALISTA_VERIFICACAO, "Alan");
        Usuario outraAnalista = interna(Papel.ANALISTA_VERIFICACAO, "Alice");
        Usuario curador = interna(Papel.CURADOR, "Cura");
        UUID pedido = UUID.randomUUID();
        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_por, decidido_em, motivo) "
                + "VALUES (?, 'PESSOA', ?, 'RECUSADO', ?, now(), now(), ?, now(), 'DOCUMENTO_ILEGIVEL')", pedido, ana.getId(), ana.getId(), analista.getId());
        UUID antigo = UUID.randomUUID();
        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_por, decidido_em) "
                + "VALUES (?, 'PESSOA', ?, 'RECUSADO', ?, now(), now(), ?, now() - interval '20 days')", antigo, ana.getId(), ana.getId(), analista.getId());
        corpo(post("/api/v1/recursos"), ana, recurso("VERIFICACAO", antigo.toString())).andExpect(jsonPath("$.codigo").value("prazo_vencido"));
        corpo(post("/api/v1/recursos"), eva, recurso("VERIFICACAO", pedido.toString())).andExpect(status().isForbidden());
        String rv = JsonPath.read(corpo(post("/api/v1/recursos"), ana, recurso("VERIFICACAO", pedido.toString())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/admin/recursos/" + rv + "/julgamento"), curador, "{\"deferir\":true,\"motivo\":\"x\"}").andExpect(status().isForbidden());
        pedir(get("/api/v1/admin/recursos"), outraAnalista).andExpect(jsonPath("$[?(@.id == '" + rv + "')]").isNotEmpty());
        corpo(post("/api/v1/admin/recursos/" + rv + "/julgamento"), outraAnalista, "{\"deferir\":true,\"motivo\":\"Documento legível na segunda via\"}")
                .andExpect(jsonPath("$.situacao").value("DEFERIDO"));
        assertThat(jdbc.queryForObject("SELECT situacao FROM pedido_verificacao WHERE id = ?", String.class, pedido)).isEqualTo("EM_ANALISE");

        String startup = publicada(eva);
        jdbc.update("UPDATE startup SET situacao = 'RECUSADA' WHERE id = ?::uuid", startup);
        UUID revisao = UUID.randomUUID();
        jdbc.update("INSERT INTO revisao_curadoria (id, startup_id, tipo, situacao, enviada_em, enviada_por, decidida_em, decidida_por) "
                + "VALUES (?, ?::uuid, 'PUBLICACAO', 'RECUSADA', now(), ?, now(), ?)", revisao, startup, eva.getId(), curador.getId());
        corpo(post("/api/v1/recursos"), ana, recurso("CURADORIA", revisao.toString())).andExpect(status().isForbidden());
        String rc = JsonPath.read(corpo(post("/api/v1/recursos"), eva, recurso("CURADORIA", revisao.toString())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        pedir(get("/api/v1/admin/recursos"), curador).andExpect(jsonPath("$[?(@.id == '" + rc + "')]").isEmpty());
        Usuario outroCurador = interna(Papel.CURADOR, "Clara");
        corpo(post("/api/v1/admin/recursos/" + rc + "/julgamento"), outroCurador, "{\"deferir\":false,\"motivo\":\"Os dados seguem inconsistentes\"}")
                .andExpect(jsonPath("$.situacao").value("INDEFERIDO"));
        assertThat(contar(eva, "Recurso negado")).isEqualTo(1);
        assertThat(situacaoDaStartup(startup)).isEqualTo("RECUSADA");
        UUID devolvida = UUID.randomUUID();
        jdbc.update("INSERT INTO revisao_curadoria (id, startup_id, tipo, situacao, enviada_em, decidida_em) VALUES (?, ?::uuid, 'PUBLICACAO', 'DEVOLVIDA', now(), now())",
                devolvida, startup);
        corpo(post("/api/v1/recursos"), eva, recurso("CURADORIA", devolvida.toString())).andExpect(jsonPath("$.codigo").value("decisao_sem_recurso"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-88, KAN-89
     * Título: Central de atendimento
     * Objetivo: Ler o catálogo de serviços e os artigos sem login, abrir e conduzir um chamado pelos níveis (nota interna,
     *           aguardando o usuário, escalonamento, resolução e avaliação), manter respostas padrão e artigos, abrir um
     *           chamado sem conta e rodar R14 e R20.
     * Decisões: O e-mail simulado guarda o link de confirmação.
     */
    @Test
    void centralDeAtendimento() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario n1 = interna(Papel.SUPORTE_N1, "Nina");
        Usuario n2 = interna(Papel.SUPORTE_N2, "Nilo");
        mvc.perform(get("/api/v1/ajuda/servicos")).andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[?(@.codigo == 'DISPUTAS')].prazo").value("10 dias úteis"));
        mvc.perform(get("/api/v1/ajuda/artigos")).andExpect(jsonPath("$[?(@.titulo == 'O que é o Match')]").isNotEmpty());

        String chamado = JsonPath.read(corpo(post("/api/v1/chamados"), ana, "{\"categoria\":\"CONEXOES\",\"descricao\":\"Não consigo ver o Match\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nivel").value("N1")).andExpect(jsonPath("$.servico").value("ONBOARDING"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        pedir(get("/api/v1/admin/atendimento/chamados"), ana).andExpect(status().isForbidden());
        pedir(get("/api/v1/admin/atendimento/chamados"), n1).andExpect(jsonPath("$[?(@.id == '" + chamado + "')]").isNotEmpty());
        pedir(post("/api/v1/admin/atendimento/chamados/" + chamado + "/assumir"), n1).andExpect(jsonPath("$.situacao").value("EM_ATENDIMENTO"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + chamado + "/mensagens"), n1, "{\"texto\":\"Conferir com o time\",\"interna\":true}")
                .andExpect(status().isOk());
        corpo(post("/api/v1/admin/atendimento/chamados/" + chamado + "/mensagens"), n1, "{\"texto\":\"Pode mandar um print?\",\"aguardarUsuario\":true}")
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_USUARIO"));
        assertThat(contar(ana, "Resposta do atendimento")).isEqualTo(1);
        pedir(get("/api/v1/chamados/" + chamado), ana).andExpect(jsonPath("$.mensagens.length()").value(1))
                .andExpect(jsonPath("$.emailContato").doesNotExist());
        corpo(post("/api/v1/chamados/" + chamado + "/mensagens"), ana, "{\"texto\":\"Segue o print\"}").andExpect(jsonPath("$.situacao").value("EM_ATENDIMENTO"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + chamado + "/escalonamento"), n1, "{\"nivel\":\"N1\",\"motivo\":\"x\"}")
                .andExpect(jsonPath("$.codigo").value("nivel_invalido"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + chamado + "/escalonamento"), n1, "{\"nivel\":\"N2\",\"motivo\":\"Falha que o N1 não resolve\"}")
                .andExpect(jsonPath("$.nivel").value("N2"));
        pedir(post("/api/v1/admin/atendimento/chamados/" + chamado + "/assumir"), n1).andExpect(status().isForbidden());
        corpo(post("/api/v1/chamados/" + chamado + "/avaliacao"), ana, "{\"nota\":5}").andExpect(jsonPath("$.codigo").value("avaliacao_indisponivel"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + chamado + "/resolucao"), n2, "{\"texto\":\"Corrigido: o Match aparece em Conexões.\"}")
                .andExpect(jsonPath("$.situacao").value("RESOLVIDO"));
        corpo(post("/api/v1/chamados/" + chamado + "/avaliacao"), ana, "{\"nota\":6}").andExpect(jsonPath("$.codigo").value("nota_invalida"));
        corpo(post("/api/v1/chamados/" + chamado + "/avaliacao"), ana, "{\"nota\":5,\"comentario\":\"Rápido\"}").andExpect(jsonPath("$.situacao").value("FECHADO"));
        corpo(post("/api/v1/chamados/" + chamado + "/mensagens"), ana, "{\"texto\":\"Obrigada\"}").andExpect(jsonPath("$.codigo").value("chamado_fechado"));
        pedir(get("/api/v1/chamados"), ana).andExpect(jsonPath("$[0].situacao").value("FECHADO"));
        pedir(get("/api/v1/admin/atendimento/indicadores"), n2).andExpect(jsonPath("$[?(@.servico == 'ONBOARDING')].satisfacao").isNotEmpty());

        corpo(post("/api/v1/admin/atendimento/respostas"), n1, "{\"titulo\":\"Boas-vindas\",\"texto\":\"Olá!\"}").andExpect(status().isForbidden());
        String resposta = JsonPath.read(corpo(post("/api/v1/admin/atendimento/respostas"), n2, "{\"titulo\":\"Boas-vindas\",\"texto\":\"Olá!\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(put("/api/v1/admin/atendimento/respostas/" + resposta), n2, "{\"titulo\":\"Boas-vindas\",\"texto\":\"Olá, tudo bem?\"}").andExpect(status().isOk());
        pedir(get("/api/v1/admin/atendimento/respostas"), n1).andExpect(jsonPath("$[?(@.titulo == 'Boas-vindas')].texto").value("Olá, tudo bem?"));
        String artigo = JsonPath.read(corpo(post("/api/v1/admin/atendimento/artigos"), n1, "{\"titulo\":\"Como recorrer\",\"conteudo\":\"Pela central.\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/api/v1/ajuda/artigos")).andExpect(jsonPath("$[?(@.titulo == 'Como recorrer')]").isEmpty());
        corpo(put("/api/v1/admin/atendimento/artigos/" + artigo), n1, "{\"titulo\":\"Como recorrer\",\"conteudo\":\"Pela central.\",\"publicado\":true}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/ajuda/artigos")).andExpect(jsonPath("$[?(@.titulo == 'Como recorrer')]").isNotEmpty());
        pedir(get("/api/v1/admin/atendimento/artigos"), n1).andExpect(status().isOk());

        mvc.perform(post("/api/v1/ajuda/chamados-publicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Zeca\",\"email\":\"zeca\",\"categoria\":\"ACESSO\",\"descricao\":\"Perdi o acesso\"}"))
                .andExpect(jsonPath("$.codigo").value("email_invalido"));
        mvc.perform(post("/api/v1/ajuda/chamados-publicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Zeca\",\"email\":\"zeca@x.com\",\"categoria\":\"DISPUTA\",\"descricao\":\"x\"}"))
                .andExpect(jsonPath("$.codigo").value("categoria_invalida"));
        mvc.perform(post("/api/v1/ajuda/chamados-publicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Zeca\",\"email\":\"zeca@x.com\",\"categoria\":\"ACESSO\",\"descricao\":\"Perdi o acesso ao e-mail\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagens = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagens.capture());
        List<EnvioDeEmail.MensagemEmail> enviadas = mensagens.getAllValues();
        Matcher m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(enviadas.get(enviadas.size() - 1).texto());
        assertThat(m.find()).isTrue();
        String publico = JsonPath.read(mvc.perform(post("/api/v1/ajuda/chamados-publicos/confirmacao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + m.group(1) + "\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.protocolo");
        mvc.perform(post("/api/v1/ajuda/chamados-publicos/confirmacao").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + m.group(1) + "\"}"))
                .andExpect(jsonPath("$.codigo").value("link_invalido"));
        pedir(get("/api/v1/admin/atendimento/chamados/" + publico), n2).andExpect(jsonPath("$.nivel").value("N2"))
                .andExpect(jsonPath("$.emailContato").value("zeca@x.com"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + publico + "/mensagens"), n2, "{\"texto\":\"Confirme os dados\",\"aguardarUsuario\":true}")
                .andExpect(status().isOk());
        verify(email, atLeastOnce()).enviar(org.mockito.ArgumentMatchers.argThat(e -> e != null && "chamado_resposta".equals(e.rotulo())));

        jdbc.update("UPDATE chamado SET aguardando_desde = now() - interval '8 days' WHERE id = ?::uuid", publico);
        rotinas.executarManual("R20", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM chamado WHERE id = ?::uuid", String.class, publico)).isEqualTo("FECHADO");

        String atrasado = JsonPath.read(corpo(post("/api/v1/chamados"), ana, "{\"categoria\":\"OUTRO\",\"descricao\":\"Dúvida qualquer\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        jdbc.update("UPDATE chamado SET prazo_primeira_resposta = now() - interval '1 hour' WHERE id = ?::uuid", atrasado);
        rotinas.executarManual("R14", null);
        assertThat(jdbc.queryForObject("SELECT vencido_alertado_em IS NOT NULL FROM chamado WHERE id = ?::uuid", Boolean.class, atrasado)).isTrue();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-103
     * Título: Disputa entre usuários
     * Objetivo: Abrir a disputa sobre a participação na rodada, ouvir a outra parte e resolver sem acordo, marcando a
     *           participação "em disputa".
     * Decisões: Rodada e investimento criados direto no banco.
     */
    @Test
    void disputaEntreUsuarios() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario n2 = interna(Papel.SUPORTE_N2, "Nilo");
        Usuario n1 = interna(Papel.SUPORTE_N1, "Nina");
        String startup = publicada(eva);
        UUID rodada = UUID.randomUUID();
        jdbc.update("INSERT INTO rodada (id, startup_id, situacao, estagio, instrumento, valor_alvo, valor_minimo, valor_maximo, prazo, criada_por, criada_em, "
                + "atualizada_em, aberta_em) VALUES (?, ?::uuid, 'ENCERRADA', 'SEED', 'MUTUO_CONVERSIVEL', 250000, 100000, 300000, current_date, ?, now(), now(), now())",
                rodada, startup, eva.getId());
        UUID participante = UUID.randomUUID();
        jdbc.update("INSERT INTO rodada_participante (id, rodada_id, investidor_chave, pessoa_id, situacao, entrou_em, atualizado_em) VALUES (?, ?, ?, ?, 'ATIVO', now(), now())",
                participante, rodada, ana.getId(), ana.getId());
        UUID investimento = UUID.randomUUID();
        jdbc.update("INSERT INTO rodada_investimento (id, rodada_id, participante_id, valor, situacao, criado_em) VALUES (?, ?, ?, 50000, 'CONTESTADO', now())",
                investimento, rodada, participante);

        corpo(post("/api/v1/chamados"), eva, "{\"categoria\":\"DISPUTA\",\"descricao\":\"A investidora contestou\"}")
                .andExpect(jsonPath("$.codigo").value("outra_parte_invalida"));
        String disputa = JsonPath.read(corpo(post("/api/v1/chamados"), eva, "{\"categoria\":\"DISPUTA\",\"descricao\":\"A investidora contestou\",\"outraParteId\":\""
                + ana.getId() + "\",\"referenciaTipo\":\"RODADA_INVESTIMENTO\",\"referenciaId\":\"" + investimento + "\"}")
                .andExpect(jsonPath("$.servico").value("DISPUTAS")).andExpect(jsonPath("$.nivel").value("N2"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(contar(ana, "Disputa aberta com você")).isEqualTo(1);
        pedir(get("/api/v1/chamados/" + disputa), ana).andExpect(jsonPath("$.souOutraParte").value(true));
        corpo(post("/api/v1/chamados/" + disputa + "/mensagens"), ana, "{\"texto\":\"Não investi esse valor\"}").andExpect(status().isOk());
        pedir(get("/api/v1/chamados"), ana).andExpect(jsonPath("$[0].souOutraParte").value(true));
        pedir(get("/api/v1/admin/atendimento/chamados/" + disputa), n1).andExpect(status().isForbidden());
        corpo(post("/api/v1/admin/atendimento/chamados/" + disputa + "/resolucao"), n2, "{\"texto\":\"Sem acordo\"}")
                .andExpect(jsonPath("$.codigo").value("resultado_invalido"));
        corpo(post("/api/v1/admin/atendimento/chamados/" + disputa + "/resolucao"), n2, "{\"texto\":\"Sem acordo entre as partes\",\"resultadoDisputa\":\"EM_DISPUTA\"}")
                .andExpect(jsonPath("$.resultadoDisputa").value("EM_DISPUTA"));
        assertThat(jdbc.queryForObject("SELECT situacao FROM rodada_investimento WHERE id = ?", String.class, investimento)).isEqualTo("EM_DISPUTA");
        assertThat(contar(eva, "Chamado resolvido")).isEqualTo(1);
        assertThat(contar(ana, "Chamado resolvido")).isEqualTo(1);

        corpo(post("/api/v1/chamados"), ana, "{\"categoria\":\"DISPUTA\",\"descricao\":\"Registro errado\",\"referenciaTipo\":\"RODADA_INVESTIMENTO\",\"referenciaId\":\""
                + investimento + "\"}").andExpect(jsonPath("$.outraParte").value("Eva"));
        corpo(post("/api/v1/chamados"), eva, "{\"categoria\":\"DISPUTA\",\"descricao\":\"Registro errado\",\"referenciaTipo\":\"RODADA_INVESTIMENTO\",\"referenciaId\":\""
                + investimento + "\"}").andExpect(jsonPath("$.outraParte").value("Ana"));
        corpo(post("/api/v1/chamados"), n2, "{\"categoria\":\"DISPUTA\",\"descricao\":\"Registro errado\",\"referenciaTipo\":\"RODADA_INVESTIMENTO\",\"referenciaId\":\""
                + investimento + "\"}").andExpect(jsonPath("$.codigo").value("outra_parte_invalida"));
    }

    private String novoCaso(Usuario quem, Usuario alvo, String categoria) throws Exception {
        corpo(post("/api/v1/denuncias"), quem, denuncia("PERFIL", alvo.getId(), categoria)).andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT id::text FROM caso_moderacao WHERE objeto_id = ? AND situacao <> 'DECIDIDO'", String.class, alvo.getId());
    }

    private static String denuncia(String tipo, UUID id, String categoria) {
        return "{\"objetoTipo\":\"" + tipo + "\",\"objetoId\":\"" + id + "\",\"categoria\":\"" + categoria + "\",\"descricao\":\"Comportamento inadequado repetido\"}";
    }

    private static String recurso(String tipo, String id) {
        return "{\"origemTipo\":\"" + tipo + "\",\"origemId\":\"" + id + "\",\"argumentos\":\"A decisão não considerou os documentos novos que enviei.\"}";
    }

    private Instant suspensaAte(Usuario u) {
        java.sql.Timestamp t = jdbc.queryForObject("SELECT suspensa_ate FROM usuario WHERE id = ?", java.sql.Timestamp.class, u.getId());
        return t == null ? null : t.toInstant();
    }

    private String situacaoDaStartup(String id) {
        return jdbc.queryForObject("SELECT situacao FROM startup WHERE id = ?::uuid", String.class, id);
    }

    private String situacaoDaConexao(String id) {
        return jdbc.queryForObject("SELECT situacao FROM conexao WHERE id = ?::uuid", String.class, id);
    }

    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String publicada(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Moderada\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");
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
        Usuario u = Usuario.novo(nome, "m-" + UUID.randomUUID() + "@exemplo.com", senhas.encode("Senha!123"), Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    private Usuario interna(Papel papel, String nome) {
        Usuario u = Usuario.novoInterno(nome, "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
