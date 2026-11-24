output "regiao" {
  value = azurerm_resource_group.compartilhado.location
}

output "log_analytics_id" {
  value = azurerm_log_analytics_workspace.logs.id
}

output "postgres_servidor_id" {
  value = azurerm_postgresql_flexible_server.principal.id
}

output "postgres_servidor_nome" {
  value = azurerm_postgresql_flexible_server.principal.name
}

output "postgres_host" {
  value = azurerm_postgresql_flexible_server.principal.fqdn
}

output "postgres_admin_usuario" {
  value = azurerm_postgresql_flexible_server.principal.administrator_login
}

output "postgres_admin_senha" {
  value     = random_password.postgres_admin.result
  sensitive = true
}

output "container_apps_ambiente_id" {
  value = azapi_resource.container_apps.id
}

output "container_apps_dominio" {
  value = azapi_resource.container_apps.output.properties.defaultDomain
}
