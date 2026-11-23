# Back-end — Orbit Hub API

API REST em **Java 21 + Spring Boot 4.1**, construída com **Maven** (use o `mvnw` do projeto).
A organização em módulos e camadas está em [`../docs/arquitetura.md`](../docs/arquitetura.md).

## Comandos

| Comando | O que faz |
|---|---|
| `./mvnw spring-boot:test-run` | Sobe a API com um PostgreSQL em container (Testcontainers) |
| `./mvnw test` | Testes de unidade (`*Test`) |
| `./mvnw verify` | Unidade + integração (`*IT`, com Testcontainers) + relatório e portão de cobertura JaCoCo |
| `docker build -t orbit-hub-api .` | Gera a imagem (multi-stage, JRE 21 Alpine, usuário sem privilégios) |

Os testes de integração precisam do Docker em execução.

## Endereços

| Caminho | Conteúdo |
|---|---|
| `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | Contrato OpenAPI |
| `/actuator/health` | Saúde (com `/liveness` e `/readiness`) |
| `/actuator/prometheus` | Métricas no formato Prometheus |
| `/api/v1/sobre` | Versão e ambiente da API |

## Configuração

Tudo vem de variáveis de ambiente. Na Azure, os segredos vêm do Key Vault.

| Variável | Padrão | Uso |
|---|---|---|
| `ORBIT_AMBIENTE` | `local` | `local`, `homologacao` ou `producao` |
| `ORBIT_DB_URL` | `jdbc:postgresql://localhost:5432/orbithub` | Banco de dados |
| `ORBIT_DB_USUARIO` | `orbithub` | Usuário do banco |
| `ORBIT_DB_SENHA` | (vazio) | Senha do banco |
| `ORBIT_ARMAZENAMENTO_TIPO` | `local` | `local` (disco) ou `azure-blob` |
| `ORBIT_ARMAZENAMENTO_PASTA` | `./dados/arquivos` | Pasta raiz no modo `local` |
| `ORBIT_BLOB_ENDPOINT` | (vazio) | URL da Storage Account (identidade gerenciada, sem chave) |
| `ORBIT_BLOB_CONTAINER` | `arquivos` | Container de arquivos |
| `ORBIT_BLOB_CONNECTION_STRING` | (vazio) | Só para o Azurite, o emulador local |

## Armazenamento de arquivos (padrão Adapter)

Os módulos dependem apenas da interface `ArmazenamentoArquivos`, no pacote `compartilhado.armazenamento`.

- `DiscoLocalArmazenamento`: desenvolvimento e Docker Compose.
- `AzureBlobArmazenamento`: homologação e produção, autenticado por identidade gerenciada.

Os dois adapters passam pelo mesmo conjunto de testes de contrato (`ContratoArmazenamento`). O Azure é testado contra o Azurite. As chaves são validadas para impedir saída da pasta raiz (`../`).
