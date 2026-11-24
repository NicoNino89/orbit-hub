import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { simularApi } from '../test/apiSimulada'
import { CHAVE_SESSAO_ATIVA } from './contexto'

const AGORA = new Date('2026-11-24T04:20:00Z')

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:20
 * Cartão Jira: KAN-15
 * Título: Sessão de apoio
 * Objetivo: Montar a resposta de sessão com o fim por inatividade a tantos minutos de agora.
 * Decisões: Usa o relógio simulado do teste.
 */
function sessao(minutosAteOFim: number): SessaoApi {
  return {
    tokenAcesso: 'token',
    expiraEm: new Date(Date.now() + 15 * 60_000).toISOString(),
    sessaoExpiraEm: new Date(Date.now() + minutosAteOFim * 60_000).toISOString(),
    usuario: { id: 'u1', nome: 'Ana Souza', email: 'ana@exemplo.com', papeis: ['EMPREENDEDOR'] },
    termosPendentes: false,
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Abrir já logado
 * Objetivo: Abrir o endereço com a sessão restaurada pelo cookie.
 * Decisões: A marca de sessão ativa faz o provedor renovar ao abrir. O act vazio no fim roda os efeitos pendentes
 *           (com o relógio simulado, a resposta do sino do KAN-146 os deixava para depois do avanço do tempo).
 */
async function abrirLogado(endereco: string) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  render(
    <MemoryRouter initialEntries={[endereco]}>
      <Rotas />
    </MemoryRouter>,
  )
  await screen.findByRole('heading', { name: 'Olá, Ana' })
  await act(async () => {})
}

describe('Sessão por inatividade (KAN-15)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.setSystemTime(AGORA)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Expira e leva ao login
   * Objetivo: Garantir que, sem uso até o fim da sessão, a tela vai para o login com o aviso e com o
   *           endereço de volta.
   * Decisões: Avança 30 minutos no relógio simulado.
   */
  it('leva ao login com aviso quando a sessão expira por inatividade', async () => {
    simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: sessao(30) }) })
    await abrirLogado('/painel?aba=resumo')

    await act(async () => {
      vi.advanceTimersByTime(30 * 60_000)
    })

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.getByText(/Sua sessão expirou por inatividade/)).toBeInTheDocument()
    expect(localStorage.getItem(CHAVE_SESSAO_ATIVA)).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Volta para a página depois de entrar
   * Objetivo: Garantir que, depois de entrar de novo, a pessoa volta ao endereço em que estava.
   * Decisões: O parâmetro "voltar" leva o caminho e a busca da página.
   */
  it('volta para a página em que estava depois de entrar de novo', async () => {
    const { chamadas } = simularApi({
      'POST /api/v1/auth/renovar': () => ({ corpo: sessao(30) }),
      'POST /api/v1/auth/entrar': () => ({ corpo: sessao(30) }),
    })
    await abrirLogado('/painel?aba=resumo')
    await act(async () => {
      vi.advanceTimersByTime(30 * 60_000)
    })
    await screen.findByRole('heading', { name: 'Entrar' })

    fireEvent.change(screen.getByLabelText('E-mail'), { target: { value: 'ana@exemplo.com' } })
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'uma-senha-bem-longa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/auth/entrar')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Uso perto do fim renova
   * Objetivo: Garantir que digitar ou clicar nos últimos minutos renova a sessão e adia o fim.
   * Decisões: Fim em 4 minutos (abaixo do limiar de 5); depois do uso, o fim passa a 30 minutos.
   */
  it('renova a sessão quando há uso perto do fim', async () => {
    let renovacoes = 0
    simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: sessao(++renovacoes === 1 ? 4 : 30) }) })
    await abrirLogado('/painel')

    fireEvent.keyDown(window, { key: 'a' })
    await act(async () => {
      vi.advanceTimersByTime(10 * 60_000)
    })

    expect(renovacoes).toBe(2)
    expect(screen.getByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Uso longe do fim não renova
   * Objetivo: Garantir que o uso no começo da sessão não gera renovações à toa.
   * Decisões: Fim em 30 minutos; um clique não chama a API.
   */
  it('não renova quando a sessão ainda está longe do fim', async () => {
    let renovacoes = 0
    simularApi({
      'POST /api/v1/auth/renovar': () => {
        renovacoes++
        return { corpo: sessao(30) }
      },
    })
    await abrirLogado('/painel')

    fireEvent.pointerDown(window)
    expect(renovacoes).toBe(1)
  })
})
