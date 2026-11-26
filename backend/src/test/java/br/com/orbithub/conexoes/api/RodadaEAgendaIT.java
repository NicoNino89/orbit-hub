package br.com.orbithub.conexoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-106, KAN-109, KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
 * Título: Testes de integração da rodada e da agenda
 * Objetivo: Percorrer participantes com intenção e excesso de demanda, liderança, convite do líder com aprovação,
 *           prorrogação, pedido de encerramento, conclusão com confirmação, cancelamento com volta das conexões, R6 e R7; e
 *           a agenda: proposta, contraproposta, aceite, convite .ics, registro com ausência, anotação, coletiva, R5 e o
 *           cancelamento automático pelo fim da conexão.
 * Decisões: Rodadas abertas direto no banco (sem exigir CNPJ); verificações direto no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RodadaEAgendaIT {

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
     * Hora: 14:23
     * Cartão Jira: KAN-59, KAN-64, KAN-109
     * Título: Rodada completa
     * Objetivo: Entrar com intenção, sinalizar excesso, definir o líder, aprovar o convidado do líder, prorrogar, pedir o
     *           encerramento, concluir e confirmar o investimento.
     * Decisões: O co-investidor não vê os valores dos outros.
     */
    @Test
    void rodadaCompleta() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario bia = conta(Papel.INVESTIDOR, "Bia");
        Usuario cai = conta(Papel.INVESTIDOR, "Cai");
        String startup = publicada(eva);
        String rodada = rodada(startup, eva, 30);
        String cAna = match(eva, ana, startup);
        String cBia = match(eva, bia, startup);

        String pAna = JsonPath.read(corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), ana, "{\"intencao\":200000}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), ana, "{\"intencao\":1}").andExpect(jsonPath("$.codigo").value("ja_participa"));
        corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), bia, "{\"intencao\":150000}").andExpect(status().isCreated());
        assertThat(situacao(cAna)).isEqualTo("EM_NEGOCIACAO");
        pedir(get("/api/v1/rodadas/" + rodada + "/painel"), eva).andExpect(jsonPath("$.somaIntencoes").value(350000))
                .andExpect(jsonPath("$.excessoDeDemanda").value(true)).andExpect(jsonPath("$.participantes.length()").value(2));
        pedir(get("/api/v1/catalogo/startups/" + startup + "/rodada"), bia).andExpect(jsonPath("$.participantes.length()").value(0))
                .andExpect(jsonPath("$.somaIntencoes").doesNotExist()).andExpect(jsonPath("$.minhaParticipacao.intencao").value(150000));

        pedir(post("/api/v1/rodadas/" + rodada + "/participantes/" + pAna + "/lideranca"), eva).andExpect(status().isNoContent());
        corpo(post("/api/v1/rodadas/" + rodada + "/lideranca/resposta"), bia, "{\"aprovar\":true}").andExpect(jsonPath("$.codigo").value("sem_convite"));
        corpo(post("/api/v1/rodadas/" + rodada + "/lideranca/resposta"), ana, "{\"aprovar\":true}").andExpect(status().isNoContent());
        pedir(get("/api/v1/catalogo/startups/" + startup + "/rodada"), ana).andExpect(jsonPath("$.souLider").value(true))
                .andExpect(jsonPath("$.somaIntencoes").value(350000)).andExpect(jsonPath("$.participantes.length()").value(2));
        pedir(get("/api/v1/catalogo/startups/" + startup + "/rodada"), bia).andExpect(jsonPath("$.lider").value("Ana"));

        corpo(post("/api/v1/rodadas/" + rodada + "/convites"), bia, "{\"email\":\"" + cai.getEmail() + "\"}").andExpect(status().isForbidden());
        String pCai = JsonPath.read(corpo(post("/api/v1/rodadas/" + rodada + "/convites"), ana, "{\"email\":\"" + cai.getEmail() + "\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/rodadas/" + rodada + "/participantes/" + pCai + "/decisao"), eva, "{\"aprovar\":true}")
                .andExpect(jsonPath("$.situacao").value("ATIVO"));
        assertThat(jdbc.queryForObject("SELECT situacao FROM conexao WHERE startup_id = ?::uuid AND pessoa_id = ?", String.class, startup, cai.getId()))
                .isEqualTo("EM_NEGOCIACAO");

        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        corpo(post("/api/v1/rodadas/" + rodada + "/prorrogacao"), eva, "{\"prazo\":\"" + hoje.plusDays(90) + "\"}")
                .andExpect(jsonPath("$.codigo").value("prorrogacao_invalida"));
        corpo(post("/api/v1/rodadas/" + rodada + "/prorrogacao"), eva, "{\"prazo\":\"" + hoje.plusDays(45) + "\"}").andExpect(status().isOk());
        corpo(post("/api/v1/rodadas/" + rodada + "/prorrogacao"), eva, "{\"prazo\":\"" + hoje.plusDays(50) + "\"}")
                .andExpect(jsonPath("$.codigo").value("prorrogacao_esgotada"));
        corpo(put("/api/v1/rodadas/" + rodada + "/politica"), eva, "{\"politica\":\"PROPORCIONAL\"}").andExpect(jsonPath("$.politica").value("PROPORCIONAL"));
        pedir(post("/api/v1/rodadas/" + rodada + "/pedido-de-encerramento"), ana).andExpect(status().isNoContent());
        assertThat(contar(eva, "O líder pediu o encerramento%")).isEqualTo(1);

        corpo(post("/api/v1/rodadas/" + rodada + "/conclusao"), eva, "{\"valorCaptado\":200000,\"instrumento\":\"Mútuo\",\"dataFechamento\":\""
                + hoje + "\",\"investidores\":[{\"participanteId\":\"" + pAna + "\",\"valor\":200000}]}").andExpect(jsonPath("$.resultado").value("CONCLUIDA"));
        assertThat(situacao(cBia)).isEqualTo("ENCERRADA");
        String investimento = jdbc.queryForObject("SELECT id::text FROM rodada_investimento WHERE participante_id = ?::uuid", String.class, pAna);
        corpo(post("/api/v1/rodadas/investimentos/" + investimento + "/resposta"), bia, "{\"aprovar\":true}").andExpect(status().isNotFound());
        corpo(post("/api/v1/rodadas/investimentos/" + investimento + "/resposta"), ana, "{\"aprovar\":true}").andExpect(jsonPath("$.situacao").value("CONFIRMADA"));
        assertThat(situacao(cAna)).isEqualTo("INVESTIU");
        pedir(get("/api/v1/rodadas/" + rodada + "/painel"), eva).andExpect(jsonPath("$.valorCaptado").value(200000))
                .andExpect(jsonPath("$.investimentos[0].situacao").value("CONFIRMADA"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109, KAN-106
     * Título: Cancelamento e rotinas
     * Objetivo: Cancelar devolvendo a conexão, e conferir R6 (aguardando e sem resultado) e R7 (lembrete e não confirmada).
     * Decisões: Datas recuadas no banco.
     */
    @Test
    void cancelamentoERotinas() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String startup = publicada(eva);
        String c = match(eva, ana, startup);
        String rodada = rodada(startup, eva, 30);
        corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), ana, "{\"intencao\":1000}").andExpect(status().isCreated());
        corpo(post("/api/v1/rodadas/" + rodada + "/cancelamento"), eva, "{}").andExpect(jsonPath("$.codigo").value("motivo_obrigatorio"));
        corpo(post("/api/v1/rodadas/" + rodada + "/cancelamento"), eva, "{\"motivo\":\"Mudança de estratégia\"}").andExpect(jsonPath("$.resultado").value("CANCELADA"));
        assertThat(situacao(c)).isEqualTo("MATCH");
        assertThat(contar(ana, "Rodada cancelada")).isEqualTo(1);

        String vencida = rodada(startup, eva, -1);
        rotinas.executarManual("R6", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM rodada WHERE id = ?::uuid", String.class, vencida)).isEqualTo("AGUARDANDO_RESULTADO");
        corpo(post("/api/v1/rodadas/" + vencida + "/participantes"), ana, "{\"intencao\":1000}").andExpect(jsonPath("$.codigo").value("rodada_fechada"));
        jdbc.update("UPDATE rodada SET aguardando_desde = now() - interval '20 days' WHERE id = ?::uuid", vencida);
        rotinas.executarManual("R6", null);
        assertThat(jdbc.queryForObject("SELECT resultado FROM rodada WHERE id = ?::uuid", String.class, vencida)).isEqualTo("SEM_RESULTADO");

        String outra = rodada(startup, eva, 30);
        String p = JsonPath.read(corpo(post("/api/v1/rodadas/" + outra + "/participantes"), ana, "{\"intencao\":1000}").andReturn().getResponse()
                .getContentAsString(), "$.id");
        corpo(post("/api/v1/rodadas/" + outra + "/conclusao"), eva, "{\"valorCaptado\":1000,\"dataFechamento\":\"2026-11-25\",\"investidores\":[{\"participanteId\":\""
                + p + "\"}]}").andExpect(status().isOk());
        rotinas.executarManual("R7", null);
        rotinas.executarManual("R7", null);
        assertThat(contar(ana, "Confirme o seu investimento")).isEqualTo(2);
        jdbc.update("UPDATE rodada_investimento SET criado_em = now() - interval '20 days' WHERE participante_id = ?::uuid", p);
        rotinas.executarManual("R7", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM rodada_investimento WHERE participante_id = ?::uuid", String.class, p)).isEqualTo("NAO_CONFIRMADA");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94, KAN-95, KAN-96, KAN-97
     * Título: Reunião individual
     * Objetivo: Propor, contrapropor, aceitar, baixar o convite, registrar com ausência, anotar, lembrar (R5) e cancelar
     *           sozinho pelo fim da conexão.
     * Decisões: Horários a partir de dois dias à frente.
     */
    @Test
    void reuniaoIndividual() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String startup = publicada(eva);
        String c = match(eva, ana, startup);
        Instant base = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(Instant.now().plus(1, ChronoUnit.HOURS)))
                .andExpect(jsonPath("$.codigo").value("horario_cedo"));
        String r = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(base, base.plus(1, ChronoUnit.DAYS)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ladoDaVez").value("STARTUP")).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/reunioes/" + r + "/aceite"), ana, "{\"ordem\":1}").andExpect(jsonPath("$.codigo").value("nao_e_sua_vez"));
        corpo(post("/api/v1/reunioes/" + r + "/contraproposta"), eva, "{\"opcoes\":[\"" + base.plus(2, ChronoUnit.DAYS) + "\"]}")
                .andExpect(jsonPath("$.ladoDaVez").value("INVESTIDOR"));
        corpo(post("/api/v1/reunioes/" + r + "/aceite"), ana, "{\"ordem\":1}").andExpect(jsonPath("$.situacao").value("CONFIRMADA"));
        pedir(get("/api/v1/reunioes/" + r + "/convite.ics"), eva).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")));
        pedir(get("/api/v1/reunioes"), eva).andExpect(jsonPath("$[0].situacao").value("CONFIRMADA"));
        corpo(post("/api/v1/reunioes/" + r + "/registro"), eva, "{\"resultado\":\"REALIZADA\"}").andExpect(jsonPath("$.codigo").value("antes_da_reuniao"));

        jdbc.update("UPDATE reuniao SET inicio = now() + interval '30 minutes' WHERE id = ?::uuid", r);
        rotinas.executarManual("R5", null);
        assertThat(contar(ana, "Lembrete de reunião")).isEqualTo(1);
        jdbc.update("UPDATE reuniao SET inicio = now() - interval '2 hours' WHERE id = ?::uuid", r);
        corpo(post("/api/v1/reunioes/" + r + "/registro"), eva, "{\"resultado\":\"AUSENCIA_OUTRA_PARTE\"}").andExpect(status().isOk());
        corpo(post("/api/v1/reunioes/" + r + "/registro"), ana, "{\"resultado\":\"REALIZADA\",\"proximoPasso\":\"ANALISE\",\"aplicarNaConexao\":true}")
                .andExpect(jsonPath("$.registros.length()").value(2));
        assertThat(situacao(c)).isEqualTo("EM_ANALISE");
        corpo(put("/api/v1/reunioes/" + r + "/anotacao"), ana, "{\"texto\":\"Equipe forte\",\"compartilhar\":false}").andExpect(status().isNoContent());
        pedir(get("/api/v1/reunioes"), ana).andExpect(jsonPath("$[0].minhaAnotacao").value("Equipe forte"));
        pedir(get("/api/v1/reunioes"), eva).andExpect(jsonPath("$[0].minhaAnotacao").doesNotExist());

        String futura = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), eva, proposta(base)).andReturn().getResponse()
                .getContentAsString(), "$.id");
        corpo(post("/api/v1/reunioes/" + futura + "/recusa"), ana, "{\"motivo\":\"INVENTADO\"}").andExpect(status().is4xxClientError());
        corpo(post("/api/v1/conexoes/" + c + "/encerramento"), ana, "{\"motivo\":\"OUTRO\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT situacao FROM reuniao WHERE id = ?::uuid", String.class, futura)).isEqualTo("CANCELADA");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-98, KAN-96
     * Título: Reunião coletiva e cancelamento tardio
     * Objetivo: Convocar a reunião da rodada, responder presença; cancelar reunião individual em cima da hora (tardia) e
     *           expirar proposta vencida (R5).
     * Decisões: Fundadora administradora convoca.
     */
    @Test
    void coletivaECancelamento() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        String startup = publicada(eva);
        String c = match(eva, ana, startup);
        String rodada = rodada(startup, eva, 30);
        corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), ana, "{\"intencao\":1000}").andExpect(status().isCreated());
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        String col = JsonPath.read(corpo(post("/api/v1/startups/" + startup + "/rodada/reunioes"), eva, "{\"inicio\":\"" + inicio
                + "\",\"formato\":\"VIDEO\",\"local\":\"https://meet.exemplo.com/x\",\"pauta\":\"Atualização\",\"duracaoMinutos\":60}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.coletiva").value(true)).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/reunioes/" + col + "/presenca"), ana, "{\"resposta\":\"TALVEZ\"}")
                .andExpect(jsonPath("$.participantes[?(@.resposta == 'TALVEZ')]").isNotEmpty());

        Instant base = Instant.now().plus(2, ChronoUnit.DAYS);
        String r = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(base)).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/reunioes/" + r + "/aceite"), eva, "{\"ordem\":1}").andExpect(status().isOk());
        jdbc.update("UPDATE reuniao SET inicio = now() + interval '3 hours' WHERE id = ?::uuid", r);
        corpo(post("/api/v1/reunioes/" + r + "/cancelamento"), eva, "{\"motivo\":\"Imprevisto\"}").andExpect(jsonPath("$.tardia").value(true));

        String velha = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(base)).andReturn().getResponse().getContentAsString(), "$.id");
        jdbc.update("UPDATE reuniao_opcao SET inicio = now() - interval '1 hour' WHERE reuniao_id = ?::uuid", velha);
        rotinas.executarManual("R5", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM reuniao WHERE id = ?::uuid", String.class, velha)).isEqualTo("EXPIRADA");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
     * Título: Regras e caminhos da agenda
     * Objetivo: Conferir as validações da proposta, o limite de contrapropostas, a recusa, a remarcação, a reunião
     *           convocada pelo líder e o alerta de ausências repetidas.
     * Decisões: Limites reduzidos no banco e devolvidos no fim.
     */
    @Test
    void regrasDaAgenda() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario ana = conta(Papel.INVESTIDOR, "Ana");
        Usuario chefe = interna();
        String startup = publicada(eva);
        String c = match(eva, ana, startup);
        Instant base = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String ruim = "{\"tipo\":\"APRESENTACAO\",\"formato\":\"VIDEO\",\"local\":\"http://x\",\"pauta\":\"P\",\"duracaoMinutos\":45,\"opcoes\":[\"" + base + "\"]}";
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, ruim).andExpect(jsonPath("$.codigo").value("link_invalido"));
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, ruim.replace("http://x", "https://x").replace(":45", ":5"))
                .andExpect(jsonPath("$.codigo").value("duracao_invalida"));
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, ruim.replace("http://x", "").replace("VIDEO", "PRESENCIAL"))
                .andExpect(jsonPath("$.codigo").value("local_obrigatorio"));
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, ruim.replace("http://x", "https://x").replace("\"P\"", "\"\""))
                .andExpect(jsonPath("$.codigo").value("pauta_invalida"));
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana,
                "{\"tipo\":\"APRESENTACAO\",\"formato\":\"VIDEO\",\"local\":\"https://x\",\"pauta\":\"P\",\"duracaoMinutos\":45,\"opcoes\":[]}")
                .andExpect(jsonPath("$.codigo").value("horarios_invalidos"));
        corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana,
                ruim.replace("http://x", "https://x").replace("\"opcoes\"", "\"membros\":[\"" + eva.getId() + "\"],\"opcoes\""))
                .andExpect(jsonPath("$.codigo").value("membros_invalidos"));

        jdbc.update("UPDATE parametro SET valor = '1' WHERE chave = 'reuniao.max_contrapropostas'");
        try {
            String r = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(base)).andReturn().getResponse().getContentAsString(), "$.id");
            pedir(get("/api/v1/reunioes/" + r + "/convite.ics"), ana).andExpect(jsonPath("$.codigo").value("reuniao_nao_confirmada"));
            corpo(post("/api/v1/reunioes/" + r + "/contraproposta"), eva, "{\"opcoes\":[\"" + base.plus(1, ChronoUnit.DAYS) + "\"]}").andExpect(status().isOk());
            corpo(post("/api/v1/reunioes/" + r + "/contraproposta"), ana, "{\"opcoes\":[\"" + base + "\"]}")
                    .andExpect(jsonPath("$.codigo").value("contrapropostas_esgotadas"));
            corpo(post("/api/v1/reunioes/" + r + "/recusa"), ana, "{\"motivo\":\"SEM_DISPONIBILIDADE\"}").andExpect(jsonPath("$.situacao").value("RECUSADA"));
            corpo(post("/api/v1/reunioes/" + r + "/cancelamento"), ana, "{\"motivo\":\"x\"}").andExpect(jsonPath("$.codigo").value("reuniao_encerrada"));
        } finally {
            jdbc.update("UPDATE parametro SET valor = '3' WHERE chave = 'reuniao.max_contrapropostas'");
        }

        String r2 = JsonPath.read(corpo(post("/api/v1/conexoes/" + c + "/reunioes"), ana, proposta(base)).andReturn().getResponse().getContentAsString(), "$.id");
        corpo(post("/api/v1/reunioes/" + r2 + "/cancelamento"), ana, "{}").andExpect(jsonPath("$.codigo").value("motivo_obrigatorio"));
        corpo(post("/api/v1/reunioes/" + r2 + "/aceite"), eva, "{\"ordem\":9}").andExpect(jsonPath("$.codigo").value("opcao_invalida"));
        corpo(post("/api/v1/reunioes/" + r2 + "/aceite"), eva, "{\"ordem\":1}").andExpect(status().isOk());
        corpo(post("/api/v1/reunioes/" + r2 + "/presenca"), eva, "{\"resposta\":\"TALVEZ\"}").andExpect(jsonPath("$.codigo").value("resposta_invalida"));
        corpo(put("/api/v1/reunioes/" + r2 + "/anotacao"), eva, "{\"texto\":\"\"}").andExpect(jsonPath("$.codigo").value("anotacao_invalida"));
        String nova = JsonPath.read(corpo(post("/api/v1/reunioes/" + r2 + "/remarcacao"), eva, "{\"opcoes\":[\"" + base.plus(3, ChronoUnit.DAYS) + "\"]}")
                .andExpect(jsonPath("$.situacao").value("PROPOSTA")).andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(jdbc.queryForObject("SELECT situacao FROM reuniao WHERE id = ?::uuid", String.class, r2)).isEqualTo("REMARCADA");
        corpo(post("/api/v1/reunioes/" + nova + "/aceite"), ana, "{\"ordem\":1}").andExpect(status().isOk());

        jdbc.update("UPDATE parametro SET valor = '1' WHERE chave = 'reuniao.limite_ausencias'");
        try {
            jdbc.update("UPDATE reuniao SET inicio = now() - interval '1 hour' WHERE id = ?::uuid", nova);
            corpo(post("/api/v1/reunioes/" + nova + "/registro"), eva, "{\"resultado\":\"AUSENCIA_OUTRA_PARTE\"}").andExpect(status().isOk());
            assertThat(contar(chefe, "Usuário com ausências repetidas")).isEqualTo(1);
        } finally {
            jdbc.update("UPDATE parametro SET valor = '2' WHERE chave = 'reuniao.limite_ausencias'");
        }

        String rodada = rodada(startup, eva, 30);
        String p = JsonPath.read(corpo(post("/api/v1/rodadas/" + rodada + "/participantes"), ana, "{\"intencao\":1000}").andReturn().getResponse()
                .getContentAsString(), "$.id");
        String coletiva = "{\"inicio\":\"" + base + "\",\"formato\":\"VIDEO\",\"local\":\"https://x\",\"pauta\":\"P\",\"duracaoMinutos\":30}";
        corpo(post("/api/v1/startups/" + startup + "/rodada/reunioes"), ana, coletiva).andExpect(status().isForbidden());
        pedir(post("/api/v1/rodadas/" + rodada + "/participantes/" + p + "/lideranca"), eva).andExpect(status().isNoContent());
        corpo(post("/api/v1/rodadas/" + rodada + "/lideranca/resposta"), ana, "{\"aprovar\":true}").andExpect(status().isNoContent());
        String col = JsonPath.read(corpo(post("/api/v1/startups/" + startup + "/rodada/reunioes"), ana, coletiva).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        jdbc.update("UPDATE reuniao SET inicio = now() - interval '1 hour' WHERE id = ?::uuid", col);
        corpo(post("/api/v1/reunioes/" + col + "/registro"), eva, "{\"resultado\":\"REALIZADA\"}").andExpect(status().isForbidden());
        corpo(post("/api/v1/reunioes/" + col + "/registro"), ana, "{\"resultado\":\"REALIZADA\",\"resumo\":\"Alinhados\"}")
                .andExpect(jsonPath("$.resumo").value("Alinhados"));
    }

    private Usuario interna() {
        Usuario u = Usuario.novoInterno("Chefe", "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(Papel.ADMIN), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    private String proposta(Instant... opcoes) {
        StringBuilder s = new StringBuilder("{\"tipo\":\"APRESENTACAO\",\"formato\":\"VIDEO\",\"local\":\"https://meet.exemplo.com/a\",\"pauta\":\"Conhecer\","
                + "\"duracaoMinutos\":45,\"opcoes\":[");
        for (int i = 0; i < opcoes.length; i++) {
            s.append(i > 0 ? "," : "").append('"').append(opcoes[i]).append('"');
        }
        return s.append("]}").toString();
    }

    private String rodada(String startup, Usuario dono, int diasDePrazo) {
        UUID id = UUID.randomUUID();
        jdbc.update("UPDATE rodada SET situacao = 'ENCERRADA', resultado = 'CANCELADA' WHERE startup_id = ?::uuid AND situacao <> 'ENCERRADA'", startup);
        jdbc.update("INSERT INTO rodada (id, startup_id, situacao, estagio, instrumento, valor_alvo, valor_minimo, valor_maximo, prazo, criada_por, criada_em, "
                + "atualizada_em, aberta_em) VALUES (?, ?::uuid, 'ABERTA', 'SEED', 'MUTUO_CONVERSIVEL', 250000, 100000, 300000, ?, ?, now(), now(), now())",
                id, startup, java.sql.Date.valueOf(LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(diasDePrazo)), dono.getId());
        return id.toString();
    }

    private String match(Usuario eva, Usuario investidor, String startup) throws Exception {
        verificar(investidor);
        String c = JsonPath.read(corpo(post("/api/v1/catalogo/startups/" + startup + "/interesse"), investidor, "{}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        pedir(post("/api/v1/conexoes/" + c + "/aceite"), eva).andExpect(status().isOk());
        return c;
    }

    private String situacao(String conexao) {
        return jdbc.queryForObject("SELECT situacao FROM conexao WHERE id = ?::uuid", String.class, conexao);
    }

    private ResultActions pedir(MockHttpServletRequestBuilder p, Usuario quem) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    private ResultActions corpo(MockHttpServletRequestBuilder p, Usuario quem, String json) throws Exception {
        return mvc.perform(p.header(HttpHeaders.AUTHORIZATION, bearer(quem)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String publicada(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Rodante\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}"))
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
        Usuario u = Usuario.novo(nome, "r-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
