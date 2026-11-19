import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Testes das rotas da S0
 * Objetivo: Verificar qual tela abre para cada endereço.
 * Decisões: fetch substituído por uma promessa que nunca resolve, para a página inicial não chamar a rede.
 */
describe('App (rotas da S0)', () => {
  afterEach(() => {
    window.location.hash = ''
    vi.unstubAllGlobals()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Página inicial por padrão
   * Objetivo: Garantir que sem hash a página inicial seja exibida.
   * Decisões: Identifica a tela pelo título, como o usuário a reconhece.
   */
  it('abre a página inicial por padrão', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    render(<App />)
    expect(screen.getByRole('heading', { name: 'A plataforma está sendo construída' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Rota da prova de conceito
   * Objetivo: Garantir que #/poc-catalogo abra o catálogo.
   * Decisões: Dispara o hashchange dentro de act, como o navegador faria.
   */
  it('abre a prova de conceito do catálogo pelo endereço #/poc-catalogo', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    render(<App />)
    act(() => {
      window.location.hash = '#/poc-catalogo'
      window.dispatchEvent(new HashChangeEvent('hashchange'))
    })
    expect(screen.getByRole('heading', { name: 'Catálogo com virada de páginas' })).toBeInTheDocument()
  })
})
