import { requisitar } from './cliente'
import type { SecaoDaStartup } from './startups'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Campo do catálogo
 * Objetivo: Tipar um campo das seções do cadastro como a API descreve.
 * Decisões: Os tipos crescem com as próximas histórias.
 */
export interface CampoDoCatalogo {
  campo: string
  secao: SecaoDaStartup
  rotulo: string
  tipo: 'TEXTO' | 'SIM_NAO' | 'LISTA' | 'PERCENTUAL' | 'INTEIRO' | 'ESCALA'
  limite: number | null
  lista: string | null
  ajuda: string | null
  /** Níveis da escala com a explicação (só no tipo ESCALA, KAN-26). */
  opcoes?: { codigo: string; rotulo: string; explicacao: string }[] | null
}

export type ValoresDasSecoes = Record<string, string>

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Chamadas das seções
 * Objetivo: Buscar o catálogo e os valores, e salvar uma seção.
 * Decisões: Campo vazio vai como texto vazio e o servidor apaga.
 */
export function catalogoDeCampos() {
  return requisitar<CampoDoCatalogo[]>('/api/v1/startups/campos')
}

export function valoresDasSecoes(id: string) {
  return requisitar<ValoresDasSecoes>(`/api/v1/startups/${id}/secoes`)
}

export function salvarSecao(id: string, secao: SecaoDaStartup, valores: ValoresDasSecoes) {
  return requisitar<ValoresDasSecoes>(`/api/v1/startups/${id}/secoes/${secao}`, { metodo: 'PUT', corpo: valores })
}
