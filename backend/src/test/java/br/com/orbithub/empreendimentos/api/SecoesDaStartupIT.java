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
import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.empreendimentos.service.SecoesDaStartupService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Testes de integração das seções da startup
 * Objetivo: Conferir, com PostgreSQL real, o catálogo de campos, o salvamento por seção com limites e recusas, o
 *           apagamento do campo vazio e a edição do colaborador só na seção autorizada.
 * Decisões: E-mail simulado, para o convite do colaborador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecoesDaStartupIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    SecoesDaStartupService secoes;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Problema, solução e produto
     * Objetivo: Conferir o catálogo, as recusas (seção inválida, campo de outra seção, texto longo, sim/não), o
     *           salvamento normalizado, o apagamento do vazio e as permissões do colaborador e de quem é de fora.
     * Decisões: Eva cria; Lia é colaboradora só do produto; Caio é de fora.
     */
    @Test
    void problemaSolucaoEProduto() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        Usuario lia = conta(Papel.EMPREENDEDOR);
        Usuario caio = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);

        mvc.perform(get("/api/v1/startups/campos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[0].campo").value("PROBLEMA")).andExpect(jsonPath("$[0].secao").value("PROBLEMA_E_SOLUCAO"))
                .andExpect(jsonPath("$[0].limite").value(1000))
                .andExpect(jsonPath("$[?(@.campo == 'TEM_MVP')].tipo").value("SIM_NAO"));

        secao(eva, id, "CADASTRO", "{}").andExpect(jsonPath("$.codigo").value("secao_invalida"));
        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"TEM_MVP\":\"SIM\"}").andExpect(jsonPath("$.codigo").value("campo_invalido"));
        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"INVENTADO\":\"x\"}").andExpect(jsonPath("$.codigo").value("campo_invalido"));
        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"" + "a".repeat(1001) + "\"}").andExpect(jsonPath("$.codigo").value("texto_longo"));
        secao(eva, id, "PRODUTO", "{\"TEM_MVP\":\"TALVEZ\"}").andExpect(jsonPath("$.codigo").value("valor_invalido"));

        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"  Frete caro no interior  \",\"SOLUCAO\":\"Rotas compartilhadas\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.PROBLEMA").value("Frete caro no interior"));
        secao(eva, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"Frete caro\",\"SOLUCAO\":\" \"}")
                .andExpect(jsonPath("$.PROBLEMA").value("Frete caro")).andExpect(jsonPath("$.SOLUCAO").doesNotExist());

        aceitar(lia, convidar(eva, id, lia.getEmail(), "COLABORADOR", "\"PRODUTO\"")).andExpect(status().isOk());
        secao(lia, id, "PROBLEMA_E_SOLUCAO", "{\"PROBLEMA\":\"outro\"}").andExpect(status().isForbidden());
        secao(lia, id, "PRODUTO", "{\"TEM_MVP\":\"NAO\",\"TECNOLOGIA\":\"Java e React\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.TEM_MVP").value("NAO")).andExpect(jsonPath("$.PROBLEMA").value("Frete caro"));
        mvc.perform(get("/api/v1/startups/" + id + "/secoes").header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(jsonPath("$.TECNOLOGIA").value("Java e React"));
        mvc.perform(get("/api/v1/startups/" + id + "/secoes").header(HttpHeaders.AUTHORIZATION, bearer(caio))).andExpect(status().isNotFound());
        secao(caio, id, "PRODUTO", "{}").andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:02
     * Cartão Jira: KAN-23
     * Título: Mercado e modelo de negócio
     * Objetivo: Conferir os campos de mercado e a classificação do modelo pela lista gerenciável (aceita e recusada).
     * Decisões: SAAS existe na lista MODELO_NEGOCIO; ESPACIAL, não.
     */
    @Test
    void mercadoEModeloDeNegocio() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        mvc.perform(get("/api/v1/startups/campos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[?(@.campo == 'MODELO')].lista").value("MODELO_NEGOCIO"))
                .andExpect(jsonPath("$[?(@.campo == 'MODELO')].limite").value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
        secao(eva, id, "MODELO_DE_NEGOCIO", "{\"MODELO\":\"ESPACIAL\"}").andExpect(jsonPath("$.codigo").value("opcao_indisponivel"));
        secao(eva, id, "MODELO_DE_NEGOCIO", "{\"MODELO\":\"SAAS\",\"PRECIFICACAO\":\"Por usuário ao mês\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.MODELO").value("SAAS"));
        secao(eva, id, "MERCADO", "{\"PUBLICO_ALVO\":\"Cooperativas\",\"CONCORRENTES\":\"Transportadoras locais\"}")
                .andExpect(jsonPath("$.PUBLICO_ALVO").value("Cooperativas")).andExpect(jsonPath("$.MODELO").value("SAAS"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:26
     * Cartão Jira: KAN-25
     * Título: Planejamento e necessidade de investimento
     * Objetivo: Conferir os percentuais (faixa, casas, soma até 100%), o prazo inteiro com o máximo e o salvamento normalizado.
     * Decisões: Destinação 40 + 35,5 + 24,5 = 100.
     */
    @Test
    void planejamentoENecessidade() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        mvc.perform(get("/api/v1/startups/campos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[?(@.campo == 'PRAZO_USO_MESES')].limite").value(org.hamcrest.Matchers.contains(120)));
        String n = "NECESSIDADE_DE_INVESTIMENTO";
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"101\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"-1\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"10,555\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"muito\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"60\",\"DESTINO_EQUIPE\":\"50\"}").andExpect(jsonPath("$.codigo").value("percentuais_acima"));
        secao(eva, id, n, "{\"PRAZO_USO_MESES\":\"0\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"PRAZO_USO_MESES\":\"121\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"PRAZO_USO_MESES\":\"18.5\"}").andExpect(jsonPath("$.codigo").value("numero_invalido"));
        secao(eva, id, n, "{\"DESTINO_PRODUTO\":\"40\",\"DESTINO_COMERCIAL\":\"35,5\",\"DESTINO_EQUIPE\":\"24.50\",\"PRAZO_USO_MESES\":\"18\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.DESTINO_COMERCIAL").value("35.5"))
                .andExpect(jsonPath("$.DESTINO_EQUIPE").value("24.5")).andExpect(jsonPath("$.PRAZO_USO_MESES").value("18"));
        secao(eva, id, "PLANEJAMENTO", "{\"RISCOS\":\"Sazonalidade da safra\",\"MITIGACAO\":\"Clientes em duas regiões\"}")
                .andExpect(jsonPath("$.RISCOS").value("Sazonalidade da safra")).andExpect(jsonPath("$.PRAZO_USO_MESES").value("18"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Estágio do negócio
     * Objetivo: Conferir a escala no catálogo com as explicações, a recusa de nível inexistente, o salvamento e a leitura pelo serviço público.
     * Decisões: MVP; ESPACIAL não existe.
     */
    @Test
    void estagioDoNegocio() throws Exception {
        Usuario eva = conta(Papel.EMPREENDEDOR);
        String id = startup(eva);
        mvc.perform(get("/api/v1/startups/campos").header(HttpHeaders.AUTHORIZATION, bearer(eva)))
                .andExpect(jsonPath("$[?(@.campo == 'ESTAGIO')].opcoes[0].codigo").value(org.hamcrest.Matchers.contains("IDEACAO")))
                .andExpect(jsonPath("$[?(@.campo == 'ESTAGIO')].opcoes.length()").value(org.hamcrest.Matchers.contains(7)))
                .andExpect(jsonPath("$[?(@.campo == 'ESTAGIO')].opcoes[2].explicacao").value(org.hamcrest.Matchers.contains(
                        org.hamcrest.Matchers.containsString("primeira versão"))))
                .andExpect(jsonPath("$[0].opcoes").doesNotExist());
        assertThat(secoes.estagio(UUID.fromString(id))).isEmpty();
        secao(eva, id, "MATURIDADE", "{\"ESTAGIO\":\"ESPACIAL\"}").andExpect(jsonPath("$.codigo").value("opcao_indisponivel"));
        secao(eva, id, "MATURIDADE", "{\"ESTAGIO\":\"MVP\"}").andExpect(status().isOk()).andExpect(jsonPath("$.ESTAGIO").value("MVP"));
        assertThat(secoes.estagio(UUID.fromString(id))).contains(MaturidadeDoNegocio.MVP);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Salvar seção pela API
     * Objetivo: Enviar os valores de uma seção.
     * Decisões: Corpo em JSON.
     */
    private ResultActions secao(Usuario quem, String id, String secao, String corpo) throws Exception {
        return mvc.perform(put("/api/v1/startups/" + id + "/secoes/" + secao).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
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
     * Hora: 22:56
     * Cartão Jira: KAN-22
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
     * Hora: 22:56
     * Cartão Jira: KAN-22
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
     * Hora: 22:56
     * Cartão Jira: KAN-22
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
     * Hora: 22:56
     * Cartão Jira: KAN-22
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
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
