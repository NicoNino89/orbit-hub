package br.com.orbithub.identidade.api;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Teste de integração do perfil e dos papéis
 * Objetivo: Verificar, com API, banco e armazenamento local reais, a edição do perfil, a foto, a troca de
 *           e-mail confirmada pelo link e a ativação e desativação de papéis com histórico.
 * Decisões: Conta empreendedora com termos aceitos; e-mail simulado para ler o link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerfilIT {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder senhas;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @MockitoBean
    EnvioDeEmail email;

    private Usuario ana;
    private String bearer;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Conta e sessão de apoio
     * Objetivo: Criar uma empreendedora ativa, com os termos básicos aceitos, e abrir a sessão.
     * Decisões: A confidencialidade fica pendente, para testar a ativação de investidor.
     */
    @BeforeEach
    void preparar() {
        ana = Usuario.novo("Ana", "ana-" + UUID.randomUUID() + "@exemplo.com", senhas.encode("senha-atual-longa"),
                Set.of(Papel.EMPREENDEDOR), Instant.now());
        ana.confirmarEmail(Instant.now());
        usuarios.save(ana);
        aceites.aceitar(ana.getId(), aceites.pendentes(ana.getId(), false).stream().map(DocumentoLegal::getId).toList());
        bearer = "Bearer " + sessoes.abrir(ana).tokenAcesso();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Perfil e foto
     * Objetivo: Garantir a edição de nome e telefone, a validação e o envio e leitura da foto.
     * Decisões: Telefone inválido dá 400 no campo; texto disfarçado de imagem dá 422.
     */
    @Test
    void editaPerfilEFoto() throws Exception {
        mvc.perform(put("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Lima\",\"telefone\":\"(11) 99999-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.telefone").value("(11) 99999-0000"));
        mvc.perform(put("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"telefone\":\"<script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.telefone").exists());

        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/conta/foto")
                        .file(new MockMultipartFile("arquivo", "foto.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temFoto").value(true));
        mvc.perform(get("/api/v1/conta/foto").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(PNG));
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT, "/api/v1/conta/foto")
                        .file(new MockMultipartFile("arquivo", "foto.png", "image/png", "<html>".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnprocessableContent());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Troca de e-mail confirmada
     * Objetivo: Garantir que o e-mail só muda depois do link enviado ao novo endereço.
     * Decisões: Confere a conta antes e depois da confirmação.
     */
    @Test
    void trocaEmailComConfirmacao() throws Exception {
        String novo = "novo-" + UUID.randomUUID() + "@exemplo.com";
        mvc.perform(post("/api/v1/conta/email").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"novoEmail\":\"" + novo + "\",\"senhaAtual\":\"senha-atual-longa\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.email").value(ana.getEmail()));

        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getAllValues().stream().filter(m -> m.para().equals(novo)).findFirst().orElseThrow().texto();
        String token = texto.substring(texto.indexOf("token=") + 6, texto.indexOf('\n', texto.indexOf("token=")));

        mvc.perform(post("/api/v1/contas/email/confirmacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.email").value(novo));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Papéis com histórico
     * Objetivo: Garantir a ativação de investidor com a confidencialidade, a desativação de empreendedor e o
     *           registro no histórico.
     * Decisões: Sem a confidencialidade, a ativação é recusada; o último papel não pode sair.
     */
    @Test
    void ativaEDesativaPapeis() throws Exception {
        mvc.perform(post("/api/v1/conta/papeis/INVESTIDOR").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("aceite_obrigatorio"));

        List<UUID> confidencialidade = aceites.pendentes(ana.getId(), true).stream().map(DocumentoLegal::getId).toList();
        mvc.perform(post("/api/v1/conta/papeis/INVESTIDOR").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceites\":[\"" + confidencialidade.get(0) + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoPasso").value("/investidor/onboarding"));

        mvc.perform(delete("/api/v1/conta/papeis/EMPREENDEDOR").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/conta/papeis/INVESTIDOR").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ultimo_papel"));

        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$.papeis[0]").value("INVESTIDOR"))
                .andExpect(jsonPath("$.papeis.length()").value(1));
        mvc.perform(get("/api/v1/conta/historico").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[0].evento").value("PAPEL_DESATIVADO"))
                .andExpect(jsonPath("$[1].evento").value("PAPEL_ATIVADO"))
                .andExpect(jsonPath("$[1].detalhe").value("INVESTIDOR"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Trocar senha mantém esta sessão
     * Objetivo: Garantir que a sessão em uso continua valendo depois da troca de senha.
     * Decisões: A mesma credencial chama /conta depois da troca.
     */
    @Test
    void trocaSenhaMantemASessaoAtual() throws Exception {
        mvc.perform(put("/api/v1/conta/senha").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"senha-atual-longa\",\"novaSenha\":\"nova-senha-bem-longa\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/conta").header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
    }
}
