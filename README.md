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
│  React + TypeScript      │                     │  Java 21 + Spring Boot 4     │
│  Azure Container Apps    │ ◀────────────────── │  Azure Container Apps        │
└──────────────────────────┘                     └──────────────┬───────────────┘
                                                                │
                                         ┌──────────────────────┴──────────────┐
                                         │  PostgreSQL          Blob Storage   │
                                         │  (dados)             (arquivos)     │
                                         └─────────────────────────────────────┘
```

Detalhes em [`docs/arquitetura.md`](docs/arquitetura.md).

Complementos aos requisitos do produto ficam em [`docs/requisitos/`](docs/requisitos/), como [tipos de investidor, rodadas e regras de negócio revisadas](docs/requisitos/tipos-de-investidor-e-rodadas.md), [processos, rotinas e operação da plataforma](docs/requisitos/processos-e-operacao.md) e [serviços, base de conhecimento e comunidade](docs/requisitos/servicos-e-conhecimento.md).

A ordem de execução, as sprints e o fluxo de desenvolvimento e teste estão em [`docs/planejamento/revisao-e-sprints.md`](docs/planejamento/revisao-e-sprints.md).

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Front-end | React, TypeScript, Vite, Tailwind CSS |
| Catálogo digital | react-pageflip (em avaliação — KAN-58) |
| Back-end | Java 21 LTS, Spring Boot 4.1 |
| Persistência | Spring Data JPA / Hibernate, PostgreSQL, Flyway |
| Segurança | Spring Security, JWT com refresh token revogável |
| API e documentação | REST, Swagger / OpenAPI |
| Build | Maven (back-end), Vite (front-end) |
| Testes | JUnit 5, Mockito, Testcontainers, Vitest, React Testing Library |
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

### Pré-requisitos

- Docker Desktop (com WSL2 no Windows)
- Java 21 e Maven, para desenvolver o back-end fora do container (o projeto traz o `mvnw`)
- Node.js LTS, para desenvolver o front-end fora do container
- Terraform e Azure CLI, para trabalhar na infraestrutura

### Tudo em containers

```bash
cp .env.example .env        # troque as senhas
docker compose up --build
```

| Serviço | Endereço |
|---|---|
| Front-end | http://localhost:5173 |
| API (Swagger) | http://localhost:8080/swagger-ui.html |
| Saúde da API | http://localhost:8080/actuator/health |
| Grafana | http://localhost:3000 (usuário `admin`, senha `GRAFANA_SENHA` do `.env`) |
| Prometheus | http://localhost:9090 |

As portas ficam presas a `127.0.0.1`, então nada fica exposto na rede.

### Desenvolvimento sem containers da aplicação

```bash
# Back-end, com um PostgreSQL em container criado pelo Testcontainers
cd backend
./mvnw spring-boot:test-run

# Front-end, com recarga automática; /api vai para http://localhost:8080
cd frontend
npm install
npm run dev
```

### Testes

```bash
cd backend && ./mvnw verify       # unidade, integração (Testcontainers) e portão de cobertura JaCoCo
cd frontend && npm run lint && npm run typecheck && npm test
```

## Ambientes na Azure

| Ambiente | Como é publicado | Dados |
|---|---|---|
| **Homologação** | Manualmente, quando decidimos publicar: aba *Actions* → *Entrega em homologação* → *Run workflow*. Termina com um smoke test. Volta a ser automática (a cada merge na `main`, depois do CI verde) no fim do projeto, ver [ADR 0008](docs/adr/0008-homologacao-manual.md) | Só fictícios |
| **Produção** | Versões numeradas da `main`, com aprovação manual: aba *Actions* → *Publicação em produção*. Smoke test, volta automática se falhar, tag e release no GitHub; rollback pelo workflow *Rollback em produção*. Ver [`docs/operacao/producao.md`](docs/operacao/producao.md) e [ADR 0009](docs/adr/0009-producao-versionada-com-revisoes.md) | Reais |

Os endereços aparecem no resumo de cada execução do workflow e na aba *Environments* do GitHub. A API de homologação escala a zero: a primeira chamada depois de um tempo parada leva cerca de 1 minuto. A infraestrutura está em [`infra/README.md`](infra/README.md).

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
