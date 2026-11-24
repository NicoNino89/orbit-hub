import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Parametro } from '../api/parametros'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'b1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis: ['ADMIN'] },
}

const PARAMETROS: Parametro[] = [
  { chave: 'conexao.expiracao_dias', grupo: 'conexao', descricao: 'Manifestação sem resposta expira', tipo: 'INTEIRO',
    unidade: 'dias', valor: '30', valorPadrao: '30', minimo: 7, maximo: 90, opcoes: [], sensivel: false, atualizadoEm: null },
  { chave: 'conexao.lembrete_dias', grupo: 'conexao', descricao: 'Lembretes ao empreendedor', tipo: 'LISTA_INTEIROS',
    unidade: 'dias', valor: '7,21', valorPadrao: '7,21', minimo: 1, maximo: 60, opcoes: [], sensivel: false, atualizadoEm: null },
  { chave: 'nivel2.validade_dias', grupo: 'conexao', descricao: 'Validade do acesso ao Nível 2', tipo: 'INTEIRO',
    unidade: 'dias', valor: '90', valorPadrao: '90', minimo: 7, maximo: 365, opcoes: [], sensivel: true, atualizadoEm: null },
  { chave: 'catalogo.frequencia_edicoes', grupo: 'catalogo', descricao: 'Frequência das edições', tipo: 'OPCAO',
    unidade: '', valor: 'MENSAL', valorPadrao: 'MENSAL', minimo: null, maximo: null, opcoes: ['SEMANAL', 'MENSAL'],
    sensivel: false, atualizadoEm: null },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Abrir a tela de parâmetros
 * Objetivo: Renderizar a tela com o catálogo simulado.
 * Decisões: A alteração do sensível volta como proposta.
 */
function abrir() {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/admin/parametros': () => ({ corpo: PARAMETROS }),
    'PUT /api/v1/admin/parametros/conexao.expiracao_dias': () => ({
      corpo: { aplicado: true, propostaId: null, parametro: { ...PARAMETROS[0], valor: '45' } },
    }),
    'PUT /api/v1/admin/parametros/nivel2.validade_dias': () => ({
      corpo: { aplicado: false, propostaId: 'p1', parametro: PARAMETROS[2] },
    }),
    'GET /api/v1/admin/parametros/conexao.expiracao_dias/historico': () => ({
      corpo: [{ valorAnterior: '30', valorNovo: '45', autor: 'Bia Admin', aprovadoPor: null,
        justificativa: 'Pedido do atendimento', alteradoEm: '2026-11-24T14:00:00Z' }],
    }),
  })
  render(<MemoryRouter initialEntries={['/admin/parametros']}><Rotas /></MemoryRouter>)
  return api
}

describe('Parâmetros (KAN-78)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:08
   * Cartão Jira: KAN-78
   * Título: Catálogo por grupo
   * Objetivo: Conferir os grupos, os valores com unidade, a lista legível e a marca de sensível.
   * Decisões: "7,21" aparece como "7 e 21 dias".
   */
  it('mostra o catálogo por grupo com unidades e sensíveis', async () => {
    abrir()
    const conexoes = await screen.findByRole('list', { name: 'Conexões e Nível 2' })
    expect(within(conexoes).getByText('30 dias')).toBeInTheDocument()
    expect(within(conexoes).getByText('7 e 21 dias')).toBeInTheDocument()
    expect(within(within(conexoes).getAllByRole('listitem')[2]).getByText('Sensível')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Catálogo' })).toHaveTextContent('MENSAL')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:08
   * Cartão Jira: KAN-78
   * Título: Alterar com justificativa e ver histórico
   * Objetivo: Conferir que a alteração exige justificativa, envia valor e motivo e que o histórico aparece.
   * Decisões: Parâmetro comum, aplicado na hora.
   */
  it('altera um parâmetro comum e mostra o histórico', async () => {
    const { chamadas } = abrir()
    const usuario = userEvent.setup()
    const linha = within(await screen.findByRole('list', { name: 'Conexões e Nível 2' })).getAllByRole('listitem')[0]
    await usuario.click(within(linha).getByRole('button', { name: 'Alterar' }))
    expect(within(linha).getByText('Aceita de 7 a 90 dias.')).toBeInTheDocument()
    const novo = within(linha).getByLabelText('Novo valor')
    await usuario.clear(novo)
    await usuario.type(novo, '45')
    expect(within(linha).getByRole('button', { name: 'Salvar' })).toBeDisabled()
    await usuario.type(within(linha).getByLabelText('Justificativa'), 'Pedido do atendimento')
    await usuario.click(within(linha).getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByRole('status')).toHaveTextContent('conexao.expiracao_dias alterado para 45 dias.')
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/admin/parametros/conexao.expiracao_dias')?.corpo)
      .toEqual({ valor: '45', justificativa: 'Pedido do atendimento' })

    const atualizada = within(await screen.findByRole('list', { name: 'Conexões e Nível 2' })).getAllByRole('listitem')[0]
    await usuario.click(within(atualizada).getByRole('button', { name: 'Histórico' }))
    expect(await within(atualizada).findByText(/30 → 45, por Bia Admin\. Motivo: Pedido do atendimento/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:08
   * Cartão Jira: KAN-78
   * Título: Sensível vai para segunda aprovação
   * Objetivo: Conferir que o botão do sensível diz que vai para aprovação e que o aviso explica que ainda não vale.
   * Decisões: O valor exibido continua o antigo.
   */
  it('envia o sensível para a segunda aprovação', async () => {
    abrir()
    const usuario = userEvent.setup()
    const linha = within(await screen.findByRole('list', { name: 'Conexões e Nível 2' })).getAllByRole('listitem')[2]
    await usuario.click(within(linha).getByRole('button', { name: 'Alterar' }))
    const novo = within(linha).getByLabelText('Novo valor')
    await usuario.clear(novo)
    await usuario.type(novo, '60')
    await usuario.type(within(linha).getByLabelText('Justificativa'), 'Pedido do jurídico')
    await usuario.click(within(linha).getByRole('button', { name: 'Enviar para segunda aprovação' }))
    expect(await screen.findByRole('status')).toHaveTextContent('foi para a segunda aprovação')
  })
})
