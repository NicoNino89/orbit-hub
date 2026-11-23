package br.com.orbithub.compartilhado.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Configuração do contrato OpenAPI
 * Objetivo: Publicar a documentação da API (Swagger UI e /v3/api-docs).
 * Decisões: Contrato gerado a partir do código (springdoc), para nunca divergir da implementação.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Metadados do contrato OpenAPI
     * Objetivo: Definir título, versão e descrição exibidos na documentação.
     * Decisões: A descrição deixa explícito que a plataforma só conecta as partes: não intermedeia oferta
     *           de valores mobiliários nem recomenda investimento (Resolução CVM 88).
     */
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
