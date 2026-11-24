import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import type { Membro } from '../api/equipe'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Sessão de teste
 * Objetivo: Montar a renovação de sessão com os papéis pedidos.
 * Decisões: A conta de teste é a "b1".
 */
function sessao(papeis: Papel[]): SessaoApi {
  return {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'b1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis },
  }
}

const MEMBROS: Membro[] = [
  { id: 'b1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis: ['ADMIN'], situacao: 'ATIVO', segundoFatorAtivo: true,
    criadoEm: '2026-11-01T00:00:00Z', desligadoEm: null },
  { id: 'c1', nome: 'Cris Curadora', email: 'cris@orbithub.com', papeis: ['CURADOR'], situacao: 'ATIVO', segundoFatorAtivo: true,
    criadoEm: '2026-11-01T00:00:00Z', desligadoEm: null },
  { id: 'e1', nome: 'Edu Jurídico', email: 'edu@orbithub.com', papeis: ['JURIDICO'], situacao: 'CONVIDADO', segundoFatorAtivo: false,
    criadoEm: '2026-11-20T00:00:00Z', desligadoEm: null },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Abrir a tela logado
 * Objetivo: Renderizar as rotas num endereço do backoffice com a API simulada.
 * Decisões: Revisão vencida por padrão.
 */
function abrir(endereco: string, papeis: Papel[]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao(papeis) }),
    'GET /api/v1/admin/equipe': () => ({ corpo: MEMBROS }),
    'GET /api/v1/admin/equipe/revisao': () => ({
      corpo: { ultimaEm: null, ultimoRevisor: null, proximaEm: '2026-11-24T00:00:00Z', vencida: true, intervaloMeses: 3 },
    }),
    'POST /api/v1/admin/equipe': ({ corpo }) => ({ status: 201, corpo: { ...MEMBROS[2], ...(corpo as object), id: 'n1' } }),
    'POST /api/v1/admin/equipe/c1/desligamento': () => ({ corpo: { casosDevolvidos: 2 } }),
    'PUT /api/v1/admin/equipe/c1/papeis': () => ({ corpo: MEMBROS[1] }),
    'POST /api/v1/admin/equipe/revisao': () => ({ status: 201, corpo: {} }),
    'GET /api/v1/admin/auditoria': () => ({ corpo: { itens: [], pagina: 0, tamanho: 50, total: 0 } }),
    'GET /api/v1/admin/auditoria/acoes': () => ({ corpo: [] }),
    'GET /api/v1/admin/auditoria/atores': () => ({ corpo: [] }),
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Equipe interna (KAN-75)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Lista e ações por situação
   * Objetivo: Conferir a lista, as ações por situação e que a própria conta não tem "desligar".
   * Decisões: Convidado tem "reenviar convite".
   */
  it('lista a equipe com as ações de cada situação', async () => {
    abrir('/admin/equipe', ['ADMIN'])
    const lista = await screen.findByRole('list', { name: 'Membros da equipe' })
    const [bia, cris, edu] = within(lista).getAllByRole('listitem')
    expect(within(bia).getByText('(você)')).toBeInTheDocument()
    expect(within(bia).queryByRole('button', { name: 'Desligar' })).not.toBeInTheDocument()
    expect(within(cris).getByRole('button', { name: 'Desligar' })).toBeInTheDocument()
    expect(within(cris).getByText('Segundo fator ativo')).toBeInTheDocument()
    expect(within(edu).getByText('Convidado')).toBeInTheDocument()
    expect(within(edu).getByRole('button', { name: 'Reenviar convite' })).toBeInTheDocument()
    expect(screen.getByText(/A revisão está vencida/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/senha/i)).not.toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Convidar com papéis
   * Objetivo: Conferir que o convite exige papel e envia nome, e-mail e papéis escolhidos.
   * Decisões: Sem papel, a tela avisa antes de chamar a API.
   */
  it('convida um membro com os papéis escolhidos', async () => {
    const { chamadas } = abrir('/admin/equipe', ['ADMIN'])
    const usuario = userEvent.setup()
    const formulario = await screen.findByRole('form', { name: 'Convidar membro' })
    await usuario.type(within(formulario).getByLabelText('Nome'), 'Davi Suporte')
    await usuario.type(within(formulario).getByLabelText('E-mail da equipe'), 'davi@orbithub.com')
    await usuario.click(within(formulario).getByRole('button', { name: 'Enviar convite' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Escolha pelo menos um papel.')

    await usuario.click(within(formulario).getByLabelText('Suporte nível 1'))
    await usuario.click(within(formulario).getByLabelText('Suporte nível 2'))
    await usuario.click(within(formulario).getByRole('button', { name: 'Enviar convite' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Convite enviado para davi@orbithub.com.')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/equipe')?.corpo)
      .toEqual({ nome: 'Davi Suporte', email: 'davi@orbithub.com', papeis: ['SUPORTE_N1', 'SUPORTE_N2'] })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Desligar pede motivo e confirmação
   * Objetivo: Conferir que o desligamento só sai com motivo e mostra quantos casos voltaram para a fila.
   * Decisões: A confirmação é na própria tela.
   */
  it('desliga só com motivo e avisa os casos devolvidos', async () => {
    const { chamadas } = abrir('/admin/equipe', ['ADMIN'])
    const usuario = userEvent.setup()
    const lista = await screen.findByRole('list', { name: 'Membros da equipe' })
    const cris = within(lista).getAllByRole('listitem')[1]
    await usuario.click(within(cris).getByRole('button', { name: 'Desligar' }))
    const confirmar = within(cris).getByRole('button', { name: 'Confirmar desligamento' })
    expect(confirmar).toBeDisabled()
    await usuario.type(within(cris).getByLabelText('Motivo do desligamento'), 'Fim do contrato')
    await usuario.click(confirmar)
    expect(await screen.findByRole('status')).toHaveTextContent('Cris Curadora foi desligado. 2 caso(s) voltaram para a fila.')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/equipe/c1/desligamento')?.corpo)
      .toEqual({ justificativa: 'Fim do contrato' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Trocar papéis e confirmar revisão
   * Objetivo: Conferir a troca de papéis e o registro da revisão de acessos.
   * Decisões: A troca envia o conjunto inteiro.
   */
  it('troca papéis e confirma a revisão', async () => {
    const { chamadas } = abrir('/admin/equipe', ['ADMIN'])
    const usuario = userEvent.setup()
    const lista = await screen.findByRole('list', { name: 'Membros da equipe' })
    const cris = within(lista).getAllByRole('listitem')[1]
    await usuario.click(within(cris).getByRole('button', { name: 'Trocar papéis' }))
    await usuario.click(within(cris).getByLabelText('Moderador'))
    await usuario.click(within(cris).getByRole('button', { name: 'Salvar papéis' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Papéis de Cris Curadora atualizados.')
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/admin/equipe/c1/papeis')?.corpo)
      .toEqual({ papeis: ['CURADOR', 'MODERADOR'] })

    await usuario.click(screen.getByRole('button', { name: 'Confirmar revisão dos acessos atuais' }))
    expect(await screen.findByText('Revisão de acessos registrada.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Menu por papel
   * Objetivo: Conferir que o encarregado de dados vê a auditoria, mas não a gestão da equipe.
   * Decisões: O servidor também recusa; aqui é a camada da interface.
   */
  it('mostra no menu só o que o papel permite', async () => {
    const { chamadas } = abrir('/admin/equipe', ['ENCARREGADO_DADOS'])
    expect(await screen.findByRole('heading', { name: 'Área da equipe' })).toBeInTheDocument()
    const menu = screen.getByRole('navigation', { name: 'Backoffice' })
    expect(within(menu).getByRole('link', { name: 'Auditoria' })).toBeInTheDocument()
    expect(within(menu).queryByRole('link', { name: 'Equipe' })).not.toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'GET /api/v1/admin/equipe')).toBe(false)
  })
})
