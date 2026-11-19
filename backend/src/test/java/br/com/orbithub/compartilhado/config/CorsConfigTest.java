package br.com.orbithub.compartilhado.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(CorsConfig.class)
@TestPropertySource(properties = {
        "orbit.ambiente=homologacao",
        "orbit.cors.origens=https://web.orbit.exemplo, https://outro.orbit.exemplo"
})
class CorsConfigTest {

    @Autowired
    MockMvc mvc;

    @Test
    void permiteOFrontEndConfigurado() throws Exception {
        mvc.perform(get("/api/v1/sobre").header("Origin", "https://web.orbit.exemplo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://web.orbit.exemplo"));
    }

    @Test
    void aceitaVariasOrigensSeparadasPorVirgula() throws Exception {
        mvc.perform(options("/api/v1/sobre")
                        .header("Origin", "https://outro.orbit.exemplo")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://outro.orbit.exemplo"));
    }

    @Test
    void recusaOrigemDesconhecida() throws Exception {
        mvc.perform(get("/api/v1/sobre").header("Origin", "https://site-malicioso.exemplo"))
                .andExpect(status().isForbidden());
    }
}
