variable "estado_grupo" {
  description = "Grupo de recursos do estado remoto do Terraform (criado pelo bootstrap)."
  type        = string
  default     = "rg-orbithub-tfstate"
}

variable "estado_conta" {
  description = "Storage Account do estado remoto do Terraform (criada pelo bootstrap)."
  type        = string
}

variable "assinatura_id" {
  description = "Assinatura da Azure. Vem de ARM_SUBSCRIPTION_ID no pipeline (TF_VAR_assinatura_id)."
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
