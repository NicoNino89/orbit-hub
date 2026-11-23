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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:16
 * Cartão Jira: KAN-53
 * Título: Teste da política de CORS
 * Objetivo: Verificar que só as origens configuradas chamam a API pelo navegador.
 * Decisões: Duas origens na configuração, com espaço depois da vírgula, para cobrir a leitura da lista.
 */
@WebMvcTest
@Import(CorsConfig.class)
@TestPropertySource(properties = {
        "orbit.ambiente=homologacao",
        "orbit.cors.origens=https://web.orbit.exemplo, https://outro.orbit.exemplo"
})
class CorsConfigTest {

    @Autowired
    MockMvc mvc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Front-end configurado é aceito
     * Objetivo: Garantir que a origem configurada receba o cabeçalho Access-Control-Allow-Origin.
     * Decisões: Requisição simples (GET), o caso mais comum do front-end.
     */
    @Test
    void permiteOFrontEndConfigurado() throws Exception {
        mvc.perform(get("/api/v1/sobre").header("Origin", "https://web.orbit.exemplo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://web.orbit.exemplo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Várias origens separadas por vírgula
     * Objetivo: Garantir que a segunda origem da lista também seja aceita no preflight.
     * Decisões: Usa OPTIONS, a requisição de preflight que o navegador manda antes de chamadas com
     *           credenciais.
     */
    @Test
    void aceitaVariasOrigensSeparadasPorVirgula() throws Exception {
        mvc.perform(options("/api/v1/sobre")
                        .header("Origin", "https://outro.orbit.exemplo")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://outro.orbit.exemplo"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Origem desconhecida é recusada
     * Objetivo: Garantir que uma origem fora da lista receba 403.
     * Decisões: Recusa explícita, em vez de só omitir o cabeçalho, para o erro aparecer no teste.
     */
    @Test
    void recusaOrigemDesconhecida() throws Exception {
        mvc.perform(get("/api/v1/sobre").header("Origin", "https://site-malicioso.exemplo"))
                .andExpect(status().isForbidden());
    }
}
