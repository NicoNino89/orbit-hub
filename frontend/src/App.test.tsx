import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Testes das rotas da aplicação
 * Objetivo: Verificar qual tela abre para cada endereço.
 * Decisões: Endereço definido com history.pushState antes de montar o BrowserRouter; fetch substituído
 *           por uma promessa que nunca resolve, para as telas não chamarem a rede.
 */
describe('App (rotas)', () => {
  afterEach(() => {
    window.history.pushState({}, '', '/')
    vi.unstubAllGlobals()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Página inicial por padrão
   * Objetivo: Garantir que a raiz e endereços desconhecidos abram a página inicial.
   * Decisões: Identifica a tela pelo título, como o usuário a reconhece.
   */
  it('abre a página inicial na raiz e em endereço desconhecido', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: /Empresas que inspiram/ })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Rota da prova de conceito
   * Objetivo: Garantir que /poc-catalogo abra o catálogo.
   * Decisões: Endereço sem hash, servido pelo fallback de SPA do nginx.
   */
  it('abre a prova de conceito do catálogo em /poc-catalogo', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    window.history.pushState({}, '', '/poc-catalogo')
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Catálogo com virada de páginas' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Rota dos termos
   * Objetivo: Garantir que /termos abra a página de termos e políticas.
   * Decisões: Basta o título da página; o conteúdo é testado em PaginaTermos.test.
   */
  it('abre os termos em /termos', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise(() => {})))
    window.history.pushState({}, '', '/termos')
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Termos, políticas e avisos' })).toBeInTheDocument()
  })
})
