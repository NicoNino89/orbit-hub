package br.com.orbithub.compartilhado.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Endpoint "sobre" da API
 * Objetivo: Informar qual versão da API está no ar e em qual ambiente, para o smoke test e o teste humano.
 * Decisões: Endpoint público e sem dados sensíveis (só nome, versão e ambiente).
 */
@RestController
@RequestMapping("/api/v1/sobre")
@Tag(name = "Plataforma", description = "Informações gerais da API")
class SobreController {

    private final String versao;
    private final String ambiente;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Resolução da versão publicada
     * Objetivo: Definir a versão exibida: o commit publicado na nuvem ou, na falta dele, a versão do build.
     * Decisões: Na nuvem o pipeline informa o commit em ORBIT_VERSAO, para o testador saber exatamente o que
     *           está testando; sem build-info (rodando pela IDE) a versão aparece como "desenvolvimento".
     */
    SobreController(ObjectProvider<BuildProperties> build,
                    @Value("${orbit.ambiente}") String ambiente,
                    @Value("${orbit.versao:}") String versaoPublicada) {
        BuildProperties props = build.getIfAvailable();
        String versaoDoBuild = props != null ? props.getVersion() : "desenvolvimento";
        this.versao = versaoPublicada.isBlank() ? versaoDoBuild : versaoPublicada;
        this.ambiente = ambiente;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Consulta de versão e ambiente
     * Objetivo: Responder GET /api/v1/sobre com nome, versão e ambiente da API.
     * Decisões: Valores calculados uma vez no construtor; a resposta é imutável e barata.
     */
    @GetMapping
    @Operation(summary = "Versão e ambiente da API")
    SobreResponse sobre() {
        return new SobreResponse("Orbit Hub API", versao, ambiente);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Resposta do endpoint "sobre"
     * Objetivo: Contrato JSON do endpoint.
     * Decisões: Record imutável, publicado no contrato OpenAPI.
     */
    record SobreResponse(String nome, String versao, String ambiente) {
    }
}
