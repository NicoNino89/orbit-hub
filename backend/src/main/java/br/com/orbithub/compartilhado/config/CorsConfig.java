package br.com.orbithub.compartilhado.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Na Azure, o front-end (Static Web Apps) e a API (Container Apps) ficam em domínios diferentes.
 * Só as origens configuradas em {@code orbit.cors.origens} podem chamar a API pelo navegador.
 */
@Configuration(proxyBeanMethods = false)
class CorsConfig implements WebMvcConfigurer {

    private final String[] origens;

    CorsConfig(@Value("${orbit.cors.origens}") String origens) {
        this.origens = Arrays.stream(origens.split(","))
                .map(String::strip)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origens)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
