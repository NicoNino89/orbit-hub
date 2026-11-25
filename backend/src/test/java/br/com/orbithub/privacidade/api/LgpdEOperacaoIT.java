package br.com.orbithub.privacidade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
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
 * Hora: 20:54
 * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
 * Título: Testes de integração da LGPD e da operação
 * Objetivo: Conferir os pedidos do titular, a cópia dos dados, a exclusão da conta (pela pessoa e fora do fluxo com
 *           segunda aprovação), os incidentes, os times com rodízio e ausências, o painel, a R14 nas filas, a R11, a R12 e
 *           a titularidade (cadastro assistido e transferência).
 * Decisões: Banco real (Testcontainers) e e-mail simulado; equipe criada direto no repositório.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LgpdEOperacaoIT {

    private static final String SENHA = "Senha!123";

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
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Pedidos do titular
     * Objetivo: Pedir com a senha, receber a cópia dos dados, responder pelo encarregado e excluir fora do fluxo com
     *           segunda aprovação.
     * Decisões: Dois encarregados e um administrador.
     */
    @Test
    void pedidosDoTitular() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario bia = conta(Papel.INVESTIDOR, "Bia");
        Usuario enc = interna(Papel.ENCARREGADO_DADOS, "Enzo");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        corpo(post("/api/v1/privacidade/pedidos"), ana, "{\"tipo\":\"CORRECAO\",\"senha\":\"errada\"}").andExpect(jsonPath("$.codigo").value("senha_incorreta"));
        String pedido = JsonPath.read(corpo(post("/api/v1/privacidade/pedidos"), ana, "{\"tipo\":\"CORRECAO\",\"detalhes\":\"Nome errado\",\"senha\":\""
                + SENHA + "\"}").andExpect(status().isCreated()).andExpect(jsonPath("$.situacao").value(Matchers.oneOf("ABERTO", "EM_ANALISE")))
                .andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/privacidade/pedidos"), ana, "{\"tipo\":\"CORRECAO\",\"senha\":\"" + SENHA + "\"}").andExpect(jsonPath("$.codigo").value("pedido_em_aberto"));
        corpo(post("/api/v1/privacidade/meus-dados"), ana, "{\"senha\":\"" + SENHA + "\"}").andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("meus-dados")))
                .andExpect(jsonPath("$.conta[0].email").value(ana.getEmail())).andExpect(jsonPath("$.papeis[0]").value("INVESTIDOR"));
        pedir(get("/api/v1/privacidade/pedidos"), ana).andExpect(jsonPath("$.length()").value(2));
        pedir(get("/api/v1/admin/privacidade/pedidos"), ana).andExpect(status().isForbidden());
        pedir(get("/api/v1/admin/privacidade/pedidos"), enc).andExpect(jsonPath("$[?(@.id == '" + pedido + "')].titular").value("Ana"));
        corpo(post("/api/v1/admin/privacidade/pedidos/" + pedido + "/resposta"), enc, "{}").andExpect(jsonPath("$.codigo").value("resposta_obrigatoria"));
        corpo(post("/api/v1/admin/privacidade/pedidos/" + pedido + "/resposta"), enc, "{\"resposta\":\"Nome corrigido\"}")
                .andExpect(jsonPath("$.situacao").value("CONCLUIDO"));
        corpo(post("/api/v1/admin/privacidade/pedidos/" + pedido + "/resposta"), enc, "{\"resposta\":\"x\"}").andExpect(jsonPath("$.codigo").value("pedido_encerrado"));
        assertThat(contar(ana, "Resposta ao seu pedido de privacidade")).isEqualTo(1);

        String eliminacao = JsonPath.read(corpo(post("/api/v1/privacidade/pedidos"), bia, "{\"tipo\":\"ELIMINACAO\",\"senha\":\"" + SENHA + "\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/admin/privacidade/pedidos/" + eliminacao + "/exclusao"), enc, "{\"justificativa\":\"Titular sem acesso ao e-mail\"}")
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_APROVACAO"));
        String proposta = jdbc.queryForObject("SELECT id::text FROM proposta_aprovacao WHERE objeto_id = ? AND situacao = 'PENDENTE'", String.class, eliminacao);
        corpo(post("/api/v1/admin/aprovacoes/" + proposta + "/aprovacao"), enc, "{\"justificativa\":\"Eu mesmo\"}").andExpect(status().isUnprocessableContent());
        corpo(post("/api/v1/admin/aprovacoes/" + proposta + "/aprovacao"), chefe, "{\"justificativa\":\"Conferido\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT excluida_em IS NOT NULL FROM usuario WHERE id = ?", Boolean.class, bia.getId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT situacao FROM pedido_titular WHERE id = ?::uuid", String.class, eliminacao)).isEqualTo("CONCLUIDO");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Exclusão da conta pelo titular
     * Objetivo: Exigir senha e confirmação, impedir quando há administração pendente e anonimizar a conta.
     * Decisões: Startup com outro membro ativo.
     */
    @Test
    void exclusaoDaConta() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario caio = conta(Papel.EMPREENDEDOR, "Caio");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String startup = startup(eva);
        membro(startup, caio, "COLABORADOR");
        pedir(get("/api/v1/conta/exclusao"), eva).andExpect(jsonPath("$.pendencias[0]").value("Excluível"));
        corpo(post("/api/v1/conta/exclusao"), eva, "{\"senha\":\"" + SENHA + "\",\"confirmacao\":\"EXCLUIR\"}")
                .andExpect(jsonPath("$.codigo").value("administracao_pendente"));
        corpo(post("/api/v1/conta/exclusao"), ana, "{\"senha\":\"x\",\"confirmacao\":\"EXCLUIR\"}").andExpect(jsonPath("$.codigo").value("senha_incorreta"));
        corpo(post("/api/v1/conta/exclusao"), ana, "{\"senha\":\"" + SENHA + "\",\"confirmacao\":\"sim\"}").andExpect(jsonPath("$.codigo").value("confirmacao_invalida"));
        String emailAntigo = ana.getEmail();
        corpo(post("/api/v1/conta/exclusao"), ana, "{\"senha\":\"" + SENHA + "\",\"confirmacao\":\"EXCLUIR\"}").andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT email FROM usuario WHERE id = ?", String.class, ana.getId())).startsWith("excluida-");
        assertThat(jdbc.queryForObject("SELECT status FROM usuario WHERE id = ?", String.class, ana.getId())).isEqualTo("DESATIVADO");
        verify(email, atLeastOnce()).enviar(argThat(m -> m != null && "conta_excluida".equals(m.rotulo()) && emailAntigo.equals(m.para())));
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + emailAntigo + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
        corpo(post("/api/v1/conta/exclusao"), interna(Papel.ADMIN, "Chefe"), "{\"senha\":\"x\",\"confirmacao\":\"EXCLUIR\"}")
                .andExpect(jsonPath("$.codigo").value("conta_da_equipe"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Incidente de segurança
     * Objetivo: Registrar pela equipe, avaliar, comunicar e encerrar só com as lições e as comunicações do risco alto.
     * Decisões: Moderador registra; encarregado conduz.
     */
    @Test
    void incidenteDeSeguranca() throws Exception {
        Usuario mo = interna(Papel.MODERADOR, "Mo");
        Usuario enc = interna(Papel.ENCARREGADO_DADOS, "Enzo");
        corpo(post("/api/v1/admin/incidentes"), mo, "{\"titulo\":\"\",\"descricao\":\"x\"}").andExpect(jsonPath("$.codigo").value("titulo_invalido"));
        corpo(post("/api/v1/admin/incidentes"), mo, "{\"titulo\":\"Planilha exposta\",\"descricao\":\"x\",\"detectadoEm\":\"2999-01-01T00:00:00Z\"}")
                .andExpect(jsonPath("$.codigo").value("data_invalida"));
        String id = JsonPath.read(corpo(post("/api/v1/admin/incidentes"), mo, "{\"titulo\":\"Planilha exposta\",\"descricao\":\"Link público de uma planilha\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.situacao").value("ABERTO")).andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(contar(enc, "Incidente de segurança registrado")).isEqualTo(1);
        pedir(get("/api/v1/admin/incidentes"), mo).andExpect(status().isForbidden());
        String base = "/api/v1/admin/incidentes/" + id;
        corpo(post(base + "/encerramento"), enc, "{}").andExpect(jsonPath("$.codigo").value("licoes_obrigatorias"));
        corpo(post(base + "/encerramento"), enc, "{\"licoes\":\"l\",\"acoesPreventivas\":\"a\"}").andExpect(jsonPath("$.codigo").value("avaliacao_pendente"));
        corpo(put(base + "/avaliacao"), enc, "{\"risco\":\"ENORME\"}").andExpect(jsonPath("$.codigo").value("risco_invalido"));
        corpo(put(base + "/avaliacao"), enc, "{\"risco\":\"ALTO\",\"contencao\":\"Link revogado\",\"titularesAfetados\":\"120 investidores\"}")
                .andExpect(jsonPath("$.situacao").value("CONTIDO"));
        corpo(post(base + "/encerramento"), enc, "{\"licoes\":\"l\",\"acoesPreventivas\":\"a\"}").andExpect(jsonPath("$.codigo").value("comunicacao_pendente"));
        corpo(post(base + "/comunicacao"), enc, "{\"destino\":\"X\"}").andExpect(jsonPath("$.codigo").value("destino_invalido"));
        corpo(post(base + "/comunicacao"), enc, "{\"destino\":\"ANPD\"}").andExpect(jsonPath("$.comunicadoAnpdEm").isNotEmpty());
        corpo(post(base + "/comunicacao"), enc, "{\"destino\":\"TITULARES\"}").andExpect(status().isOk());
        corpo(post(base + "/encerramento"), enc, "{\"licoes\":\"Revisar links\",\"acoesPreventivas\":\"Bloquear compartilhamento público\"}")
                .andExpect(jsonPath("$.situacao").value("ENCERRADO")).andExpect(jsonPath("$.historico.length()").value(5));
        corpo(put(base + "/avaliacao"), enc, "{}").andExpect(jsonPath("$.codigo").value("incidente_encerrado"));
        pedir(get("/api/v1/admin/incidentes"), enc).andExpect(jsonPath("$[?(@.id == '" + id + "')].situacao").value("ENCERRADO"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76, KAN-82, KAN-108
     * Título: Times, painel e monitor de prazos
     * Objetivo: Definir líder, registrar ausência (que pula o rodízio), devolver casos, ver o painel e rodar a R14.
     * Decisões: Dois moderadores; um ausente hoje.
     */
    @Test
    void timesPainelEMonitor() throws Exception {
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        Usuario m1 = interna(Papel.MODERADOR, "Moa");
        Usuario m2 = interna(Papel.MODERADOR, "Mel");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario caio = conta(Papel.INVESTIDOR, "Caio");
        corpo(put("/api/v1/admin/times/MODERACAO/lider"), chefe, "{\"membroId\":\"" + ana.getId() + "\"}").andExpect(jsonPath("$.codigo").value("fora_do_time"));
        corpo(put("/api/v1/admin/times/MODERACAO/lider"), m1, "{\"membroId\":\"" + m2.getId() + "\"}").andExpect(status().isForbidden());
        corpo(put("/api/v1/admin/times/MODERACAO/lider"), chefe, "{\"membroId\":\"" + m2.getId() + "\"}").andExpect(status().isNoContent());
        pedir(get("/api/v1/admin/times"), m1).andExpect(jsonPath("$[?(@.time == 'MODERACAO')].lider").value("Mel"));
        LocalDate hoje = LocalDate.now();
        corpo(post("/api/v1/admin/times/ausencias"), m1, "{\"inicio\":\"" + hoje + "\",\"fim\":\"" + hoje.minusDays(1) + "\"}")
                .andExpect(jsonPath("$.codigo").value("periodo_invalido"));
        String ausencia = JsonPath.read(corpo(post("/api/v1/admin/times/ausencias"), m1, "{\"inicio\":\"" + hoje.minusDays(1) + "\",\"fim\":\"" + hoje.plusDays(5)
                + "\",\"motivo\":\"Férias\"}").andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        pedir(get("/api/v1/admin/times/ausencias"), m1).andExpect(jsonPath("$[0].motivo").value("Férias"));
        for (int i = 0; i < 3; i++) {
            Usuario alvo = conta(Papel.INVESTIDOR, "Alvo" + i);
            corpo(post("/api/v1/denuncias"), ana, "{\"objetoTipo\":\"PERFIL\",\"objetoId\":\"" + alvo.getId() + "\",\"categoria\":\"SPAM\",\"descricao\":\"Mensagens repetidas\"}")
                    .andExpect(status().isCreated());
            UUID resp = jdbc.queryForObject("SELECT responsavel_id FROM caso_moderacao WHERE objeto_id = ?", UUID.class, alvo.getId());
            assertThat(resp).isNotNull().isNotEqualTo(m1.getId());
        }
        pedir(delete("/api/v1/admin/times/ausencias/" + ausencia), m2).andExpect(status().isNotFound());
        pedir(delete("/api/v1/admin/times/ausencias/" + ausencia), m1).andExpect(status().isNoContent());
        pedir(post("/api/v1/admin/times/membros/" + m2.getId() + "/devolucao"), chefe).andExpect(jsonPath("$.casos").value(Matchers.greaterThanOrEqualTo(0)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM caso_moderacao WHERE responsavel_id = ? AND situacao <> 'DECIDIDO'", Integer.class, m2.getId())).isZero();

        String perto = JsonPath.read(corpo(post("/api/v1/chamados"), caio, "{\"categoria\":\"OUTRO\",\"descricao\":\"Dúvida\"}").andReturn().getResponse()
                .getContentAsString(), "$.id");
        jdbc.update("UPDATE chamado SET criado_em = now() - interval '10 hours', prazo_primeira_resposta = now() + interval '1 hour' WHERE id = ?::uuid", perto);
        String caso = jdbc.queryForObject("SELECT id::text FROM caso_moderacao WHERE situacao <> 'DECIDIDO' ORDER BY criado_em DESC LIMIT 1", String.class);
        jdbc.update("UPDATE caso_moderacao SET prazo = now() - interval '1 hour' WHERE id = ?::uuid", caso);
        rotinas.executarManual("R14", null);
        assertThat(jdbc.queryForObject("SELECT alertado_em IS NOT NULL AND vencido_alertado_em IS NULL FROM chamado WHERE id = ?::uuid", Boolean.class, perto)).isTrue();
        assertThat(jdbc.queryForObject("SELECT vencido_alertado_em IS NOT NULL FROM caso_moderacao WHERE id = ?::uuid", Boolean.class, caso)).isTrue();
        assertThat(contar(m2, "Denúncia com prazo vencido")).isPositive();

        pedir(get("/api/v1/admin/operacao/painel"), m1).andExpect(jsonPath("$.filas.length()").value(7))
                .andExpect(jsonPath("$.filas[?(@.fila == 'MODERACAO')].vencidos").value(Matchers.hasItem(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.ecossistema.novosCadastros").value(Matchers.greaterThanOrEqualTo(2)));
        pedir(get("/api/v1/admin/operacao/painel"), ana).andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-107, KAN-93
     * Título: Contas inativas e retenção
     * Objetivo: Avisar e desativar a conta inativa (R11) e anonimizar o cadastro não confirmado e apagar visitas antigas (R12).
     * Decisões: Datas ajustadas no banco.
     */
    @Test
    void contasInativasERetencao() throws Exception {
        Usuario velha = conta(Papel.INVESTIDOR, "Velha");
        jdbc.update("UPDATE usuario SET ultimo_acesso_em = now() - interval '13 months' WHERE id = ?", velha.getId());
        rotinas.executarManual("R11", null);
        assertThat(contar(velha, "Sua conta está sem uso")).isEqualTo(1);
        rotinas.executarManual("R11", null);
        assertThat(contar(velha, "Sua conta está sem uso")).isEqualTo(1);
        jdbc.update("UPDATE usuario SET aviso_inatividade_em = now() - interval '31 days' WHERE id = ?", velha.getId());
        rotinas.executarManual("R11", null);
        assertThat(jdbc.queryForObject("SELECT status FROM usuario WHERE id = ?", String.class, velha.getId())).isEqualTo("DESATIVADO");

        Usuario pendente = usuarios.save(Usuario.novo("Pendente", "p-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(Papel.INVESTIDOR), Instant.now()));
        jdbc.update("UPDATE usuario SET criado_em = now() - interval '10 days' WHERE id = ?", pendente.getId());
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        String startup = startup(eva);
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        jdbc.update("INSERT INTO catalogo_visualizacao (usuario_id, startup_id, vista_em) VALUES (?, ?::uuid, now() - interval '13 months')", ana.getId(), startup);
        rotinas.executarManual("R12", null);
        assertThat(jdbc.queryForObject("SELECT nome FROM usuario WHERE id = ?", String.class, pendente.getId())).isEqualTo("Conta excluída");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo_visualizacao WHERE usuario_id = ?", Integer.class, ana.getId())).isZero();
        assertThat(jdbc.queryForObject("SELECT mensagem FROM rotina_execucao WHERE rotina = 'R12' ORDER BY iniciada_em DESC LIMIT 1", String.class))
                .contains("visualização(ões) do catálogo");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Titularidade
     * Objetivo: Criar a organização convidada e transferir a titularidade da startup com segunda aprovação.
     * Decisões: N2 propõe, administrador aprova.
     */
    @Test
    void titularidade() throws Exception {
        Usuario n2 = interna(Papel.SUPORTE_N2, "Nilo");
        Usuario n1 = interna(Papel.SUPORTE_N1, "Nina");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        String cnpj = cnpj();
        String corpoOrg = "{\"nome\":\"Fundo Parceiro\",\"tipo\":\"FUNDO_VC\",\"cnpj\":\"" + cnpj + "\",\"emailResponsavel\":\"resp@fundo.com\"}";
        corpo(post("/api/v1/admin/atendimento/organizacoes-assistidas"), n1, corpoOrg).andExpect(status().isForbidden());
        String org = JsonPath.read(corpo(post("/api/v1/admin/atendimento/organizacoes-assistidas"), n2, corpoOrg).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.organizacaoId");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organizacao_membro WHERE organizacao_id = ?::uuid", Integer.class, org)).isZero();
        verify(email, atLeastOnce()).enviar(argThat(m -> m != null && "resp@fundo.com".equals(m.para())));

        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario caio = conta(Papel.EMPREENDEDOR, "Caio");
        String startup = startup(eva);
        membro(startup, caio, "FUNDADOR");
        corpo(post("/api/v1/admin/atendimento/transferencias"), n2, "{\"alvo\":\"STARTUP\",\"alvoId\":\"" + startup + "\",\"emailNovoTitular\":\"ninguem@x.com\","
                + "\"comprovacao\":\"x\"}").andExpect(jsonPath("$.codigo").value("titular_invalido"));
        String proposta = JsonPath.read(corpo(post("/api/v1/admin/atendimento/transferencias"), n2, "{\"alvo\":\"STARTUP\",\"alvoId\":\"" + startup
                + "\",\"emailNovoTitular\":\"" + caio.getEmail() + "\",\"comprovacao\":\"Atestado de óbito conferido\"}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.propostaId");
        corpo(post("/api/v1/admin/aprovacoes/" + proposta + "/aprovacao"), chefe, "{\"justificativa\":\"Documentos conferidos\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT papel FROM startup_membro WHERE startup_id = ?::uuid AND usuario_id = ? AND saiu_em IS NULL", String.class,
                startup, caio.getId())).isEqualTo("FUNDADOR_ADMINISTRADOR");
        assertThat(jdbc.queryForObject("SELECT papel FROM startup_membro WHERE startup_id = ?::uuid AND usuario_id = ? AND saiu_em IS NULL", String.class,
                startup, eva.getId())).isEqualTo("FUNDADOR");
        assertThat(contar(caio, "Você agora é o titular")).isEqualTo(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: startup
     * Objetivo: Rotina auxiliar startup.
     * Decisões: Ver a documentação da classe.
     */
    private String startup(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Excluível\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: membro
     * Objetivo: Rotina auxiliar membro.
     * Decisões: Ver a documentação da classe.
     */
    private void membro(String startup, Usuario u, String papel) {
        jdbc.update("INSERT INTO startup_membro (id, startup_id, usuario_id, papel, entrou_em) VALUES (?, ?::uuid, ?, ?, now())", UUID.randomUUID(), startup,
                u.getId(), papel);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: cnpj
     * Objetivo: Rotina auxiliar cnpj.
     * Decisões: Ver a documentação da classe.
     */
    private static String cnpj() {
        int[] d = new int[14];
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            d[i] = r.nextInt(10);
        }
        d[12] = digito(d, 12);
        d[13] = digito(d, 13);
        StringBuilder s = new StringBuilder();
        for (int x : d) {
            s.append(x);
        }
        return s.toString();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: digito
     * Objetivo: Rotina auxiliar digito.
     * Decisões: Ver a documentação da classe.
     */
    private static int digito(int[] d, int n) {
        int[] pesos = n == 12 ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2} : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma += d[i] * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: pedir
     * Objetivo: Rotina auxiliar pedir.
     * Decisões: Ver a documentação da classe.
     */
    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: corpo
     * Objetivo: Rotina auxiliar corpo.
     * Decisões: Ver a documentação da classe.
     */
    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: contar
     * Objetivo: Rotina auxiliar contar.
     * Decisões: Ver a documentação da classe.
     */
    private int contar(Usuario quem, String titulo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo LIKE ?", Integer.class, quem.getId(), titulo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: conta
     * Objetivo: Rotina auxiliar conta.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario conta(Papel papel, String nome) {
        Usuario u = Usuario.novo(nome, "l-" + UUID.randomUUID() + "@exemplo.com", senhas.encode(SENHA), Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: interna
     * Objetivo: Rotina auxiliar interna.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario interna(Papel papel, String nome) {
        Usuario u = Usuario.novoInterno(nome, "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
     * Título: bearer
     * Objetivo: Rotina auxiliar bearer.
     * Decisões: Ver a documentação da classe.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
