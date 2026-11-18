# Orbit Hub

> Um ecossistema que conecta capital, ideias, conhecimento e oportunidades.

O **Orbit Hub** é uma plataforma digital que conecta empreendedores que buscam investimento a investidores interessados em oportunidades de negócio. A plataforma reúne descoberta de oportunidades, matchmaking, serviços especializados, capacitação e networking em um único ecossistema.

Projeto desenvolvido para a disciplina de **DevOps** do curso de Engenharia de Software.

- **Gestão do projeto:** [Jira — Orbit Hub](https://orbithub.atlassian.net/jira/software/projects/KAN)
- **Status:** em estruturação (Fase 1 — MVP)

---

## Escopo do MVP (Fase 1)

| Épico | Descrição |
|---|---|
| KAN-4 | Cadastro, autenticação e perfis de usuário |
| KAN-5 | Perfil de investimento (tese do investidor) |
| KAN-6 | Cadastro estruturado de startups |
| KAN-7 | Confidencialidade e controle de acesso às informações |
| KAN-8 | Catálogo de oportunidades (Descobrir) |
| KAN-9 | Favoritos e acompanhamento de oportunidades |
| KAN-10 | Manifestação de interesse e Match |
| KAN-11 | Fundação DevOps: repositório, CI/CD e ambientes |

## Arquitetura

Arquitetura **cliente-servidor**, com API REST. O back-end é um **monólito modular** organizado em camadas (Controller → Service → Repository).

```
┌──────────────────────────┐      REST/JSON      ┌──────────────────────────────┐
│  Front-end (SPA)         │ ──────────────────▶ │  Back-end (API REST)         │
│  React + TypeScript      │                     │  Java 21 + Spring Boot 3     │
│  Azure Static Web Apps   │ ◀────────────────── │  Azure Container Apps        │
└──────────────────────────┘                     └──────────────┬───────────────┘
                                                                │
                                         ┌──────────────────────┴──────────────┐
                                         │  PostgreSQL          Blob Storage   │
                                         │  (dados)             (arquivos)     │
                                         └─────────────────────────────────────┘
```

Detalhes em [`docs/arquitetura.md`](docs/arquitetura.md).

Complementos aos requisitos do produto ficam em [`docs/requisitos/`](docs/requisitos/), como [tipos de investidor, rodadas e regras de negócio revisadas](docs/requisitos/tipos-de-investidor-e-rodadas.md) e [processos, rotinas e operação da plataforma](docs/requisitos/processos-e-operacao.md).

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Front-end | React, TypeScript, Vite, Tailwind CSS |
| Catálogo digital | react-pageflip (em avaliação — KAN-58) |
| Back-end | Java 21 LTS, Spring Boot 3.x |
| Persistência | Spring Data JPA / Hibernate, PostgreSQL, Flyway |
| Segurança | Spring Security, JWT com refresh token revogável |
| API e documentação | REST, Swagger / OpenAPI |
| Build | Maven (back-end), Vite (front-end) |
| Testes | JUnit 5, Mockito, Testcontainers |
| Qualidade | JaCoCo, SonarCloud |
| Containers | Docker, Docker Compose, GitHub Container Registry |
| CI/CD | GitHub Actions |
| Nuvem | Microsoft Azure (Azure for Students) |
| Infraestrutura como código | Terraform (provider `azurerm`) |
| Observabilidade | Spring Boot Actuator, Application Insights; Prometheus e Grafana no ambiente local |

## Estrutura do repositório

```
orbit-hub/
├── backend/        # API REST — Java 21 + Spring Boot (Maven)
├── frontend/       # SPA — React + TypeScript + Vite
├── infra/          # Infraestrutura como código — Terraform (Azure)
├── docs/           # Documentação de arquitetura e decisões
└── .github/        # Templates de PR e workflows do GitHub Actions
```

## Como rodar localmente

> O código da aplicação ainda será criado. Esta seção será completada junto com as histórias KAN-52 (containerização) e seguintes.

Pré-requisitos previstos:

- Docker e Docker Compose
- Java 21 e Maven (para desenvolver o back-end fora do container)
- Node.js LTS (para desenvolver o front-end fora do container)
- Terraform (para trabalhar na infraestrutura)

Execução prevista:

```bash
docker compose up
```

## Como contribuir

Leia o [`CONTRIBUTING.md`](CONTRIBUTING.md). Em resumo:

- Fluxo **GitHub Flow**: branches curtas a partir da `main`, integradas via pull request.
- Commits no padrão **Conventional Commits**, citando a issue do Jira (por exemplo, `feat(catalogo): navegação horizontal KAN-35`).
- Todo pull request precisa de revisão e do pipeline aprovado.

## Princípios de desenvolvimento

- SOLID e separação de responsabilidades
- Arquitetura em camadas e módulos com fronteiras claras
- Padrões GoF: Factory Method, Adapter e Strategy
- Testes automatizados como parte do pipeline
- Segurança e proteção de dados desde a concepção (LGPD)
