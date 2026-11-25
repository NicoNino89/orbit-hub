package br.com.orbithub.administracao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import java.time.Instant;
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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Testes de integração das listas
 * Objetivo: Conferir, com PostgreSQL real, os itens iniciais, a inclusão, a renomeação com código estável, a
 *           desativação que tira o item dos formulários sem apagar, o histórico e a trilha.
 * Decisões: Itens novos com rótulo aleatório, para os testes não colidirem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ListasIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    ListasService listas;

    @Autowired
    RegistroAuditoriaRepository registros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Ciclo de um item
     * Objetivo: Conferir inclusão, renomeação, desativação, reativação, histórico e o que os formulários veem.
     * Decisões: Lista de segmentos. São 18 listas desde os motivos de decisão da verificação (KAN-83).
     */
    @Test
    void incluiRenomeiaDesativaEReativa() throws Exception {
        Usuario bia = admin();
        String bearer = bearer(bia);
        String sufixo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        mvc.perform(get("/api/v1/listas/TIPO_INVESTIDOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ANJO"))
                .andExpect(jsonPath("$[-1].codigo").value("OUTROS"));
        mvc.perform(get("/api/v1/admin/listas").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(18))
                .andExpect(jsonPath("$[?(@.lista == 'LOCALIZACAO')].ativos").value(28));

        mvc.perform(post("/api/v1/admin/listas/SEGMENTO").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rotulo\":\"Turismo " + sufixo + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TURISMO_" + sufixo))
                .andExpect(jsonPath("$.ordem").value(Matchers.lessThan(99)));
        String codigo = "TURISMO_" + sufixo;
        mvc.perform(post("/api/v1/admin/listas/SEGMENTO").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rotulo\":\"Turismo " + sufixo + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("codigo_em_uso"));

        mvc.perform(put("/api/v1/admin/listas/SEGMENTO/" + codigo).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rotulo\":\"Turismo e hotelaria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.rotulo").value("Turismo e hotelaria"));
        mvc.perform(post("/api/v1/admin/listas/SEGMENTO/" + codigo + "/desativacao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/v1/listas/SEGMENTO"))
                .andExpect(jsonPath("$[?(@.codigo == '" + codigo + "')]").isEmpty());
        mvc.perform(get("/api/v1/admin/listas/SEGMENTO").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[?(@.codigo == '" + codigo + "')].ativo").value(false));
        assertThat(listas.rotulo(Lista.SEGMENTO, codigo)).isEqualTo("Turismo e hotelaria");
        assertThatThrownBy(() -> listas.exigirAtivo(Lista.SEGMENTO, codigo)).extracting("codigo")
                .isEqualTo("opcao_indisponivel");

        mvc.perform(post("/api/v1/admin/listas/SEGMENTO/" + codigo + "/reativacao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.ativo").value(true));
        mvc.perform(get("/api/v1/admin/listas/SEGMENTO/" + codigo + "/historico").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].acao").value("REATIVADO"))
                .andExpect(jsonPath("$[2].acao").value("RENOMEADO"))
                .andExpect(jsonPath("$[2].rotuloAnterior").value("Turismo " + sufixo))
                .andExpect(jsonPath("$[3].autor").value("Bia Admin"));
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(bia.getId())).extracting(RegistroAuditoria::getAcao)
                .contains("LISTA_ITEM_INCLUIDO", "LISTA_ITEM_RENOMEADO", "LISTA_ITEM_DESATIVADO", "LISTA_ITEM_REATIVADO");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:06
     * Cartão Jira: KAN-79
     * Título: Grupo obrigatório e permissões
     * Objetivo: Conferir que lista com grupo exige grupo, que não há rota de exclusão e que só o administrador gere.
     * Decisões: Curador recebe 403.
     */
    @Test
    void exigeGrupoESoAdministradorGere() throws Exception {
        String bearer = bearer(admin());
        mvc.perform(post("/api/v1/admin/listas/DOCUMENTO_VERIFICACAO").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rotulo\":\"Selfie com documento\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Informe o grupo do item."));
        mvc.perform(delete("/api/v1/admin/listas/SEGMENTO/FINTECH").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().is4xxClientError());
        Usuario curador = Usuario.novoInterno("Cris", "c-" + UUID.randomUUID() + "@orbithub.com", "{noop}x",
                Set.of(Papel.CURADOR), Instant.now());
        curador.aceitarConvite("{noop}x", Instant.now());
        usuarios.save(curador);
        mvc.perform(get("/api/v1/admin/listas").header(HttpHeaders.AUTHORIZATION, bearer(curador)))
                .andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:06
     * Cartão Jira: KAN-79
     * Título: Administrador de teste
     * Objetivo: Criar um administrador ativo.
     * Decisões: Nome fixo para o histórico.
     */
    private Usuario admin() {
        Usuario admin = Usuario.novoInterno("Bia Admin", "l-" + UUID.randomUUID() + "@orbithub.com", "{noop}x",
                Set.of(Papel.ADMIN), Instant.now());
        admin.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(admin);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:06
     * Cartão Jira: KAN-79
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
