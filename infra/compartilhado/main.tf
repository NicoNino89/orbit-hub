# Recursos compartilhados entre homologação e produção (KAN-55).
# Para economizar o crédito do Azure for Students, há UM servidor PostgreSQL e UM workspace de logs.
# Cada ambiente cria o próprio banco e o próprio usuário nesse servidor (modules/ambiente).

terraform {
  required_version = ">= 1.14"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.14"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }

  backend "azurerm" {
    key              = "compartilhado.tfstate"
    use_azuread_auth = true
    # resource_group_name, storage_account_name e container_name vêm de -backend-config (bootstrap/backend.hcl)
  }
}

provider "azurerm" {
  features {}
  storage_use_azuread = true
}

data "azurerm_client_config" "atual" {}

locals {
  tags = {
    projeto  = "orbit-hub"
    camada   = "compartilhado"
    gerencia = "terraform"
  }
}

resource "azurerm_resource_group" "compartilhado" {
  name     = "rg-orbithub-compartilhado"
  location = var.regiao
  tags     = local.tags
}

resource "random_string" "sufixo" {
  length  = 5
  upper   = false
  special = false
}

# ---------- Orçamento (antes de qualquer outro recurso) ----------

resource "azurerm_consumption_budget_subscription" "credito" {
  name            = "orcamento-orbithub"
  subscription_id = "/subscriptions/${data.azurerm_client_config.atual.subscription_id}"
  amount          = var.orcamento_usd
  time_grain      = "Annually"

  time_period {
    start_date = var.orcamento_inicio
  }

  dynamic "notification" {
    for_each = [50, 80, 100]
    content {
      enabled        = true
      threshold      = notification.value
      operator       = "GreaterThanOrEqualTo"
      threshold_type = "Actual"
      contact_emails = [var.email_alertas]
    }
  }

  # Aviso antecipado: previsão de passar do crédito
  notification {
    enabled        = true
    threshold      = 100
    operator       = "GreaterThanOrEqualTo"
    threshold_type = "Forecasted"
    contact_emails = [var.email_alertas]
  }
}

# ---------- Logs ----------

resource "azurerm_log_analytics_workspace" "logs" {
  name                = "log-orbithub"
  location            = azurerm_resource_group.compartilhado.location
  resource_group_name = azurerm_resource_group.compartilhado.name
  sku                 = "PerGB2018"
  retention_in_days   = 30
  daily_quota_gb      = var.cota_diaria_logs_gb
  tags                = local.tags
}

# ---------- PostgreSQL ----------

resource "random_password" "postgres_admin" {
  length           = 32
  special          = true
  override_special = "-_.~"
}

resource "azurerm_postgresql_flexible_server" "principal" {
  name                          = "psql-orbithub-${random_string.sufixo.result}"
  location                      = azurerm_resource_group.compartilhado.location
  resource_group_name           = azurerm_resource_group.compartilhado.name
  version                       = "17"
  sku_name                      = "B_Standard_B1ms"
  storage_mb                    = 32768
  backup_retention_days         = 7
  geo_redundant_backup_enabled  = false
  public_network_access_enabled = true
  administrator_login           = "orbitadmin"
  administrator_password        = random_password.postgres_admin.result
  zone                          = null
  tags                          = local.tags

  lifecycle {
    # A zona é escolhida pela Azure na criação; mudar depois recriaria o servidor
    ignore_changes = [zone]
  }
}

# Libera o acesso a partir de serviços da própria Azure (Container Apps).
# Execuções do Terraform no GitHub Actions abrem e fecham uma regra temporária para o IP do runner.
resource "azurerm_postgresql_flexible_server_firewall_rule" "servicos_azure" {
  name             = "servicos-azure"
  server_id        = azurerm_postgresql_flexible_server.principal.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}

resource "azurerm_postgresql_flexible_server_configuration" "exigir_tls" {
  name      = "require_secure_transport"
  server_id = azurerm_postgresql_flexible_server.principal.id
  value     = "on"
}

# ---------- Segredos do servidor ----------

resource "azurerm_key_vault" "compartilhado" {
  name                       = "kv-orbithub-cmp-${random_string.sufixo.result}"
  location                   = azurerm_resource_group.compartilhado.location
  resource_group_name        = azurerm_resource_group.compartilhado.name
  tenant_id                  = data.azurerm_client_config.atual.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  purge_protection_enabled   = false
  soft_delete_retention_days = 7
  tags                       = local.tags
}

resource "azurerm_role_assignment" "kv_compartilhado_operador" {
  scope                = azurerm_key_vault.compartilhado.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.atual.object_id
}

resource "azurerm_key_vault_secret" "postgres_admin" {
  name         = "postgres-admin-senha"
  value        = random_password.postgres_admin.result
  key_vault_id = azurerm_key_vault.compartilhado.id
  content_type = "senha do usuário administrador do PostgreSQL"

  depends_on = [azurerm_role_assignment.kv_compartilhado_operador]
}
