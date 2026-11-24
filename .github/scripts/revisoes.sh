#!/usr/bin/env bash
# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Revisões do Azure Container Apps
# Objetivo: Descobrir a revisão no ar e a anterior, reativar uma revisão e mandar todo o tráfego para ela, e
#           desativar as que sobraram. É a base da publicação em produção e do rollback.
# Decisões: Os apps ficam em modo de várias revisões ("Multiple"); cada publicação cria uma revisão nova (sufixo da
#           versão) e as antigas ficam desativadas, sem custo, prontas para voltar. Uso:
#             revisoes.sh no-ar <grupo> <app>
#             revisoes.sh anterior <grupo> <app> <revisao>
#             revisoes.sh versao <grupo> <app> <revisao>
#             revisoes.sh fixar <grupo> <app> <revisao>
#             revisoes.sh desativar-outras <grupo> <app> <revisao>
set -euo pipefail

comando="${1:?comando}"
grupo="${2:?grupo}"
app="${3:?app}"
revisao="${4:-}"

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Revisão no ar
# Objetivo: Devolver a revisão que recebe o tráfego.
# Decisões: A de maior peso; sem peso explícito (tráfego na "mais recente"), a última revisão pronta.
no_ar() {
  local nome
  nome=$(az containerapp revision list -g "$grupo" -n "$app" \
    --query "sort_by([?properties.trafficWeight > \`0\`], &properties.trafficWeight)[-1].name" -o tsv)
  if [[ -z "$nome" ]]; then
    nome=$(az containerapp show -g "$grupo" -n "$app" --query properties.latestReadyRevisionName -o tsv)
  fi
  echo "$nome"
}

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Revisão anterior
# Objetivo: Devolver a revisão criada logo antes da informada, ativa ou não.
# Decisões: Pela data de criação; vazio se não houver.
anterior() {
  az containerapp revision list -g "$grupo" -n "$app" --all \
    --query "sort_by([], &properties.createdTime)[].name" -o tsv |
    awk -v alvo="$1" '$0 == alvo { print anterior; achou = 1; exit } { anterior = $0 } END { if (!achou) exit 1 }'
}

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Versão de uma revisão
# Objetivo: Ler o ORBIT_VERSAO gravado na revisão, para o smoke test conferir a versão que voltou.
# Decisões: Vazio para a web, que não tem a variável.
versao() {
  az containerapp revision show -g "$grupo" -n "$app" --revision "$1" \
    --query "properties.template.containers[0].env[?name=='ORBIT_VERSAO'].value | [0]" -o tsv
}

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Fixar o tráfego numa revisão
# Objetivo: Reativar a revisão, se preciso, esperar ficar pronta e mandar 100% do tráfego para ela.
# Decisões: Até 5 minutos de espera; falha se a revisão não ficar pronta.
fixar() {
  local alvo="$1" ativa estado
  ativa=$(az containerapp revision show -g "$grupo" -n "$app" --revision "$alvo" --query properties.active -o tsv)
  if [[ "$ativa" != "true" ]]; then
    echo "Reativando $alvo"
    az containerapp revision activate -g "$grupo" -n "$app" --revision "$alvo" --output none
  fi
  for i in $(seq 1 30); do
    estado=$(az containerapp revision show -g "$grupo" -n "$app" --revision "$alvo" --query properties.provisioningState -o tsv)
    [[ "$estado" == "Provisioned" ]] && break
    echo "...  $alvo: $estado (tentativa $i/30)"
    sleep 10
  done
  if [[ "$estado" != "Provisioned" ]]; then
    echo "FALHOU $alvo não ficou pronta (estado: $estado)"
    return 1
  fi
  az containerapp ingress traffic set -g "$grupo" -n "$app" --revision-weight "$alvo=100" --output none
  echo "OK   $app: 100% do tráfego em $alvo"
}

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Desativar as outras revisões
# Objetivo: Deixar ativa só a revisão informada.
# Decisões: Desativada não gasta crédito e pode ser reativada pelo rollback.
desativar_outras() {
  local manter="$1" nome
  for nome in $(az containerapp revision list -g "$grupo" -n "$app" --query "[?properties.active].name" -o tsv); do
    if [[ "$nome" != "$manter" ]]; then
      az containerapp revision deactivate -g "$grupo" -n "$app" --revision "$nome" --output none
      echo "OK   $app: $nome desativada"
    fi
  done
}

case "$comando" in
  no-ar) no_ar ;;
  anterior) anterior "${revisao:?revisao}" ;;
  versao) versao "${revisao:?revisao}" ;;
  fixar) fixar "${revisao:?revisao}" ;;
  desativar-outras) desativar_outras "${revisao:?revisao}" ;;
  *) echo "Comando desconhecido: $comando" >&2; exit 64 ;;
esac
