output "grupo_de_recursos" {
  value = azurerm_resource_group.ambiente.name
}

output "api_nome" {
  value = azurerm_container_app.api.name
}

output "api_url" {
  value = "https://${azurerm_container_app.api.ingress[0].fqdn}"
}

output "web_nome" {
  value = azurerm_static_web_app.web.name
}

output "web_url" {
  value = "https://${azurerm_static_web_app.web.default_host_name}"
}

output "web_token_publicacao" {
  description = "Token de publicação do Static Web Apps, usado pelo pipeline de entrega."
  value       = azurerm_static_web_app.web.api_key
  sensitive   = true
}

output "banco" {
  value = azurerm_postgresql_flexible_server_database.ambiente.name
}

output "key_vault" {
  value = azurerm_key_vault.ambiente.name
}
