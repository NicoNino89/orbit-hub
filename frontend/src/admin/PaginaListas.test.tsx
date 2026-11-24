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
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Abrir a tela das listas
 * Objetivo: Renderizar a tela com duas listas e dois itens de segmento (um desativado).
 * Decisões: Respostas simuladas.
 */
function abrir() {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/admin/listas': () => ({
      corpo: [
        { lista: 'SEGMENTO', titulo: 'Segmentos econômicos', comGrupo: false, ativos: 1, total: 2 },
        { lista: 'DOCUMENTO_VERIFICACAO', titulo: 'Documentos de verificação', comGrupo: true, ativos: 9, total: 9 },
      ],
    }),
    'GET /api/v1/admin/listas/SEGMENTO': () => ({
      corpo: [
        { codigo: 'FINTECH', rotulo: 'Serviços financeiros (fintech)', grupo: null, ordem: 2, ativo: true },
        { codigo: 'TURISMO', rotulo: 'Turismo', grupo: null, ordem: 18, ativo: false },
      ],
    }),
    'GET /api/v1/admin/listas/DOCUMENTO_VERIFICACAO': () => ({ corpo: [] }),
    'POST /api/v1/admin/listas/SEGMENTO': () => ({ status: 201, corpo: { codigo: 'ESPORTES', rotulo: 'Esportes', grupo: null, ordem: 19, ativo: true } }),
    'POST /api/v1/admin/listas/SEGMENTO/FINTECH/desativacao': () => ({ corpo: {} }),
    'POST /api/v1/admin/listas/SEGMENTO/TURISMO/reativacao': () => ({ corpo: {} }),
    'GET /api/v1/admin/listas/SEGMENTO/FINTECH/historico': () => ({
      corpo: [{ acao: 'RENOMEADO', rotuloAnterior: 'Fintech', rotuloNovo: 'Serviços financeiros (fintech)', autor: 'Bia Admin',
        ocorridoEm: '2026-11-24T15:00:00Z' }],
    }),
  })
  render(<MemoryRouter initialEntries={['/admin/listas']}><Rotas /></MemoryRouter>)
  return api
}

describe('Listas gerenciáveis (KAN-79)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:06
   * Cartão Jira: KAN-79
   * Título: Itens, inclusão e sem exclusão
   * Objetivo: Conferir os itens com código, o desativado marcado, a inclusão e que não existe botão de excluir.
   * Decisões: Lista de segmentos.
   */
  it('mostra os itens, inclui e não oferece exclusão', async () => {
    const { chamadas } = abrir()
    const usuario = userEvent.setup()
    const itens = within(await screen.findByRole('list', { name: 'Itens da lista' })).getAllByRole('listitem')
    expect(within(itens[0]).getByText('FINTECH')).toBeInTheDocument()
    expect(within(itens[1]).getByText('Desativado')).toBeInTheDocument()
    expect(within(itens[1]).getByRole('button', { name: 'Reativar' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /excluir|apagar/i })).not.toBeInTheDocument()

    await usuario.type(screen.getByLabelText('Novo item'), 'Esportes')
    await usuario.click(screen.getByRole('button', { name: 'Incluir' }))
    expect(await screen.findByRole('status')).toHaveTextContent('"Esportes" incluído.')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/listas/SEGMENTO')?.corpo).toEqual({ rotulo: 'Esportes' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:06
   * Cartão Jira: KAN-79
   * Título: Desativar, histórico e grupo
   * Objetivo: Conferir a desativação, o histórico do item e o campo de grupo nas listas com grupo.
   * Decisões: Troca para a lista de documentos de verificação.
   */
  it('desativa, mostra o histórico e pede grupo quando a lista usa', async () => {
    const { chamadas } = abrir()
    const usuario = userEvent.setup()
    const fintech = within(await screen.findByRole('list', { name: 'Itens da lista' })).getAllByRole('listitem')[0]
    await usuario.click(within(fintech).getByRole('button', { name: 'Histórico' }))
    expect(await within(fintech).findByText(/Renomeado \(Fintech → Serviços financeiros \(fintech\)\), por Bia Admin/)).toBeInTheDocument()
    await usuario.click(within(fintech).getByRole('button', { name: 'Desativar' }))
    expect(await screen.findByRole('status')).toHaveTextContent('desativado')
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/admin/listas/SEGMENTO/FINTECH/desativacao')).toBe(true)

    expect(screen.queryByLabelText('Grupo')).not.toBeInTheDocument()
    await usuario.selectOptions(screen.getByLabelText('Lista'), 'DOCUMENTO_VERIFICACAO')
    expect(await screen.findByLabelText('Grupo')).toBeInTheDocument()
  })
})
