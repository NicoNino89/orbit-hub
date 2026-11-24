import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Registro da trilha de auditoria
 * Objetivo: Tipar um registro como a API devolve.
 * Decisões: Datas em texto ISO, convertidas só na exibição.
 */
export interface RegistroAuditoria {
  id: string
  atorId: string | null
  atorNome: string | null
  acao: string
  objetoTipo: string | null
  objetoId: string | null
  resultado: 'SUCESSO' | 'FALHA'
  statusHttp: number | null
  detalhe: string | null
  justificativa: string | null
  ocorridoEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Página da trilha
 * Objetivo: Tipar a resposta paginada da consulta.
 * Decisões: Página começa em zero, como na API.
 */
export interface PaginaAuditoria {
  itens: RegistroAuditoria[]
  pagina: number
  tamanho: number
  total: number
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Filtros da trilha
 * Objetivo: Tipar os filtros aceitos pela consulta.
 * Decisões: Datas de início e fim em ISO-8601 (instantes); campos vazios não filtram.
 */
export interface FiltrosAuditoria {
  ator?: string
  acao?: string
  de?: string
  ate?: string
  pagina?: number
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Consultar a trilha
 * Objetivo: Buscar uma página da trilha com os filtros informados.
 * Decisões: Só os filtros preenchidos vão na URL.
 */
export function consultarAuditoria(filtros: FiltrosAuditoria) {
  const parametros = new URLSearchParams()
  Object.entries(filtros).forEach(([chave, valor]) => {
    if (valor !== undefined && valor !== '') parametros.set(chave, String(valor))
  })
  const consulta = parametros.toString()
  return requisitar<PaginaAuditoria>(`/api/v1/admin/auditoria${consulta ? `?${consulta}` : ''}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Ações da trilha
 * Objetivo: Listar os códigos de ação para o filtro.
 * Decisões: Sem cache: a lista muda conforme a equipe trabalha.
 */
export function listarAcoesAuditadas() {
  return requisitar<string[]>('/api/v1/admin/auditoria/acoes')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Pessoas da trilha
 * Objetivo: Listar quem aparece na trilha, para o filtro por pessoa.
 * Decisões: Inclui pessoas já desligadas.
 */
export function listarAtoresAuditados() {
  return requisitar<{ id: string; nome: string | null }[]>('/api/v1/admin/auditoria/atores')
}
