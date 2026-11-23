# Banco e usuário próprios do ambiente, no servidor compartilhado.
# O usuário da aplicação só enxerga o próprio banco: homologação não lê produção, e vice-versa.

resource "azurerm_postgresql_flexible_server_database" "ambiente" {
  name      = "orbithub_${var.sigla}"
  server_id = var.postgres_servidor_id
  charset   = "UTF8"
  collation = "en_US.utf8"

  lifecycle {
    prevent_destroy = false
  }
}

resource "random_password" "db_app" {
  length           = 32
  special          = true
  override_special = "-_.~"
}

resource "postgresql_role" "app" {
  name     = "orbit_${var.sigla}"
  login    = true
  password = random_password.db_app.result
}

# Ninguém além do dono conecta no banco do ambiente
resource "postgresql_grant" "revogar_public" {
  database    = azurerm_postgresql_flexible_server_database.ambiente.name
  role        = "public"
  object_type = "database"
  privileges  = []
}

resource "postgresql_grant" "conectar" {
  database    = azurerm_postgresql_flexible_server_database.ambiente.name
  role        = postgresql_role.app.name
  object_type = "database"
  privileges  = ["CONNECT", "CREATE", "TEMPORARY"]
}

# O Flyway roda com o usuário da aplicação e cria as tabelas no schema próprio do ambiente
resource "postgresql_schema" "app" {
  name     = "orbit"
  database = azurerm_postgresql_flexible_server_database.ambiente.name
  owner    = postgresql_role.app.name

  depends_on = [postgresql_grant_role.admin_age_como_app]
}

# No PostgreSQL 16+, quem cria um papel não pode agir como ele por padrão. O administrador precisa
# receber o papel da aplicação para criar o schema em nome dela.
resource "postgresql_grant_role" "admin_age_como_app" {
  role       = var.postgres_admin_usuario
  grant_role = postgresql_role.app.name
}
