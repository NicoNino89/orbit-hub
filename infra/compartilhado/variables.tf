variable "regiao" {
  description = "Região da Azure permitida pela política do Azure for Students (o script bootstrap/preparar-azure.sh lista as permitidas)."
  type        = string
}

variable "email_alertas" {
  description = "E-mail que recebe os avisos do orçamento. Vem do segredo ALERTAS_EMAIL do GitHub, nunca do código."
  type        = string
  sensitive   = true
}

variable "orcamento_usd" {
  description = "Crédito anual do Azure for Students, em dólares."
  type        = number
  default     = 100
}

variable "orcamento_inicio" {
  description = "Primeiro dia do mês em que o orçamento começa a contar (AAAA-MM-01T00:00:00Z)."
  type        = string
  default     = "2026-11-01T00:00:00Z"
}

variable "cota_diaria_logs_gb" {
  description = "Teto diário de ingestão do Log Analytics, para não estourar o crédito."
  type        = number
  default     = 0.2
}
