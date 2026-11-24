# ADR 0007 — Um ambiente do Container Apps para homologação e produção

- **Data:** 23/11/2026
- **Situação:** aceita
- **Histórias:** KAN-55

## Contexto

O primeiro apply de produção falhou ao criar o ambiente do Container Apps:

> The subscription cannot have more than 1 Container App Environments in Canada Central.

A assinatura Azure for Students permite **um ambiente do Container Apps por região**, e o de homologação (`cae-orbithub-hml`) já ocupava `canadacentral`. Colocar produção em outra região permitida (`mexicocentral`, `chilecentral`) afastaria a API do banco, que está em `canadacentral` (ADR 0004): mais latência e tráfego pago entre regiões.

## Decisão

- Criar **um** ambiente, `cae-orbithub`, na camada `compartilhado`, ao lado do PostgreSQL e do Log Analytics. Ele continua pelo azapi, no modo `WorkloadProfiles` só com o perfil Consumption (ADR 0005).
- O módulo `ambiente` deixa de criar o ambiente e recebe `container_apps_ambiente_id` e `container_apps_dominio` da camada compartilhada.
- Os apps de cada ambiente continuam separados, nos próprios grupos (`rg-orbithub-hml`, `rg-orbithub-prd`), com identidades, Key Vaults, Blob Storage, Application Insights e bancos próprios. Um app de homologação não tem permissão sobre nada de produção.

## Migração (feita uma vez)

1. Apagar os apps e o ambiente de homologação (`az containerapp delete`, `az containerapp env delete`). Homologação só tem dados fictícios, e o banco não é tocado.
2. Rodar `compartilhado` com `apply`: cria `cae-orbithub`.
3. Rodar `homologacao` com `apply`: o ambiente antigo já não existe e sai do estado; os apps são recriados no ambiente novo. Depois, rodar *Entrega em homologação* para publicar a imagem atual.
4. Rodar `producao` com `apply`.

## Consequências

- O endereço de homologação muda, porque o domínio padrão é do ambiente.
- Os logs de console dos dois ambientes chegam ao mesmo workspace (já era assim); os de cada app são separados pelo nome do app.
- Uma falha ou manutenção do ambiente afeta homologação e produção ao mesmo tempo. É aceitável enquanto não houver uso real; se a assinatura mudar, dá para voltar a ter um ambiente por camada.
