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
  { campo: 'OUTROS_INDICADORES', secao: 'TRACAO', rotulo: 'Outros indicadores de desempenho', tipo: 'TEXTO', limite: 1000, lista: null, ajuda: null },
]

const INDICADORES = [
  { indicador: 'CLIENTES', rotulo: 'Quantidade de clientes', formato: 'QUANTIDADE', desatualizado: false,
    atual: { valor: 150, referencia: '2026-11', registradoEm: '2026-11-24T20:00:00Z' },
    historico: [{ valor: 150, referencia: '2026-11', registradoEm: '2026-11-24T20:00:00Z' }, { valor: 120, referencia: '2026-10', registradoEm: '2026-11-01T20:00:00Z' }] },
  { indicador: 'FATURAMENTO_MENSAL', rotulo: 'Faturamento mensal (R$)', formato: 'VALOR', desatualizado: true,
    atual: { valor: 48000.5, referencia: '2026-03', registradoEm: '2026-03-30T20:00:00Z' }, historico: [{ valor: 48000.5, referencia: '2026-03', registradoEm: '2026-03-30T20:00:00Z' }] },
  { indicador: 'RETENCAO', rotulo: 'Retenção de clientes (%)', formato: 'PERCENTUAL', desatualizado: false, atual: null, historico: [] },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
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

describe('Indicadores de tração (KAN-24)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:11
   * Cartão Jira: KAN-24
   * Título: Ver e declarar indicadores
   * Objetivo: Conferir o aviso de não auditado, a marca de desatualizado, o histórico e a declaração com o mês.
   * Decisões: O servidor simulado devolve a retenção com o valor novo.
   */
  it('mostra os indicadores com o histórico e registra um valor com o mês', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'GET /api/v1/startups/s1/indicadores': () => ({ corpo: INDICADORES }),
      'POST /api/v1/startups/s1/indicadores': () => ({ status: 201, corpo: { ...INDICADORES[2],
        atual: { valor: 92.5, referencia: '2026-11', registradoEm: '2026-11-24T21:00:00Z' }, historico: [{ valor: 92.5, referencia: '2026-11', registradoEm: '2026-11-24T21:00:00Z' }] } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Tração e resultados' }))
    const painel = await screen.findByRole('region', { name: 'Indicadores de tração' })
    expect(within(painel).getByText(/Declarado pelo empreendedor, não auditado/)).toBeInTheDocument()
    expect(within(painel).getByText('Desatualizado')).toBeInTheDocument()
    await userEvent.click(within(painel).getByRole('button', { name: 'Ver histórico (2)' }))
    expect(within(painel).getByRole('list', { name: 'Histórico de Quantidade de clientes' }).children).toHaveLength(2)

    const form = within(painel).getByRole('form', { name: 'Declarar indicador' })
    await userEvent.selectOptions(within(form).getByLabelText('Indicador'), 'RETENCAO')
    await userEvent.type(within(form).getByLabelText('Valor'), '92,5')
    await userEvent.type(within(form).getByLabelText('Mês de referência'), '2026-11')
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar valor' }))
    expect(await screen.findByText('Retenção de clientes (%): valor registrado.')).toBeInTheDocument()
    expect(within(painel).getByText('92,5%')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/indicadores')?.corpo)
      .toEqual({ indicador: 'RETENCAO', valor: 92.5, referencia: '2026-11' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:11
   * Cartão Jira: KAN-24
   * Título: Só leitura
   * Objetivo: Conferir que quem não edita a tração vê os indicadores sem o formulário.
   * Decisões: Colaboradora só de mercado.
   */
  it('mostra os indicadores sem formulário para quem não edita a tração', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'COLABORADOR', minhasSecoes: ['MERCADO'] } }),
      'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'GET /api/v1/startups/s1/indicadores': () => ({ corpo: INDICADORES }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Tração e resultados' }))
    expect(await screen.findByText('Quantidade de clientes')).toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Declarar indicador' })).not.toBeInTheDocument()
  })
})
