import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App (rotas da S0)', () => {
  afterEach(() => {
    window.location.hash = ''
    vi.unstubAllGlobals()
  })

  it('abre a página inicial por padrão', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    render(<App />)
    expect(screen.getByRole('heading', { name: 'A plataforma está sendo construída' })).toBeInTheDocument()
  })

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
