variable "estado_grupo" {
  description = "Grupo de recursos do estado remoto do Terraform (criado pelo bootstrap)."
  type        = string
  default     = "rg-orbithub-tfstate"
}

variable "estado_conta" {
  description = "Storage Account do estado remoto do Terraform (criada pelo bootstrap)."
  type        = string
}

variable "regiao_web" {
  description = "Região do Static Web Apps."
  type        = string
  default     = "eastus2"
}

variable "ghcr_token" {
  type      = string
  sensitive = true
}

variable "email_alertas" {
  type      = string
  sensitive = true
}
