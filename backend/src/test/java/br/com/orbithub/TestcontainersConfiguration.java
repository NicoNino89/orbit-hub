package br.com.orbithub;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: PostgreSQL dos testes de integração
 * Objetivo: Fornecer um PostgreSQL real aos testes de integração.
 * Decisões: Mesma versão major usada na Azure (17), para o teste pegar diferenças de comportamento
 *           que um banco em memória esconderia.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final DockerImageName POSTGRES = DockerImageName.parse("postgres:17-alpine");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Contêiner do PostgreSQL
     * Objetivo: Criar o contêiner e ligar a conexão da aplicação a ele.
     * Decisões: {@code @ServiceConnection} preenche URL, usuário e senha sozinho, sem propriedades no teste.
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES);
    }
}
