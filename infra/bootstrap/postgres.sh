#!/usr/bin/env bash
# Liga ou desliga o PostgreSQL compartilhado. Servidor parado não consome computação (só o disco).
# A Azure religa sozinha um servidor parado há 7 dias; o desligamento agendado volta a parar.
# Uso: postgres.sh ligar|desligar|estado
set -euo pipefail

GRUPO="rg-orbithub-compartilhado"
SERVIDOR=$(az postgres flexible-server list -g "$GRUPO" --query "[0].name" -o tsv)
if [[ -z "$SERVIDOR" ]]; then
  echo "Servidor PostgreSQL ainda não existe."
  exit 0
fi
ESTADO=$(az postgres flexible-server show -g "$GRUPO" -n "$SERVIDOR" --query state -o tsv)

case "${1:-estado}" in
  ligar)
    if [[ "$ESTADO" == "Stopped" ]]; then
      az postgres flexible-server start -g "$GRUPO" -n "$SERVIDOR" --output none
      echo "Servidor $SERVIDOR ligado."
    else
      echo "Servidor $SERVIDOR já está $ESTADO."
    fi
    ;;
  desligar)
    if [[ "$ESTADO" == "Ready" ]]; then
      az postgres flexible-server stop -g "$GRUPO" -n "$SERVIDOR" --output none
      echo "Servidor $SERVIDOR desligado."
    else
      echo "Servidor $SERVIDOR está $ESTADO; nada a fazer."
    fi
    ;;
  estado)
    echo "$SERVIDOR: $ESTADO" ;;
  *)
    echo "Uso: $0 ligar|desligar|estado"; exit 2 ;;
esac
