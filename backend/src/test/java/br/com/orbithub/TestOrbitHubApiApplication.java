package br.com.orbithub;

import org.springframework.boot.SpringApplication;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: API local com banco em contêiner
 * Objetivo: Subir a API localmente com um PostgreSQL em contêiner, sem precisar do Docker Compose.
 * Decisões: Reaproveita a mesma configuração do Testcontainers dos testes de integração.
 */
public class TestOrbitHubApiApplication {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Inicialização da API de desenvolvimento
     * Objetivo: Rodar o {@code main} da aplicação com o PostgreSQL do Testcontainers.
     * Decisões: {@code SpringApplication.from} evita duplicar a configuração da aplicação real.
     */
    public static void main(String[] args) {
        SpringApplication.from(OrbitHubApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
