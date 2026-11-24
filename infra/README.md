# Infraestrutura — Terraform (Azure)

Infraestrutura como código do Orbit Hub, com **Terraform** e o provider **`azurerm` 5.x** (KAN-55).

## Camadas

```
infra/
├── bootstrap/          # preparação única da assinatura (estado remoto, OIDC) e scripts de apoio
├── compartilhado/      # orçamento, Log Analytics, PostgreSQL B1ms, Key Vault do servidor e ambiente do Container Apps
├── modules/ambiente/   # tudo o que um ambiente precisa
└── envs/
    ├── homologacao/    # usa o módulo com escala a zero e sem teste de disponibilidade
    └── producao/       # usa o módulo com escala a zero; teste de disponibilidade desligado por ora (ADR 0006)
```

| Recurso | Camada | Configuração econômica |
|---|---|---|
| Orçamento (Cost Management) | compartilhado | US$ 100 por ano, avisos em 50%, 80% e 100% e previsão de estouro |
| Log Analytics | compartilhado | Retenção de 30 dias, teto diário de 0,2 GB |
| PostgreSQL Flexible Server | compartilhado | **Um** servidor B1ms (PostgreSQL 17), 32 GB, desligado toda noite pelo workflow *PostgreSQL ligar e desligar* |
| Ambiente do Container Apps | compartilhado | **Um** ambiente (`cae-orbithub`, modo WorkloadProfiles só com o perfil Consumption) para homologação e produção: a assinatura permite um por região ([ADR 0007](../docs/adr/0007-ambiente-container-apps-compartilhado.md)) |
| Banco, usuário e schema | ambiente | `orbithub_hml` e `orbithub_prd`, cada um com usuário próprio que não acessa o outro banco |
| Container Apps (API) | ambiente | No ambiente compartilhado, plano de consumo, de 0 a 1 réplica, 0,5 vCPU e 1 GiB |
| Container Apps (web) | ambiente | Mesmo ambiente da API, de 0 a 1 réplica, 0,25 vCPU e 0,5 GiB ([ADR 0003](../docs/adr/0003-front-end-no-container-apps.md)) |
| Blob Storage | ambiente | LRS, sem chave de acesso (só identidade gerenciada) e lixeira de 7 dias |
| Key Vault | ambiente | Standard, autorização por RBAC. Guarda a senha do banco, a connection string do Application Insights e o token de leitura do GHCR |
| Application Insights | ambiente | Baseado no workspace, teto diário de 0,1 GB, amostragem de 50% no agente |
| Alertas (Azure Monitor) | ambiente | Taxa de erros, resposta lenta e reinícios. Em produção, também "fora do ar" |
| Imagens | — | GitHub Container Registry (sem custo de Azure Container Registry) |

## Segurança

- O GitHub Actions entra na Azure por **OIDC** (credencial federada), sem senha guardada.
- A API usa uma **identidade gerenciada** para ler o Key Vault e gravar no Blob Storage, sem chave de acesso.
- O PostgreSQL só aceita TLS. Ele fica aberto para os serviços da Azure e, durante cada execução do Terraform, para o IP do runner. A regra do runner é removida no fim.
- Os valores sensíveis (`ALERTAS_EMAIL` e `GHCR_LEITURA_TOKEN`) vêm de segredos do GitHub, como variáveis `TF_VAR_*`. Nunca versione `*.tfvars` nem `*.tfstate`.

## Primeira vez

1. Entre na Azure e no GitHub pelo terminal: `az login` e `gh auth login`.
2. Rode a preparação. Ela lista as regiões permitidas; escolha uma e rode de novo com a variável:
   ```bash
   bash infra/bootstrap/preparar-azure.sh
   REGIAO=<regiao-permitida> bash infra/bootstrap/preparar-azure.sh
   ```
3. Grave os segredos do repositório: `ALERTAS_EMAIL`, `GHCR_LEITURA_TOKEN` (token clássico só com `read:packages`) e `SONAR_TOKEN`.
4. Em *Actions › Infraestrutura › Run workflow*, rode `compartilhado` com `apply`. O orçamento é o primeiro recurso dessa camada.
5. Rode `homologacao` com `apply`. O resumo da execução mostra os endereços da web e da API.
6. Rode `producao` com `apply` quando for a hora (marco M1, S2).

## Dia a dia

| Situação | O que acontece |
|---|---|
| PR que mexe em `infra/` | O pipeline roda `fmt`, `validate` e `plan` das três camadas e mostra o plano no resumo |
| Aplicar uma mudança | Depois do merge, rode o workflow *Infraestrutura* com `apply`. Disparar o workflow é a aprovação |
| Fim de uma demonstração | Rode `destroy` na camada `producao` ou `homologacao`. O código recria tudo depois |
| Economizar crédito | O PostgreSQL desliga às 23h. Para ligar antes de testar, use *PostgreSQL ligar e desligar › ligar* |

Acompanhe o consumo do crédito no portal do Azure for Students.
