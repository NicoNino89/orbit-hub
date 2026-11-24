package br.com.orbithub.identidade.api;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Teste de integração da recuperação de senha
 * Objetivo: Verificar, com API e banco reais, o pedido, a troca da senha, a queda das sessões e o login com a
 *           senha nova.
 * Decisões: Envio de e-mail simulado para o teste ler o link.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RecuperacaoSenhaIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    PasswordEncoder senhas;

    @Autowired
    SessaoService sessoes;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Recuperação completa
     * Objetivo: Garantir que a senha é trocada pelo link, as sessões abertas caem e a senha nova entra.
     * Decisões: Abre uma sessão antes, para conferir que ela é encerrada.
     */
    @Test
    void recuperaASenhaPeloLink() throws Exception {
        String emailConta = "r-" + UUID.randomUUID() + "@exemplo.com";
        Usuario usuario = Usuario.novo("Ana", emailConta, senhas.encode("senha-antiga-longa"), Set.of(Papel.INVESTIDOR),
                Instant.now());
        usuario.confirmarEmail(Instant.now());
        usuarios.save(usuario);
        var sessaoAberta = sessoes.abrir(usuario);

        mvc.perform(post("/api/v1/contas/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailConta + "\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getValue().texto();
        String token = texto.substring(texto.indexOf("token=") + 6, texto.indexOf('\n', texto.indexOf("token=")));

        mvc.perform(post("/api/v1/contas/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"senha\":\"senha-nova-bem-longa\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/renovar").cookie(new Cookie(CookieDeSessao.NOME, sessaoAberta.refreshToken())))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailConta + "\",\"senha\":\"senha-antiga-longa\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailConta + "\",\"senha\":\"senha-nova-bem-longa\"}"))
                .andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Resposta igual e link inválido
     * Objetivo: Garantir 202 para e-mail inexistente e 422 para token inventado.
     * Decisões: Nenhuma conta envolvida.
     */
    @Test
    void respostaIgualELinkInvalido() throws Exception {
        mvc.perform(post("/api/v1/contas/senha/esqueci").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem-" + UUID.randomUUID() + "@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/contas/senha/redefinir").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"inventado\",\"senha\":\"senha-nova-bem-longa\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("link_invalido"));
    }
}
