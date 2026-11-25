import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Tipos dos indicadores
 * Objetivo: Tipar o indicador de tração com o valor atual, a marca de desatualizado e o histórico.
 * Decisões: Mesmos nomes da API; referência no formato AAAA-MM.
 */
export type FormatoDoIndicador = 'QUANTIDADE' | 'VALOR' | 'PERCENTUAL' | 'VARIACAO'

export interface ValorDoIndicador {
  valor: number
  referencia: string
  registradoEm: string
}

export interface Indicador {
  indicador: string
  rotulo: string
  formato: FormatoDoIndicador
  atual: ValorDoIndicador | null
  desatualizado: boolean
  historico: ValorDoIndicador[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Chamadas dos indicadores
 * Objetivo: Listar os indicadores e declarar um novo valor.
 * Decisões: O servidor confere o formato e o mês.
 */
export function indicadoresDaStartup(id: string) {
  return requisitar<Indicador[]>(`/api/v1/startups/${id}/indicadores`)
}

export function declararIndicador(id: string, indicador: string, valor: number, referencia: string) {
  return requisitar<Indicador>(`/api/v1/startups/${id}/indicadores`, { metodo: 'POST', corpo: { indicador, valor, referencia } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Escrever valor e mês
 * Objetivo: Mostrar o número conforme o formato e o mês de referência em português.
 * Decisões: Reais com centavos; percentuais com o sinal; mês por extenso abreviado.
 */
export function escreverValor(formato: FormatoDoIndicador, valor: number) {
  if (formato === 'VALOR') return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
  if (formato === 'QUANTIDADE') return valor.toLocaleString('pt-BR')
  return `${valor.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`
}

export function escreverMes(referencia: string) {
  const [ano, mes] = referencia.split('-').map(Number)
  return new Date(ano, mes - 1, 1).toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })
}
