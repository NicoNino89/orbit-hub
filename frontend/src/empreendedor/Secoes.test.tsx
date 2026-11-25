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
  id: 's1', nome: 'NovaSafra', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: 'AGRONEGOCIO', localizacao: 'MT', site: null,
  redes: {}, fundadores: [], situacao: 'RASCUNHO', constituida: false, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
  meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [],
}

const CATALOGO = [
  { campo: 'PROBLEMA', secao: 'PROBLEMA_E_SOLUCAO', rotulo: 'Qual problema a startup resolve', tipo: 'TEXTO', limite: 20, lista: null, ajuda: 'Descreva a dor.' },
  { campo: 'SOLUCAO', secao: 'PROBLEMA_E_SOLUCAO', rotulo: 'Solução proposta', tipo: 'TEXTO', limite: 1000, lista: null, ajuda: null },
  { campo: 'TEM_MVP', secao: 'PRODUTO', rotulo: 'Já existe um MVP?', tipo: 'SIM_NAO', limite: null, lista: null, ajuda: null },
  { campo: 'MODELO', secao: 'MODELO_DE_NEGOCIO', rotulo: 'Classificação do modelo de negócio', tipo: 'LISTA', limite: null, lista: 'MODELO_NEGOCIO', ajuda: 'Usada nos filtros.' },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Abrir como empreendedora
 * Objetivo: Renderizar o endereço com a sessão da empreendedora e as rotas informadas.
 * Decisões: Papel ativo de empreendedor; listas do formulário cadastral simuladas.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'EMPREENDEDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Seções da startup (KAN-22)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:56
   * Cartão Jira: KAN-22
   * Título: Preencher problema e produto
   * Objetivo: Conferir as etapas vindas do catálogo, o contador de caracteres e o salvamento de texto e de sim/não.
   * Decisões: O servidor simulado devolve o que recebe.
   */
  it('mostra as seções do catálogo, conta os caracteres e salva', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: { SOLUCAO: 'Rotas' } }),
      'PUT /api/v1/startups/s1/secoes/PROBLEMA_E_SOLUCAO': ({ corpo }) => ({ corpo }),
      'PUT /api/v1/startups/s1/secoes/PRODUTO': ({ corpo }) => ({ corpo: { SOLUCAO: 'Rotas', ...(corpo as object) } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Problema e solução' }))
    const form = await screen.findByRole('form', { name: 'Problema e solução' })
    expect(within(form).getByLabelText('Solução proposta')).toHaveValue('Rotas')
    const problema = within(form).getByLabelText('Qual problema a startup resolve')
    await userEvent.type(problema, 'Frete caro')
    expect(within(form).getByText('10/20')).toBeInTheDocument()
    expect(problema).toHaveAttribute('maxLength', '20')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar problema e solução' }))
    expect(await screen.findByText('Problema e solução: salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/secoes/PROBLEMA_E_SOLUCAO')?.corpo)
      .toEqual({ PROBLEMA: 'Frete caro', SOLUCAO: 'Rotas' })

    await userEvent.click(screen.getByRole('button', { name: 'Produto ou serviço' }))
    const produto = await screen.findByRole('form', { name: 'Produto ou serviço' })
    await userEvent.click(within(produto).getByLabelText('Sim'))
    await userEvent.click(within(produto).getByRole('button', { name: 'Salvar produto ou serviço' }))
    expect(await screen.findByText('Produto ou serviço: salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/secoes/PRODUTO')?.corpo).toEqual({ TEM_MVP: 'SIM' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:56
   * Cartão Jira: KAN-22
   * Título: Colaborador fora da seção
   * Objetivo: Conferir que a seção não autorizada aparece bloqueada, sem botão de salvar.
   * Decisões: Colaboradora só do produto.
   */
  it('bloqueia a seção que o colaborador não pode editar', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'COLABORADOR', minhasSecoes: ['PRODUTO'] } }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Problema e solução' }))
    const form = await screen.findByRole('form', { name: 'Problema e solução' })
    expect(within(form).getByLabelText('Solução proposta')).toBeDisabled()
    expect(within(form).queryByRole('button', { name: /Salvar/ })).not.toBeInTheDocument()
    expect(within(form).getByText(/não tem autorização para editar esta seção/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Produto ou serviço' }))
    expect(await screen.findByRole('button', { name: 'Salvar produto ou serviço' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:02
   * Cartão Jira: KAN-23
   * Título: Classificar o modelo de negócio
   * Objetivo: Conferir que o campo de lista mostra as opções da lista gerenciável e salva o código.
   * Decisões: Lista simulada com SaaS e Marketplace.
   */
  it('classifica o modelo de negócio pela lista', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'GET /api/v1/listas/MODELO_NEGOCIO': () => ({ corpo: [{ codigo: 'SAAS', rotulo: 'SaaS', grupo: null }, { codigo: 'MARKETPLACE', rotulo: 'Marketplace', grupo: null }] }),
      'PUT /api/v1/startups/s1/secoes/MODELO_DE_NEGOCIO': ({ corpo }) => ({ corpo }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Modelo de negócio' }))
    const form = await screen.findByRole('form', { name: 'Modelo de negócio' })
    await within(form).findByRole('option', { name: 'Marketplace' })
    await userEvent.selectOptions(within(form).getByLabelText('Classificação do modelo de negócio'), 'MARKETPLACE')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar modelo de negócio' }))
    expect(await screen.findByText('Modelo de negócio: salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/secoes/MODELO_DE_NEGOCIO')?.corpo).toEqual({ MODELO: 'MARKETPLACE' })
  })
})
