package br.com.orbithub.compartilhado.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Teste do endpoint "sobre"
 * Objetivo: Verificar a resposta do endpoint sem subir a aplicação inteira.
 * Decisões: {@code @WebMvcTest} só da camada web, para o teste rodar em segundos.
 */
@WebMvcTest(SobreController.class)
@TestPropertySource(properties = "orbit.ambiente=homologacao")
class SobreControllerTest {

    @Autowired
    MockMvc mvc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Nome, versão e ambiente informados
     * Objetivo: Garantir que a resposta traga os três campos preenchidos.
     * Decisões: A versão só é conferida como não vazia, porque muda a cada build.
     */
    @Test
    void informaNomeVersaoEAmbiente() throws Exception {
        mvc.perform(get("/api/v1/sobre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Orbit Hub API"))
                .andExpect(jsonPath("$.versao").isNotEmpty())
                .andExpect(jsonPath("$.ambiente").value("homologacao"));
    }
}
