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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Abrir o backoffice
 * Objetivo: Renderizar o endereço com a sessão da analista e as rotas informadas.
 * Decisões: Sessão restaurada pelo cookie.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }), ...rotas })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Conflito de interesse (KAN-87)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:43
   * Cartão Jira: KAN-87
   * Título: Declarar e remover vínculo
   * Objetivo: Conferir o envio da declaração (tipo, alvo, descrição), a lista e a remoção.
   * Decisões: O servidor simulado acrescenta o vínculo à lista.
   */
  it('declara e remove um vínculo', async () => {
    let lista: unknown[] = []
    const { chamadas } = abrir('/admin/vinculos', {
      'GET /api/v1/admin/vinculos/meus': () => ({ corpo: lista }),
      'POST /api/v1/admin/vinculos': ({ corpo }) => {
        lista = [{ id: 'v1', membroId: 'l1', membro: null, tipo: 'PESSOA', alvo: 'irma@exemplo.com', descricao: (corpo as { descricao: string }).descricao,
          criadoEm: '2026-11-24T17:00:00Z' }]
        return { status: 201, corpo: lista[0] }
      },
      'DELETE /api/v1/admin/vinculos/v1': () => { lista = []; return { status: 204 } },
    })
    const form = await screen.findByRole('form', { name: 'Declarar vínculo' })
    await userEvent.selectOptions(within(form).getByLabelText('Tipo'), 'PESSOA')
    await userEvent.type(within(form).getByLabelText('E-mail'), 'irma@exemplo.com')
    await userEvent.type(within(form).getByLabelText('Qual é o vínculo'), 'É minha irmã')
    await userEvent.click(within(form).getByRole('button', { name: 'Declarar vínculo' }))
    const declarados = await screen.findByRole('list', { name: 'Vínculos declarados' })
    expect(within(declarados).getByText(/irma@exemplo.com/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/vinculos')?.corpo)
      .toEqual({ tipo: 'PESSOA', alvo: 'irma@exemplo.com', descricao: 'É minha irmã' })
    await userEvent.click(within(declarados).getByRole('button', { name: 'Remover' }))
    expect(await screen.findByText('Nenhum vínculo declarado.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:43
   * Cartão Jira: KAN-87
   * Título: Conflito na fila e impedimento
   * Objetivo: Conferir a marca de conflito na fila e o envio do impedimento com o motivo.
   * Decisões: Pedido em análise sem responsável.
   */
  it('marca o conflito na fila e declara impedimento', async () => {
    const pedido = { id: 'p1', situacao: 'EM_ANALISE', criadoEm: '2026-11-24T12:00:00Z', enviadoEm: '2026-11-24T12:00:00Z',
      decididoEm: null, motivo: null, observacao: null, validoAte: null, prazoComplementoAte: null, encaminhadoModeracao: false,
      documentosApagados: false }
    const { chamadas } = abrir('/admin/verificacao', {
      'GET /api/v1/admin/verificacoes': () => ({
        corpo: [{ id: 'p1', sujeitoTipo: 'PESSOA', sujeito: 'Ana', enviadoEm: '2026-11-24T12:00:00Z', prazo: '2026-11-26T12:00:00Z',
          atrasado: false, responsavel: null, conflitoComigo: true, semAnalistaLivre: true }],
      }),
      'GET /api/v1/admin/verificacoes/p1': () => ({
        corpo: { pedido, sujeitoTipo: 'PESSOA', sujeito: 'Ana', responsavel: null, documentos: [], historico: [], motivos: [] },
      }),
      'POST /api/v1/admin/verificacoes/p1/impedimento': () => ({ corpo: pedido }),
    })
    expect(await screen.findByText('Você tem conflito')).toBeInTheDocument()
    expect(screen.getByText('Sem analista livre: administrador')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Ana' }))
    const form = await screen.findByRole('form', { name: 'Impedimento' })
    await userEvent.type(within(form).getByLabelText(/Não posso analisar/), 'Conheço a investidora.')
    await userEvent.click(within(form).getByRole('button', { name: 'Declarar impedimento' }))
    expect(await screen.findByText('Impedimento registrado.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/verificacoes/p1/impedimento')?.corpo)
      .toEqual({ motivo: 'Conheço a investidora.' })
  })
})
