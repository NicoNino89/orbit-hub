package br.com.orbithub;

import org.springframework.boot.SpringApplication;

/** Sobe a API localmente com um PostgreSQL em container, sem precisar do Docker Compose. */
public class TestOrbitHubApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrbitHubApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
