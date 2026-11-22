# ADR 0004 — Aplicação na região canadacentral

- **Data:** 22/11/2026
- **Situação:** aceita
- **Histórias:** KAN-55

## Contexto

A política da assinatura Azure for Students só permite `canadacentral`, `chilecentral`, `eastus`, `mexicocentral` e `southcentralus`. A infraestrutura foi planejada em `eastus`, mas o primeiro apply mostrou que a Azure **não deixa esta assinatura criar PostgreSQL Flexible Server** em `eastus` nem em `southcentralus` ("Provisioning is restricted in this region"). Nas outras três regiões o B1ms com PostgreSQL 17 está disponível.

## Decisão

Colocar **toda a aplicação** (banco, API, web, Key Vault, logs e monitoramento) em `canadacentral`, pela variável `AZURE_REGIAO` do GitHub. É a região permitida mais próxima de `eastus` e mantém banco e API juntos, sem latência nem tráfego pago entre regiões.

O estado remoto do Terraform continua na conta de armazenamento criada pelo bootstrap em `eastus`; ele não precisa estar na mesma região da aplicação.

## Consequências

- Os recursos parciais criados em `eastus` no primeiro apply (grupo, Log Analytics e Key Vault, vazios) foram destruídos pelo workflow e recriados em `canadacentral`.
- Se um dia a restrição de `eastus` cair, mudar de região exige recriar a infraestrutura; não há motivo para isso enquanto o custo for o mesmo.
