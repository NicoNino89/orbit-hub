# ADR 0006 — Produção sem teste de disponibilidade, por enquanto

- **Data:** 23/11/2026
- **Situação:** aceita
- **Histórias:** KAN-55, KAN-56

## Contexto

O módulo `ambiente` cria, quando `teste_disponibilidade = true`, um teste padrão do Application Insights que chama `/actuator/health` da API a cada 15 minutos, com alerta por e-mail se falhar. Em produção a API tem `replicas_minimas = 0`: cada chamada do teste acorda uma réplica e o banco fica sendo consultado, o que consome crédito da assinatura Azure for Students o dia inteiro, mesmo sem nenhum usuário.

## Decisão

Aplicar produção com `teste_disponibilidade = false`. A API continua escalando a zero e o monitoramento de logs, métricas e alertas de erro (KAN-56) segue ligado.

## Consequências

- Uma queda da API em produção não gera alerta sozinha; ela aparece no primeiro acesso, nos logs e no painel.
- Religar é mudar a variável para `true` em `infra/envs/producao/main.tf` e rodar o workflow "Infraestrutura". Vamos reavaliar quando a produção tiver uso real ou quando formos testar a disponibilidade de novo.
