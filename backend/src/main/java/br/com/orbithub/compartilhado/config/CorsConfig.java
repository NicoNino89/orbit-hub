package br.com.orbithub.compartilhado.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:16
 * Cartão Jira: KAN-53
 * Título: Política de CORS da API
 * Objetivo: Permitir que o front-end, em outro domínio na Azure (Static Web Apps), chame a API
 *           (Container Apps) pelo navegador.
 * Decisões: Só as origens de {@code orbit.cors.origens} são aceitas; nada de curinga "*", porque a
 *           API usa credenciais (cookie de refresh).
 */
@Configuration(proxyBeanMethods = false)
class CorsConfig implements WebMvcConfigurer {

    private final String[] origens;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Leitura das origens permitidas
     * Objetivo: Converter a lista separada por vírgulas da configuração em origens válidas.
     * Decisões: Remove espaços e itens vazios, para uma vírgula sobrando na variável de ambiente não
     *           liberar uma origem vazia.
     */
    CorsConfig(@Value("${orbit.cors.origens}") String origens) {
        this.origens = Arrays.stream(origens.split(","))
                .map(String::strip)
                .filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 21:16
     * Cartão Jira: KAN-53
     * Título: Registro do CORS nas rotas da API
     * Objetivo: Aplicar a política às rotas /api/**.
     * Decisões: Métodos e cabeçalhos listados um a um (Authorization e Content-Type); credenciais
     *           ligadas para o cookie HttpOnly; preflight em cache por 1 hora para reduzir requisições.
     */
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
