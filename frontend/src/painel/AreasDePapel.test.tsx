import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import { simularApi } from '../test/apiSimulada'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Sessão com papéis
 * Objetivo: Montar uma sessão com os papéis informados.
 * Decisões: Sem expiração próxima.
 */
function sessao(papeis: Papel[]): SessaoApi {
  return {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Ana Souza', email: 'ana@exemplo.com', papeis },
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Abrir logado
 * Objetivo: Abrir o endereço com a sessão restaurada e as rotas simuladas.
 * Decisões: A renovação devolve a sessão com os papéis do teste.
 */
function abrir(endereco: string, papeis: Papel[], extras: Parameters<typeof simularApi>[0] = {}) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: sessao(papeis) }), ...extras })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Áreas por papel (KAN-34)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:50
   * Cartão Jira: KAN-34
   * Título: Área de outro papel
   * Objetivo: Garantir que o empreendedor não vê a área do investidor e nem chama a API dela.
   * Decisões: A tela orienta a ativar o papel.
   */
  it('não mostra a área de investidor para quem só é empreendedor', async () => {
    const { chamadas } = abrir('/investidor', ['EMPREENDEDOR'])
    expect(await screen.findByRole('heading', { name: 'Área exclusiva' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para Minha conta' })).toHaveAttribute('href', '/conta')
    expect(chamadas.some((c) => c.chave === 'GET /api/v1/investidores/eu')).toBe(false)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:33
   * Cartão Jira: KAN-21
   * Título: Área do próprio papel
   * Objetivo: Garantir que o empreendedor vê a própria área com os dados da API.
   * Decisões: Lista de startups vazia (KAN-21 lê as startups de verdade).
   */
  it('mostra a área do empreendedor com os dados da API', async () => {
    abrir('/empreendedor', ['EMPREENDEDOR'], {
      'GET /api/v1/startups/minhas': () => ({ corpo: [] }),
    })
    expect(await screen.findByText(/ainda não cadastrou nenhuma startup/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:50
   * Cartão Jira: KAN-34
   * Título: Recusa da API
   * Objetivo: Garantir que um 403 da API aparece como mensagem, mesmo com a tela liberada.
   * Decisões: Simula o papel desativado em outro aparelho.
   */
  it('mostra a recusa do servidor', async () => {
    abrir('/investidor', ['INVESTIDOR'], {
      'GET /api/v1/investidores/eu': () => ({ status: 403, corpo: { detail: 'Você não tem permissão para fazer isso.', codigo: 'acesso_negado' } }),
    })
    expect(await screen.findByRole('alert')).toHaveTextContent('Você não tem permissão para fazer isso.')
  })
})
