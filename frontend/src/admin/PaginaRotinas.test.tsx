import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'b1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis: ['ADMIN'] },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Abrir a tela das rotinas
 * Objetivo: Renderizar a tela com uma rotina que falhou e uma pausada.
 * Decisões: Respostas simuladas.
 */
function abrir() {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/admin/rotinas': () => ({
      corpo: [
        { codigo: 'R12', descricao: 'Limpeza e retenção', cron: '0 0 3 * * *', modulo: 'identidade',
          parametros: ['retencao.tokens_dias'], pausada: false, proximaExecucao: '2026-11-25T06:00:00Z',
          ultimaExecucao: { disparo: 'AUTOMATICO', iniciadaEm: '2026-11-24T06:00:00Z', terminadaEm: '2026-11-24T06:00:01Z',
            itens: 2, erros: 1, resultado: 'FALHA', mensagem: 'Erro: IllegalStateException' } },
        { codigo: 'R15', descricao: 'Propostas paradas', cron: '0 5 * * * *', modulo: 'administracao',
          parametros: [], pausada: true, proximaExecucao: '2026-11-24T16:05:00Z', ultimaExecucao: null },
      ],
    }),
    'POST /api/v1/admin/rotinas/R12/execucao': () => ({ corpo: { itens: 5, erros: 0, mensagem: 'ok' } }),
    'POST /api/v1/admin/rotinas/R15/retomada': () => ({ status: 200, corpo: {} }),
  })
  render(<MemoryRouter initialEntries={['/admin/rotinas']}><Rotas /></MemoryRouter>)
  return api
}

describe('Rotinas automáticas (KAN-104)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:17
   * Cartão Jira: KAN-104
   * Título: Status, execução manual e retomada
   * Objetivo: Conferir a falha e a pausa na lista, a execução manual com o resultado e a retomada.
   * Decisões: Rotina pausada mostra "Retomar".
   */
  it('mostra o status e executa ou retoma', async () => {
    const { chamadas } = abrir()
    const usuario = userEvent.setup()
    const [r12, r15] = within(await screen.findByRole('list', { name: 'Rotinas' })).getAllByRole('listitem')
    expect(within(r12).getByText('Falha')).toBeInTheDocument()
    expect(within(r12).getByText(/Erro: IllegalStateException/)).toBeInTheDocument()
    expect(within(r15).getByText('Pausada')).toBeInTheDocument()
    expect(within(r15).getByText('Ainda não rodou.')).toBeInTheDocument()

    await usuario.click(within(r12).getByRole('button', { name: 'Executar agora' }))
    expect(await screen.findByRole('status')).toHaveTextContent('R12 executada: 5 item(ns), 0 erro(s).')
    const lista = within(await screen.findByRole('list', { name: 'Rotinas' })).getAllByRole('listitem')
    await usuario.click(within(lista[1]).getByRole('button', { name: 'Retomar' }))
    expect(await screen.findByText('R15 retomada.')).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/admin/rotinas/R15/retomada')).toBe(true)
  })
})
