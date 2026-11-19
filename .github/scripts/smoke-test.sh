#!/usr/bin/env bash
# Smoke test depois da publicação (KAN-53).
# A API escala a zero, então a primeira chamada pode levar até ~2 min para acordar o contêiner.
# Variáveis: API_URL, WEB_URL e VERSAO (commit curto esperado em /api/v1/sobre).
set -euo pipefail

tentar() {
  local descricao="$1" url="$2" esperado="$3"
  for i in $(seq 1 30); do
    corpo=$(curl -fsS --max-time 20 "$url" 2>/dev/null || true)
    if [[ -n "$corpo" && "$corpo" == *"$esperado"* ]]; then
      echo "OK   $descricao"
      return 0
    fi
    echo "...  $descricao (tentativa $i/30)"
    sleep 10
  done
  echo "FALHOU $descricao: $url não respondeu com \"$esperado\""
  echo "Última resposta: ${corpo:-<vazia>}"
  return 1
}

tentar "API no ar (/actuator/health)" "$API_URL/actuator/health" '"status":"UP"'
tentar "Versão publicada é $VERSAO"   "$API_URL/api/v1/sobre"     "\"versao\":\"$VERSAO\""
tentar "Ambiente é homologação"       "$API_URL/api/v1/sobre"     '"ambiente":"homologacao"'
tentar "Swagger publicado"            "$API_URL/v3/api-docs"      '"Orbit Hub API"'
tentar "Web publicada"                "$WEB_URL/"                 '<title>Orbit Hub</title>'

# A web precisa poder chamar a API pelo navegador (CORS)
origem_permitida=$(curl -fsS -o /dev/null -D - -H "Origin: $WEB_URL" "$API_URL/api/v1/sobre" | tr -d '\r' | grep -i '^access-control-allow-origin:' | cut -d' ' -f2 || true)
if [[ "$origem_permitida" != "$WEB_URL" ]]; then
  echo "FALHOU CORS: a API não autorizou a origem $WEB_URL (recebido: ${origem_permitida:-nada})"
  exit 1
fi
echo "OK   CORS libera a web de homologação"
