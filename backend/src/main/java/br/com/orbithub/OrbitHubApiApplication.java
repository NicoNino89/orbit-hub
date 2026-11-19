package br.com.orbithub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Aplicação Spring Boot da API do Orbit Hub
 * Objetivo: Ponto de entrada do monólito modular; liga a autoconfiguração e o registro das propriedades.
 * Decisões: {@code @ConfigurationPropertiesScan} registra todos os records de configuração sem
 *           precisar listar cada um, para que novos módulos tragam seus parâmetros sem tocar aqui.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OrbitHubApiApplication {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Inicialização da API
     * Objetivo: Subir o contexto Spring e o servidor HTTP.
     * Decisões: Nenhuma lógica além do bootstrap padrão; configuração fica nos perfis e variáveis de ambiente.
     */
    public static void main(String[] args) {
        SpringApplication.run(OrbitHubApiApplication.class, args);
    }
}
