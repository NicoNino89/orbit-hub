variable "estado_grupo" {
  description = "Grupo de recursos do estado remoto do Terraform (criado pelo bootstrap)."
  type        = string
  default     = "rg-orbithub-tfstate"
}

variable "estado_conta" {
  description = "Storage Account do estado remoto do Terraform (criada pelo bootstrap)."
  type        = string
}

variable "ghcr_token" {
  type      = string
  sensitive = true
}

variable "email_alertas" {
  type      = string
  sensitive = true
}

variable "primeiro_admin_email" {
  description = "Primeiro administrador da plataforma (KAN-75); vazio não cria ninguém."
  type        = string
  default     = ""
  sensitive   = true
}

variable "primeiro_admin_nome" {
  type      = string
  default   = "Administrador"
  sensitive = true
}
