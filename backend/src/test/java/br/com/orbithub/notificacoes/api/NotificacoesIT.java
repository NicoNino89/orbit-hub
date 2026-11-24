package br.com.orbithub.notificacoes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.domain.TipoDocumento;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.administracao.service.DocumentoLegalService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.rotinas.ExecutorDeRotinas;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import br.com.orbithub.notificacoes.domain.Notificacao;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.sql.Timestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Testes de integração da central de notificações
 * Objetivo: Conferir, com PostgreSQL real, a entrega por canal, o contador, a leitura, as preferências (com aviso
 *           obrigatório travado), o resumo diário, o envio assíncrono com nova tentativa, a limpeza e os eventos reais.
 * Decisões: E-mail simulado; relógio ajustável, para avançar entre as tentativas de envio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, NotificacoesIT.Relogio.class})
class NotificacoesIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    NotificacoesService notificacoes;

    @Autowired
    ExecutorDeRotinas rotinas;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    DocumentoLegalService documentos;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RelogioAjustavel relogio;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Sino, leitura e preferências
     * Objetivo: Conferir o aviso no sino com contador, a leitura, a escolha de só e-mail e a recusa de desligar aviso
     *           obrigatório; aviso de outra pessoa não é encontrado.
     * Decisões: Conta de investidora.
     */
    @Test
    void sinoLeituraEPreferencias() throws Exception {
        Usuario ana = conta();
        String bearer = bearer(ana);
        Notificacao match = notificacoes.notificar(ana.getId(), TipoDeNotificacao.MATCH, "Match com a NovaSafra",
                "A NovaSafra aceitou o seu interesse.", "/investidor");
        mvc.perform(get("/api/v1/notificacoes/contagem").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.naoLidas").value(1));
        mvc.perform(get("/api/v1/notificacoes").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.itens[0].titulo").value("Match com a NovaSafra"))
                .andExpect(jsonPath("$.itens[0].lida").value(false));
        assertThat(pendentes(ana.getId())).isEqualTo(1);

        mvc.perform(post("/api/v1/notificacoes/" + match.getId() + "/leitura").header(HttpHeaders.AUTHORIZATION, bearer(conta())))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notificacoes/" + match.getId() + "/leitura").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notificacoes/contagem").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.naoLidas").value(0));

        mvc.perform(put("/api/v1/notificacoes/preferencias").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canais\":{\"SEGURANCA_CONTA\":\"PLATAFORMA\"},\"resumoDiario\":false}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("aviso_obrigatorio"));
        mvc.perform(put("/api/v1/notificacoes/preferencias").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canais\":{\"MATCH\":\"EMAIL\"},\"resumoDiario\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipos[?(@.tipo == 'MATCH')].canal").value("EMAIL"))
                .andExpect(jsonPath("$.tipos[?(@.tipo == 'TERMOS_ATUALIZADOS')].obrigatoria").value(true));
        notificacoes.notificar(ana.getId(), TipoDeNotificacao.MATCH, "Outro Match", "Texto", null);
        mvc.perform(get("/api/v1/notificacoes/contagem").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.naoLidas").value(0));
        assertThat(pendentes(ana.getId())).isEqualTo(2);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Envio com nova tentativa e resumo diário
     * Objetivo: Conferir que a R17 envia, reagenda a falha e desiste no limite, e que a R18 junta os avisos do resumo
     *           num único e-mail, sem incluir os obrigatórios.
     * Decisões: O limite padrão é de 5 tentativas.
     */
    @Test
    void envioComNovaTentativaEResumo() {
        reset(email);
        Usuario bia = conta();
        when(email.tentarEnviar(any())).thenReturn(false);
        notificacoes.notificar(bia.getId(), TipoDeNotificacao.MATCH, "Match", "Texto", "/investidor");
        rotinas.executarManual("R17", null);
        assertThat(jdbc.queryForObject("SELECT tentativas FROM notificacao_email WHERE usuario_id = ?", Integer.class,
                bia.getId())).isEqualTo(1);
        for (int i = 0; i < 4; i++) {
            relogio.avancar(Duration.ofHours(7));
            rotinas.executarManual("R17", null);
        }
        assertThat(jdbc.queryForObject("SELECT situacao FROM notificacao_email WHERE usuario_id = ?", String.class,
                bia.getId())).isEqualTo("FALHOU");

        when(email.tentarEnviar(any())).thenReturn(true);
        Usuario caio = conta();
        jdbc.update("INSERT INTO preferencia_resumo (usuario_id, resumo_diario) VALUES (?, TRUE)", caio.getId());
        notificacoes.notificar(caio.getId(), TipoDeNotificacao.MATCH, "Match com a NovaSafra", "Texto", null);
        notificacoes.notificar(caio.getId(), TipoDeNotificacao.NIVEL2_LIBERADO, "Acesso liberado", "Texto", null);
        notificacoes.notificar(caio.getId(), TipoDeNotificacao.SEGURANCA_CONTA, "Senha alterada", "Texto", null);
        rotinas.executarManual("R17", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao_email WHERE usuario_id = ? AND situacao = 'ENVIADO'",
                Integer.class, caio.getId())).isEqualTo(1);
        reset(email);
        when(email.tentarEnviar(any())).thenReturn(true);
        rotinas.executarManual("R18", null);
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).tentarEnviar(mensagem.capture());
        assertThat(mensagem.getAllValues()).filteredOn(m -> m.assunto().equals("Orbit Hub: resumo do dia"))
                .anySatisfy(m -> assertThat(m.texto()).contains("- Match com a NovaSafra").contains("- Acesso liberado")
                        .doesNotContain("Senha alterada"));
        assertThat(pendentes(caio.getId())).isZero();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Eventos reais e limpeza
     * Objetivo: Conferir os avisos de senha trocada e de termos atualizados e a limpeza das notificações antigas.
     * Decisões: A senha é trocada pela API de perfil.
     */
    @Test
    void eventosReaisELimpeza() throws Exception {
        when(email.tentarEnviar(any())).thenReturn(true);
        Usuario davi = conta();
        mvc.perform(put("/api/v1/conta/senha").header(HttpHeaders.AUTHORIZATION, bearer(davi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Senha-antiga-2026\",\"novaSenha\":\"Senha-nova-de-teste-2026\"}"))
                .andExpect(status().isNoContent());
        assertThat(titulos(davi.getId())).contains("Senha alterada");

        UUID admin = UUID.randomUUID();
        Usuario equipe = Usuario.novoInterno("Bia", "n-" + admin + "@orbithub.com", "{noop}x", Set.of(Papel.ADMIN),
                Instant.now());
        equipe.aceitarConvite("{noop}x", Instant.now());
        usuarios.save(equipe);
        documentos.publicar(TipoDocumento.POLITICA_DE_PRIVACIDADE, "Política de Privacidade", "Texto novo.", null,
                equipe.getId());
        assertThat(titulos(davi.getId())).contains("Política de Privacidade atualizado");
        assertThat(titulos(equipe.getId())).isEmpty();

        jdbc.update("UPDATE notificacao SET criada_em = ? WHERE usuario_id = ?",
                Timestamp.from(relogio.instant().minus(Duration.ofDays(400))), davi.getId());
        rotinas.executarManual("R19", null);
        assertThat(titulos(davi.getId())).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: E-mails pendentes
     * Objetivo: Contar os e-mails ainda pendentes de uma pessoa.
     * Decisões: SQL direto.
     */
    private int pendentes(UUID usuario) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao_email WHERE usuario_id = ? AND situacao = 'PENDENTE'",
                Integer.class, usuario);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Títulos dos avisos
     * Objetivo: Listar os títulos das notificações de uma pessoa.
     * Decisões: SQL direto.
     */
    private List<String> titulos(UUID usuario) {
        return jdbc.queryForList("SELECT titulo FROM notificacao WHERE usuario_id = ?", String.class, usuario);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Conta de teste
     * Objetivo: Criar uma investidora ativa com os termos aceitos e senha conhecida.
     * Decisões: Senha com o codificador real.
     */
    private Usuario conta() {
        Usuario u = Usuario.novo("Ana", "n-" + UUID.randomUUID() + "@exemplo.com",
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
                        .encode("Senha-antiga-2026"), Set.of(Papel.INVESTIDOR), Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Relógio ajustável
     * Objetivo: Deixar o teste avançar o tempo entre as tentativas de envio.
     * Decisões: Começa agora, para os tokens continuarem válidos.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class Relogio {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:36
         * Cartão Jira: KAN-146
         * Título: Bean do relógio
         * Objetivo: Substituir o relógio do sistema neste contexto.
         * Decisões: @Primary.
         */
        @Bean
        @Primary
        RelogioAjustavel relogioDeTeste() {
            return new RelogioAjustavel(Instant.now());
        }
    }
}
