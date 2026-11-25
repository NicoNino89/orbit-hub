package br.com.orbithub.catalogo.api;

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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38, KAN-39, KAN-41, KAN-42, KAN-43, KAN-44
 * Título: Testes de integração da navegação do catálogo
 * Objetivo: Conferir a seleção personalizada (ordem e justificativas), as novas oportunidades, os filtros, os filtros
 *           de rodada só para verificado, favoritos, histórico e descartes.
 * Decisões: Startups publicadas direto no banco; o catálogo é compartilhado entre testes, então as conferências usam
 *           os identificadores criados aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NavegacaoDoCatalogoIT {

    static final String PERFIL = """
            {"tipo":"ANJO","ticketMinimo":50000,"ticketMaximo":250000,"lideranca":"LIDERA","followOn":true,
             "participacaoNaGestao":"ACEITA_OBSERVADOR","instrumentos":["MUTUO_CONVERSIVEL"],
             "segmentos":["AGRONEGOCIO"],"estagios":["SEED"],"maturidades":["MVP"],"localizacoes":["MT"]}""";

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
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39, KAN-38
     * Título: Seleção personalizada e filtros
     * Objetivo: A startup que atende mais critérios vem antes, com as justificativas; critérios de rodada só para
     *           verificado; filtros combinados; sem perfil, ordem por data.
     * Decisões: A verificação é ligada direto no banco.
     */
    @Test
    void selecaoPersonalizadaEFiltros() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario leo = conta(Papel.INVESTIDOR);
        mvc.perform(put("/api/v1/investidores/perfil").header(HttpHeaders.AUTHORIZATION, bearer(leo))
                .contentType(MediaType.APPLICATION_JSON).content(PERFIL)).andExpect(status().isOk());
        String agro = publicada(eva, "Agro Um", "AGRONEGOCIO", "MT", "MVP", 2);
        String fintech = publicada(eva, "Fin Dois", "FINTECH", "SP", "TRACAO", 1);
        rodada(agro, eva);

        String corpo = navegar(leo, "").andExpect(status().isOk()).andExpect(jsonPath("$.comPerfil").value(true))
                .andExpect(jsonPath("$.verificado").value(false)).andReturn().getResponse().getContentAsString();
        java.util.List<String> ids = JsonPath.read(corpo, "$.itens[*].startup.id");
        org.assertj.core.api.Assertions.assertThat(ids.indexOf(agro)).isLessThan(ids.indexOf(fintech));
        navegar(leo, "").andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')].criterios[0]")
                        .value("Atua em Agronegócio e agtech, um dos seus segmentos"))
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')].criterios.length()").value(3))
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')].rodadaAberta").value(true));

        jdbc.update("INSERT INTO pedido_verificacao (id, sujeito_tipo, sujeito_id, situacao, criado_por, criado_em, enviado_em, decidido_em, valido_ate) "
                + "VALUES (?, 'PESSOA', ?, 'VERIFICADO', ?, now(), now(), now(), now() + interval '1 year')", UUID.randomUUID(), leo.getId(), leo.getId());
        navegar(leo, "").andExpect(jsonPath("$.verificado").value(true))
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')].criterios.length()").value(7));
        navegar(leo, "?segmento=FINTECH").andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')]").isEmpty())
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + fintech + "')]").isNotEmpty());
        navegar(leo, "?comRodada=true&estagio=SEED&captacaoMinima=100000&maturidade=MVP&localizacao=MT")
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')]").isNotEmpty())
                .andExpect(jsonPath("$.itens[?(@.startup.id == '" + fintech + "')]").isEmpty());
        navegar(leo, "?captacaoMaxima=1000").andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')]").isEmpty());
        navegar(leo, "?lista=NOVAS").andExpect(jsonPath("$.itens[?(@.startup.id == '" + agro + "')].criterios.length()").value(0));
        jdbc.update("UPDATE startup SET publicada_em = now() - interval '90 days' WHERE id = ?::uuid", fintech);
        navegar(leo, "?lista=NOVAS").andExpect(jsonPath("$.itens[?(@.startup.id == '" + fintech + "')]").isEmpty());

        Usuario semPerfil = conta(Papel.INVESTIDOR);
        navegar(semPerfil, "").andExpect(jsonPath("$.comPerfil").value(false));
        navegar(semPerfil, "?estagio=PRE_SEED").andExpect(jsonPath("$.itens[?(@.startup.id == '" + fintech + "')]").isNotEmpty());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41, KAN-42, KAN-43, KAN-44
     * Título: Favoritos, histórico e descartes
     * Objetivo: Favoritar, listar (com indisponível), registrar e limpar o histórico, descartar e desfazer.
     * Decisões: Despublicada direto no banco.
     */
    @Test
    void listasPessoais() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario leo = conta(Papel.INVESTIDOR);
        String id = publicada(eva, "Favorita", "SAUDE", "SP", "MVP", 0);
        acao(put("/api/v1/catalogo/favoritos/" + id), leo).andExpect(status().isNoContent());
        acao(put("/api/v1/catalogo/favoritos/" + id), leo).andExpect(status().isNoContent());
        acao(get("/api/v1/catalogo/startups/" + id + "/marcacoes"), leo).andExpect(jsonPath("$.favorito").value(true));
        navegar(leo, "").andExpect(jsonPath("$.itens[?(@.startup.id == '" + id + "')].favorito").value(true));
        acao(get("/api/v1/catalogo/startups/" + id), leo).andExpect(status().isOk());
        acao(get("/api/v1/catalogo/minhas/HISTORICO"), leo).andExpect(jsonPath("$[0].startup.nome").value("Favorita"));
        acao(delete("/api/v1/catalogo/minhas/HISTORICO"), leo).andExpect(status().isNoContent());
        acao(get("/api/v1/catalogo/minhas/HISTORICO"), leo).andExpect(jsonPath("$.length()").value(0));

        acao(put("/api/v1/catalogo/descartadas/" + id), leo).andExpect(status().isNoContent());
        acao(get("/api/v1/catalogo/startups/" + id + "/marcacoes"), leo).andExpect(jsonPath("$.favorito").value(false))
                .andExpect(jsonPath("$.descartada").value(true));
        navegar(leo, "").andExpect(jsonPath("$.itens[?(@.startup.id == '" + id + "')]").isEmpty());
        navegar(leo, "?lista=NOVAS").andExpect(jsonPath("$.itens[?(@.startup.id == '" + id + "')]").isNotEmpty());
        acao(get("/api/v1/catalogo/minhas/DESCARTADAS"), leo).andExpect(jsonPath("$[0].startup.id").value(id));
        acao(delete("/api/v1/catalogo/descartadas/" + id), leo).andExpect(status().isNoContent());
        navegar(leo, "").andExpect(jsonPath("$.itens[?(@.startup.id == '" + id + "')]").isNotEmpty());

        acao(put("/api/v1/catalogo/favoritos/" + id), leo).andExpect(status().isNoContent());
        jdbc.update("UPDATE startup SET situacao = 'DESPUBLICADA' WHERE id = ?::uuid", id);
        acao(get("/api/v1/catalogo/minhas/FAVORITOS"), leo).andExpect(jsonPath("$[0].disponivel").value(false));
        acao(put("/api/v1/catalogo/favoritos/" + id), leo).andExpect(status().isNotFound());
        acao(delete("/api/v1/catalogo/favoritos/" + id), leo).andExpect(status().isNoContent());
        acao(get("/api/v1/catalogo/minhas/FAVORITOS"), leo).andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-38
     * Título: Navegar
     * Objetivo: Pedir a lista do catálogo com os filtros na consulta.
     * Decisões: Consulta crua.
     */
    private ResultActions navegar(Usuario quem, String consulta) throws Exception {
        return mvc.perform(get("/api/v1/catalogo/startups" + consulta).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Ação
     * Objetivo: Enviar a requisição com o token.
     * Decisões: Genérica.
     */
    private ResultActions acao(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido, Usuario quem) throws Exception {
        return mvc.perform(pedido.header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Startup publicada
     * Objetivo: Criar a startup pela API e publicá-la no banco, com os valores aprovados.
     * Decisões: Publicada há poucos dias, para entrar nas novas oportunidades.
     */
    private String publicada(Usuario dono, String nome, String segmento, String uf, String estagio, int diasAtras) throws Exception {
        String corpo = mvc.perform(post("/api/v1/startups").header(HttpHeaders.AUTHORIZATION, bearer(dono)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"segmento\":\"" + segmento + "\",\"localizacao\":\"" + uf + "\",\"cnpj\":null}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.id");
        mvc.perform(put("/api/v1/startups/" + id + "/secoes/MODELO_DE_NEGOCIO").header(HttpHeaders.AUTHORIZATION, bearer(dono))
                .contentType(MediaType.APPLICATION_JSON).content("{\"MODELO\":\"SAAS\"}")).andExpect(status().isOk());
        jdbc.update("UPDATE startup SET situacao = 'PUBLICADA', publicada_em = now() - make_interval(days => ?), nome_aprovado = nome, "
                + "segmento_aprovado = segmento, estagio_aprovado = ? WHERE id = ?::uuid", diasAtras, estagio, id);
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Rodada aberta
     * Objetivo: Abrir uma rodada seed por mútuo conversível de R$ 500 mil, direto no banco.
     * Decisões: Sem CNPJ, só para o teste do catálogo.
     */
    private void rodada(String startup, Usuario dono) {
        jdbc.update("INSERT INTO rodada (id, startup_id, situacao, estagio, instrumento, valor_alvo, criada_por, criada_em, atualizada_em) "
                + "VALUES (?, ?::uuid, 'ABERTA', 'SEED', 'MUTUO_CONVERSIVEL', 500000, ?, now(), now())", UUID.randomUUID(), startup, dono.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Conta pública
     * Objetivo: Criar uma conta confirmada, com os termos aceitos.
     * Decisões: Direto no repositório.
     */
    private Usuario conta(Papel papel) {
        Usuario u = Usuario.novo("Pessoa " + papel, "n-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
