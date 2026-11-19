package br.com.orbithub.compartilhado.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:16
 * Cartão Jira: KAN-53
 * Título: Teste da versão publicada
 * Objetivo: Verificar que, na nuvem, a versão exibida é o commit publicado pelo pipeline.
 * Decisões: Classe separada porque a propriedade {@code orbit.versao} muda o contexto do Spring.
 */
@WebMvcTest(SobreController.class)
@TestPropertySource(properties = {"orbit.ambiente=homologacao", "orbit.versao=3f9c2ab"})
class SobreControllerVersaoTest {

    @Autowired
    MockMvc mvc;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Commit publicado na versão
     * Objetivo: Garantir que ORBIT_VERSAO substitua a versão do build.
     * Decisões: Usa um hash curto fictício, no mesmo formato que o pipeline envia.
     */
    @Test
    void mostraOCommitPublicado() throws Exception {
        mvc.perform(get("/api/v1/sobre")).andExpect(jsonPath("$.versao").value("3f9c2ab"));
    }
}
