import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

afterEach(() => cleanup())

/**
 * O jsdom não implementa matchMedia. Os testes ligam o movimento reduzido com
 * `definirMovimentoReduzido(true)`.
 */
let movimentoReduzido = false
export function definirMovimentoReduzido(valor: boolean) {
  movimentoReduzido = valor
}
afterEach(() => definirMovimentoReduzido(false))

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
