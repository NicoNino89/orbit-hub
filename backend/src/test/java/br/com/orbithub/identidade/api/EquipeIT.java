package br.com.orbithub.identidade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Totp;
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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:45
 * Cartão Jira: KAN-75
 * Título: Testes de integração da equipe interna
 * Objetivo: Percorrer, pela API e com PostgreSQL real, o ciclo de um membro: convite, senha, segundo fator,
 *           acesso por papel, troca de papéis, desligamento e revisão de acessos, com tudo na trilha.
 * Decisões: O envio de e-mail é simulado para capturar o link do convite. Os códigos TOTP são calculados com o
 *           segredo devolvido pela configuração, como faria o aplicativo do celular.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EquipeIT {

    static final String SENHA = "Senha-da-equipe-2026";

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    RegistroAuditoriaRepository registros;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Ciclo completo do membro
     * Objetivo: Conferir convite, aceite, configuração do segundo fator, entrada, permissão por papel, recusa de
     *           código reutilizado e o registro na trilha.
     * Decisões: O membro é curador: entra no backoffice, mas não abre a gestão da equipe nem a trilha.
     */
    @Test
    void membroConvidadoDefineSenhaConfiguraSegundoFatorEEntra() throws Exception {
        Usuario admin = administrador();
        String bearerAdmin = bearer(admin);
        String emailMembro = "cur-" + UUID.randomUUID() + "@orbithub.com";

        String criado = mvc.perform(post("/api/v1/admin/equipe").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cris Curadora\",\"email\":\"" + emailMembro + "\",\"papeis\":[\"CURADOR\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("CONVIDADO"))
                .andExpect(jsonPath("$.segundoFatorAtivo").value(false))
                .andReturn().getResponse().getContentAsString();
        String membroId = JsonPath.read(criado, "$.id");

        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailMembro + "\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isUnauthorized());

        String convite = ultimoToken();
        mvc.perform(post("/api/v1/equipe/convite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + convite + "\",\"senha\":\"curta\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("senha_fraca"));
        mvc.perform(post("/api/v1/equipe/convite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + convite + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/equipe/convite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + convite + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnprocessableContent());

        String desafio = entrarComSenha(emailMembro, "CONFIGURAR");
        String configuracao = mvc.perform(post("/api/v1/auth/segundo-fator/configuracao")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"desafio\":\"" + desafio + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco").value(org.hamcrest.Matchers.startsWith("otpauth://totp/Orbit%20Hub")))
                .andReturn().getResponse().getContentAsString();
        String segredo = ((String) JsonPath.read(configuracao, "$.segredo")).replace(" ", "");

        String codigo = Totp.codigo(segredo, Totp.passo(Instant.now()));
        String sessao = mvc.perform(post("/api/v1/auth/segundo-fator").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desafio\":\"" + desafio + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papeis[0]").value("CURADOR"))
                .andExpect(jsonPath("$.termosPendentes").value(false))
                .andReturn().getResponse().getContentAsString();
        String bearerMembro = "Bearer " + JsonPath.read(sessao, "$.tokenAcesso");

        mvc.perform(get("/api/v1/admin/equipe").header(HttpHeaders.AUTHORIZATION, bearerMembro))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearerMembro))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/conta/papeis/INVESTIDOR").header(HttpHeaders.AUTHORIZATION, bearerMembro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"aceites\":[]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("conta_interna"));

        String outroDesafio = entrarComSenha(emailMembro, "CODIGO");
        mvc.perform(post("/api/v1/auth/segundo-fator").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desafio\":\"" + outroDesafio + "\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("codigo_invalido"));

        mvc.perform(get("/api/v1/admin/equipe").header(HttpHeaders.AUTHORIZATION, bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + membroId + "')].segundoFatorAtivo").value(true))
                .andExpect(jsonPath("$[?(@.id == '" + membroId + "')].situacao").value("ATIVO"));

        UUID idDoMembro = UUID.fromString(membroId);
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(idDoMembro)).extracting(RegistroAuditoria::getAcao)
                .contains("EQUIPE_ENTROU", "SEGUNDO_FATOR_CONFIGURADO");
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId())).extracting(RegistroAuditoria::getAcao)
                .contains("EQUIPE_MEMBRO_CONVIDADO");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Códigos errados bloqueiam
     * Objetivo: Conferir que códigos errados seguidos bloqueiam a entrada, como as senhas erradas.
     * Decisões: Limite padrão de 5 tentativas; o membro é criado direto no banco com segundo fator.
     */
    @Test
    void codigosErradosBloqueiamAEntrada() throws Exception {
        String emailMembro = "mod-" + UUID.randomUUID() + "@orbithub.com";
        criarMembroComSenha(emailMembro, Papel.MODERADOR);
        String desafio = entrarComSenha(emailMembro, "CONFIGURAR");
        mvc.perform(post("/api/v1/auth/segundo-fator/configuracao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"desafio\":\"" + desafio + "\"}")).andExpect(status().isOk());

        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/api/v1/auth/segundo-fator").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"desafio\":\"" + desafio + "\",\"codigo\":\"000000\"}"))
                    .andExpect(status().isUnprocessableContent());
        }
        mvc.perform(post("/api/v1/auth/segundo-fator").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desafio\":\"" + desafio + "\",\"codigo\":\"000000\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("login_bloqueado"));
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailMembro + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Troca de papéis e desligamento valem na hora
     * Objetivo: Conferir que trocar papéis e desligar derrubam a sessão do membro imediatamente, que o desligado
     *           não entra mais e que as ações ficam na trilha com justificativa.
     * Decisões: O membro tem sessão aberta direto pelo serviço.
     */
    @Test
    void trocaDePapeisEDesligamentoValemNaHora() throws Exception {
        Usuario admin = administrador();
        String bearerAdmin = bearer(admin);
        Usuario membro = criarMembroComSenha("sup-" + UUID.randomUUID() + "@orbithub.com", Papel.SUPORTE_N1);
        String bearerMembro = bearer(membro);
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearerMembro)).andExpect(status().isOk());

        mvc.perform(put("/api/v1/admin/equipe/" + membro.getId() + "/papeis").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papeis\":[\"SUPORTE_N2\",\"ENCARREGADO_DADOS\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis.length()").value(2));
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearerMembro))
                .andExpect(status().isUnauthorized());

        String bearerNovo = bearer(usuarios.findById(membro.getId()).orElseThrow());
        mvc.perform(get("/api/v1/admin/auditoria").header(HttpHeaders.AUTHORIZATION, bearerNovo))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/admin/equipe/" + membro.getId() + "/desligamento")
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Fim do contrato de estágio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.casosDevolvidos").value(0));
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearerNovo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + membro.getEmail() + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/equipe/" + admin.getId() + "/desligamento")
                        .header(HttpHeaders.AUTHORIZATION, bearerAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Teste\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("auto_desligamento"));

        List<RegistroAuditoria> doAdmin = registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId());
        assertThat(doAdmin).filteredOn(r -> r.getAcao().equals("EQUIPE_MEMBRO_DESLIGADO")
                        && r.getObjetoId().equals(membro.getId().toString()))
                .singleElement()
                .satisfies(r -> assertThat(r.getJustificativa()).isEqualTo("Fim do contrato de estágio"));
        assertThat(doAdmin).filteredOn(r -> r.getAcao().equals("EQUIPE_PAPEIS_ALTERADOS")).singleElement()
                .satisfies(r -> assertThat(r.getDetalhe()).isEqualTo(
                        "De: SUPORTE_N1 | Para: ENCARREGADO_DADOS, SUPORTE_N2"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Convite com e-mail de conta pública e revisão de acessos
     * Objetivo: Conferir que o e-mail de uma conta pública é recusado no convite e que a revisão de acessos é
     *           registrada e muda a situação para em dia.
     * Decisões: A revisão é global, então só confere que a próxima ficou no futuro.
     */
    @Test
    void recusaEmailDeContaPublicaERegistraRevisao() throws Exception {
        Usuario admin = administrador();
        String bearerAdmin = bearer(admin);
        Usuario publica = Usuario.novo("Ana", "ana-" + UUID.randomUUID() + "@exemplo.com", "{noop}x",
                Set.of(Papel.INVESTIDOR), Instant.now());
        usuarios.save(publica);
        mvc.perform(post("/api/v1/admin/equipe").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"" + publica.getEmail() + "\",\"papeis\":[\"ADMIN\"]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("email_em_uso"));

        mvc.perform(post("/api/v1/admin/equipe/revisao").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacao\":\"Revisão do trimestre\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membros[?(@.membroId == '" + admin.getId() + "')].papeis").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/equipe/revisao").header(HttpHeaders.AUTHORIZATION, bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vencida").value(false))
                .andExpect(jsonPath("$.intervaloMeses").value(3));
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId())).extracting(RegistroAuditoria::getAcao)
                .contains("EQUIPE_REVISAO_CONFIRMADA");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Administrador de teste
     * Objetivo: Criar um administrador ativo direto no banco.
     * Decisões: A sessão é aberta pelo serviço, sem passar pelo segundo fator (coberto no primeiro teste).
     */
    private Usuario administrador() {
        return criarMembroComSenha("adm-" + UUID.randomUUID() + "@orbithub.com", Papel.ADMIN);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Membro de teste com senha
     * Objetivo: Criar um membro ativo com a senha de teste, sem segundo fator configurado.
     * Decisões: Senha com o codificador real do Spring ({bcrypt}), para o login conferir de verdade.
     */
    private Usuario criarMembroComSenha(String emailMembro, Papel papel) {
        Usuario membro = Usuario.novoInterno("Membro " + papel, emailMembro, "{noop}x", Set.of(papel), Instant.now());
        membro.aceitarConvite(org.springframework.security.crypto.factory.PasswordEncoderFactories
                .createDelegatingPasswordEncoder().encode(SENHA), Instant.now());
        return usuarios.save(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Entrar com a senha
     * Objetivo: Fazer o primeiro passo do login de um membro e devolver o desafio.
     * Decisões: Confere a etapa esperada.
     */
    private String entrarComSenha(String emailMembro, String etapa) throws Exception {
        String resposta = mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailMembro + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segundoFator").value(etapa))
                .andExpect(jsonPath("$.tokenAcesso").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.desafio");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Token do último convite
     * Objetivo: Ler o token do link do último e-mail de convite enviado.
     * Decisões: O link termina na quebra de linha.
     */
    private String ultimoToken() {
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getValue().texto();
        int inicio = texto.indexOf("token=") + 6;
        return texto.substring(inicio, texto.indexOf('\n', inicio));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:45
     * Cartão Jira: KAN-75
     * Título: Token de uma conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
