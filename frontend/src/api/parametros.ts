import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Parâmetro configurável
 * Objetivo: Tipar um parâmetro como a API devolve.
 * Decisões: Unidade já em texto ("dias", "horas úteis"...).
 */
export interface Parametro {
  chave: string
  grupo: string
  descricao: string
  tipo: 'INTEIRO' | 'LISTA_INTEIROS' | 'OPCAO'
  unidade: string
  valor: string
  valorPadrao: string
  minimo: number | null
  maximo: number | null
  opcoes: string[]
  sensivel: boolean
  atualizadoEm: string | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Alteração no histórico
 * Objetivo: Tipar uma alteração de parâmetro.
 * Decisões: Aprovador só nos sensíveis.
 */
export interface AlteracaoDeParametro {
  valorAnterior: string
  valorNovo: string
  autor: string
  aprovadoPor: string | null
  justificativa: string
  alteradoEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Listar parâmetros
 * Objetivo: Buscar o catálogo.
 * Decisões: Sem cache.
 */
export function listarParametros() {
  return requisitar<Parametro[]>('/api/v1/admin/parametros')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Alterar parâmetro
 * Objetivo: Pedir a alteração com justificativa.
 * Decisões: Sensível volta "aplicado": false (foi para segunda aprovação).
 */
export function alterarParametro(chave: string, valor: string, justificativa: string) {
  return requisitar<{ aplicado: boolean; propostaId: string | null; parametro: Parametro }>(
    `/api/v1/admin/parametros/${encodeURIComponent(chave)}`, { metodo: 'PUT', corpo: { valor, justificativa } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Histórico de um parâmetro
 * Objetivo: Buscar as alterações de um parâmetro.
 * Decisões: Da mais recente para a mais antiga.
 */
export function historicoDoParametro(chave: string) {
  return requisitar<AlteracaoDeParametro[]>(`/api/v1/admin/parametros/${encodeURIComponent(chave)}/historico`)
}
