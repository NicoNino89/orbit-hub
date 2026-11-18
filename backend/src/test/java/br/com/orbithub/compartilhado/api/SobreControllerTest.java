package br.com.orbithub.compartilhado.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SobreController.class)
@TestPropertySource(properties = "orbit.ambiente=homologacao")
class SobreControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void informaNomeVersaoEAmbiente() throws Exception {
        mvc.perform(get("/api/v1/sobre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Orbit Hub API"))
                .andExpect(jsonPath("$.versao").isNotEmpty())
                .andExpect(jsonPath("$.ambiente").value("homologacao"));
    }
}
