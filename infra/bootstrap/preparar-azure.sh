#!/usr/bin/env bash
# Preparação única da assinatura Azure para o Orbit Hub (KAN-55 e KAN-53).
#
# O que faz (é seguro rodar de novo; tudo é idempotente):
#   1. Mostra as regiões permitidas pela política do Azure for Students.
#   2. Registra os provedores de recursos usados pelo projeto.
#   3. Cria a Storage Account do estado remoto do Terraform (bloqueio de estado por lease do blob).
#   4. Cria a identidade do GitHub Actions no Entra ID, com credenciais federadas (OIDC, sem senha).
#   5. Dá à identidade as permissões mínimas para o Terraform.
#   6. Grava no GitHub as variáveis do repositório (ids, conta do estado e região).
#
# Uso:  az login  &&  gh auth login  &&  REGIAO=<regiao-permitida> bash infra/bootstrap/preparar-azure.sh

set -euo pipefail

REPO="${REPO:-NicoNino89/orbit-hub}"
REGIAO="${REGIAO:-}"
GRUPO_ESTADO="rg-orbithub-tfstate"
APP_NOME="orbit-hub-github-actions"

echo "==> Assinatura em uso"
az account show --query "{assinatura:name, id:id, tenant:tenantId}" -o table
ASSINATURA_ID=$(az account show --query id -o tsv)
TENANT_ID=$(az account show --query tenantId -o tsv)

echo "==> Regiões permitidas pela política da assinatura"
az policy assignment list --query "[?parameters.listOfAllowedLocations].parameters.listOfAllowedLocations.value" -o tsv | tr '\t' '\n' | sort -u || true
if [[ -z "$REGIAO" ]]; then
  echo "Defina REGIAO com uma das regiões acima e rode de novo. Exemplo: REGIAO=eastus2 bash $0"
  exit 1
fi

echo "==> Registrando provedores de recursos"
for p in Microsoft.App Microsoft.OperationalInsights Microsoft.Insights Microsoft.AlertsManagement \
         Microsoft.DBforPostgreSQL Microsoft.Web Microsoft.KeyVault Microsoft.Storage \
         Microsoft.ManagedIdentity Microsoft.Consumption; do
  az provider register --namespace "$p" --output none
done

echo "==> Estado remoto do Terraform"
az group create --name "$GRUPO_ESTADO" --location "$REGIAO" --tags projeto=orbit-hub gerencia=bootstrap --output none
CONTA_ESTADO=$(az storage account list -g "$GRUPO_ESTADO" --query "[0].name" -o tsv)
if [[ -z "$CONTA_ESTADO" ]]; then
  CONTA_ESTADO="sttforbithub$(tr -dc 'a-z0-9' </dev/urandom | head -c 6)"
  az storage account create --name "$CONTA_ESTADO" --resource-group "$GRUPO_ESTADO" --location "$REGIAO" \
    --sku Standard_LRS --kind StorageV2 --min-tls-version TLS1_2 --allow-blob-public-access false \
    --allow-shared-key-access false --output none
fi
CONTA_ESTADO_ID=$(az storage account show -n "$CONTA_ESTADO" -g "$GRUPO_ESTADO" --query id -o tsv)
az storage account blob-service-properties update --account-name "$CONTA_ESTADO" -g "$GRUPO_ESTADO" \
  --enable-versioning true --enable-delete-retention true --delete-retention-days 14 --output none

EU_ID=$(az ad signed-in-user show --query id -o tsv)
az role assignment create --assignee-object-id "$EU_ID" --assignee-principal-type User \
  --role "Storage Blob Data Contributor" --scope "$CONTA_ESTADO_ID" --output none 2>/dev/null || true
az storage container create --name tfstate --account-name "$CONTA_ESTADO" --auth-mode login --output none

echo "==> Identidade do GitHub Actions (OIDC)"
APP_ID=$(az ad app list --display-name "$APP_NOME" --query "[0].appId" -o tsv)
if [[ -z "$APP_ID" ]]; then
  APP_ID=$(az ad app create --display-name "$APP_NOME" --query appId -o tsv)
fi
az ad sp show --id "$APP_ID" --output none 2>/dev/null || az ad sp create --id "$APP_ID" --output none
SP_ID=$(az ad sp show --id "$APP_ID" --query id -o tsv)

criar_federacao() {
  local nome="$1" sujeito="$2"
  if ! az ad app federated-credential list --id "$APP_ID" --query "[?name=='$nome']" -o tsv | grep -q .; then
    az ad app federated-credential create --id "$APP_ID" --output none --parameters "{
      \"name\": \"$nome\",
      \"issuer\": \"https://token.actions.githubusercontent.com\",
      \"subject\": \"$sujeito\",
      \"audiences\": [\"api://AzureADTokenExchange\"]
    }"
  fi
}
criar_federacao "main"              "repo:$REPO:ref:refs/heads/main"
criar_federacao "pull-request"      "repo:$REPO:pull_request"
criar_federacao "env-compartilhado" "repo:$REPO:environment:compartilhado"
criar_federacao "env-homologacao"   "repo:$REPO:environment:homologacao"
criar_federacao "env-producao"      "repo:$REPO:environment:producao"

echo "==> Permissões da identidade"
ESCOPO="/subscriptions/$ASSINATURA_ID"
for papel in "Contributor" "Role Based Access Control Administrator" "Key Vault Secrets Officer"; do
  az role assignment create --assignee-object-id "$SP_ID" --assignee-principal-type ServicePrincipal \
    --role "$papel" --scope "$ESCOPO" --output none 2>/dev/null || true
done
az role assignment create --assignee-object-id "$SP_ID" --assignee-principal-type ServicePrincipal \
  --role "Storage Blob Data Contributor" --scope "$CONTA_ESTADO_ID" --output none 2>/dev/null || true

echo "==> Variáveis do repositório no GitHub"
gh variable set AZURE_CLIENT_ID       --repo "$REPO" --body "$APP_ID"
gh variable set AZURE_TENANT_ID       --repo "$REPO" --body "$TENANT_ID"
gh variable set AZURE_SUBSCRIPTION_ID --repo "$REPO" --body "$ASSINATURA_ID"
gh variable set AZURE_REGIAO          --repo "$REPO" --body "$REGIAO"
gh variable set TF_ESTADO_CONTA       --repo "$REPO" --body "$CONTA_ESTADO"
for amb in compartilhado homologacao producao; do
  gh api -X PUT "repos/$REPO/environments/$amb" --silent
done

cat <<EOF

Pronto. Falta gravar três segredos no repositório (Settings › Secrets and variables › Actions):
  ALERTAS_EMAIL        e-mail que recebe os alertas e os avisos do orçamento
  GHCR_LEITURA_TOKEN   token clássico do GitHub só com o escopo read:packages
  SONAR_TOKEN          token do SonarCloud (opcional até a análise ser ligada)

Depois, rode o workflow "Infraestrutura" na ordem: compartilhado → homologacao → producao.
EOF
