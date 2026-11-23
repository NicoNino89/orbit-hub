package br.com.orbithub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Teste de integração da aplicação completa
 * Objetivo: Subir a aplicação inteira com PostgreSQL real e conferir o que a S0 promete: saúde,
 *           métricas, logs, segurança do Actuator, Swagger e migrações.
 * Decisões: PostgreSQL via Testcontainers, na mesma versão major da Azure; {@code @AutoConfigureMetrics}
 *           porque os testes desligam a exportação de métricas por padrão.
 */
@SpringBootTest
@AutoConfigureMetrics
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AplicacaoIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    Flyway flyway;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Health check responde UP
     * Objetivo: Garantir que /actuator/health responda 200 com status UP.
     * Decisões: É a sonda usada pelo Docker Compose, pelo Container Apps e pelo smoke test.
     */
    @Test
    void healthCheckRespondeUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Sondas de liveness e readiness
     * Objetivo: Garantir que as duas sondas do Container Apps respondam.
     * Decisões: Sondas separadas: liveness reinicia o contêiner; readiness só tira do balanceamento.
     */
    @Test
    void sondasDeLivenessEReadinessRespondem() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Health check sem detalhes internos
     * Objetivo: Garantir que o health público não mostre componentes (banco, disco).
     * Decisões: Detalhes internos ajudariam um atacante a mapear a infraestrutura.
     */
    @Test
    void healthCheckNaoExpoeDetalhesInternos() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Métricas no formato Prometheus
     * Objetivo: Garantir que /actuator/prometheus publique as métricas da JVM.
     * Decisões: Confere uma métrica que sempre existe (memória da JVM), para o teste não depender de carga.
     */
    @Test
    void metricasNoFormatoPrometheus() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:06
     * Cartão Jira: KAN-56
     * Título: Histograma de tempo de resposta
     * Objetivo: Garantir que as requisições gerem o histograma usado no painel e no alerta de tempo de
     *           resposta.
     * Decisões: Confere o bucket de 200 ms (le="0.2"), que é a meta de resposta do painel do Grafana.
     */
    @Test
    void metricasDeRequisicaoTemHistogramaParaOPainelDeTempoDeResposta() throws Exception {
        mvc.perform(get("/api/v1/sobre")).andExpect(status().isOk());

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "http_server_requests_seconds_bucket{")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("uri=\"/api/v1/sobre\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("le=\"0.2\"")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:06
     * Cartão Jira: KAN-56
     * Título: Logs estruturados em JSON
     * Objetivo: Garantir que o console saia em JSON no formato ECS.
     * Decisões: ECS é lido direto pelo Log Analytics da Azure, sem parser próprio.
     */
    @Test
    void logsSaemEstruturadosEmJson(@Autowired Environment ambiente) {
        assertThat(ambiente.getProperty("logging.structured.format.console")).isEqualTo("ecs");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Actuator sem endpoints sensíveis
     * Objetivo: Garantir que /actuator/env e /actuator/beans não fiquem expostos.
     * Decisões: Esses endpoints revelam variáveis e configuração; só health e prometheus são publicados.
     */
    @Test
    void endpointsSensiveisDoActuatorNaoFicamExpostos() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Contrato OpenAPI publicado
     * Objetivo: Garantir que /v3/api-docs traga o título da API e o endpoint "sobre".
     * Decisões: O contrato é a referência para o front-end e para o teste de segurança (ZAP).
     */
    @Test
    void contratoOpenApiPublicado() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Orbit Hub API"))
                .andExpect(jsonPath("$.paths['/api/v1/sobre']").exists());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Migrações do Flyway aplicadas
     * Objetivo: Garantir que a base suba com todas as migrações aplicadas e nenhuma pendente.
     * Decisões: Esquema versionado só pelo Flyway; o Hibernate apenas valida, nunca altera o banco.
     */
    @Test
    void migracoesDoFlywayAplicadas() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
