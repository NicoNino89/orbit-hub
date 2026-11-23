#!/usr/bin/env bash
# Autor: Nicholas Bruno Pimentel
# Data: 18/11/2026
# Hora: 21:12
# Cartão Jira: KAN-55
# Título: Firewall do PostgreSQL para o runner
# Objetivo: Abrir ou fechar o PostgreSQL compartilhado para o IP do runner do GitHub Actions.
# Decisões: Uma regra por execução (GITHUB_RUN_ID), apagada no fim; fechar nunca falha, para a limpeza
#           rodar mesmo depois de um erro; parâmetros por nome longo (--server-name, --name), porque
#           o Azure CLI mudou o significado de -n nos comandos de firewall-rule.
#
# Abre ou fecha o PostgreSQL compartilhado para o IP do runner do GitHub Actions.
# O Terraform precisa conectar no servidor para criar o banco, o usuário e o schema de cada ambiente.
# Uso: firewall-runner.sh abrir|fechar
set -euo pipefail

GRUPO="rg-orbithub-compartilhado"
REGRA="github-runner-${GITHUB_RUN_ID:-local}"
SERVIDOR=$(az postgres flexible-server list -g "$GRUPO" --query "[0].name" -o tsv)

if [[ -z "$SERVIDOR" ]]; then
  echo "Servidor PostgreSQL ainda não existe; rode antes a camada compartilhado."
  exit 1
fi

case "${1:-}" in
  abrir)
    IP=$(curl -fsS https://api.ipify.org)
    az postgres flexible-server firewall-rule create -g "$GRUPO" --server-name "$SERVIDOR" --name "$REGRA" \
      --start-ip-address "$IP" --end-ip-address "$IP" --output none
    echo "Acesso liberado para $IP (regra $REGRA)."
    ;;
  fechar)
    az postgres flexible-server firewall-rule delete -g "$GRUPO" --server-name "$SERVIDOR" --name "$REGRA" --yes --output none || true
    echo "Regra $REGRA removida."
    ;;
  *)
    echo "Uso: $0 abrir|fechar"; exit 2 ;;
esac
