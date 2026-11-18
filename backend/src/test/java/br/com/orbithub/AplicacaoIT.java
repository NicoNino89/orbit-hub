package br.com.orbithub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Sobe a aplicação inteira com PostgreSQL real e confere o que a S0 promete: saúde, métricas e Swagger. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AplicacaoIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    Flyway flyway;

    @Test
    void healthCheckRespondeUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void sondasDeLivenessEReadinessRespondem() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void healthCheckNaoExpoeDetalhesInternos() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void metricasNoFormatoPrometheus() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    @Test
    void endpointsSensiveisDoActuatorNaoFicamExpostos() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    @Test
    void contratoOpenApiPublicado() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Orbit Hub API"))
                .andExpect(jsonPath("$.paths['/api/v1/sobre']").exists());
    }

    @Test
    void migracoesDoFlywayAplicadas() {
        assertThat(flyway.info().applied()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
