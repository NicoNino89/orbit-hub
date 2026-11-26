package br.com.orbithub.conexoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-174, KAN-176
 * Título: Testes de integração da rede e da carteira
 * Objetivo: Conferir a conversa da conexão (antes e depois do Match, não lidas, só leitura depois do fim) e a carteira.
 * Decisões: Banco real (Testcontainers); rodada e investimento criados direto no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RedeIT {

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-176
     * Título: Conversa da conexão
     * Objetivo: Começar no Match, contar não lidas, responder e travar depois do fim.
     * Decisões: Quem não é parte recebe 404.
     */
    @Test
    void conversaDaConexao() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        Usuario intruso = conta(Papel.INVESTIDOR, "Ivo");
        String startup = publicada(eva);
        verificar(leo);
        String c = JsonPath.read(corpo(post("/api/v1/catalogo/startups/" + startup + "/interesse"), leo, "{}").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        pedir(get("/api/v1/conexoes/" + c + "/mensagens"), leo).andExpect(jsonPath("$.codigo").value("sem_conversa"));
        pedir(post("/api/v1/conexoes/" + c + "/aceite"), eva).andExpect(status().isOk());

        corpo(post("/api/v1/conexoes/" + c + "/mensagens"), leo, "{\"texto\":\"  \"}").andExpect(jsonPath("$.codigo").value("mensagem_invalida"));
        corpo(post("/api/v1/conexoes/" + c + "/mensagens"), leo, "{\"texto\":\"Olá, podemos conversar?\"}")
                .andExpect(jsonPath("$.mensagens[0].minha").value(true)).andExpect(jsonPath("$.podeEscrever").value(true));
        pedir(get("/api/v1/conexoes/" + c + "/mensagens"), intruso).andExpect(status().isNotFound());
        pedir(get("/api/v1/mensagens/contagem"), eva).andExpect(jsonPath("$.naoLidas").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND titulo = 'Nova mensagem'", Integer.class, eva.getId())).isEqualTo(1);
        pedir(get("/api/v1/conexoes/" + c + "/mensagens"), eva).andExpect(jsonPath("$.mensagens[0].autor").value("Leo"));
        pedir(get("/api/v1/mensagens/contagem"), eva).andExpect(jsonPath("$.naoLidas").value(0));
        corpo(post("/api/v1/conexoes/" + c + "/mensagens"), eva, "{\"texto\":\"Claro!\"}").andExpect(jsonPath("$.mensagens.length()").value(2));
        pedir(get("/api/v1/mensagens/contagem"), leo).andExpect(jsonPath("$.naoLidas").value(1));

        corpo(post("/api/v1/conexoes/" + c + "/encerramento"), leo, "{\"motivo\":\"MOMENTO_INADEQUADO\"}").andExpect(status().isOk());
        pedir(get("/api/v1/conexoes/" + c + "/mensagens"), leo).andExpect(jsonPath("$.podeEscrever").value(false));
        corpo(post("/api/v1/conexoes/" + c + "/mensagens"), leo, "{\"texto\":\"Mais uma\"}").andExpect(jsonPath("$.codigo").value("conversa_encerrada"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174
     * Título: Carteira
     * Objetivo: Listar o investimento registrado e a rodada aberta de que o investidor participa.
     * Decisões: Pessoa física.
     */
    @Test
    void carteira() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR, "Eva");
        Usuario leo = conta(Papel.INVESTIDOR, "Leo");
        String startup = publicada(eva);
        UUID fechada = rodada(startup, eva, "ENCERRADA");
        UUID p1 = participante(fechada, leo);
        jdbc.update("UPDATE rodada SET data_fechamento = current_date, instrumento_utilizado = 'MUTUO_CONVERSIVEL' WHERE id = ?", fechada);
        jdbc.update("INSERT INTO rodada_investimento (id, rodada_id, participante_id, valor, situacao, criado_em) VALUES (?, ?, ?, 50000, 'CONFIRMADO', now())",
                UUID.randomUUID(), fechada, p1);
        participante(rodada(startup, eva, "ABERTA"), leo);
        pedir(get("/api/v1/investidores/carteira"), leo).andExpect(jsonPath("$.investimentos[0].situacao").value("CONFIRMADO"))
                .andExpect(jsonPath("$.investimentos[0].valor").value(50000.0)).andExpect(jsonPath("$.rodadas.length()").value(1))
                .andExpect(jsonPath("$.rodadas[0].situacao").value("ABERTA"));
        pedir(get("/api/v1/investidores/carteira"), eva).andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: rodada
     * Objetivo: Rotina auxiliar rodada.
     * Decisões: Ver a documentação da classe.
     */
    private UUID rodada(String startup, Usuario dono, String situacao) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO rodada (id, startup_id, situacao, estagio, instrumento, valor_alvo, valor_minimo, valor_maximo, prazo, criada_por, criada_em, "
                + "atualizada_em, aberta_em) VALUES (?, ?::uuid, ?, 'SEED', 'MUTUO_CONVERSIVEL', 250000, 100000, 300000, current_date + 30, ?, now(), now(), now())",
                id, startup, situacao, dono.getId());
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: participante
     * Objetivo: Rotina auxiliar participante.
     * Decisões: Ver a documentação da classe.
     */
    private UUID participante(UUID rodada, Usuario u) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO rodada_participante (id, rodada_id, investidor_chave, pessoa_id, situacao, intencao, entrou_em, atualizado_em) "
                + "VALUES (?, ?, ?, ?, 'ATIVO', 10000, now(), now())", id, rodada, u.getId(), u.getId());
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: publicada
     * Objetivo: Rotina auxiliar publicada.
     * Decisões: Ver a documentação da classe.
     */
    private String publicada(Usuario dono) throws Exception {
        String id = JsonPath.read(corpo(post("/api/v1/startups"), dono, "{\"nome\":\"Rede\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), nome_aprovado = nome, segmento_aprovado = segmento, "
                + "estagio_aprovado = 'MVP' WHERE id = ?::uuid", id);
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: verificar
     * Objetivo: Rotina auxiliar verificar.
     * Decisões: Ver a documentação da classe.
     */
    private void verificar(Usuario u) {
        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_em, valido_ate) "
                + "VALUES (?, 'PESSOA', ?, 'VERIFICADO', ?, now(), now(), now(), now() + interval '1 year')", UUID.randomUUID(), u.getId(), u.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
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
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
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
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: conta
     * Objetivo: Rotina auxiliar conta.
     * Decisões: Ver a documentação da classe.
     */
    private Usuario conta(Papel papel, String nome) {
        Usuario u = Usuario.novo(nome, "r-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:58
     * Cartão Jira: KAN-174, KAN-176
     * Título: bearer
     * Objetivo: Rotina auxiliar bearer.
     * Decisões: Ver a documentação da classe.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
