# Observabilidade do Orbit Hub (KAN-56)

## O que é coletado

| Sinal | Local (Docker Compose) | Azure (homologação e produção) |
|---|---|---|
| Saúde | `/actuator/health`, com `/liveness` e `/readiness` | As mesmas sondas, usadas pelo Container Apps para reiniciar ou tirar a réplica do ar |
| Métricas | Prometheus coleta `/actuator/prometheus` a cada 15 s | Application Insights, pelo agente Java e pela ponte do Micrometer |
| Logs | JSON no padrão ECS na saída do container | Application Insights (tabela `traces`) |
| Rastreamento | — | Application Insights: requisições, dependências (PostgreSQL e Blob) e exceções |
| Painel | Grafana › *Orbit Hub — API* | Application Insights › *Application map* e *Performance* |
| Alertas | Prometheus › *Alerts* (`observabilidade/prometheus/alertas.yml`) | Azure Monitor, declarado no Terraform (`infra/`) |

## Painel do Grafana

Abra http://localhost:3000 e vá em *Dashboards › Orbit Hub › Orbit Hub — API*. O painel mostra:

- disponibilidade agora e nas últimas 24 h;
- taxa de erros 5xx;
- tempo de resposta p50, p95 e p99, calculado pelo histograma de `http.server.requests`;
- requisições por segundo por status;
- as 5 rotas mais lentas;
- memória heap e conexões ativas com o banco.

As rotas do Actuator ficam fora das contas de tempo de resposta, para a coleta do Prometheus não distorcer os números.

## Alertas

| Alerta | Condição | Gravidade |
|---|---|---|
| API fora do ar | Sem resposta por mais de 1 minuto | Crítica |
| Taxa de erros alta | Mais de 5% de respostas 5xx em 5 minutos | Alta |
| Resposta lenta | p95 acima de 1 segundo por 10 minutos | Média |

Os limites são os mesmos no Prometheus local e no Azure Monitor.

## Application Insights

- O agente Java (`applicationinsights-agent`) vai dentro da imagem, em `/app/agente/`, mas só é ligado na Azure. O Terraform define `JAVA_TOOL_OPTIONS=-javaagent:/app/agente/applicationinsights-agent.jar` e a variável `APPLICATIONINSIGHTS_CONNECTION_STRING`, que vem do Key Vault.
- A amostragem está em 50% (`backend/docker/applicationinsights.json`), para ficar dentro da cota gratuita de ingestão.

## Regras para logs

1. **Nunca registre dados pessoais ou sensíveis:** nome, e-mail, CPF, CNPJ de pessoa física, telefone, senha, token, documento do Data Room, valores de proposta. Para identificar algo, use o ID interno (UUID).
2. Não registre corpo de requisição nem de resposta.
3. Use os níveis com critério: `ERROR` para o que exige ação, `WARN` para o que é anormal mas foi tratado, `INFO` para eventos de negócio sem dados pessoais, `DEBUG` só localmente.
4. Os logs saem em JSON (ECS). Para ler com mais conforto, use `docker compose logs api --no-log-prefix | jq`.
