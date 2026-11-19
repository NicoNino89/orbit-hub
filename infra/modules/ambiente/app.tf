# API no Azure Container Apps (plano de consumo, escala a zero) e front-end no Static Web Apps (Free).

resource "azurerm_container_app_environment" "ambiente" {
  name                       = "cae-${local.nome}"
  location                   = azurerm_resource_group.ambiente.location
  resource_group_name        = azurerm_resource_group.ambiente.name
  log_analytics_workspace_id = var.log_analytics_id
  tags                       = local.tags
}

resource "azurerm_container_app" "api" {
  name                         = "ca-${local.nome}-api"
  container_app_environment_id = azurerm_container_app_environment.ambiente.id
  resource_group_name          = azurerm_resource_group.ambiente.name
  revision_mode                = "Single"
  tags                         = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.api.id]
  }

  # Segredos lidos do Key Vault pela identidade da API; nenhum valor fica na definição do app
  secret {
    name                = "db-senha"
    key_vault_secret_id = azurerm_key_vault_secret.db_senha.versionless_id
    identity            = azurerm_user_assigned_identity.api.id
  }

  secret {
    name                = "appinsights-connection-string"
    key_vault_secret_id = azurerm_key_vault_secret.appinsights.versionless_id
    identity            = azurerm_user_assigned_identity.api.id
  }

  secret {
    name                = "ghcr-token"
    key_vault_secret_id = azurerm_key_vault_secret.ghcr_token.versionless_id
    identity            = azurerm_user_assigned_identity.api.id
  }

  registry {
    server               = "ghcr.io"
    username             = var.ghcr_usuario
    password_secret_name = "ghcr-token"
  }

  ingress {
    external_enabled = true
    target_port      = 8080
    transport        = "auto"

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = var.replicas_minimas
    max_replicas = var.replicas_maximas

    container {
      name   = "api"
      image  = var.imagem_api
      cpu    = var.cpu
      memory = var.memoria

      env {
        name  = "ORBIT_AMBIENTE"
        value = var.ambiente
      }
      env {
        name  = "ORBIT_DB_URL"
        value = "jdbc:postgresql://${var.postgres_host}:5432/${azurerm_postgresql_flexible_server_database.ambiente.name}?sslmode=require&currentSchema=${postgresql_schema.app.name}"
      }
      env {
        name  = "ORBIT_DB_USUARIO"
        value = postgresql_role.app.name
      }
      env {
        name        = "ORBIT_DB_SENHA"
        secret_name = "db-senha"
      }
      env {
        name  = "ORBIT_ARMAZENAMENTO_TIPO"
        value = "azure-blob"
      }
      env {
        name  = "ORBIT_BLOB_ENDPOINT"
        value = azurerm_storage_account.arquivos.primary_blob_endpoint
      }
      env {
        name  = "ORBIT_BLOB_CONTAINER"
        value = azurerm_storage_container.arquivos.name
      }
      env {
        name  = "AZURE_CLIENT_ID"
        value = azurerm_user_assigned_identity.api.client_id
      }
      env {
        name  = "ORBIT_CORS_ORIGENS"
        value = join(",", concat(["https://${azurerm_static_web_app.web.default_host_name}"], var.dominios_extras_cors))
      }
      env {
        name        = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        secret_name = "appinsights-connection-string"
      }
      env {
        name  = "JAVA_TOOL_OPTIONS"
        value = "-javaagent:/app/agente/applicationinsights-agent.jar"
      }

      startup_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/liveness"
        initial_delay           = 5
        interval_seconds        = 5
        failure_count_threshold = 24
      }

      liveness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/liveness"
        interval_seconds        = 30
        failure_count_threshold = 3
      }

      readiness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/actuator/health/readiness"
        interval_seconds        = 10
        failure_count_threshold = 3
      }
    }

    http_scale_rule {
      name                = "requisicoes"
      concurrent_requests = "50"
    }
  }

  lifecycle {
    # A imagem é trocada pelo pipeline de entrega (KAN-53) a cada versão
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [
    azurerm_role_assignment.kv_leitura_api,
    azurerm_role_assignment.blob_api,
  ]
}

resource "azurerm_static_web_app" "web" {
  name                = "swa-${local.nome}"
  location            = var.regiao_web
  resource_group_name = azurerm_resource_group.ambiente.name
  sku_tier            = "Free"
  sku_size            = "Free"
  tags                = local.tags
}
