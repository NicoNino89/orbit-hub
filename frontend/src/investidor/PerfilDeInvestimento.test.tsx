import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_ATUACAO } from '../sessao/atuacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'a1', nome: 'Ana Investidora', email: 'ana@exemplo.com', papeis: ['INVESTIDOR'] },
}

const LISTAS = {
  'GET /api/v1/listas/TIPO_INVESTIDOR': () => ({ corpo: [
    { codigo: 'ANJO', rotulo: 'Investidor-anjo', grupo: null }, { codigo: 'FUNDO_VC', rotulo: 'Fundo de Venture Capital', grupo: null },
    { codigo: 'OUTROS', rotulo: 'Outros', grupo: null },
  ] }),
  'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [{ codigo: 'FINTECH', rotulo: 'Fintech', grupo: null }] }),
  'GET /api/v1/listas/ESTAGIO_RODADA': () => ({ corpo: [{ codigo: 'SEED', rotulo: 'Seed', grupo: null }] }),
  'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [
    { codigo: 'SP', rotulo: 'São Paulo', grupo: 'SUDESTE' }, { codigo: 'MG', rotulo: 'Minas Gerais', grupo: 'SUDESTE' },
  ] }),
  'GET /api/v1/listas/HORIZONTE_INVESTIMENTO': () => ({ corpo: [{ codigo: 'DE_5_A_8_ANOS', rotulo: 'De 5 a 8 anos', grupo: null }] }),
  'GET /api/v1/listas/INTERESSE_ESTRATEGICO': () => ({ corpo: [{ codigo: 'IMPACTO', rotulo: 'Impacto socioambiental', grupo: null }] }),
  'GET /api/v1/listas/INSTRUMENTO': () => ({ corpo: [
    { codigo: 'MUTUO_CONVERSIVEL', rotulo: 'Mútuo conversível', grupo: null },
    { codigo: 'PARTICIPACAO_SOCIETARIA', rotulo: 'Participação societária', grupo: null },
  ] }),
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Abrir o perfil
 * Objetivo: Renderizar a tela do perfil com as listas e as rotas informadas. Endereço opcional (perfil ou onboarding).
 * Decisões: Papel ativo de investidor.
 */
function abrir(rotas: Parameters<typeof simularApi>[0], endereco = '/investidor/perfil') {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/organizacoes/minhas': () => ({ corpo: [] }),
    ...LISTAS,
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Perfil, tese e preferências de investimento (KAN-61, KAN-18, KAN-19)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:04
   * Cartão Jira: KAN-19
   * Título: Perfil pessoal
   * Objetivo: Conferir os tipos de pessoa física, a descrição de "outros", a tese (com o atalho da região), as preferências complementares e o envio dos campos.
   * Decisões: O servidor devolve o perfil salvo.
   */
  it('salva o perfil pessoal', async () => {
    const { chamadas } = abrir({
      'GET /api/v1/investidores/perfil': () => ({
        corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: null, atualizadoPor: null, atualizadoEm: null },
      }),
      'PUT /api/v1/investidores/perfil': ({ corpo }) => ({
        corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: corpo, atualizadoPor: 'Ana Investidora',
          atualizadoEm: '2026-11-24T17:00:00Z' },
      }),
    })
    const form = await screen.findByRole('form', { name: 'Perfil de investimento' })
    const tipo = within(form).getByLabelText('Tipo de investidor')
    await within(form).findByRole('option', { name: 'Outros' })
    expect(within(tipo).queryByRole('option', { name: 'Fundo de Venture Capital' })).not.toBeInTheDocument()
    await userEvent.selectOptions(tipo, 'OUTROS')
    await userEvent.type(within(form).getByLabelText('Descreva o seu tipo'), 'Clube de investimento')
    await userEvent.type(within(form).getByLabelText('Ticket mínimo (R$)'), '50000')
    await userEvent.type(within(form).getByLabelText('Ticket máximo (R$)'), '250000')
    await userEvent.click(within(form).getByLabelText('Apenas co-invisto'))
    await userEvent.click(within(form).getByLabelText('Aceito ser observador do conselho'))
    await userEvent.click(within(form).getByLabelText(/follow-on/))
    await userEvent.click(within(form).getByLabelText('Mútuo conversível'))
    await userEvent.click(await within(form).findByLabelText('Fintech'))
    await userEvent.click(within(form).getByLabelText('Seed'))
    await userEvent.click(within(form).getByLabelText('Tração'))
    await userEvent.click(within(form).getByRole('button', { name: 'Marcar Sudeste' }))
    await userEvent.click(within(form).getByLabelText('Minas Gerais'))
    await userEvent.selectOptions(within(form).getByLabelText('Horizonte de investimento'), 'DE_5_A_8_ANOS')
    await userEvent.click(within(form).getByLabelText('Impacto socioambiental'))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar perfil' }))
    expect(await screen.findByText('Perfil salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/investidores/perfil')?.corpo).toEqual({
      tipo: 'OUTROS', tipoOutro: 'Clube de investimento', ticketMinimo: 50000, ticketMaximo: 250000, lideranca: 'SO_COINVESTE',
      followOn: true, participacaoNaGestao: 'ACEITA_OBSERVADOR', instrumentos: ['MUTUO_CONVERSIVEL'],
      segmentos: ['FINTECH'], estagios: ['SEED'], maturidades: ['TRACAO'], localizacoes: ['SP'],
      complementares: { modelos: [], modalidades: [], horizonte: 'DE_5_A_8_ANOS', perfilRisco: null, experiencia: null, interesses: ['IMPACTO'] },
    })
    expect(screen.getByText(/Última alteração por Ana Investidora/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:16
   * Cartão Jira: KAN-61
   * Título: Perfil da organização sem ser administrador
   * Objetivo: Conferir o tipo fixo da organização, o aviso e o formulário bloqueado, sem botão de salvar.
   * Decisões: Atuação em nome do Fundo Aurora.
   */
  it('bloqueia o perfil da organização para quem não é administrador', async () => {
    localStorage.setItem(CHAVE_ATUACAO, JSON.stringify({ usuario: 'a1', organizacao: 'o1', nome: 'Fundo Aurora' }))
    const { chamadas } = abrir({
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: [{ id: 'o1', nome: 'Fundo Aurora', tipo: 'FUNDO_VC', cnpj: '', site: null,
        descricao: null, meuPapel: 'MEMBRO' }] }),
      'GET /api/v1/investidores/perfil': () => ({
        corpo: { daOrganizacao: true, podeEditar: false, tipoFixo: 'FUNDO_VC', atualizadoPor: 'Bia', atualizadoEm: '2026-11-24T17:00:00Z',
          perfil: { tipo: 'FUNDO_VC', tipoOutro: null, ticketMinimo: 1000000, ticketMaximo: 5000000, lideranca: 'LIDERA', followOn: true,
            participacaoNaGestao: 'EXIGE_CONSELHO', instrumentos: ['PARTICIPACAO_SOCIETARIA'] } },
      }),
    })
    expect(await screen.findByText('Só o administrador da organização altera este perfil.')).toBeInTheDocument()
    expect((await screen.findAllByText('Fundo de Venture Capital')).length).toBeGreaterThan(0)
    expect(screen.getByLabelText('Ticket mínimo (R$)')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Salvar perfil' })).not.toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'GET /api/v1/investidores/perfil')?.cabecalhos['X-Orbit-Organizacao']).toBe('o1')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:25
   * Cartão Jira: KAN-18
   * Título: Pular o onboarding
   * Objetivo: Conferir o título do onboarding, o pulo registrado e a ida para a área do investidor.
   * Decisões: Área do investidor simulada com o onboarding pulado.
   */
  it('pula o onboarding e segue para a área do investidor', async () => {
    const { chamadas } = abrir({
      'GET /api/v1/investidores/perfil': () => ({
        corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: null, atualizadoPor: null, atualizadoEm: null },
      }),
      'POST /api/v1/investidores/onboarding/pulo': () => ({ status: 204 }),
      'GET /api/v1/investidores/eu': () => ({
        corpo: { teseCadastrada: false, proximoPasso: '/investidor/onboarding', atuandoPor: null, onboarding: 'PULADO' },
      }),
    }, '/investidor/onboarding')
    expect(await screen.findByRole('heading', { name: 'Sua tese de investimento' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Pular por agora' }))
    expect(await screen.findByText(/Você deixou a tese para depois/)).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/investidores/onboarding/pulo')).toBe(true)
  })
})
