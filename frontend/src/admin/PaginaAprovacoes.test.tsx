import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { Proposta } from '../api/aprovacoes'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'c1', nome: 'Caio Admin', email: 'caio@orbithub.com', papeis: ['ADMIN'] },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Proposta de teste
 * Objetivo: Montar uma proposta pendente com os campos que o teste quiser mudar.
 * Decisões: Parâmetro sensível como exemplo.
 */
function proposta(extra: Partial<Proposta>): Proposta {
  return {
    id: 'p1', tipo: 'PARAMETRO_SENSIVEL', tipoDescricao: 'Alteração de parâmetro sensível', objetoTipo: 'parametro',
    objetoId: 'nivel2.validade_dias', resumo: 'nivel2.validade_dias: 90 → 60', dados: { valor: '60' },
    justificativa: 'Pedido do jurídico', propostoPor: 'Bia Admin', propostoEm: '2026-11-22T12:00:00Z', situacao: 'PENDENTE',
    vencida: false, decididoPor: null, decididoEm: null, justificativaDecisao: null, possoDecidir: true, minha: false,
    ...extra,
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Abrir a tela
 * Objetivo: Renderizar a fila com as propostas informadas.
 * Decisões: Lista de ações com uma disponível e uma ainda não.
 */
function abrir(propostas: Proposta[]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/admin/aprovacoes': () => ({
      corpo: {
        propostas,
        acoes: [
          { acao: 'PARAMETRO_SENSIVEL', descricao: 'Alteração de parâmetro sensível', disponivel: true },
          { acao: 'BANIMENTO_DEFINITIVO', descricao: 'Banimento definitivo de usuário ou organização', disponivel: false },
        ],
      },
    }),
    'POST /api/v1/admin/aprovacoes/p1/aprovacao': () => ({ corpo: proposta({ situacao: 'APROVADA' }) }),
    'POST /api/v1/admin/aprovacoes/p2/cancelamento': () => ({ corpo: proposta({ id: 'p2', situacao: 'CANCELADA' }) }),
  })
  render(<MemoryRouter initialEntries={['/admin/aprovacoes']}><Rotas /></MemoryRouter>)
  return api
}

describe('Segunda aprovação (KAN-81)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:52
   * Cartão Jira: KAN-81
   * Título: Aprovar com justificativa
   * Objetivo: Conferir que a aprovação exige justificativa, envia a decisão e avisa que a ação foi executada.
   * Decisões: Vencida aparece em destaque.
   */
  it('aprova com justificativa e destaca a vencida', async () => {
    const { chamadas } = abrir([proposta({ vencida: true })])
    const usuario = userEvent.setup()
    const lista = await screen.findByRole('list', { name: 'Propostas pendentes' })
    const cartao = within(lista).getByRole('listitem')
    expect(within(cartao).getByText('Prazo vencido')).toBeInTheDocument()
    const aprovar = within(cartao).getByRole('button', { name: 'Aprovar e executar' })
    expect(aprovar).toBeDisabled()
    await usuario.type(within(cartao).getByLabelText('Justificativa da decisão'), 'Conferi com o jurídico')
    await usuario.click(aprovar)
    expect(await screen.findByRole('status')).toHaveTextContent('Proposta aprovada: a ação foi executada.')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/aprovacoes/p1/aprovacao')?.corpo)
      .toEqual({ justificativa: 'Conferi com o jurídico' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:52
   * Cartão Jira: KAN-81
   * Título: Própria proposta e papel sem permissão
   * Objetivo: Conferir que a própria proposta só pode ser cancelada e que papel sem permissão não vê botões.
   * Decisões: Ações sem executor aparecem como "chega com a funcionalidade dela".
   */
  it('na própria proposta só cancela e sem permissão não decide', async () => {
    const { chamadas } = abrir([
      proposta({ id: 'p2', minha: true, possoDecidir: false, resumo: 'Minha proposta' }),
      proposta({ id: 'p3', possoDecidir: false, resumo: 'De outro time' }),
    ])
    const usuario = userEvent.setup()
    const [minha, outra] = within(await screen.findByRole('list', { name: 'Propostas pendentes' })).getAllByRole('listitem')
    expect(within(minha).queryByRole('button', { name: 'Aprovar e executar' })).not.toBeInTheDocument()
    expect(within(minha).getByText('Aguardando outra pessoa analisar.')).toBeInTheDocument()
    expect(within(outra).getByText('Seu papel não decide este tipo de ação.')).toBeInTheDocument()
    expect(screen.getByText(/chega com a funcionalidade dela/)).toBeInTheDocument()

    await usuario.click(within(minha).getByRole('button', { name: 'Cancelar proposta' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Proposta cancelada.')
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/admin/aprovacoes/p2/cancelamento')).toBe(true)
  })
})
