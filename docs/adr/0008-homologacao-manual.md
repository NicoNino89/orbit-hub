# ADR 0008 — Publicação em homologação manual, por enquanto

- **Data:** 24/11/2026
- **Situação:** aceita (provisória, até o fim do projeto)
- **Histórias:** KAN-53, KAN-163

## Contexto

O workflow *Entrega em homologação* rodava sozinho a cada merge na `main` e, para publicar, liga o PostgreSQL Flexible da Azure. Na assinatura Azure for Students, isso consome crédito e obriga a desligar o banco depois de cada merge. O ambiente local (`compose.yaml`: banco, API, web, Mailpit, Prometheus e Grafana) já cobre o desenvolvimento e o teste humano das histórias.

## Decisão

Desligar o gatilho automático (`workflow_run`) e manter só o disparo manual (`workflow_dispatch`). Desenvolvimento e teste ficam na máquina local; a publicação em homologação acontece quando decidirmos, em geral no fechamento da sprint ou para uma demonstração. O CI e o SonarCloud seguem rodando em todo PR e em todo merge, sem mudança.

## Consequências

- A `main` pode ficar à frente do que está publicado em homologação. O resumo de cada execução do workflow mostra o commit publicado.
- Antes de publicar, se a sprint trouxe mudança de infraestrutura, rodar o workflow *Infraestrutura* (`apply` em homologação) primeiro.
- Depois de publicar, desligar o banco (`infra/bootstrap/postgres.sh desligar`).
- **Voltar ao automático no fim do projeto** (cartão KAN-163): descomentar o bloco `workflow_run` em `.github/workflows/cd-homologacao.yml` e atualizar o README.
