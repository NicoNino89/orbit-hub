package br.com.orbithub.compartilhado.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Informa qual versão da API está no ar e em qual ambiente. Útil no smoke test e no teste humano. */
@RestController
@RequestMapping("/api/v1/sobre")
@Tag(name = "Plataforma", description = "Informações gerais da API")
class SobreController {

    private final String versao;
    private final String ambiente;

    SobreController(ObjectProvider<BuildProperties> build,
                    @Value("${orbit.ambiente}") String ambiente,
                    @Value("${orbit.versao:}") String versaoPublicada) {
        BuildProperties props = build.getIfAvailable();
        String versaoDoBuild = props != null ? props.getVersion() : "desenvolvimento";
        // Na nuvem, o pipeline informa o commit publicado (ORBIT_VERSAO); assim o teste humano sabe o que está testando
        this.versao = versaoPublicada.isBlank() ? versaoDoBuild : versaoPublicada;
        this.ambiente = ambiente;
    }

    @GetMapping
    @Operation(summary = "Versão e ambiente da API")
    SobreResponse sobre() {
        return new SobreResponse("Orbit Hub API", versao, ambiente);
    }

    record SobreResponse(String nome, String versao, String ambiente) {
    }
}
