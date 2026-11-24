package br.com.orbithub.identidade.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Teste de integração do cadastro
 * Objetivo: Verificar, com API e banco reais, o cadastro com os dois papéis, os aceites, a confirmação do
 *           e-mail e as recusas do formulário.
 * Decisões: O envio de e-mail é simulado (MockitoBean) para o teste ler o link enviado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CadastroIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Cadastro completo com confirmação
     * Objetivo: Garantir que a conta nasce pendente com os dois papéis e é ativada pelo link do e-mail.
     * Decisões: Aceita todos os documentos que exigem aceite, inclusive a confidencialidade (investidor).
     */
    @Test
    void cadastroComOsDoisPapeisEConfirmacao() throws Exception {
        String emailConta = "ana-" + UUID.randomUUID() + "@exemplo.com";
        String resposta = mvc.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana Souza", emailConta, "uma-senha-bem-longa", "\"INVESTIDOR\",\"EMPREENDEDOR\"",
                                aceitaveis())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE_CONFIRMACAO"))
                .andExpect(jsonPath("$.confirmacaoPendente").value(true))
                .andExpect(jsonPath("$.papeis.length()").value(2))
                .andExpect(jsonPath("$.proximosPassos.length()").value(2))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(resposta, "$.id"));

        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getValue().texto();
        String token = texto.substring(texto.indexOf("token=") + 6, texto.indexOf('\n', texto.indexOf("token=")));

        mvc.perform(post("/api/v1/contas/confirmacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNoContent());
        assertThat(usuarios.findById(id)).get().extracting(u -> u.getStatus()).isEqualTo(StatusUsuario.ATIVO);

        mvc.perform(post("/api/v1/contas/confirmacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("link_invalido"));

        mvc.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Ana", emailConta.toUpperCase(), "outra-senha-longa", "\"EMPREENDEDOR\"", aceitaveis())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("email_em_uso"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Recusas do formulário
     * Objetivo: Garantir 400 com os campos inválidos e 422 para senha comum e aceite faltando.
     * Decisões: Investidor sem a confidencialidade é recusado.
     */
    @Test
    void recusasDoFormulario() throws Exception {
        mvc.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"curta\",\"papeis\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").value("E-mail inválido."))
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.papeis").exists());

        mvc.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Bia", "bia-" + UUID.randomUUID() + "@exemplo.com", "1234567890",
                                "\"EMPREENDEDOR\"", aceitaveis())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("senha_fraca"));

        mvc.perform(post("/api/v1/contas").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Bia", "bia-" + UUID.randomUUID() + "@exemplo.com", "uma-senha-bem-longa",
                                "\"INVESTIDOR\"", basicos())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("aceite_obrigatorio"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Reenvio não revela contas
     * Objetivo: Garantir 202 para qualquer e-mail no reenvio.
     * Decisões: E-mail inexistente.
     */
    @Test
    void reenvioRespondeIgualParaQualquerEmail() throws Exception {
        mvc.perform(post("/api/v1/contas/confirmacao/reenvio").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem-" + UUID.randomUUID() + "@exemplo.com\"}"))
                .andExpect(status().isAccepted());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Ids aceitáveis
     * Objetivo: Ler os ids de todos os documentos vigentes que exigem aceite.
     * Decisões: Pela própria API pública.
     */
    private String aceitaveis() throws Exception {
        String vigentes = mvc.perform(get("/api/v1/documentos-legais")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(vigentes, "$[?(@.aceite != 'NENHUM')].id");
        return "\"" + String.join("\",\"", ids) + "\"";
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Ids básicos
     * Objetivo: Ler só os ids de Termos de Uso e Política de Privacidade.
     * Decisões: Sem a confidencialidade.
     */
    private String basicos() throws Exception {
        String vigentes = mvc.perform(get("/api/v1/documentos-legais")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(vigentes, "$[?(@.aceite == 'TODOS_NO_ACESSO')].id");
        return "\"" + String.join("\",\"", ids) + "\"";
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Corpo do cadastro
     * Objetivo: Montar o JSON do pedido de cadastro.
     * Decisões: Papéis e aceites já vêm formatados como lista JSON.
     */
    private static String corpo(String nome, String email, String senha, String papeis, String aceites) {
        return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\",\"papeis\":["
                + papeis + "],\"aceites\":[" + aceites + "]}";
    }
}
