# Arquitetura do Orbit Hub

## Visão geral

- **Estilo:** cliente-servidor, com API REST.
- **Back-end:** monólito modular em Java 21 + Spring Boot 4.1, organizado em camadas.
- **Front-end:** SPA em React + TypeScript + Vite.
- **Dados:** PostgreSQL (migrações com Flyway) e armazenamento de arquivos em Blob Storage.
- **Nuvem:** Microsoft Azure, com infraestrutura declarada em Terraform.

## Camadas do back-end

```
Controller  →  Service  →  Repository
   (API)       (regras)     (persistência)
```

- **Controller:** recebe as requisições REST, valida a entrada e converte entre DTOs e o domínio.
- **Service:** concentra as regras de negócio e as transações.
- **Repository:** acesso a dados com Spring Data JPA.
- Entidades JPA não são expostas diretamente na API: a comunicação usa DTOs.

## Módulos

Cada módulo é um pacote de topo com suas próprias camadas. Um módulo só usa outro por meio de serviços públicos, nunca acessando os repositórios de outro módulo diretamente.

| Módulo | Responsabilidade | Épicos |
|---|---|---|
| `identidade` | Cadastro, autenticação, perfis, tokens, organizações e verificação | KAN-4 |
| `investidores` | Tipo de investidor, tese e preferências de investimento | KAN-5 |
| `empreendimentos` | Cadastro das startups, equipe, curadoria, visibilidade (Nível 1 e Nível 2) e bloqueios | KAN-6, KAN-7 |
| `catalogo` | Edições, filtros, favoritos, histórico | KAN-8, KAN-9 |
| `conexoes` | Manifestação de interesse, Match, ciclo de vida da conexão, rodadas com investidor líder e registro de investimentos | KAN-10 |
| `recomendacao` | Critérios de compatibilidade e justificativas | KAN-39 |
| `agenda` | Propostas, confirmação, lembretes e resultado de reuniões | Processo P6 |
| `moderacao` | Denúncias, sanções, recursos, disputas e violações de confidencialidade | Processos P8 a P11 |
| `publicidade` | Anunciantes, campanhas, aprovação, veiculação e biblioteca de anúncios | Fase 2 (P17) |
| `marketplace` | Prestadores, serviços, propostas, contratações, avaliações e pagamentos | KAN-115 (Fase 2) |
| `conhecimento` | Conteúdos, taxonomia, fluxo editorial, cursos, trilhas, acesso e Market Insights | KAN-116 (Fase 2) |
| `comunidade` | Feed, seguidores, grupos, perguntas e respostas, discussões, reputação e mensagens | KAN-117 (Fase 3) |
| `administracao` | Usuários e papéis internos, parâmetros e listas, filas de verificação e curadoria, edições do catálogo, termos versionados e trilha de auditoria | KAN-62, KAN-66, P2 a P5, P14, P15 |

As rotinas automáticas (R1 a R14) ficam em cada módulo dono da regra, com agendamento e monitoramento comuns em `compartilhado`. Prazos e limites vêm sempre dos **parâmetros configuráveis**, nunca de valores fixos no código. Detalhes em [`requisitos/processos-e-operacao.md`](requisitos/processos-e-operacao.md) e, para marketplace, conhecimento e comunidade, em [`requisitos/servicos-e-conhecimento.md`](requisitos/servicos-e-conhecimento.md).

Estrutura prevista do pacote base:

```
br.com.orbithub
├── identidade
│   ├── api          # controllers e DTOs
│   ├── service
│   ├── domain       # entidades e regras
│   └── repository
├── investidores
├── empreendimentos
├── catalogo
├── conexoes
├── recomendacao
└── compartilhado    # configuração, segurança, erros, utilitários
```

## Padrões de projeto (GoF)

| Categoria | Padrão | Onde será aplicado |
|---|---|---|
| Criacional | Factory Method | Criação dos perfis de usuário (investidor e empreendedor), cada um com seu onboarding (KAN-12) |
| Estrutural | Adapter | Integrações externas atrás de interfaces próprias: armazenamento de arquivos (disco local ou Azure Blob Storage), envio de e-mail (KAN-14) e validação de CNPJ (KAN-21) |
| Comportamental | Strategy | Cada critério de compatibilidade (segmento, estágio, faixa de aporte, localização, modelo de negócio) é uma estratégia que devolve sua justificativa (KAN-39). Na Fase 2, também a segmentação publicitária |

## Segurança

- Spring Security com **access token JWT** de curta duração.
- **Refresh token** em cookie `HttpOnly`, com apenas o hash guardado no banco, rotação a cada uso e revogação individual ou por usuário (KAN-57).
- Permissões verificadas no servidor para todas as operações (KAN-34).
- Separação entre informações públicas (Nível 1) e confidenciais (Nível 2), com liberação explícita pelo empreendedor (KAN-28 a KAN-33).
- Tratamento de dados pessoais conforme a LGPD.

## Infraestrutura (Azure for Students)

| Recurso | Uso |
|---|---|
| Azure Container Apps | Back-end, com escala a zero |
| Azure Static Web Apps | Front-end (plano Free) |
| Azure Database for PostgreSQL | Um servidor Flexible B1ms, com bancos de homologação e produção |
| Azure Blob Storage | Logotipos e documentos |
| Azure Key Vault | Segredos |
| Application Insights | Logs, métricas e rastreamento |
| GitHub Container Registry | Imagens Docker |

Toda a infraestrutura é declarada em Terraform na pasta `infra/` (KAN-55).
