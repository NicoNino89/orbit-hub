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
  value = azurerm_container_app.web.name
}

output "web_url" {
  value = "https://${azurerm_container_app.web.ingress[0].fqdn}"
}

output "banco" {
  value = azurerm_postgresql_flexible_server_database.ambiente.name
}

output "key_vault" {
  value = azurerm_key_vault.ambiente.name
}
