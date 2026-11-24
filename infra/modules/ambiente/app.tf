# API e front-end no Azure Container Apps (plano de consumo, escala a zero), no mesmo ambiente e na mesma região.
# O Static Web Apps não existe nas regiões que a política da assinatura permite (ADR 0003).

# O ambiente do Container Apps é um só para homologação e produção e fica na camada compartilhado (ADR 0007):
# a Azure for Students permite um ambiente por região. O módulo recebe o id e o domínio padrão dele.

# Até 22/11/2026 o ambiente era um azurerm_container_app_environment; o recurso antigo sai do estado sem ser apagado.
removed {
  from = azurerm_container_app_environment.ambiente

  lifecycle {
    destroy = false
  }
}

resource "azurerm_container_app" "api" {
  name                         = "ca-${local.nome}-api"
  container_app_environment_id = var.container_apps_ambiente_id
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
    name                = "jwt-chave"
    key_vault_secret_id = azurerm_key_vault_secret.jwt_chave.versionless_id
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
        name        = "ORBIT_JWT_CHAVE"
        secret_name = "jwt-chave"
      }
      env {
        name  = "ORBIT_WEB_URL"
        value = local.web_url
      }
      # Sem provedor de e-mail na nuvem por enquanto (decisão pendente): os e-mails só são registrados no
      # log e a confirmação de e-mail fica desligada, para os cadastros funcionarem (KAN-12).
      env {
        name  = "ORBIT_EMAIL_TIPO"
        value = "registro"
      }
      env {
        name  = "ORBIT_CONTA_CONFIRMAR_EMAIL"
        value = tostring(var.confirmar_email)
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
  web_url  = "https://${local.web_nome}.${var.container_apps_dominio}"
}

# Front-end: nginx sem root servindo o build do Vite. Repassa /api para a API, então o navegador
# fala com uma origem só. A imagem é a mesma do Docker Compose e a mesma em homologação e produção.
resource "azurerm_container_app" "web" {
  name                         = local.web_nome
  container_app_environment_id = var.container_apps_ambiente_id
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
