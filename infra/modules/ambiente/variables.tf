variable "ambiente" {
  description = "Nome do ambiente: homologacao ou producao."
  type        = string

  validation {
    condition     = contains(["homologacao", "producao"], var.ambiente)
    error_message = "O ambiente deve ser homologacao ou producao."
  }
}

variable "sigla" {
  description = "Sigla curta usada nos nomes dos recursos (hml ou prd)."
  type        = string
}

variable "regiao" {
  description = "Região dos recursos do ambiente."
  type        = string
}

variable "log_analytics_id" {
  description = "Workspace de logs compartilhado."
  type        = string
}

variable "postgres_servidor_id" {
  description = "Servidor PostgreSQL compartilhado."
  type        = string
}

variable "postgres_host" {
  description = "Endereço do servidor PostgreSQL compartilhado."
  type        = string
}

variable "imagem_api" {
  description = "Imagem da API no GHCR, com tag. O pipeline de entrega troca a tag a cada versão."
  type        = string
  default     = "ghcr.io/niconino89/orbit-hub-api:main"
}

variable "imagem_web" {
  description = "Imagem do front-end no GHCR, com tag. O pipeline de entrega troca a tag a cada versão."
  type        = string
  default     = "ghcr.io/niconino89/orbit-hub-web:main"
}

variable "ghcr_usuario" {
  description = "Usuário do GitHub com acesso de leitura ao pacote da imagem (o pacote é privado)."
  type        = string
  default     = "NicoNino89"
}

variable "ghcr_token" {
  description = "Token do GitHub só com read:packages. Vem do segredo GHCR_LEITURA_TOKEN do GitHub."
  type        = string
  sensitive   = true
}

variable "replicas_minimas" {
  description = "0 em homologação (escala a zero). Produção também pode ficar em 0 para economizar crédito."
  type        = number
  default     = 0
}

variable "replicas_maximas" {
  type    = number
  default = 1
}

variable "cpu" {
  type    = number
  default = 0.5
}

variable "memoria" {
  type    = string
  default = "1Gi"
}

variable "cpu_web" {
  type    = number
  default = 0.25
}

variable "memoria_web" {
  type    = string
  default = "0.5Gi"
}

variable "email_alertas" {
  description = "E-mail que recebe os alertas do Azure Monitor. Vem do segredo ALERTAS_EMAIL do GitHub."
  type        = string
  sensitive   = true
}

variable "teste_disponibilidade" {
  description = "Liga o teste de disponibilidade que chama a API a cada 15 minutos. Desligado em homologação e, por ora, em produção (ADR 0006), para a escala a zero funcionar e não gastar crédito."
  type        = bool
  default     = false
}

variable "cota_diaria_app_insights_gb" {
  description = "Teto diário de ingestão do Application Insights."
  type        = number
  default     = 0.1
}

variable "dominios_extras_cors" {
  description = "Outras origens que podem chamar a API, além da web do ambiente."
  type        = list(string)
  default     = []
}

variable "postgres_admin_usuario" {
  description = "Usuário administrador do servidor compartilhado."
  type        = string
}
