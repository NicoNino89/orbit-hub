# ADR 0005 — Ambiente do Container Apps criado pelo azapi

- **Data:** 22/11/2026
- **Situação:** aceita
- **Histórias:** KAN-55

## Contexto

Na API `2026-07-01`, os ambientes do Azure Container Apps ganharam a propriedade `environmentMode` (`ConsumptionOnly`, `WorkloadProfiles`, `Express`, `Archived`). Quando o modo não é informado, a Azure cria o ambiente como **Express**.

O modo Express **não aceita segredos lidos do Key Vault** (`ExpressKeyVaultSecretsNotSupported`). É assim que a API recebe a senha do banco, a connection string do Application Insights e o token do GHCR. O provedor `azurerm` 5.14 usa uma API anterior e não tem como informar o modo, então o primeiro apply de homologação criou o ambiente como Express e falhou ao criar os apps.

## Decisão

- Criar o ambiente com o recurso `azapi_resource` (provedor `Azure/azapi`), na API `2026-07-01`, com `environmentMode = "WorkloadProfiles"` e só o perfil **Consumption**. Continua o plano de consumo: escala a zero e nenhum custo fixo.
- Os apps (`azurerm_container_app`) continuam no `azurerm` e só passam a apontar para o id do ambiente criado pelo azapi.
- A chave do Log Analytics vai em `sensitive_body`, para não ficar no estado.
- Homologação já tinha o ambiente criado como Express. Ele foi convertido para `WorkloadProfiles` com uma chamada direta à API (`az rest --method patch`) e entrou no azapi por um bloco `import`, sem ser recriado. O recurso antigo saiu do estado por um bloco `removed` com `destroy = false`.

## Consequências

- Produção nasce direto no modo certo.
- Quando o `azurerm` passar a aceitar o modo, dá para voltar ao `azurerm_container_app_environment` com um novo `import`. Não há pressa: o azapi é mantido pela Microsoft.
- O bloco `import` de homologação pode ser removido depois do primeiro apply. Se ficar, não muda nada.

> **Atualização em 23/11/2026:** o ambiente saiu do módulo `ambiente` e passou a ser um só, na camada `compartilhado` ([ADR 0007](0007-ambiente-container-apps-compartilhado.md)). Continua criado pelo azapi, no mesmo modo. O bloco `import` de homologação foi removido.
