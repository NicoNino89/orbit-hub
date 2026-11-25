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
  { campo: 'DESTINO_PRODUTO', secao: 'NECESSIDADE_DE_INVESTIMENTO', rotulo: 'Destinação para produto (%)', tipo: 'PERCENTUAL', limite: null, lista: null, ajuda: 'Valores ficam só na rodada.' },
  { campo: 'DESTINO_EQUIPE', secao: 'NECESSIDADE_DE_INVESTIMENTO', rotulo: 'Destinação para equipe (%)', tipo: 'PERCENTUAL', limite: null, lista: null, ajuda: null },
  { campo: 'PRAZO_USO_MESES', secao: 'NECESSIDADE_DE_INVESTIMENTO', rotulo: 'Prazo de uso (meses)', tipo: 'INTEIRO', limite: 120, lista: null, ajuda: null },
  { campo: 'ESTAGIO', secao: 'MATURIDADE', rotulo: 'Estágio do negócio', tipo: 'ESCALA', limite: null, lista: null, ajuda: 'Obrigatório para publicar.',
    opcoes: [{ codigo: 'IDEACAO', rotulo: 'Ideação', explicacao: 'Há a ideia.' }, { codigo: 'MVP', rotulo: 'MVP', explicacao: 'Uma primeira versão está em uso.' }] },
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:26
   * Cartão Jira: KAN-25
   * Título: Destinação dos recursos
   * Objetivo: Conferir a soma das destinações à vista, o destaque acima de 100% e o salvamento dos números.
   * Decisões: O servidor também recusa a soma acima de 100%.
   */
  it('mostra a soma das destinações e salva os números', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'PUT /api/v1/startups/s1/secoes/NECESSIDADE_DE_INVESTIMENTO': ({ corpo }) => ({ corpo }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Necessidade de investimento' }))
    const form = await screen.findByRole('form', { name: 'Necessidade de investimento' })
    await userEvent.type(within(form).getByLabelText('Destinação para produto (%)'), '70')
    await userEvent.type(within(form).getByLabelText('Destinação para equipe (%)'), '40')
    expect(within(form).getByText('Soma das destinações: 110% de 100%')).toHaveClass('text-crit')
    await userEvent.clear(within(form).getByLabelText('Destinação para equipe (%)'))
    await userEvent.type(within(form).getByLabelText('Destinação para equipe (%)'), '29,5')
    expect(within(form).getByText('Soma das destinações: 99,5% de 100%')).not.toHaveClass('text-crit')
    await userEvent.type(within(form).getByLabelText('Prazo de uso (meses)'), '18')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar necessidade de investimento' }))
    expect(await screen.findByText('Necessidade de investimento: salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/secoes/NECESSIDADE_DE_INVESTIMENTO')?.corpo)
      .toEqual({ DESTINO_PRODUTO: '70', DESTINO_EQUIPE: '29,5', PRAZO_USO_MESES: '18' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:30
   * Cartão Jira: KAN-26
   * Título: Escolher o estágio
   * Objetivo: Conferir que cada nível mostra a explicação e que a escolha é salva.
   * Decisões: Dois níveis simulados.
   */
  it('mostra os estágios com a explicação e salva a escolha', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'PUT /api/v1/startups/s1/secoes/MATURIDADE': ({ corpo }) => ({ corpo }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Estágio do negócio' }))
    const form = await screen.findByRole('form', { name: 'Estágio do negócio' })
    expect(within(form).getByText('Uma primeira versão está em uso.')).toBeInTheDocument()
    expect(within(form).getByText('Obrigatório para publicar.')).toBeInTheDocument()
    await userEvent.click(within(form).getByRole('radio', { name: /MVP/ }))
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar estágio do negócio' }))
    expect(await screen.findByText('Estágio do negócio: salvo.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/secoes/MATURIDADE')?.corpo).toEqual({ ESTAGIO: 'MVP' })
  })
})
