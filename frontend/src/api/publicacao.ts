import { requisitar } from './cliente'
import type { SecaoDaStartup, SituacaoDaStartup } from './startups'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Tipos da publicação
 * Objetivo: Tipar o progresso do cadastro e o histórico das situações.
 * Decisões: Mesmos nomes da API.
 */
export interface ProgressoDaStartup {
  percentual: number
  podeEnviar: boolean
  pendencias: string[]
  secoes: { secao: SecaoDaStartup; preenchidos: number; total: number; completa: boolean }[]
  situacao: SituacaoDaStartup
}

export interface MudancaDeSituacao {
  de: SituacaoDaStartup
  para: SituacaoDaStartup
  motivo: string | null
  pelaPlataforma: boolean
  em: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Chamadas da publicação
 * Objetivo: Ler o progresso e o histórico, enviar, cancelar o envio e despublicar.
 * Decisões: As ações devolvem a situação nova.
 */
export function progressoDaStartup(id: string) {
  return requisitar<ProgressoDaStartup>(`/api/v1/startups/${id}/progresso`)
}

export function historicoDeSituacoes(id: string) {
  return requisitar<MudancaDeSituacao[]>(`/api/v1/startups/${id}/situacoes`)
}

export function enviarParaPublicacao(id: string) {
  return requisitar<{ situacao: SituacaoDaStartup }>(`/api/v1/startups/${id}/envio`, { metodo: 'POST' })
}

export function cancelarEnvio(id: string) {
  return requisitar<{ situacao: SituacaoDaStartup }>(`/api/v1/startups/${id}/envio/cancelamento`, { metodo: 'POST' })
}

export function despublicar(id: string, motivo: string) {
  return requisitar<{ situacao: SituacaoDaStartup }>(`/api/v1/startups/${id}/despublicacao`, { metodo: 'POST', corpo: { motivo } })
}
