# ADR 0001 — Spring Boot 4.1 no lugar de 3.x

- **Data:** 18/11/2026
- **Situação:** aceita
- **História:** KAN-52

## Contexto

A arquitetura previa Spring Boot 3.x. O suporte gratuito da última linha 3.x (3.5) terminou em junho de 2026, e o Spring Initializr não oferece mais nenhuma versão 3.x. Começar um projeto novo numa versão sem correções de segurança contraria os portões de qualidade do pipeline ("nenhuma vulnerabilidade crítica nas dependências").

## Decisão

Usar **Spring Boot 4.1** (Spring Framework 7) com **Java 21 LTS**. O restante da stack não muda: Spring Data JPA, Flyway, Spring Security, springdoc-openapi (linha 3.x, compatível com o Boot 4), Testcontainers e JaCoCo.

## Consequências

- O projeto nasce numa versão com suporte e correções de segurança.
- Exemplos de internet escritos para o Boot 3 podem precisar de ajuste, principalmente nos pacotes de teste (`org.springframework.boot.webmvc.test...`) e no Jackson 3.
- Java 21 continua atendendo ao requisito mínimo do Boot 4 (Java 17).
