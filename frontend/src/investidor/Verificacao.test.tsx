import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'a1', nome: 'Ana Investidora', email: 'ana@exemplo.com', papeis: ['INVESTIDOR'] },
}

const EXIGIDOS = [
  { codigo: 'PF_DOCUMENTO_COM_FOTO', rotulo: 'Documento com foto' },
  { codigo: 'PF_CPF', rotulo: 'CPF' },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Abrir a verificação
 * Objetivo: Renderizar a tela de verificação com as rotas simuladas.
 * Decisões: Papel ativo de investidor.
 */
function abrir(rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/organizacoes/minhas': () => ({ corpo: [] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={['/investidor/verificacao']}><Rotas /></MemoryRouter>)
  return api
}

describe('Verificação de identidade (KAN-62)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:19
   * Cartão Jira: KAN-62
   * Título: Enviar documentos e pedir análise
   * Objetivo: Conferir o selo, o botão bloqueado enquanto falta documento, o envio de um arquivo e o pedido de análise.
   * Decisões: Depois do envio, o servidor simulado devolve os dois documentos.
   */
  it('envia os documentos e manda para análise', async () => {
    let enviados = [{ id: 'd1', tipo: 'PF_DOCUMENTO_COM_FOTO', nome: 'rg.pdf', formato: 'application/pdf', tamanho: 10, enviadoEm: '2026-11-24T17:00:00Z' }]
    let situacao = 'RASCUNHO'
    const { chamadas } = abrir({
      'GET /api/v1/verificacoes/minha': () => ({
        corpo: { sujeito: 'PESSOA', selo: situacao === 'EM_ANALISE' ? 'EM_ANALISE' : 'NAO_VERIFICADO', podeEnviar: true, exigidos: EXIGIDOS,
          pedido: { id: 'p1', situacao, enviadoEm: null, motivo: null, observacao: null, validoAte: null }, documentos: enviados },
      }),
      'POST /api/v1/verificacoes/documentos': () => {
        enviados = [...enviados, { id: 'd2', tipo: 'PF_CPF', nome: 'cpf.png', formato: 'image/png', tamanho: 10, enviadoEm: '2026-11-24T17:01:00Z' }]
        return { status: 201, corpo: {} }
      },
      'POST /api/v1/verificacoes/envio': () => { situacao = 'EM_ANALISE'; return { corpo: {} } },
    })
    expect(await screen.findByText('Não verificado')).toBeInTheDocument()
    const enviar = screen.getByRole('button', { name: 'Enviar para análise' })
    expect(enviar).toBeDisabled()
    await userEvent.upload(screen.getByLabelText('Arquivo de CPF'), new File(['%PDF'], 'cpf.png', { type: 'image/png' }))
    expect(await screen.findByText('CPF: enviado.')).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/verificacoes/documentos')).toBe(true)
    await userEvent.click(screen.getByRole('button', { name: 'Enviar para análise' }))
    expect(await screen.findByText('Pedido enviado para análise.')).toBeInTheDocument()
    expect(await screen.findByText('Em análise pela equipe de verificação')).toBeInTheDocument()
    expect(within(screen.getByRole('heading', { level: 1 })).getByText('Em análise')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:19
   * Cartão Jira: KAN-62
   * Título: Membro de organização
   * Objetivo: Conferir que quem não pode enviar vê o aviso, sem a lista de envio.
   * Decisões: Selo verificado da organização.
   */
  it('mostra o aviso a quem não pode enviar', async () => {
    abrir({
      'GET /api/v1/verificacoes/minha': () => ({
        corpo: { sujeito: 'ORGANIZACAO', selo: 'VERIFICADO', podeEnviar: false, exigidos: EXIGIDOS,
          pedido: { id: 'p1', situacao: 'VERIFICADO', enviadoEm: '2026-11-01T10:00:00Z', motivo: 'DOCUMENTOS_CONFEREM', observacao: null,
            validoAte: '2027-11-01T10:00:00Z' }, documentos: [] },
      }),
    })
    expect(await screen.findByText('Só o administrador da organização envia a verificação dela.')).toBeInTheDocument()
    expect(screen.getByText('Verificado', { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByText(/Válida até/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar para análise' })).not.toBeInTheDocument()
  })
})
