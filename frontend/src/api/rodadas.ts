import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Tipos da rodada
 * Objetivo: Tipar a rodada, os termos de captação e o catálogo dos termos por instrumento.
 * Decisões: Mesmos nomes da API; valores em reais.
 */
export type SituacaoDaRodada = 'RASCUNHO' | 'ABERTA' | 'EM_NEGOCIACAO' | 'ENCERRADA'
export type ResultadoDaRodada = 'CONCLUIDA' | 'CANCELADA'

export const NOMES_DA_SITUACAO_DA_RODADA: Record<SituacaoDaRodada, string> = {
  RASCUNHO: 'Rascunho', ABERTA: 'Aberta', EM_NEGOCIACAO: 'Em negociação', ENCERRADA: 'Encerrada',
}

export interface TermosDaRodada {
  estagio: string | null
  instrumento: string | null
  valorAlvo: number | null
  valorMinimo: number | null
  valorMaximo: number | null
  prazo: string | null
  especificos: Record<string, string>
}

export interface Rodada extends TermosDaRodada {
  id: string
  situacao: SituacaoDaRodada
  resultado: ResultadoDaRodada | null
  semLider: boolean
  abertaEm: string | null
  encerradaEm: string | null
  atualizadaEm: string
}

export interface TermoDoCatalogo {
  termo: string
  instrumento: string | null
  rotulo: string
  tipo: 'VALOR' | 'PERCENTUAL' | 'MESES' | 'TEXTO'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Chamadas da rodada
 * Objetivo: Ler o catálogo dos termos e as rodadas, criar, editar, abrir, negociar e encerrar.
 * Decisões: Regras no servidor.
 */
export function termosDasRodadas() {
  return requisitar<TermoDoCatalogo[]>('/api/v1/rodadas/termos')
}

export function rodadasDaStartup(startupId: string) {
  return requisitar<Rodada[]>(`/api/v1/startups/${startupId}/rodadas`)
}

export function criarRodada(startupId: string, termos: TermosDaRodada) {
  return requisitar<Rodada>(`/api/v1/startups/${startupId}/rodadas`, { metodo: 'POST', corpo: termos })
}

export function editarRodada(rodadaId: string, termos: TermosDaRodada) {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}`, { metodo: 'PUT', corpo: termos })
}

export function acaoNaRodada(rodadaId: string, acao: 'abertura' | 'negociacao') {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}/${acao}`, { metodo: 'POST' })
}

export function encerrarRodada(rodadaId: string, resultado: ResultadoDaRodada, motivo: string) {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}/encerramento`, { metodo: 'POST', corpo: { resultado, motivo } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Termos do instrumento
 * Objetivo: Escolher, no catálogo, os termos do instrumento escolhido.
 * Decisões: Sem termos próprios, usa o termo genérico (instrumento nulo).
 */
export function termosDoInstrumento(catalogo: TermoDoCatalogo[], instrumento: string | null) {
  if (!instrumento) return []
  const proprios = catalogo.filter((t) => t.instrumento === instrumento)
  return proprios.length > 0 ? proprios : catalogo.filter((t) => t.instrumento === null)
}
