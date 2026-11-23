# Application Insights e alertas do Azure Monitor (KAN-56), com os mesmos limites do Prometheus local.

resource "azurerm_application_insights" "api" {
  name                 = "appi-${local.nome}"
  location             = azurerm_resource_group.ambiente.location
  resource_group_name  = azurerm_resource_group.ambiente.name
  workspace_id         = var.log_analytics_id
  application_type     = "java"
  daily_data_cap_in_gb = var.cota_diaria_app_insights_gb
  retention_in_days    = 30
  tags                 = local.tags
}

resource "azurerm_monitor_action_group" "time" {
  name                = "ag-${local.nome}"
  resource_group_name = azurerm_resource_group.ambiente.name
  short_name          = "orbit${var.sigla}"
  tags                = local.tags

  email_receiver {
    name                    = "time"
    email_address           = var.email_alertas
    use_common_alert_schema = true
  }
}

# Taxa de erros: mais de 5% de respostas 5xx em 5 minutos (com pelo menos 10 requisições, para não alarmar à toa)
resource "azurerm_monitor_scheduled_query_rules_alert_v2" "taxa_erros" {
  name                 = "alerta-${local.nome}-taxa-erros"
  location             = azurerm_resource_group.ambiente.location
  resource_group_name  = azurerm_resource_group.ambiente.name
  scopes               = [azurerm_application_insights.api.id]
  description          = "Mais de 5% das requisições da API terminaram em erro 5xx nos últimos 5 minutos."
  severity             = 1
  evaluation_frequency = "PT5M"
  window_duration      = "PT5M"
  tags                 = local.tags

  criteria {
    query                   = <<-KQL
      requests
      | where url !contains "/actuator"
      | summarize total = count(), erros = countif(toint(resultCode) >= 500)
      | where total >= 10
      | extend taxa = todouble(erros) / total
      | where taxa > 0.05
    KQL
    time_aggregation_method = "Count"
    operator                = "GreaterThan"
    threshold               = 0
  }

  action {
    action_groups = [azurerm_monitor_action_group.time.id]
  }
}

# Resposta lenta: p95 acima de 1 segundo
resource "azurerm_monitor_scheduled_query_rules_alert_v2" "resposta_lenta" {
  name                 = "alerta-${local.nome}-resposta-lenta"
  location             = azurerm_resource_group.ambiente.location
  resource_group_name  = azurerm_resource_group.ambiente.name
  scopes               = [azurerm_application_insights.api.id]
  description          = "O p95 do tempo de resposta da API passou de 1 segundo."
  severity             = 2
  evaluation_frequency = "PT15M"
  window_duration      = "PT15M"
  tags                 = local.tags

  criteria {
    query                   = <<-KQL
      requests
      | where url !contains "/actuator"
      | summarize p95 = percentile(duration, 95), total = count()
      | where total >= 10 and p95 > 1000
    KQL
    time_aggregation_method = "Count"
    operator                = "GreaterThan"
    threshold               = 0
  }

  action {
    action_groups = [azurerm_monitor_action_group.time.id]
  }
}

# Contêiner reiniciando em sequência (falha ao subir, falta de memória)
resource "azurerm_monitor_metric_alert" "reinicios" {
  name                = "alerta-${local.nome}-reinicios"
  resource_group_name = azurerm_resource_group.ambiente.name
  scopes              = [azurerm_container_app.api.id]
  description         = "A API reiniciou mais de 3 vezes em 15 minutos."
  severity            = 1
  frequency           = "PT5M"
  window_size         = "PT15M"
  tags                = local.tags

  criteria {
    metric_namespace = "Microsoft.App/containerApps"
    metric_name      = "RestartCount"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 3
  }

  action {
    action_group_id = azurerm_monitor_action_group.time.id
  }
}

# API fora do ar: só em produção. O teste chama a API a cada 15 minutos, o que impediria
# a escala a zero em homologação e gastaria crédito.
resource "azurerm_application_insights_standard_web_test" "saude" {
  count                   = var.teste_disponibilidade ? 1 : 0
  name                    = "disponibilidade-${local.nome}"
  location                = azurerm_resource_group.ambiente.location
  resource_group_name     = azurerm_resource_group.ambiente.name
  application_insights_id = azurerm_application_insights.api.id
  geo_locations           = ["us-va-ash-azr", "latam-br-gru-edge"]
  frequency               = 900
  timeout                 = 60
  enabled                 = true
  retry_enabled           = true
  tags                    = local.tags

  request {
    url = "https://${azurerm_container_app.api.ingress[0].fqdn}/actuator/health"
  }

  validation_rules {
    expected_status_code = 200
  }
}

resource "azurerm_monitor_metric_alert" "fora_do_ar" {
  count               = var.teste_disponibilidade ? 1 : 0
  name                = "alerta-${local.nome}-fora-do-ar"
  resource_group_name = azurerm_resource_group.ambiente.name
  scopes              = [azurerm_application_insights_standard_web_test.saude[0].id, azurerm_application_insights.api.id]
  description         = "O teste de disponibilidade falhou: a API está fora do ar."
  severity            = 0
  tags                = local.tags

  application_insights_web_test_location_availability_criteria {
    web_test_id           = azurerm_application_insights_standard_web_test.saude[0].id
    component_id          = azurerm_application_insights.api.id
    failed_location_count = 1
  }

  action {
    action_group_id = azurerm_monitor_action_group.time.id
  }
}
