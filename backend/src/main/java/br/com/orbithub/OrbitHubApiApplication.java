package br.com.orbithub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OrbitHubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrbitHubApiApplication.class, args);
    }
}
