package br.com.orbithub.compartilhado.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI orbitHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Orbit Hub API")
                .version("v1")
                .description("""
                        API REST do Orbit Hub. A plataforma conecta investidores, empreendedores, \
                        prestadores e instituições. Ela não intermedeia oferta de valores mobiliários \
                        nem faz recomendação de investimento."""));
    }
}
