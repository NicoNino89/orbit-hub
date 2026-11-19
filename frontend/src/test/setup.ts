/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Preparação dos testes do front-end
 * Objetivo: Ligar os matchers do jest-dom e limpar a tela renderizada depois de cada teste.
 * Decisões: Limpeza explícita no afterEach, para um teste nunca enxergar o DOM do anterior.
 */
import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => cleanup())

let movimentoReduzido = false

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Simular movimento reduzido
 * Objetivo: Permitir que um teste ligue ou desligue a preferência de movimento reduzido.
 * Decisões: O jsdom não implementa matchMedia; a preferência volta a falso depois de cada teste.
 */
export function definirMovimentoReduzido(valor: boolean) {
  movimentoReduzido = valor
}
afterEach(() => definirMovimentoReduzido(false))

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: matchMedia simulado
 * Objetivo: Responder às consultas de mídia nos testes.
 * Decisões: Só prefers-reduced-motion responde de verdade; as demais consultas dão falso.
 */
Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: (consulta: string) => ({
  matches: consulta.includes('prefers-reduced-motion') && movimentoReduzido,
  media: consulta,
  onchange: null,
  addEventListener: () => {},
  removeEventListener: () => {},
  addListener: () => {},
  removeListener: () => {},
  dispatchEvent: () => false,
  }),
})
