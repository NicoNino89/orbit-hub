import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { simularApi } from '../test/apiSimulada'
import { ConducaoDaRodada } from './Conducao'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109
 * Título: Testes da condução da rodada (startup)
 * Objetivo: Ver o excesso de demanda, aprovar o convidado do líder, convidar o líder, prorrogar, concluir com o investidor
 *           marcado e cancelar.
 * Decisões: Componente isolado, com a API simulada.
 */
describe('Condução da rodada (KAN-59, KAN-64, KAN-109)', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 14:23
   * Cartão Jira: KAN-59, KAN-64, KAN-109
   * Título: Ações do fundador administrador
   * Objetivo: Percorrer as ações do painel e conferir o que vai para a API.
   * Decisões: Um participante ativo e um convidado.
   */
  it('conduz a rodada pelo painel', async () => {
    const { chamadas } = simularApi({
      'GET /api/v1/rodadas/r1/painel': () => ({ corpo: { id: 'r1', situacao: 'EM_NEGOCIACAO', resultado: null, politica: 'LIDER_PRIMEIRO', lider: null,
        liderConvidado: null, somaIntencoes: 350000, valorAlvo: 250000, valorMinimo: 100000, valorMaximo: 300000, excessoDeDemanda: true, prazo: '2026-12-30',
        prorrogacoes: 0, encerramentoPedido: true, valorCaptado: null, instrumentoUtilizado: null, dataFechamento: null, investimentos: [],
        participantes: [{ id: 'p1', investidor: 'Ana', situacao: 'ATIVO', intencao: 200000, condicoes: null, entrouEm: '2026-11-25T12:00:00Z' },
          { id: 'p2', investidor: 'Cai', situacao: 'AGUARDANDO_APROVACAO', intencao: null, condicoes: null, entrouEm: '2026-11-25T12:00:00Z' }] } }),
      'POST /api/v1/rodadas/r1/participantes/p2/decisao': () => ({ corpo: {} }),
      'POST /api/v1/rodadas/r1/participantes/p1/lideranca': () => ({ status: 204 }),
      'PUT /api/v1/rodadas/r1/politica': () => ({ corpo: { politica: 'PROPORCIONAL' } }),
      'POST /api/v1/rodadas/r1/prorrogacao': () => ({ corpo: {} }),
      'POST /api/v1/rodadas/r1/conclusao': () => ({ corpo: {} }),
      'POST /api/v1/rodadas/r1/cancelamento': () => ({ corpo: {} }),
    })
    render(<ConducaoDaRodada rodadaId="r1" startupId="s1" admin />)
    const painel = await screen.findByRole('region', { name: 'Condução da rodada' })
    expect(painel).toHaveTextContent('Excesso de demanda')
    expect(painel).toHaveTextContent('o líder pediu o encerramento')
    await userEvent.click(within(painel).getByRole('button', { name: 'Aprovar' }))
    await userEvent.click(within(painel).getByRole('button', { name: 'Convidar para liderar' }))
    await userEvent.selectOptions(within(painel).getByLabelText('Política de alocação'), 'PROPORCIONAL')
    await userEvent.type(within(painel).getByLabelText('Novo prazo'), '2027-01-15')
    await userEvent.click(within(painel).getByRole('button', { name: 'Prorrogar' }))
    await userEvent.click(within(painel).getByLabelText('Investiu: Ana'))
    await userEvent.type(within(painel).getByLabelText('Valor de Ana'), '200000')
    await userEvent.type(within(painel).getByLabelText('Valor captado (R$)'), '200000')
    await userEvent.type(within(painel).getByLabelText('Data do fechamento'), '2026-12-20')
    await userEvent.click(within(painel).getByRole('button', { name: 'Concluir e registrar o resultado' }))
    await userEvent.type(within(painel).getByLabelText('Motivo do cancelamento'), 'Mudança')
    await userEvent.click(within(painel).getByRole('button', { name: 'Cancelar a rodada' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/conclusao')?.corpo).toEqual({ valorCaptado: 200000, instrumento: '',
      dataFechamento: '2026-12-20', investidores: [{ participanteId: 'p1', valor: 200000 }] })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/participantes/p2/decisao')?.corpo).toEqual({ aprovar: true })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/prorrogacao')?.corpo).toEqual({ prazo: '2027-01-15' })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/cancelamento')?.corpo).toEqual({ motivo: 'Mudança' })
  })
})
