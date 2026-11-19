# Um ambiente completo do Orbit Hub (homologação ou produção) - KAN-55.

data "azurerm_client_config" "atual" {}

locals {
  nome = "orbithub-${var.sigla}"
  tags = {
    projeto  = "orbit-hub"
    ambiente = var.ambiente
    gerencia = "terraform"
  }
}

resource "azurerm_resource_group" "ambiente" {
  name     = "rg-${local.nome}"
  location = var.regiao
  tags     = local.tags
}

resource "random_string" "sufixo" {
  length  = 5
  upper   = false
  special = false
}

# Identidade da API: lê segredos do Key Vault e grava no Blob Storage, sem senha nem chave de acesso.
resource "azurerm_user_assigned_identity" "api" {
  name                = "id-${local.nome}-api"
  location            = azurerm_resource_group.ambiente.location
  resource_group_name = azurerm_resource_group.ambiente.name
  tags                = local.tags
}

# ---------- Key Vault ----------

resource "azurerm_key_vault" "ambiente" {
  name                       = "kv-${local.nome}-${random_string.sufixo.result}"
  location                   = azurerm_resource_group.ambiente.location
  resource_group_name        = azurerm_resource_group.ambiente.name
  tenant_id                  = data.azurerm_client_config.atual.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  purge_protection_enabled   = false
  soft_delete_retention_days = 7
  tags                       = local.tags
}

resource "azurerm_role_assignment" "kv_operador_pipeline" {
  scope                = azurerm_key_vault.ambiente.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.atual.object_id
}

resource "azurerm_role_assignment" "kv_leitura_api" {
  scope                = azurerm_key_vault.ambiente.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_user_assigned_identity.api.principal_id
}

resource "azurerm_key_vault_secret" "db_senha" {
  name         = "db-senha"
  value        = random_password.db_app.result
  key_vault_id = azurerm_key_vault.ambiente.id
  content_type = "senha do usuário da aplicação no banco do ambiente"
  depends_on   = [azurerm_role_assignment.kv_operador_pipeline]
}

resource "azurerm_key_vault_secret" "appinsights" {
  name         = "appinsights-connection-string"
  value        = azurerm_application_insights.api.connection_string
  key_vault_id = azurerm_key_vault.ambiente.id
  depends_on   = [azurerm_role_assignment.kv_operador_pipeline]
}

resource "azurerm_key_vault_secret" "ghcr_token" {
  name         = "ghcr-leitura-token"
  value        = var.ghcr_token
  key_vault_id = azurerm_key_vault.ambiente.id
  depends_on   = [azurerm_role_assignment.kv_operador_pipeline]
}

# ---------- Arquivos (Blob Storage) ----------

resource "azurerm_storage_account" "arquivos" {
  name                            = "st${replace(local.nome, "-", "")}${random_string.sufixo.result}"
  location                        = azurerm_resource_group.ambiente.location
  resource_group_name             = azurerm_resource_group.ambiente.name
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  min_tls_version                 = "TLS1_2"
  https_traffic_only_enabled      = true
  allow_nested_items_to_be_public = false
  shared_access_key_enabled       = false
  default_to_oauth_authentication = true
  tags                            = local.tags

  blob_properties {
    delete_retention_policy {
      days = 7
    }
  }
}

resource "azurerm_storage_container" "arquivos" {
  name                  = "arquivos"
  storage_account_id    = azurerm_storage_account.arquivos.id
  container_access_type = "private"
}

resource "azurerm_role_assignment" "blob_api" {
  scope                = azurerm_storage_account.arquivos.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.api.principal_id
}

resource "azurerm_role_assignment" "blob_pipeline" {
  scope                = azurerm_storage_account.arquivos.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = data.azurerm_client_config.atual.object_id
}
