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
  usuario: { id: 'l1', nome: 'Lia Analista', email: 'lia@orbithub.com', papeis: ['ANALISTA_VERIFICACAO'] },
}

const PEDIDO = {
  id: 'p1', situacao: 'EM_ANALISE', criadoEm: '2026-11-20T12:00:00Z', enviadoEm: '2026-11-20T12:00:00Z', decididoEm: null, motivo: null,
  observacao: null, validoAte: null, prazoComplementoAte: null, encaminhadoModeracao: false, documentosApagados: false,
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Abrir o backoffice da verificação
 * Objetivo: Renderizar o endereço com a sessão da analista e as rotas simuladas.
 * Decisões: Fila com um pedido atrasado.
 */
function abrir(endereco: string) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/admin/verificacoes': () => ({
      corpo: [{ id: 'p1', sujeitoTipo: 'PESSOA', sujeito: 'Ana Investidora', enviadoEm: '2026-11-20T12:00:00Z',
        prazo: '2026-11-24T12:00:00Z', atrasado: true, responsavel: null }],
    }),
    'GET /api/v1/admin/verificacoes/p1': () => ({
      corpo: { pedido: PEDIDO, sujeitoTipo: 'PESSOA', sujeito: 'Ana Investidora', responsavel: 'Lia Analista',
        documentos: [{ id: 'd1', tipo: 'PF_CPF', nome: 'cpf.pdf', formato: 'application/pdf', tamanho: 10, enviadoEm: '2026-11-20T12:00:00Z' }],
        historico: [],
        motivos: [
          { codigo: 'DOCUMENTOS_CONFEREM', rotulo: 'Documentos conferem', grupo: 'VERIFICADO' },
          { codigo: 'DOCUMENTO_ILEGIVEL', rotulo: 'Documento ilegível', grupo: 'COMPLEMENTO' },
          { codigo: 'IDENTIDADE_DE_TERCEIRO', rotulo: 'Identidade de outra pessoa', grupo: 'FRAUDE' },
        ] },
    }),
    'POST /api/v1/admin/verificacoes/p1/assumir': () => ({ corpo: PEDIDO }),
    'POST /api/v1/admin/verificacoes/p1/decisao': () => ({ corpo: { ...PEDIDO, situacao: 'PENDENTE_COMPLEMENTO' } }),
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Fila de verificação (KAN-83)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:33
   * Cartão Jira: KAN-83
   * Título: Fila
   * Objetivo: Conferir o item do menu, o pedido atrasado e o link para a análise.
   * Decisões: Analista sem outros papéis.
   */
  it('mostra a fila com o atraso', async () => {
    abrir('/admin/verificacao')
    const lista = await screen.findByRole('list', { name: 'Pedidos em análise' })
    expect(within(lista).getByRole('link', { name: 'Ana Investidora' })).toHaveAttribute('href', '/admin/verificacao/p1')
    expect(within(lista).getByText(/Atrasado/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Verificação' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:33
   * Cartão Jira: KAN-83
   * Título: Decidir
   * Objetivo: Conferir os motivos filtrados pela decisão, o aviso de fraude e o envio do complemento com a observação.
   * Decisões: Assume antes de decidir.
   */
  it('assume e pede complemento com o motivo da lista', async () => {
    const { chamadas } = abrir('/admin/verificacao/p1')
    await userEvent.click(await screen.findByRole('button', { name: 'Assumir este pedido' }))
    expect(await screen.findByText('Pedido assumido.')).toBeInTheDocument()
    const form = screen.getByRole('form', { name: 'Decisão' })
    expect(within(form).queryByRole('option', { name: 'Documento ilegível' })).not.toBeInTheDocument()
    await userEvent.click(within(form).getByLabelText('Recusado'))
    await userEvent.selectOptions(within(form).getByLabelText('Motivo'), 'IDENTIDADE_DE_TERCEIRO')
    expect(within(form).getByText(/encaminhado à administração/)).toBeInTheDocument()
    await userEvent.click(within(form).getByLabelText('Pendente de complemento'))
    await userEvent.selectOptions(within(form).getByLabelText('Motivo'), 'DOCUMENTO_ILEGIVEL')
    await userEvent.type(within(form).getByLabelText(/O que falta enviar/), 'Envie o CPF legível.')
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar decisão' }))
    expect(await screen.findByText('Decisão registrada.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/verificacoes/p1/decisao')?.corpo)
      .toEqual({ resultado: 'PENDENTE_COMPLEMENTO', motivo: 'DOCUMENTO_ILEGIVEL', observacao: 'Envie o CPF legível.' })
  })
})
