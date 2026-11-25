package br.com.orbithub.verificacao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Testes de integração da fila de verificação e da R9
 * Objetivo: Conferir, com PostgreSQL real, o acesso à fila, assumir, as decisões com motivo da lista, o ciclo do
 *           complemento, a aprovação com selo e validade, o encaminhamento por fraude e os quatro passos da R9.
 * Decisões: Contas internas criadas direto; datas ajustadas no banco para simular o tempo da R9.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FilaDeVerificacaoIT {

    static final byte[] PDF = "%PDF-1.4 documento".getBytes(StandardCharsets.US_ASCII);

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
    VerificacoesService verificacoes;

    @Autowired
    ExecutorDeRotinas rotinas;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fila, complemento e aprovação
     * Objetivo: Conferir o acesso, assumir, as recusas de decisão, o complemento com o reenvio, a leitura do documento e
     *           a aprovação, que dá o selo e a validade e avisa o investidor.
     * Decisões: Duas analistas, para conferir o pedido já assumido.
     */
    @Test
    void filaComplementoEAprovacao() throws Exception {
        Usuario ana = investidora();
        Usuario lia = interna(Papel.ANALISTA_VERIFICACAO);
        Usuario rui = interna(Papel.ANALISTA_VERIFICACAO);
        Usuario cora = interna(Papel.CURADOR);
        String pedido = pedirVerificacao(ana);

        mvc.perform(get("/api/v1/admin/verificacoes").header(HttpHeaders.AUTHORIZATION, bearer(cora))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/verificacoes").header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + pedido + "')].prazo").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == '" + pedido + "')].sujeito").value("Investidora"));
        decidir(lia, pedido, "VERIFICADO", "DOCUMENTOS_CONFEREM", null).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/assumir").header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/assumir").header(HttpHeaders.AUTHORIZATION, bearer(rui)))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("pedido_ja_assumido"));
        decidir(lia, pedido, "VERIFICADO", "DOCUMENTO_ILEGIVEL", null)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("motivo_incompativel"));
        decidir(lia, pedido, "PENDENTE_COMPLEMENTO", "DOCUMENTO_ILEGIVEL", " ")
                .andExpect(jsonPath("$.codigo").value("complemento_sem_observacao"));
        decidir(lia, pedido, "PENDENTE_COMPLEMENTO", "DOCUMENTO_ILEGIVEL", "Envie o RG frente e verso, sem reflexo.")
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("PENDENTE_COMPLEMENTO"))
                .andExpect(jsonPath("$.prazoComplementoAte").isNotEmpty());
        assertThat(avisos(ana.getId(), "VERIFICACAO_RESULTADO")).isEqualTo(1);
        mvc.perform(get("/api/v1/verificacoes/minha").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.pedido.observacao").value("Envie o RG frente e verso, sem reflexo."))
                .andExpect(jsonPath("$.pedido.prazoComplementoAte").isNotEmpty());

        enviar(ana, "PF_DOCUMENTO_COM_FOTO").andExpect(status().isCreated());
        mvc.perform(post("/api/v1/verificacoes/envio").header(HttpHeaders.AUTHORIZATION, bearer(ana))).andExpect(status().isOk());
        String detalhe = mvc.perform(get("/api/v1/admin/verificacoes/" + pedido).header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pedido.situacao").value("EM_ANALISE"))
                .andExpect(jsonPath("$.motivos[?(@.grupo == 'FRAUDE')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String documento = JsonPath.read(detalhe, "$.documentos[0].id");
        mvc.perform(get("/api/v1/admin/verificacoes/" + pedido + "/documentos/" + documento + "/arquivo")
                        .header(HttpHeaders.AUTHORIZATION, bearer(lia)))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registro_auditoria WHERE acao = 'DOCUMENTO_VERIFICACAO_LIDO' "
                + "AND objeto_id = ?", Integer.class, pedido)).isEqualTo(1);

        decidir(lia, pedido, "VERIFICADO", "DOCUMENTOS_CONFEREM", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("VERIFICADO")).andExpect(jsonPath("$.validoAte").isNotEmpty());
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, ana.getId())).isEqualTo(Selo.VERIFICADO);
        assertThat(avisos(ana.getId(), "VERIFICACAO_RESULTADO")).isEqualTo(2);
        Instant validade = jdbc.queryForObject("SELECT valido_ate FROM pedido_verificacao WHERE id = ?::uuid", Timestamp.class, pedido)
                .toInstant();
        assertThat(validade).isAfter(Instant.now().plus(Duration.ofDays(360))).isBefore(Instant.now().plus(Duration.ofDays(370)));
        decidir(lia, pedido, "RECUSADO", "CNPJ_IRREGULAR", null).andExpect(jsonPath("$.codigo").value("pedido_fora_da_fila"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Fraude
     * Objetivo: Conferir que recusa por motivo de fraude marca o encaminhamento e avisa a administração.
     * Decisões: O administrador decide sem assumir.
     */
    @Test
    void fraudeEncaminhaAAdministracao() throws Exception {
        Usuario ana = investidora();
        Usuario chefe = interna(Papel.ADMIN);
        String pedido = pedirVerificacao(ana);
        decidir(chefe, pedido, "RECUSADO", "IDENTIDADE_DE_TERCEIRO", "Foto não confere com o documento.")
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("RECUSADO"))
                .andExpect(jsonPath("$.encaminhadoModeracao").value(true));
        assertThat(avisos(chefe.getId(), "EQUIPE_ENCAMINHAMENTO")).isEqualTo(1);
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, ana.getId())).isEqualTo(Selo.NAO_VERIFICADO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Rotina R9
     * Objetivo: Conferir o encerramento do complemento vencido, o aviso antes do vencimento, o vencimento e a limpeza dos
     *           documentos depois da guarda.
     * Decisões: Quatro investidoras, uma para cada passo; datas movidas no banco.
     */
    @Test
    void rotinaR9() throws Exception {
        Usuario chefe = interna(Papel.ADMIN);
        Usuario semComplemento = investidora();
        String p1 = pedirVerificacao(semComplemento);
        decidir(chefe, p1, "PENDENTE_COMPLEMENTO", "FALTA_DOCUMENTO", "Falta o verso.").andExpect(status().isOk());
        jdbc.update("UPDATE pedido_verificacao SET prazo_complemento_ate = now() - interval '1 day' WHERE id = ?::uuid", p1);

        Usuario aVencer = investidora();
        String p2 = pedirVerificacao(aVencer);
        decidir(chefe, p2, "VERIFICADO", "DOCUMENTOS_CONFEREM", null).andExpect(status().isOk());
        jdbc.update("UPDATE pedido_verificacao SET valido_ate = now() + interval '10 days' WHERE id = ?::uuid", p2);

        Usuario vencida = investidora();
        String p3 = pedirVerificacao(vencida);
        decidir(chefe, p3, "VERIFICADO", "DOCUMENTOS_CONFEREM", null).andExpect(status().isOk());
        jdbc.update("UPDATE pedido_verificacao SET valido_ate = now() - interval '1 day' WHERE id = ?::uuid", p3);

        Usuario antiga = investidora();
        String p4 = pedirVerificacao(antiga);
        decidir(chefe, p4, "RECUSADO", "NAO_ATENDE_REQUISITOS", null).andExpect(status().isOk());
        jdbc.update("UPDATE pedido_verificacao SET decidido_em = now() - interval '7 months' WHERE id = ?::uuid", p4);

        rotinas.executarManual("R9", null);
        rotinas.executarManual("R9", null);

        assertThat(situacao(p1)).isEqualTo("ENCERRADO");
        assertThat(situacao(p2)).isEqualTo("VERIFICADO");
        assertThat(avisos(aVencer.getId(), "VERIFICACAO_RESULTADO")).isEqualTo(2);
        assertThat(situacao(p3)).isEqualTo("VENCIDO");
        assertThat(verificacoes.selo(SujeitoDaVerificacao.PESSOA, vencida.getId())).isEqualTo(Selo.NAO_VERIFICADO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM documento_verificacao WHERE pedido_id = ?::uuid", Integer.class, p4))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT documentos_apagados_em FROM pedido_verificacao WHERE id = ?::uuid", Timestamp.class, p4))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM documento_verificacao WHERE pedido_id = ?::uuid", Integer.class, p2))
                .isEqualTo(2);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedir verificação
     * Objetivo: Enviar os dois documentos da pessoa física e mandar para análise.
     * Decisões: Devolve o identificador do pedido.
     */
    private String pedirVerificacao(Usuario quem) throws Exception {
        enviar(quem, "PF_DOCUMENTO_COM_FOTO").andExpect(status().isCreated());
        enviar(quem, "PF_CPF").andExpect(status().isCreated());
        return JsonPath.read(mvc.perform(post("/api/v1/verificacoes/envio").header(HttpHeaders.AUTHORIZATION, bearer(quem)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Enviar documento
     * Objetivo: Subir um PDF de um tipo.
     * Decisões: Pessoa física.
     */
    private ResultActions enviar(Usuario quem, String tipo) throws Exception {
        return mvc.perform(multipart("/api/v1/verificacoes/documentos")
                .file(new MockMultipartFile("arquivo", tipo + ".pdf", MediaType.APPLICATION_OCTET_STREAM_VALUE, PDF))
                .param("tipo", tipo).header(HttpHeaders.AUTHORIZATION, bearer(quem)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Decidir pela API
     * Objetivo: Enviar uma decisão do backoffice.
     * Decisões: Observação opcional.
     */
    private ResultActions decidir(Usuario quem, String pedido, String resultado, String motivo, String observacao) throws Exception {
        String nota = observacao == null ? "null" : "\"" + observacao + "\"";
        return mvc.perform(post("/api/v1/admin/verificacoes/" + pedido + "/decisao").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resultado\":\"" + resultado + "\",\"motivo\":\"" + motivo + "\",\"observacao\":" + nota + "}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Situação no banco
     * Objetivo: Ler a situação de um pedido.
     * Decisões: SQL direto.
     */
    private String situacao(String pedido) {
        return jdbc.queryForObject("SELECT situacao FROM pedido_verificacao WHERE id = ?::uuid", String.class, pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Avisos recebidos
     * Objetivo: Contar as notificações de um tipo de uma conta.
     * Decisões: SQL direto.
     */
    private int avisos(UUID conta, String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND tipo = ?", Integer.class, conta, tipo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Investidora de teste
     * Objetivo: Criar uma investidora ativa com os termos aceitos.
     * Decisões: Nome fixo, para conferir a fila.
     */
    private Usuario investidora() {
        Usuario u = Usuario.novo("Investidora", "f-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(Papel.INVESTIDOR),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Conta da equipe
     * Objetivo: Criar uma conta interna ativa com o papel informado.
     * Decisões: Convite já aceito.
     */
    private Usuario interna(Papel papel) {
        Usuario u = Usuario.novoInterno("Equipe " + papel, "e-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel),
                Instant.now());
        u.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(u);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
