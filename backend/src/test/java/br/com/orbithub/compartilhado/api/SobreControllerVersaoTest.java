package br.com.orbithub.compartilhado.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Na nuvem, a versão exibida é o commit publicado pelo pipeline. */
@WebMvcTest(SobreController.class)
@TestPropertySource(properties = {"orbit.ambiente=homologacao", "orbit.versao=3f9c2ab"})
class SobreControllerVersaoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void mostraOCommitPublicado() throws Exception {
        mvc.perform(get("/api/v1/sobre")).andExpect(jsonPath("$.versao").value("3f9c2ab"));
    }
}
