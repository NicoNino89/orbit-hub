package br.com.orbithub.catalogo.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-29, KAN-36, KAN-66, KAN-84, KAN-85
 * Título: Testes de integração da publicação no catálogo
 * Objetivo: Percorrer o caminho completo: visibilidade e pré-visualização, envio, fila da curadoria com checklist,
 *           devolução com apontamentos, aprovação, apresentação no catálogo, alteração relevante, recusa por fraude com
 *           segunda aprovação e edições temáticas com a rotina R10.
 * Decisões: Banco real (Testcontainers). Contas criadas direto no repositório; sessões pelo serviço.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PublicacaoNoCatalogoIT {

    static final String CHECKLIST_OK = "[" + String.join(",", java.util.stream.Stream.of("CAMPOS_OBRIGATORIOS", "FUNDADORES_VERIFICADOS",
            "CNPJ_CONFERIDO", "CONTEUDO_ADEQUADO", "SEM_OFERTA_PUBLICA", "SEM_PROMESSA_DE_RETORNO", "INDICADORES_COM_DATA")
            .map(c -> "{\"codigo\":\"" + c + "\",\"atendido\":true}").toList()) + "]";

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
    SegundaAprovacaoService aprovacoes;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28, KAN-29
     * Título: Visibilidade e pré-visualização
     * Objetivo: Conferir os padrões, a troca de visibilidade e o efeito na pré-visualização.
     * Decisões: Faturamento começa confidencial; problema começa público.
     */
    @Test
    void visibilidadeEPrevia() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String id = startupCompleta(eva);
        mvc.perform(get("/api/v1/startups/" + id + "/visibilidade").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.participaEdicoes").value(true))
                .andExpect(jsonPath("$.itens[?(@.item.codigo == 'PROBLEMA')].publico").value(true))
                .andExpect(jsonPath("$.itens[?(@.item.codigo == 'FONTES_FATURAMENTO')].publico").value(false))
                .andExpect(jsonPath("$.itens[?(@.item.codigo == 'LOCALIZACAO')].publico").value(false));
        previa(eva, id, true).andExpect(jsonPath("$.paginas.length()").value(3))
                .andExpect(jsonPath("$.paginas[0].blocos[?(@.titulo == 'Problema')].itens[0].valor").value("Frete caro"))
                .andExpect(jsonPath("$.cabecalho.localizacao").doesNotExist())
                .andExpect(jsonPath("$.cabecalho.estagio").value("MVP"))
                .andExpect(jsonPath("$.paginas[1].blocos[?(@.titulo == 'Necessidade de investimento')].avisos[0]")
                        .value("Sem rodada aberta no momento."));
        mvc.perform(put("/api/v1/startups/" + id + "/visibilidade").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"PROBLEMA\":false,\"LOCALIZACAO\":true}"))
                .andExpect(status().isOk());
        previa(eva, id, false).andExpect(jsonPath("$.paginas[0].blocos[?(@.titulo == 'Problema')]").isEmpty())
                .andExpect(jsonPath("$.cabecalho.localizacao").value("São Paulo"))
                .andExpect(jsonPath("$.comoVerificado").value(false));
        mvc.perform(put("/api/v1/startups/" + id + "/visibilidade").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                .contentType(MediaType.APPLICATION_JSON).content("{\"INVENTADO\":true}")).andExpect(jsonPath("$.codigo").value("item_invalido"));
        Usuario rui = conta(Papel.EMPREENDEDOR);
        previa(rui, id, true).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/startups/" + id + "/participacao-edicoes").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"participa\":false}"))
                .andExpect(jsonPath("$.participaEdicoes").value(false));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66, KAN-84, KAN-36
     * Título: Curadoria completa
     * Objetivo: Enviar, assumir, devolver com apontamento, reenviar, aprovar e ver no catálogo; depois alterar o nome e
     *           conferir que o catálogo segue com o nome aprovado até a nova aprovação.
     * Decisões: O investidor não verificado não vê os dados da rodada.
     */
    @Test
    void curadoriaEApresentacao() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario ana = interna(Papel.CURADOR, "Ana Curadora");
        Usuario leo = conta(Papel.INVESTIDOR);
        String id = startupCompleta(eva);
        acao(eva, "/api/v1/startups/" + id + "/envio").andExpect(jsonPath("$.situacao").value("EM_REVISAO"));
        String revisao = revisaoDa(ana, id);
        mvc.perform(get("/api/v1/admin/curadoria/" + revisao).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.checklist.length()").value(7)).andExpect(jsonPath("$.fundadores[0].selo").value("NAO_VERIFICADO"))
                .andExpect(jsonPath("$.revisao.tipo").value("PUBLICACAO"));
        mvc.perform(get("/api/v1/admin/curadoria/startups/" + id + "/apresentacao").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.modo").value("COMPLETA")).andExpect(jsonPath("$.cabecalho.localizacao").value("São Paulo"));
        decidir(ana, revisao, "APROVADA", CHECKLIST_OK, "[]", "").andExpect(status().isForbidden());
        acao(ana, "/api/v1/admin/curadoria/" + revisao + "/assumir").andExpect(status().isOk());
        decidir(ana, revisao, "APROVADA", "[]", "[]", "").andExpect(jsonPath("$.codigo").value("checklist_incompleto"));
        decidir(ana, revisao, "APROVADA", CHECKLIST_OK.replaceFirst("true", "false"), "[]", "")
                .andExpect(jsonPath("$.codigo").value("checklist_pendente"));
        decidir(ana, revisao, "DEVOLVIDA", CHECKLIST_OK, "[]", "").andExpect(jsonPath("$.codigo").value("devolucao_sem_comentario"));
        decidir(ana, revisao, "DEVOLVIDA", CHECKLIST_OK.replaceFirst("true", "false"),
                "[{\"secao\":\"MERCADO\",\"comentario\":\"Informe a fonte do tamanho do mercado\"}]", "Ajuste o mercado")
                .andExpect(jsonPath("$.situacao").value("DEVOLVIDA"));
        mvc.perform(get("/api/v1/startups/" + id + "/curadoria").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].situacao").value("DEVOLVIDA")).andExpect(jsonPath("$[0].parecer").value("Ajuste o mercado"))
                .andExpect(jsonPath("$[0].apontamentos[?(@.codigo == 'MERCADO')].rotulo").value("Mercado"));
        assertThat(contar(eva, "A curadoria pediu ajustes%")).isEqualTo(1);
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo))).andExpect(status().isNotFound());

        acao(eva, "/api/v1/startups/" + id + "/envio").andExpect(jsonPath("$.situacao").value("EM_REVISAO"));
        String segunda = revisaoDa(ana, id);
        acao(ana, "/api/v1/admin/curadoria/" + segunda + "/assumir").andExpect(status().isOk());
        decidir(ana, segunda, "APROVADA", CHECKLIST_OK, "[]", "Tudo certo").andExpect(jsonPath("$.situacao").value("APROVADA"));
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cabecalho.nome").value("Equipe Viva"))
                .andExpect(jsonPath("$.comoVerificado").value(false));

        jdbc.update("INSERT INTO rodada (id, startup_id, situacao, estagio, instrumento, valor_alvo, criada_por, criada_em, atualizada_em) "
                + "VALUES (?, ?::uuid, 'ABERTA', 'SEED', 'MUTUO_CONVERSIVEL', 500000, ?, now(), now())", UUID.randomUUID(), id, eva.getId());
        mvc.perform(put("/api/v1/startups/" + id + "/visibilidade").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                .contentType(MediaType.APPLICATION_JSON).content("{\"DADOS_DA_RODADA\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(jsonPath("$.paginas[1].blocos[?(@.titulo == 'Necessidade de investimento')].avisos[0]")
                        .value("Os dados da rodada aparecem só para investidores verificados."));
        previa(eva, id, true).andExpect(jsonPath("$.paginas[1].blocos[?(@.titulo == 'Necessidade de investimento')].itens[?(@.rotulo == 'Valor alvo')]")
                .isNotEmpty());

        mvc.perform(put("/api/v1/startups/" + id + "/cadastro").header(HttpHeaders.AUTHORIZATION, bearer(eva)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Viva Logística\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"fundadores\":[{\"nome\":\"Eva\"}]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(jsonPath("$.cabecalho.nome").value("Equipe Viva"));
        String alteracao = revisaoDa(ana, id);
        mvc.perform(get("/api/v1/admin/curadoria/" + alteracao).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.revisao.tipo").value("ALTERACAO")).andExpect(jsonPath("$.alteracoes[0].novo").value("Viva Logística"));
        acao(ana, "/api/v1/admin/curadoria/" + alteracao + "/assumir").andExpect(status().isOk());
        decidir(ana, alteracao, "APROVADA", CHECKLIST_OK, "[]", "").andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogo/startups/" + id).header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(jsonPath("$.cabecalho.nome").value("Viva Logística"));
        acao(eva, "/api/v1/startups/" + id + "/envio").andExpect(jsonPath("$.codigo").value("sem_alteracao"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Recusa por fraude
     * Objetivo: Conferir que a recusa por fraude espera a segunda aprovação e, aprovada, recusa a startup.
     * Decisões: Rejeitar a proposta devolve a revisão à análise.
     */
    @Test
    void recusaPorFraudeComSegundaAprovacao() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario ana = interna(Papel.CURADOR, "Ana");
        Usuario chefe = interna(Papel.ADMIN, "Chefe");
        String id = startupCompleta(eva);
        acao(eva, "/api/v1/startups/" + id + "/envio").andExpect(status().isOk());
        String revisao = revisaoDa(ana, id);
        acao(ana, "/api/v1/admin/curadoria/" + revisao + "/assumir").andExpect(status().isOk());
        decidir(ana, revisao, "RECUSADA", CHECKLIST_OK, "[]", "").andExpect(jsonPath("$.codigo").value("recusa_sem_parecer"));
        decidir(ana, revisao, "RECUSADA", CHECKLIST_OK, "[]", "Documentos de outra empresa")
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_APROVACAO"));
        UUID proposta = jdbc.queryForObject("SELECT proposta_id FROM revisao_curadoria WHERE id = ?::uuid", UUID.class, revisao);
        aprovacoes.rejeitar(proposta, chefe.getId(), Set.of(Papel.ADMIN), "Faltam provas");
        assertThat(jdbc.queryForObject("SELECT situacao FROM revisao_curadoria WHERE id = ?::uuid", String.class, revisao)).isEqualTo("EM_ANALISE");
        decidir(ana, revisao, "RECUSADA", CHECKLIST_OK, "[]", "Documentos de outra empresa, com provas").andExpect(status().isOk());
        UUID nova = jdbc.queryForObject("SELECT proposta_id FROM revisao_curadoria WHERE id = ?::uuid", UUID.class, revisao);
        aprovacoes.aprovar(nova, chefe.getId(), Set.of(Papel.ADMIN), "Confirmado");
        assertThat(jdbc.queryForObject("SELECT situacao FROM startup WHERE id = ?::uuid", String.class, id)).isEqualTo("RECUSADA");
        assertThat(jdbc.queryForObject("SELECT situacao FROM revisao_curadoria WHERE id = ?::uuid", String.class, revisao)).isEqualTo("RECUSADA");
        assertThat(contar(eva, "%foi recusada pela curadoria")).isEqualTo(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edições temáticas
     * Objetivo: Criar, compor, ordenar, publicar e ver a edição; esconder quem sai das edições; arquivar pela R10.
     * Decisões: Startups publicadas direto no banco, para focar nas edições.
     */
    @Test
    void edicoesTematicas() throws Exception {
        Usuario ana = interna(Papel.CURADOR, "Ana");
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario leo = conta(Papel.INVESTIDOR);
        String a = publicada(eva, "Alfa");
        String b = publicada(eva, "Beta");
        String rascunho = startupCompleta(eva);
        LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        String edicao = JsonPath.read(mvc.perform(post("/api/v1/admin/edicoes").header(HttpHeaders.AUTHORIZATION, bearer(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Logística verde\",\"descricao\":\"Startups de frete\","
                        + "\"criterios\":\"Segmento logística\",\"inicio\":\"" + hoje + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("RASCUNHO"))
                .andExpect(jsonPath("$.fim").value(hoje.plusMonths(1).minusDays(1).toString()))
                .andReturn().getResponse().getContentAsString(), "$.id");
        acao(ana, "/api/v1/admin/edicoes/" + edicao + "/publicacao").andExpect(jsonPath("$.codigo").value("edicao_vazia"));
        mvc.perform(get("/api/v1/admin/edicoes/startups-elegiveis").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$[?(@.id == '" + a + "')]").isNotEmpty()).andExpect(jsonPath("$[?(@.id == '" + rascunho + "')]").isEmpty());
        compor(ana, edicao, "[\"" + rascunho + "\"]").andExpect(jsonPath("$.codigo").value("startup_inelegivel"));
        compor(ana, edicao, "[\"" + b + "\",\"" + b + "\"]").andExpect(jsonPath("$.codigo").value("startup_repetida"));
        compor(ana, edicao, "[\"" + b + "\",\"" + a + "\"]").andExpect(jsonPath("$.quantidade").value(2));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/admin/edicoes/" + edicao + "/capa")
                        .file(new MockMultipartFile("arquivo", "capa.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2}))
                        .header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.temCapa").value(true));
        mvc.perform(get("/api/v1/catalogo/edicoes/" + edicao).header(HttpHeaders.AUTHORIZATION, bearer(leo))).andExpect(status().isNotFound());
        acao(ana, "/api/v1/admin/edicoes/" + edicao + "/publicacao").andExpect(jsonPath("$.situacao").value("VIGENTE"));
        mvc.perform(get("/api/v1/catalogo/edicoes").header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(jsonPath("$[?(@.id == '" + edicao + "')].startups[0].nome").value("Beta"));
        mvc.perform(get("/api/v1/catalogo/edicoes/" + edicao + "/capa").header(HttpHeaders.AUTHORIZATION, bearer(leo))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/startups/" + b + "/participacao-edicoes").header(HttpHeaders.AUTHORIZATION, bearer(eva))
                .contentType(MediaType.APPLICATION_JSON).content("{\"participa\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/catalogo/edicoes/" + edicao).header(HttpHeaders.AUTHORIZATION, bearer(leo)))
                .andExpect(jsonPath("$.startups.length()").value(1)).andExpect(jsonPath("$.startups[0].nome").value("Alfa"));
        mvc.perform(get("/api/v1/admin/edicoes/" + edicao).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.startups.length()").value(2)).andExpect(jsonPath("$.startups[0].visivel").value(false))
                .andExpect(jsonPath("$.edicao.curador").value("Ana")).andExpect(jsonPath("$.historico[0].acao").value("PUBLICADA"));
        jdbc.update("UPDATE edicao SET inicio = ?, fim = ? WHERE id = ?::uuid", hoje.minusDays(10), hoje.minusDays(1), edicao);
        rotinas.executarManual("R10", null);
        assertThat(jdbc.queryForObject("SELECT situacao FROM edicao WHERE id = ?::uuid", String.class, edicao)).isEqualTo("ARQUIVADA");
        mvc.perform(get("/api/v1/admin/edicoes").header(HttpHeaders.AUTHORIZATION, bearer(ana))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/edicoes").header(HttpHeaders.AUTHORIZATION, bearer(leo))).andExpect(status().isForbidden());
        acao(ana, "/api/v1/admin/edicoes/" + edicao + "/arquivamento").andExpect(jsonPath("$.codigo").value("edicao_arquivada"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Revisão da startup na fila
     * Objetivo: Achar na fila a revisão aberta da startup.
     * Decisões: Pela lista da curadoria.
     */
    private String revisaoDa(Usuario curador, String startup) throws Exception {
        String corpo = mvc.perform(get("/api/v1/admin/curadoria").header(HttpHeaders.AUTHORIZATION, bearer(curador)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<java.util.List<String>>read(corpo, "$[?(@.startupId == '" + startup + "')].id").get(0);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Decidir
     * Objetivo: Enviar a decisão do curador.
     * Decisões: Parecer vazio vira nulo.
     */
    private ResultActions decidir(Usuario quem, String revisao, String resultado, String checklist, String apontamentos, String parecer)
            throws Exception {
        return mvc.perform(post("/api/v1/admin/curadoria/" + revisao + "/decisao").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"" + resultado + "\",\"checklist\":" + checklist
                        + ",\"apontamentos\":" + apontamentos + ",\"parecer\":\"" + parecer + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Compor edição
     * Objetivo: Enviar a composição da edição.
     * Decisões: Lista crua.
     */
    private ResultActions compor(Usuario quem, String edicao, String lista) throws Exception {
        return mvc.perform(put("/api/v1/admin/edicoes/" + edicao + "/startups").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"startups\":" + lista + "}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-29
     * Título: Pré-visualização
     * Objetivo: Pedir a pré-visualização como verificado ou não.
     * Decisões: Parâmetro verificado.
     */
    private ResultActions previa(Usuario quem, String id, boolean verificado) throws Exception {
        return mvc.perform(get("/api/v1/startups/" + id + "/apresentacao").param("verificado", String.valueOf(verificado))
                .header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Ação sem corpo
     * Objetivo: Enviar um POST sem corpo.
     * Decisões: Usado nas ações de envio, assumir e publicar.
     */
    private ResultActions acao(Usuario quem, String caminho) throws Exception {
        return mvc.perform(post(caminho).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Startup completa
     * Objetivo: Criar uma startup com os obrigatórios preenchidos, pronta para enviar.
     * Decisões: Mesmo roteiro do KAN-27.
     */
    private String startupCompleta(Usuario dono) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Equipe Viva\",\"segmento\":\"FINTECH\",\"localizacao\":\"SP\",\"fundadores\":[{\"nome\":\"Eva\"}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");
        secao(dono, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"Frete caro\",\"SOLUCAO\":\"Rotas\"}");
        secao(dono, id, "PRODUTO", "{\"DESCRICAO_PRODUTO\":\"Aplicativo\",\"TEM_MVP\":\"SIM\"}");
        secao(dono, id, "MERCADO", "{\"PUBLICO_ALVO\":\"Cooperativas\"}");
        secao(dono, id, "MODELO_DE_NEGOCIO", "{\"MODELO\":\"SAAS\",\"GERACAO_RECEITA\":\"Assinatura\",\"FONTES_FATURAMENTO\":\"Mensalidades\"}");
        secao(dono, id, "NECESSIDADE_DE_INVESTIMENTO", "{\"DESTINO_PRODUTO\":\"60\",\"PRAZO_USO_MESES\":\"18\"}");
        secao(dono, id, "MATURIDADE", "{\"ESTAGIO\":\"MVP\"}");
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Startup publicada
     * Objetivo: Criar uma startup e publicá-la direto no banco, com o nome aprovado.
     * Decisões: Atalho para os testes das edições.
     */
    private String publicada(Usuario dono, String nome) throws Exception {
        String id = startupCompleta(dono);
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now(), nome = ?, nome_aprovado = ?, "
                + "segmento_aprovado = segmento, estagio_aprovado = 'MVP' WHERE id = ?::uuid", nome, nome, id);
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-22
     * Título: Salvar seção
     * Objetivo: Preencher uma seção do cadastro.
     * Decisões: Espera 200.
     */
    private void secao(Usuario quem, String id, String secao, String corpo) throws Exception {
        mvc.perform(put("/api/v1/startups/" + id + "/secoes/" + secao).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
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
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Conta pública
     * Objetivo: Criar uma conta confirmada, com os termos aceitos.
     * Decisões: Direto no repositório.
     */
    private Usuario conta(Papel papel) {
        Usuario u = Usuario.novo("Pessoa " + papel, "s-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Conta interna
     * Objetivo: Criar uma conta da equipe interna com o papel pedido.
     * Decisões: Convite já aceito.
     */
    private Usuario interna(Papel papel, String nome) {
        Usuario u = Usuario.novoInterno(nome, "i-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel), Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
