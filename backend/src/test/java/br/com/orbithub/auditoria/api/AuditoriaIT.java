package br.com.orbithub.auditoria.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.auditoria.service.AcessoJustificadoService;
import br.com.orbithub.auditoria.service.AcessoJustificadoService.CasoAberto;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:35
 * Cartão Jira: KAN-80
 * Título: Testes de integração da trilha de auditoria
 * Objetivo: Conferir, com PostgreSQL real, que as ações do backoffice ficam registradas com autor, objeto e
 *           resultado, que a trilha não pode ser alterada nem apagada, que a consulta filtra e que o acesso
 *           justificado só acontece dentro de um caso aberto.
 * Decisões: Uma regra de caso de teste ("denuncia") registrada só neste contexto, porque os casos reais
 *           chegam com a moderação (S10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AuditoriaIT.CasosDeTeste.class})
class AuditoriaIT {

    static final UUID STARTUP = UUID.randomUUID();
    static final UUID CASO_ABERTO = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    RegistroAuditoriaRepository registros;

    @Autowired
    AcessoJustificadoService acessos;

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Ação de sucesso e de falha registradas
     * Objetivo: Conferir que publicar um termo gera DOCUMENTO_LEGAL_PUBLICADO com autor e objeto, e que a
     *           publicação recusada também fica registrada, como falha.
     * Decisões: A recusa usa vigência no passado (422).
     */
    @Test
    void registraAcoesDoBackofficeComAutorObjetoEResultado() throws Exception {
        Usuario admin = conta("Bia Admin", Papel.ADMIN);
        String bearer = bearer(admin);

        mvc.perform(post("/api/v1/admin/documentos-legais").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"POLITICA_DE_MODERACAO\",\"titulo\":\"Política\",\"conteudo\":\"Texto.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/admin/documentos-legais").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"POLITICA_DE_MODERACAO\",\"titulo\":\"Política\",\"conteudo\":\"Texto.\","
                                + "\"vigenteDesde\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isUnprocessableContent());

        List<RegistroAuditoria> doAdmin = registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId());
        assertThat(doAdmin).hasSize(2);
        RegistroAuditoria falha = doAdmin.get(0);
        RegistroAuditoria sucesso = doAdmin.get(1);
        assertThat(sucesso.getAcao()).isEqualTo("DOCUMENTO_LEGAL_PUBLICADO");
        assertThat(sucesso.getResultado()).isEqualTo(Resultado.SUCESSO);
        assertThat(sucesso.getAtorNome()).isEqualTo("Bia Admin");
        assertThat(sucesso.getObjetoTipo()).isEqualTo("documento_legal");
        assertThat(sucesso.getObjetoId()).isNotBlank();
        assertThat(sucesso.getStatusHttp()).isEqualTo(201);
        assertThat(sucesso.getDetalhe()).contains("POLITICA_DE_MODERACAO");
        assertThat(falha.getAcao()).isEqualTo("DOCUMENTO_LEGAL_PUBLICADO");
        assertThat(falha.getResultado()).isEqualTo(Resultado.FALHA);
        assertThat(falha.getStatusHttp()).isEqualTo(422);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Trilha imutável no banco
     * Objetivo: Conferir que nem um UPDATE, DELETE ou TRUNCATE direto no banco altera a trilha.
     * Decisões: Usa JDBC puro, como faria alguém que contornasse a aplicação pela mesma conexão.
     */
    @Test
    void trilhaNaoPodeSerAlteradaNemApagada() throws Exception {
        Usuario admin = conta("Caio Admin", Papel.ADMIN);
        mvc.perform(get("/api/v1/admin/auditoria/acoes").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
        RegistroAuditoria registro = registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId()).get(0);

        assertRecusado(() -> jdbc.update("UPDATE registro_auditoria SET acao = 'APAGADO' WHERE id = ?",
                registro.getId()));
        assertRecusado(() -> jdbc.update("DELETE FROM registro_auditoria WHERE id = ?", registro.getId()));
        assertRecusado(() -> jdbc.execute("TRUNCATE registro_auditoria"));
        assertThat(jdbc.queryForObject("SELECT acao FROM registro_auditoria WHERE id = ?", String.class,
                registro.getId())).isEqualTo("AUDITORIA_ACOES_LISTADAS");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Consulta com filtros e só para o administrador
     * Objetivo: Conferir os filtros por pessoa, ação e período e a recusa para quem não é administrador.
     * Decisões: A própria consulta entra na trilha.
     */
    @Test
    void administradorConsultaComFiltrosEOutrosNao() throws Exception {
        Usuario admin = conta("Dani Admin", Papel.ADMIN);
        String bearer = bearer(admin);
        mvc.perform(get("/api/v1/admin/auditoria/acoes").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("ator", admin.getId().toString()).param("acao", "AUDITORIA_ACOES_LISTADAS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].atorNome").value("Dani Admin"))
                .andExpect(jsonPath("$.itens[0].resultado").value("SUCESSO"));
        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("ator", admin.getId().toString()).param("de", "2099-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("de", "2026-12-01T00:00:00Z").param("ate", "2026-11-01T00:00:00Z"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("periodo_invalido"));
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId()))
                .extracting(RegistroAuditoria::getAcao).contains("AUDITORIA_CONSULTADA");
        mvc.perform(get("/api/v1/admin/auditoria/atores").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + admin.getId() + "')].nome").value("Dani Admin"));

        Usuario investidor = conta("Edu Investidor", Papel.INVESTIDOR);
        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearer(investidor)))
                .andExpect(status().isForbidden());
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(investidor.getId())).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Acesso justificado dentro do caso
     * Objetivo: Conferir que o acesso a dados confidenciais só é liberado com caso aberto e justificativa,
     *           fica registrado uma única vez e aparece para a startup como "equipe Orbit Hub".
     * Decisões: O acesso recusado também fica na trilha, como falha.
     */
    @Test
    void acessoAosDadosConfidenciaisSoDentroDeCasoAberto() throws Exception {
        Usuario admin = conta("Fábio Admin", Papel.ADMIN);
        String bearer = bearer(admin);
        String semCaso = "{\"casoTipo\":\"denuncia\",\"casoId\":\"" + UUID.randomUUID() + "\",\"startupId\":\""
                + STARTUP + "\",\"justificativa\":\"Conferir os documentos citados na denúncia\"}";
        mvc.perform(post("/api/v1/admin/auditoria/acessos-nivel2").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(semCaso))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("caso_nao_aberto"));

        String comCaso = "{\"casoTipo\":\"denuncia\",\"casoId\":\"" + CASO_ABERTO + "\",\"startupId\":\""
                + STARTUP + "\",\"justificativa\":\"Conferir os documentos citados na denúncia\"}";
        mvc.perform(post("/api/v1/admin/auditoria/acessos-nivel2").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(comCaso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acao").value(AcessoJustificadoService.ACAO))
                .andExpect(jsonPath("$.justificativa").value("Conferir os documentos citados na denúncia"));

        List<RegistroAuditoria> doAdmin = registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId());
        assertThat(doAdmin).extracting(RegistroAuditoria::getResultado)
                .containsExactly(Resultado.SUCESSO, Resultado.FALHA);
        assertThat(doAdmin).allMatch(r -> r.getAcao().equals(AcessoJustificadoService.ACAO));
        assertThat(acessos.acessosInternos(STARTUP)).singleElement()
                .satisfies(a -> assertThat(a.autor()).isEqualTo("equipe Orbit Hub"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-80
     * Título: Recusa do gatilho
     * Objetivo: Conferir que o comando foi recusado pelo gatilho da trilha.
     * Decisões: Confere o código de erro do PostgreSQL (42501, insufficient_privilege) e um trecho sem
     *           acento da mensagem, para não depender da codificação do console.
     */
    private static void assertRecusado(ThrowingCallable comando) {
        assertThatThrownBy(comando)
                .rootCause()
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("A trilha de auditoria")
                .extracting(e -> ((SQLException) e).getSQLState()).isEqualTo("42501");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Conta de teste
     * Objetivo: Criar uma conta ativa, com os termos vigentes aceitos.
     * Decisões: E-mail aleatório, para os testes não colidirem.
     */
    private Usuario conta(String nome, Papel papel) {
        Usuario usuario = Usuario.novo(nome, "a-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel),
                Instant.now());
        usuario.confirmarEmail(Instant.now());
        usuarios.save(usuario);
        aceites.aceitar(usuario.getId(),
                aceites.pendentes(usuario.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return usuario;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Sessão real, para o validador de sessão ativa funcionar.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:35
     * Cartão Jira: KAN-80
     * Título: Casos de teste
     * Objetivo: Registrar uma regra de denúncia que conhece um caso aberto sobre a startup de teste.
     * Decisões: Só neste contexto de teste.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class CasosDeTeste {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:35
         * Cartão Jira: KAN-80
         * Título: Regra de denúncia de teste
         * Objetivo: Responder que só o caso conhecido está aberto sobre a startup de teste.
         * Decisões: Classe anônima simples.
         */
        @Bean
        CasoAberto denunciaDeTeste() {
            return new CasoAberto() {
                @Override
                public String tipo() {
                    return "denuncia";
                }

                @Override
                public boolean abertoEnvolvendo(UUID casoId, UUID startupId) {
                    return casoId.equals(CASO_ABERTO) && startupId.equals(STARTUP);
                }
            };
        }
    }
}
