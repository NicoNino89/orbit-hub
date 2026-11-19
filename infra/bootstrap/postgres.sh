#!/usr/bin/env bash
# Autor: Nicholas Bruno Pimentel
# Data: 18/11/2026
# Hora: 21:12
# Cartão Jira: KAN-55
# Título: Ligar e desligar o PostgreSQL
# Objetivo: Ligar, desligar ou consultar o PostgreSQL compartilhado, para economizar o crédito do Azure for Students.
# Decisões: Idempotente: só liga se estiver parado e só desliga se estiver pronto; sem servidor, sai sem erro.
#
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
