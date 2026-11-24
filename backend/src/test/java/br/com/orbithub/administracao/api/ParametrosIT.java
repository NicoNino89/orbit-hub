package br.com.orbithub.administracao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
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
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Testes de integração dos parâmetros
 * Objetivo: Conferir, com PostgreSQL real, o catálogo da seção 5, a alteração com justificativa e histórico, a
 *           recusa fora dos limites e o parâmetro sensível que só muda depois da aprovação de outro administrador.
 * Decisões: Usa parâmetros que o código ainda não lê (catálogo, Nível 2), para não mudar o comportamento dos
 *           outros testes que compartilham o mesmo banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ParametrosIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    LeituraDeParametros parametros;

    @Autowired
    RegistroAuditoriaRepository registros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Catálogo e alteração direta
     * Objetivo: Conferir o catálogo, a alteração de um parâmetro comum (vale na hora, com histórico e trilha), a
     *           recusa fora dos limites e sem justificativa, e que só o administrador altera.
     * Decisões: catalogo.novas_dias (30, de 7 a 90).
     */
    @Test
    void alteraParametroComumComHistorico() throws Exception {
        Usuario bia = membro("Bia Admin", Papel.ADMIN);
        String bearer = bearer(bia);

        mvc.perform(get("/api/v1/admin/parametros").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(50)))
                .andExpect(jsonPath("$[?(@.chave == 'conexao.expiracao_dias')].valorPadrao").value("30"))
                .andExpect(jsonPath("$[?(@.chave == 'nivel2.validade_dias')].sensivel").value(true))
                .andExpect(jsonPath("$[?(@.chave == 'conexao.lembrete_dias')].tipo").value("LISTA_INTEIROS"));

        String chave = "catalogo.novas_dias";
        long antes = parametros.inteiro(chave);
        String novo = String.valueOf(antes == 45 ? 46 : 45);
        mvc.perform(alterar(chave, "200", "Teste de limite", bearer))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("valor_invalido"))
                .andExpect(jsonPath("$.detail").value("Valor fora dos limites aceitos: de 7 a 90 dias."));
        mvc.perform(put("/api/v1/admin/parametros/" + chave).header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":\"" + novo + "\",\"justificativa\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(alterar(chave, novo, "Curadoria pediu vitrine mais longa", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicado").value(true))
                .andExpect(jsonPath("$.parametro.valor").value(novo));
        assertThat(parametros.inteiro(chave)).isEqualTo(Long.parseLong(novo));

        mvc.perform(get("/api/v1/admin/parametros/" + chave + "/historico").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorAnterior").value(String.valueOf(antes)))
                .andExpect(jsonPath("$[0].valorNovo").value(novo))
                .andExpect(jsonPath("$[0].autor").value("Bia Admin"))
                .andExpect(jsonPath("$[0].aprovadoPor").doesNotExist())
                .andExpect(jsonPath("$[0].justificativa").value("Curadoria pediu vitrine mais longa"));
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(bia.getId()))
                .filteredOn(r -> r.getAcao().equals("PARAMETRO_ALTERADO") && r.getResultado() == RegistroAuditoria.Resultado.SUCESSO)
                .singleElement()
                .satisfies(r -> assertThat(r.getDetalhe()).isEqualTo(antes + " → " + novo));

        Usuario curador = membro("Cris Curadora", Papel.CURADOR);
        mvc.perform(alterar(chave, "60", "Tentativa", bearer(curador))).andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Parâmetro sensível com segunda aprovação
     * Objetivo: Conferir que o sensível não muda na alteração, vira proposta, não é aprovado por quem propôs e muda
     *           quando outro administrador aprova, com o aprovador no histórico.
     * Decisões: nivel2.validade_dias (90, sensível).
     */
    @Test
    void parametroSensivelSoMudaDepoisDaSegundaAprovacao() throws Exception {
        Usuario bia = membro("Bia Admin", Papel.ADMIN);
        Usuario caio = membro("Caio Admin", Papel.ADMIN);
        String chave = "nivel2.validade_dias";
        long antes = parametros.inteiro(chave);
        String novo = String.valueOf(antes == 60 ? 61 : 60);

        String resposta = mvc.perform(alterar(chave, novo, "Jurídico pediu prazo menor de acesso", bearer(bia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicado").value(false))
                .andExpect(jsonPath("$.propostaId").isNotEmpty())
                .andExpect(jsonPath("$.parametro.valor").value(String.valueOf(antes)))
                .andReturn().getResponse().getContentAsString();
        String proposta = JsonPath.read(resposta, "$.propostaId");
        assertThat(parametros.inteiro(chave)).isEqualTo(antes);
        mvc.perform(alterar(chave, "30", "Outra", bearer(caio)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("proposta_pendente"));

        mvc.perform(post("/api/v1/admin/aprovacoes/" + proposta + "/aprovacao").header(HttpHeaders.AUTHORIZATION, bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Eu mesma\"}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(parametros.inteiro(chave)).isEqualTo(antes);

        mvc.perform(post("/api/v1/admin/aprovacoes/" + proposta + "/aprovacao").header(HttpHeaders.AUTHORIZATION, bearer(caio))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Conferido com o jurídico\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("APROVADA"));
        assertThat(parametros.inteiro(chave)).isEqualTo(Long.parseLong(novo));
        assertThat(parametros.duracao(chave)).isEqualTo(Duration.ofDays(Long.parseLong(novo)));

        mvc.perform(get("/api/v1/admin/parametros/" + chave + "/historico").header(HttpHeaders.AUTHORIZATION, bearer(caio)))
                .andExpect(jsonPath("$[0].valorNovo").value(novo))
                .andExpect(jsonPath("$[0].autor").value("Bia Admin"))
                .andExpect(jsonPath("$[0].aprovadoPor").value("Caio Admin"))
                .andExpect(jsonPath("$[0].justificativa").value("Jurídico pediu prazo menor de acesso"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Requisição de alteração
     * Objetivo: Montar o PUT de alteração com valor e justificativa.
     * Decisões: JSON simples.
     */
    private RequestBuilder alterar(String chave, String valor, String justificativa,
                                                                       String bearer) {
        return put("/api/v1/admin/parametros/" + chave).header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\":\"" + valor + "\",\"justificativa\":\"" + justificativa + "\"}");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Membro de teste
     * Objetivo: Criar um membro ativo da equipe com o papel informado.
     * Decisões: E-mail aleatório.
     */
    private Usuario membro(String nome, Papel papel) {
        Usuario membro = Usuario.novoInterno(nome, "p-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel),
                Instant.now());
        membro.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }
}
