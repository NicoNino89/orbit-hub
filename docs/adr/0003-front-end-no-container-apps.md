# ADR 0003 — Front-end no Azure Container Apps

- **Data:** 22/11/2026
- **Situação:** aceita
- **Histórias:** KAN-55, KAN-53

## Contexto

A arquitetura previa o front-end no **Azure Static Web Apps** (plano Free). A política da assinatura Azure for Students só permite criar recursos em `canadacentral`, `chilecentral`, `eastus`, `mexicocentral` e `southcentralus`, e o Static Web Apps não é oferecido em nenhuma delas. A aplicação fica em `canadacentral` (ADR 0004).

As alternativas eram pedir ao professor uma exceção na política ou publicar o front-end de outro jeito dentro das regiões permitidas.

## Decisão

Publicar o front-end como um segundo app no **mesmo ambiente do Azure Container Apps** da API, na mesma região, usando a imagem `orbit-hub-web` que o CI já gera para o Docker Compose (nginx sem root).

- O nginx repassa `/api` para a API do ambiente (variável `ORBIT_API_UPSTREAM`), então o navegador fala com uma origem só. O CORS da API continua liberando o endereço da web.
- A imagem é a mesma em todos os ambientes; o pipeline de entrega só troca a tag pelo commit, sem refazer o build do front-end.
- A web tem identidade própria, que lê só o segredo do token do GHCR no Key Vault.
- De 0 a 1 réplica, 0,25 vCPU e 0,5 GiB. Fica dentro da cota gratuita mensal do plano de consumo.

## Consequências

- O Static Web Apps sai do Terraform, do pipeline e da documentação, junto com `staticwebapp.config.json`.
- Com escala a zero, a primeira visita depois de um tempo parado demora alguns segundos para acordar a web (e mais, se a API também estiver parada). O smoke test já espera por isso.
- Domínio próprio e certificado gerenciado continuam disponíveis no Container Apps, se forem necessários.
