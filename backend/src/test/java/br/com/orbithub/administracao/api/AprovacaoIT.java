package br.com.orbithub.administracao.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.service.ExecutorDeAcaoGrave;
import br.com.orbithub.administracao.service.SegundaAprovacaoService;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Testes de integração da segunda aprovação
 * Objetivo: Conferir, pela API e com PostgreSQL real, que a ação só acontece quando outra pessoa aprova, que
 *           quem propõe não decide, que papel sem permissão não decide e que tudo fica na trilha.
 * Decisões: Executor de teste para transferência de titularidade, registrado só neste contexto (o real chega
 *           com o atendimento, S10). As propostas nascem pelo serviço, como fará o módulo dono da ação.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, AprovacaoIT.ExecutorDeTeste.class})
class AprovacaoIT {

    static final List<String> EXECUTADAS = new CopyOnWriteArrayList<>();

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    SegundaAprovacaoService aprovacoes;

    @Autowired
    RegistroAuditoriaRepository registros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Ciclo da proposta
     * Objetivo: Conferir proposta, recusa de autoaprovação e de papel errado, aprovação pela segunda pessoa com
     *           execução única e o registro na trilha.
     * Decisões: O suporte N2 vê a fila, mas não decide (o executor de teste exige administrador). O banimento ganhou
     *           executor na moderação (KAN-101); a de dados pessoais, na privacidade (KAN-90).
     */
    @Test
    void soASegundaPessoaComPapelCertoAprovaEExecuta() throws Exception {
        Usuario bia = membro("Bia Admin", Papel.ADMIN);
        Usuario caio = membro("Caio Admin", Papel.ADMIN);
        Usuario davi = membro("Davi Suporte", Papel.SUPORTE_N2);
        String objeto = UUID.randomUUID().toString();
        PropostaDeAprovacao proposta = aprovacoes.propor(AcaoGrave.TRANSFERENCIA_TITULARIDADE, "startup", objeto,
                "Transferir a NovaSafra para Ana", Map.of("novoTitular", "ana"), bia.getId(),
                "Fundador indisponível há 60 dias, documentos conferidos");
        assertThat(EXECUTADAS).doesNotContain(objeto);

        mvc.perform(get("/api/v1/admin/aprovacoes").header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propostas[?(@.id == '" + proposta.getId() + "')].minha").value(true))
                .andExpect(jsonPath("$.propostas[?(@.id == '" + proposta.getId() + "')].possoDecidir").value(false))
                .andExpect(jsonPath("$.propostas[?(@.id == '" + proposta.getId() + "')].propostoPor").value("Bia Admin"))
                .andExpect(jsonPath("$.acoes[?(@.acao == 'TRANSFERENCIA_TITULARIDADE')].disponivel").value(true))
                .andExpect(jsonPath("$.acoes[?(@.acao == 'BANIMENTO_DEFINITIVO')].disponivel").value(true))
                .andExpect(jsonPath("$.acoes[?(@.acao == 'DADOS_PESSOAIS_FORA_DO_FLUXO')].disponivel").value(true));

        mvc.perform(decisao(proposta, "aprovacao", bia, "Eu mesma aprovo"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("autoaprovacao"));
        mvc.perform(decisao(proposta, "aprovacao", davi, "Ok"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("papel_insuficiente"));
        assertThat(EXECUTADAS).doesNotContain(objeto);

        mvc.perform(decisao(proposta, "aprovacao", caio, "Documentos conferidos com o N2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("APROVADA"))
                .andExpect(jsonPath("$.decididoPor").value("Caio Admin"));
        assertThat(EXECUTADAS).containsOnlyOnce(objeto);
        mvc.perform(decisao(proposta, "rejeicao", caio, "Mudei de ideia"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("proposta_decidida"));

        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(caio.getId()))
                .filteredOn(r -> r.getAcao().equals("APROVACAO_APROVADA"))
                .extracting(RegistroAuditoria::getResultado, RegistroAuditoria::getJustificativa)
                .contains(tuple(RegistroAuditoria.Resultado.SUCESSO,
                        "Documentos conferidos com o N2"));
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(bia.getId()))
                .filteredOn(r -> r.getAcao().equals("APROVACAO_APROVADA"))
                .extracting(RegistroAuditoria::getResultado).containsOnly(RegistroAuditoria.Resultado.FALHA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Rejeição e cancelamento
     * Objetivo: Conferir que a rejeição não executa a ação, que só quem propôs cancela e que não há duas
     *           propostas pendentes iguais.
     * Decisões: Usuário comum (investidor) nem vê a fila.
     */
    @Test
    void rejeicaoCancelamentoEPropostaRepetida() throws Exception {
        Usuario bia = membro("Bia Admin", Papel.ADMIN);
        Usuario caio = membro("Caio Admin", Papel.ADMIN);
        String objeto = UUID.randomUUID().toString();
        PropostaDeAprovacao rejeitada = aprovacoes.propor(AcaoGrave.TRANSFERENCIA_TITULARIDADE, "startup", objeto,
                "Transferir", Map.of(), bia.getId(), "Pedido do atendimento");
        mvc.perform(decisao(rejeitada, "rejeicao", caio, ""))
                .andExpect(status().isBadRequest());
        mvc.perform(decisao(rejeitada, "rejeicao", caio, "Faltou a procuração"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("REJEITADA"))
                .andExpect(jsonPath("$.justificativaDecisao").value("Faltou a procuração"));
        assertThat(EXECUTADAS).doesNotContain(objeto);

        PropostaDeAprovacao outra = aprovacoes.propor(AcaoGrave.TRANSFERENCIA_TITULARIDADE, "startup", objeto,
                "Transferir de novo", Map.of(), bia.getId(), "Procuração chegou");
        assertThatThrownBy(() -> aprovacoes.propor(
                        AcaoGrave.TRANSFERENCIA_TITULARIDADE, "startup", objeto, "Duplicada", Map.of(), caio.getId(),
                        "Repetida"))
                .extracting("codigo").isEqualTo("proposta_pendente");
        mvc.perform(post("/api/v1/admin/aprovacoes/" + outra.getId() + "/cancelamento")
                        .header(HttpHeaders.AUTHORIZATION, bearer(caio)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("cancelamento_negado"));
        mvc.perform(post("/api/v1/admin/aprovacoes/" + outra.getId() + "/cancelamento")
                        .header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("CANCELADA"));

        Usuario investidor = Usuario.novo("Edu", "e-" + UUID.randomUUID() + "@exemplo.com", "{noop}x",
                Set.of(Papel.INVESTIDOR), Instant.now());
        investidor.confirmarEmail(Instant.now());
        usuarios.save(investidor);
        mvc.perform(get("/api/v1/admin/aprovacoes").header(HttpHeaders.AUTHORIZATION, bearer(investidor)))
                .andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Requisição de decisão
     * Objetivo: Montar o POST de aprovação ou rejeição com a justificativa.
     * Decisões: Justificativa em JSON simples.
     */
    private RequestBuilder decisao(PropostaDeAprovacao proposta, String tipo,
                                                                       Usuario quem, String justificativa) {
        return post("/api/v1/admin/aprovacoes/" + proposta.getId() + "/" + tipo)
                .header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"justificativa\":\"" + justificativa + "\"}");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Membro de teste
     * Objetivo: Criar um membro ativo da equipe com o papel informado.
     * Decisões: E-mail aleatório.
     */
    private Usuario membro(String nome, Papel papel) {
        Usuario membro = Usuario.novoInterno(nome, "m-" + UUID.randomUUID() + "@orbithub.com", "{noop}x", Set.of(papel),
                Instant.now());
        membro.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
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
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Executor de teste
     * Objetivo: Registrar um executor de transferência de titularidade que só anota o objeto executado.
     * Decisões: Aprovado só por administrador.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ExecutorDeTeste {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Bean do executor de teste
         * Objetivo: Criar o executor que anota as execuções.
         * Decisões: Classe anônima simples.
         */
        @Bean("executorDeTransferencia")
        ExecutorDeAcaoGrave transferenciaDeTeste() {
            return new ExecutorDeAcaoGrave() {
                @Override
                public AcaoGrave acao() {
                    return AcaoGrave.TRANSFERENCIA_TITULARIDADE;
                }

                @Override
                public Set<Papel> papeisQueAprovam() {
                    return Set.of(Papel.ADMIN);
                }

                @Override
                public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
                    EXECUTADAS.add(proposta.getObjetoId());
                }
            };
        }
    }
}
