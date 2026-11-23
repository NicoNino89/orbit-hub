# API e front-end no Azure Container Apps (plano de consumo, escala a zero), no mesmo ambiente e na mesma região.
# O Static Web Apps não existe nas regiões que a política da assinatura permite (ADR 0003).

# O ambiente é criado pelo azapi porque a Azure passou a criar ambientes no modo "Express" quando o modo
# não é informado, e o azurerm ainda não tem como informá-lo. O modo Express não aceita segredos lidos do
# Key Vault (ADR 0005). "WorkloadProfiles" só com o perfil Consumption mantém o plano de consumo: escala a
# zero e nenhum custo fixo.
data "azurerm_log_analytics_workspace" "logs" {
  name                = element(split("/", var.log_analytics_id), 8)
  resource_group_name = element(split("/", var.log_analytics_id), 4)
}

resource "azapi_resource" "ambiente" {
  type      = "Microsoft.App/managedEnvironments@2026-07-01"
  name      = "cae-${local.nome}"
  location  = azurerm_resource_group.ambiente.location
  parent_id = azurerm_resource_group.ambiente.id
  tags      = local.tags

  body = {
    properties = {
      environmentMode = "WorkloadProfiles"
      workloadProfiles = [{
        name                = "Consumption"
        workloadProfileType = "Consumption"
      }]
      appLogsConfiguration = {
        destination = "log-analytics"
        logAnalyticsConfiguration = {
          customerId = data.azurerm_log_analytics_workspace.logs.workspace_id
        }
      }
    }
  }

  # A chave do workspace vai só na requisição; não fica no estado nem entra na comparação
  sensitive_body = {
    properties = {
      appLogsConfiguration = {
        logAnalyticsConfiguration = {
          sharedKey = data.azurerm_log_analytics_workspace.logs.primary_shared_key
        }
      }
    }
  }

  response_export_values = ["properties.defaultDomain"]
}

# Até 22/11/2026 o ambiente era um azurerm_container_app_environment. Em homologação ele já existe e é
# importado para o azapi (bloco import em envs/homologacao); aqui o recurso antigo sai do estado sem ser apagado.
removed {
  from = azurerm_container_app_environment.ambiente

  lifecycle {
    destroy = false
  }
}

resource "azurerm_container_app" "api" {
  name                         = "ca-${local.nome}-api"
  container_app_environment_id = azapi_resource.ambiente.id
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
        value = join(",", concat([local.web_url], var.dominios_extras_cors))
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

locals {
  # Endereço da web montado a partir do domínio do ambiente, para a API conhecer a origem do CORS
  # sem depender do recurso da web (que, por sua vez, depende do endereço da API).
  web_nome = "ca-${local.nome}-web"
  web_url  = "https://${local.web_nome}.${azapi_resource.ambiente.output.properties.defaultDomain}"
}

# Front-end: nginx sem root servindo o build do Vite. Repassa /api para a API, então o navegador
# fala com uma origem só. A imagem é a mesma do Docker Compose e a mesma em homologação e produção.
resource "azurerm_container_app" "web" {
  name                         = local.web_nome
  container_app_environment_id = azapi_resource.ambiente.id
  resource_group_name          = azurerm_resource_group.ambiente.name
  revision_mode                = "Single"
  tags                         = local.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.web.id]
  }

  secret {
    name                = "ghcr-token"
    key_vault_secret_id = azurerm_key_vault_secret.ghcr_token.versionless_id
    identity            = azurerm_user_assigned_identity.web.id
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
      name   = "web"
      image  = var.imagem_web
      cpu    = var.cpu_web
      memory = var.memoria_web

      env {
        name  = "ORBIT_API_UPSTREAM"
        value = "https://${azurerm_container_app.api.ingress[0].fqdn}"
      }

      liveness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/"
        interval_seconds        = 30
        failure_count_threshold = 3
      }

      readiness_probe {
        transport               = "HTTP"
        port                    = 8080
        path                    = "/"
        interval_seconds        = 10
        failure_count_threshold = 3
      }
    }

    http_scale_rule {
      name                = "requisicoes"
      concurrent_requests = "100"
    }
  }

  lifecycle {
    # A imagem é trocada pelo pipeline de entrega (KAN-53) a cada versão
    ignore_changes = [template[0].container[0].image]
  }

  depends_on = [azurerm_role_assignment.kv_leitura_web]
}
