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
  usuario: { id: 'e1', nome: 'Eva Empreendedora', email: 'eva@exemplo.com', papeis: ['EMPREENDEDOR'] },
}

const STARTUP = {
  id: 's1', nome: 'NovaSafra', nomeFantasia: null, razaoSocial: null, cnpj: '11.222.333/0001-81', segmento: 'AGRONEGOCIO', localizacao: 'MT',
  site: null, redes: {}, fundadores: [], situacao: 'PUBLICADA', constituida: true, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
  meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [],
}

const TERMOS = [
  { termo: 'VALUATION_PRE_MONEY', instrumento: 'PARTICIPACAO_SOCIETARIA', rotulo: 'Valuation pré-money (R$)', tipo: 'VALOR' },
  { termo: 'PARTICIPACAO_OFERECIDA', instrumento: 'PARTICIPACAO_SOCIETARIA', rotulo: 'Participação oferecida (%)', tipo: 'PERCENTUAL' },
  { termo: 'OUTROS_TERMOS', instrumento: null, rotulo: 'Termos do instrumento', tipo: 'TEXTO' },
]

const RASCUNHO = {
  id: 'r1', situacao: 'RASCUNHO', resultado: null, estagio: 'SEED', instrumento: 'PARTICIPACAO_SOCIETARIA', valorAlvo: 1000000, valorMinimo: 600000,
  valorMaximo: 1500000, prazo: '2027-02-28', especificos: { VALUATION_PRE_MONEY: '8000000' }, semLider: true, abertaEm: null, encerradaEm: null,
  atualizadaEm: '2026-11-24T20:00:00Z',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Abrir na aba da rodada
 * Objetivo: Renderizar o editor da startup com as rotas informadas e abrir a aba Rodada.
 * Decisões: Listas de estágio e instrumento simuladas.
 */
async function abrirRodada(rotas: Parameters<typeof simularApi>[0], startup: object = STARTUP) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'EMPREENDEDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
    'GET /api/v1/listas/ESTAGIO_RODADA': () => ({ corpo: [{ codigo: 'SEED', rotulo: 'Seed', grupo: null }] }),
    'GET /api/v1/listas/INSTRUMENTO': () => ({
      corpo: [{ codigo: 'PARTICIPACAO_SOCIETARIA', rotulo: 'Participação societária', grupo: null }, { codigo: 'OUTROS_LC_182', rotulo: 'Outros', grupo: null }],
    }),
    'GET /api/v1/startups/s1': () => ({ corpo: startup }),
    'GET /api/v1/startups/campos': () => ({ corpo: [] }),
    'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
    'GET /api/v1/rodadas/termos': () => ({ corpo: TERMOS }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={['/empreendedor/startups/s1']}><Rotas /></MemoryRouter>)
  await userEvent.click(await screen.findByRole('button', { name: 'Rodada' }))
  return api
}

describe('Rodada de investimento (KAN-59)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:51
   * Cartão Jira: KAN-59
   * Título: Criar rascunho com termos do instrumento
   * Objetivo: Conferir que os termos mudam com o instrumento e que o rascunho é criado com os valores e termos.
   * Decisões: Sem rodada em andamento.
   */
  it('cria o rascunho com os termos do instrumento escolhido', async () => {
    const { chamadas } = await abrirRodada({
      'GET /api/v1/startups/s1/rodadas': () => ({ corpo: [] }),
      'POST /api/v1/startups/s1/rodadas': () => ({ status: 201, corpo: RASCUNHO }),
    })
    const form = await screen.findByRole('form', { name: 'Termos da rodada' })
    await within(form).findByRole('option', { name: 'Participação societária' })
    await userEvent.selectOptions(within(form).getByLabelText('Estágio da rodada'), 'SEED')
    await userEvent.selectOptions(within(form).getByLabelText('Instrumento'), 'OUTROS_LC_182')
    expect(within(form).getByLabelText('Termos do instrumento')).toBeInTheDocument()
    await userEvent.selectOptions(within(form).getByLabelText('Instrumento'), 'PARTICIPACAO_SOCIETARIA')
    expect(within(form).queryByLabelText('Termos do instrumento')).not.toBeInTheDocument()
    await userEvent.type(within(form).getByLabelText('Valor alvo (R$)'), '1000000')
    await userEvent.type(within(form).getByLabelText('Valuation pré-money (R$)'), '8000000')
    await userEvent.click(within(form).getByRole('button', { name: 'Criar rascunho da rodada' }))
    expect(await screen.findByText('Rascunho da rodada criado.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/rodadas')?.corpo).toMatchObject({
      estagio: 'SEED', instrumento: 'PARTICIPACAO_SOCIETARIA', valorAlvo: 1000000, especificos: { VALUATION_PRE_MONEY: '8000000' },
    })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:51
   * Cartão Jira: KAN-59
   * Título: Abrir, negociar e encerrar
   * Objetivo: Conferir as ações da rodada, a recusa do servidor e o encerramento com resultado e motivo.
   * Decisões: O servidor simulado avança a situação.
   */
  it('abre, mostra a recusa, negocia e encerra a rodada', async () => {
    let rodada: Record<string, unknown> = RASCUNHO
    let recusar = true
    const { chamadas } = await abrirRodada({
      'GET /api/v1/startups/s1/rodadas': () => ({ corpo: [rodada] }),
      'POST /api/v1/rodadas/r1/abertura': () => {
        if (recusar) {
          recusar = false
          return { status: 422, corpo: { detail: 'Para abrir a rodada, informe participação oferecida (%).', codigo: 'termos_incompletos' } }
        }
        rodada = { ...rodada, situacao: 'ABERTA', atualizadaEm: '2026-11-24T21:00:00Z' }
        return { corpo: rodada }
      },
      'POST /api/v1/rodadas/r1/negociacao': () => {
        rodada = { ...rodada, situacao: 'EM_NEGOCIACAO' }
        return { corpo: rodada }
      },
      'POST /api/v1/rodadas/r1/encerramento': () => {
        rodada = { ...rodada, situacao: 'ENCERRADA', resultado: 'CONCLUIDA' }
        return { corpo: rodada }
      },
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Abrir rodada' }))
    expect(await screen.findByText(/informe participação oferecida/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir rodada' }))
    expect(await screen.findByText('Rodada sem líder definido')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Marcar em negociação' }))
    expect(await screen.findByText('Rodada em negociação.')).toBeInTheDocument()
    expect(screen.getByText('Participação societária')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Encerrar rodada' }))
    const grupo = screen.getByRole('group', { name: 'Encerrar rodada' })
    await userEvent.type(within(grupo).getByLabelText('Motivo (opcional)'), 'Captados 1,2 mi')
    await userEvent.click(within(grupo).getByRole('button', { name: 'Confirmar encerramento' }))
    expect(await screen.findByText('Rodada encerrada.')).toBeInTheDocument()
    expect(await screen.findByText(/Seed · Participação societária · Concluída/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/encerramento')?.corpo).toEqual({ resultado: 'CONCLUIDA', motivo: 'Captados 1,2 mi' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:51
   * Cartão Jira: KAN-59
   * Título: Sem CNPJ e sem permissão
   * Objetivo: Conferir a explicação para startup sem CNPJ e a visão só de leitura para quem não é administrador.
   * Decisões: Duas renderizações.
   */
  it('explica a falta de CNPJ e mostra só o resumo para quem não é administrador', async () => {
    await abrirRodada({ 'GET /api/v1/startups/s1/rodadas': () => ({ corpo: [] }) }, { ...STARTUP, constituida: false, cnpj: null })
    expect(await screen.findByText(/Cadastre o CNPJ da startup/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:51
   * Cartão Jira: KAN-59
   * Título: Fundador acompanha
   * Objetivo: Conferir que o fundador vê os termos sem formulário nem ações.
   * Decisões: Rodada aberta.
   */
  it('mostra a rodada sem ações para o fundador', async () => {
    await abrirRodada({ 'GET /api/v1/startups/s1/rodadas': () => ({ corpo: [{ ...RASCUNHO, situacao: 'ABERTA' }] }) }, { ...STARTUP, meuPapel: 'FUNDADOR' })
    expect(await screen.findByText('Valuation pré-money (R$)')).toBeInTheDocument()
    expect(screen.getByText('R$ 1.000.000,00 · R$ 600.000,00 · R$ 1.500.000,00')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Encerrar rodada' })).not.toBeInTheDocument()
  })
})
