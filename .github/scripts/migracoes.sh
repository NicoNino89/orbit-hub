#!/usr/bin/env bash
# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Conferência das migrações do Flyway
# Objetivo: Entre dois commits, recusar migração já existente que foi editada, apagada ou renomeada, e listar
#           as migrações novas com comando destrutivo (DROP, TRUNCATE, DELETE, RENAME, troca de tipo de coluna).
# Decisões: Uso: migracoes.sh <commit-base> <commit-novo>. Saída 1 = migração existente alterada (nunca permitido);
#           saída 2 = há migração destrutiva (quem chama decide se a revisão específica foi feita); 0 = tudo certo.
#           Linhas de comentário (--) são ignoradas. Escreve o resumo em $GITHUB_STEP_SUMMARY quando existe.
set -euo pipefail

base="${1:?informe o commit base}"
novo="${2:?informe o commit novo}"
pasta="backend/src/main/resources/db/migration"
padrao='\b(DROP|TRUNCATE)\b|\bDELETE[[:space:]]+FROM\b|\bRENAME\b|\bALTER[[:space:]]+COLUMN\b.*\bTYPE\b'
resumo="${GITHUB_STEP_SUMMARY:-/dev/null}"

# Autor: Nicholas Bruno Pimentel
# Data: 24/11/2026
# Hora: 12:49
# Cartão Jira: KAN-54
# Título: Escrever no log e no resumo
# Objetivo: Mostrar a mesma linha no log do job e no resumo da execução.
# Decisões: Resumo em Markdown.
dizer() {
  echo "$1"
  echo "$1" >> "$resumo"
}

dizer "### Migrações do Flyway (${base:0:7}..${novo:0:7})"

alteradas=$(git diff --name-status "$base" "$novo" -- "$pasta" | awk '$1 !~ /^A/ {print}' || true)
if [[ -n "$alteradas" ]]; then
  dizer "Migração já existente foi alterada, apagada ou renomeada. Uma migração aplicada nunca muda: crie uma nova."
  dizer '```'
  dizer "$alteradas"
  dizer '```'
  exit 1
fi

novas=$(git diff --name-only --diff-filter=A "$base" "$novo" -- "$pasta" | sort -V || true)
if [[ -z "$novas" ]]; then
  dizer "Nenhuma migração nova."
  exit 0
fi

destrutivas=0
while IFS= read -r arquivo; do
  achados=$(git show "$novo:$arquivo" | grep -v -E '^[[:space:]]*--' | grep -n -i -E "$padrao" || true)
  if [[ -n "$achados" ]]; then
    destrutivas=1
    dizer "- \`$(basename "$arquivo")\`: **destrutiva**"
    dizer '```sql'
    dizer "$achados"
    dizer '```'
  else
    dizer "- \`$(basename "$arquivo")\`"
  fi
done <<< "$novas"

if [[ $destrutivas -eq 1 ]]; then
  dizer "Há migração destrutiva: exige revisão específica (ver docs/operacao/producao.md)."
  exit 2
fi
