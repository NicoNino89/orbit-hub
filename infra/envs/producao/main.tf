# Ambiente de producao (KAN-55). Valores não sensíveis ficam aqui; os sensíveis vêm de variáveis de ambiente TF_VAR_*.

terraform {
  required_version = ">= 1.14"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.14"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "~> 2.14"
    }
    postgresql = {
      source  = "cyrilgdn/postgresql"
      version = "~> 1.27"
    }
  }

  backend "azurerm" {
    key              = "producao.tfstate"
    use_azuread_auth = true
  }
}

provider "azurerm" {
  features {}
  storage_use_azuread = true
}

data "terraform_remote_state" "compartilhado" {
  backend = "azurerm"
  config = {
    resource_group_name  = var.estado_grupo
    storage_account_name = var.estado_conta
    container_name       = "tfstate"
    key                  = "compartilhado.tfstate"
    use_azuread_auth     = true
  }
}

locals {
  compartilhado = data.terraform_remote_state.compartilhado.outputs
}

provider "postgresql" {
  host      = local.compartilhado.postgres_host
  port      = 5432
  username  = local.compartilhado.postgres_admin_usuario
  password  = local.compartilhado.postgres_admin_senha
  sslmode   = "require"
  superuser = false
}

module "ambiente" {
  source = "../../modules/ambiente"

  ambiente               = "producao"
  sigla                  = "prd"
  regiao                 = local.compartilhado.regiao
  log_analytics_id       = local.compartilhado.log_analytics_id
  postgres_servidor_id   = local.compartilhado.postgres_servidor_id
  postgres_host          = local.compartilhado.postgres_host
  postgres_admin_usuario = local.compartilhado.postgres_admin_usuario
  replicas_minimas       = 0
  teste_disponibilidade  = true
  ghcr_token             = var.ghcr_token
  email_alertas          = var.email_alertas
}
