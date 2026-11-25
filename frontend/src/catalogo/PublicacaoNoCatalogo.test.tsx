import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const STARTUP = {
  id: 's1', nome: 'NovaSafra', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: 'AGRONEGOCIO', localizacao: 'MT', site: null,
  redes: {}, fundadores: [], situacao: 'PUBLICADA', constituida: false, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
  meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [],
}

const PROGRESSO = {
  percentual: 100, podeEnviar: true, pendencias: [], situacao: 'PUBLICADA',
  secoes: [{ secao: 'CADASTRO', preenchidos: 1, total: 1, completa: true }, { secao: 'MERCADO', preenchidos: 1, total: 1, completa: true }],
}

const CATALOGO = [
  { campo: 'PUBLICO_ALVO', secao: 'MERCADO', rotulo: 'Público-alvo', tipo: 'TEXTO', limite: 1000, lista: null, ajuda: null, obrigatorio: true },
]

const APRESENTACAO = {
  modo: 'PREVIA', comoVerificado: true,
  cabecalho: { id: 's1', nome: 'NovaSafra', segmento: 'Agronegócio', estagio: 'MVP', localizacao: null, site: 'https://novasafra.com',
    redes: [], temLogotipo: false, naoConstituida: true, selo: 'VERIFICADO' },
  paginas: [
    { id: 'apresentacao', titulo: 'Apresentação', blocos: [{ titulo: 'Problema', itens: [{ rotulo: 'Qual problema', valor: 'Perda na colheita' }], avisos: [] }] },
    { id: 'resumo', titulo: 'Resumo executivo', blocos: [{ titulo: 'Necessidade de investimento', itens: [], avisos: ['Sem rodada aberta no momento.'] }] },
    { id: 'conheca', titulo: 'Conheça o empreendimento', blocos: [] },
  ],
}

const VISIBILIDADE = {
  participaEdicoes: true,
  itens: [
    { item: { codigo: 'PROBLEMA', rotulo: 'Qual problema a startup resolve', secao: 'PROBLEMA_E_SOLUCAO', padraoPublico: true }, publico: true },
    { item: { codigo: 'FONTES_FATURAMENTO', rotulo: 'Fontes de faturamento', secao: 'MODELO_DE_NEGOCIO', padraoPublico: false }, publico: false },
  ],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-29, KAN-36, KAN-66, KAN-85
 * Título: Abrir com sessão
 * Objetivo: Renderizar o endereço com a sessão e o papel ativo pedidos e as rotas simuladas.
 * Decisões: Listas e campos comuns já simulados.
 */
function abrir(endereco: string, papel: string, rotas: Parameters<typeof simularApi>[0]) {
  const sessao: SessaoApi = {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Pessoa', email: 'p@exemplo.com', papeis: [papel as SessaoApi['usuario']['papeis'][number]] },
  }
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, papel)
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
    'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
    'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
    'GET /api/v1/startups/s1/situacoes': () => ({ corpo: [] }),
    'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
    'GET /api/v1/startups/s1/progresso': () => ({ corpo: PROGRESSO }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Publicação no catálogo (S5)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-28, KAN-29, KAN-85
   * Título: Visibilidade e pré-visualização
   * Objetivo: Marcar um item como público, trocar a visão de verificado e sair das edições.
   * Decisões: Cada troca chama a API na hora.
   */
  it('troca a visibilidade, pré-visualiza e sai das edições', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', 'EMPREENDEDOR', {
      'GET /api/v1/startups/s1/curadoria': () => ({ corpo: [] }),
      'GET /api/v1/startups/s1/visibilidade': () => ({ corpo: VISIBILIDADE }),
      'PUT /api/v1/startups/s1/visibilidade': () => ({ corpo: { ...VISIBILIDADE, itens: VISIBILIDADE.itens.map((i) => ({ ...i, publico: true })) } }),
      'PUT /api/v1/startups/s1/participacao-edicoes': () => ({ corpo: { ...VISIBILIDADE, participaEdicoes: false } }),
      'GET /api/v1/startups/s1/apresentacao': () => ({ corpo: APRESENTACAO }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Visibilidade' }))
    const painel = await screen.findByRole('region', { name: 'Visibilidade' })
    expect(within(painel).getByText(/confidencial por padrão/)).toBeInTheDocument()
    expect(await within(painel).findAllByText('Perda na colheita')).not.toHaveLength(0)
    expect(within(painel).getAllByText('Empresa ainda não constituída').length).toBeGreaterThan(0)
    await userEvent.click(within(painel).getByLabelText('Público: Fontes de faturamento'))
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/visibilidade')?.corpo).toEqual({ FONTES_FATURAMENTO: true })
    await userEvent.click(within(painel).getByLabelText('Ver como investidor verificado'))
    expect(chamadas.filter((c) => c.chave === 'GET /api/v1/startups/s1/apresentacao').length).toBeGreaterThan(1)
    await userEvent.click(within(painel).getByLabelText('Participar das edições temáticas do catálogo'))
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/participacao-edicoes')?.corpo).toEqual({ participa: false })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-66, KAN-84
   * Título: Comentários da curadoria
   * Objetivo: Mostrar a alteração devolvida com o parecer, o comentário na seção e o botão de reenviar.
   * Decisões: Revisão devolvida simulada.
   */
  it('mostra a devolução da curadoria e reenvia a alteração', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', 'EMPREENDEDOR', {
      'GET /api/v1/startups/s1/curadoria': () => ({ corpo: [
        { id: 'r2', tipo: 'ALTERACAO', situacao: 'DEVOLVIDA', enviadaEm: '2026-11-25T12:00:00Z', decididaEm: '2026-11-25T13:00:00Z',
          parecer: 'Ajuste o mercado', prazo: null,
          apontamentos: [{ tipo: 'SECAO', codigo: 'MERCADO', rotulo: 'Mercado', comentario: 'Informe a fonte' }] },
        { id: 'r1', tipo: 'PUBLICACAO', situacao: 'APROVADA', enviadaEm: '2026-11-20T12:00:00Z', decididaEm: null, parecer: null, prazo: null, apontamentos: [] },
      ] }),
      'POST /api/v1/startups/s1/envio': () => ({ corpo: { situacao: 'PUBLICADA' } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Mercado' }))
    expect(await screen.findByRole('note')).toHaveTextContent('Informe a fonte')
    await userEvent.click(screen.getByRole('button', { name: 'Publicação' }))
    const curadoria = await screen.findByRole('region', { name: 'Curadoria' })
    expect(curadoria).toHaveTextContent('Ajuste o mercado')
    expect(curadoria).toHaveTextContent('Publicação · Aprovada')
    await userEvent.click(screen.getByRole('button', { name: 'Reenviar alteração para a curadoria' }))
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/startups/s1/envio')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-36, KAN-85
   * Título: Catálogo do investidor
   * Objetivo: Listar as edições no ar, abrir uma edição e a apresentação da startup.
   * Decisões: Investidor não verificado vê o convite para verificar.
   */
  it('mostra edições e a apresentação da startup ao investidor', async () => {
    const edicao = { id: 'e1', titulo: 'Agro digital', descricao: 'Startups do campo', temCapa: false, inicio: '2026-11-01', fim: '2026-11-30',
      startups: [{ id: 's1', nome: 'NovaSafra', segmento: 'Agronegócio', estagio: 'MVP', localizacao: null, temLogotipo: false }] }
    abrir('/catalogo/edicoes', 'INVESTIDOR', {
      'GET /api/v1/catalogo/edicoes': () => ({ corpo: [edicao] }),
      'GET /api/v1/catalogo/edicoes/e1': () => ({ corpo: edicao }),
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: { ...APRESENTACAO, modo: 'CATALOGO', comoVerificado: false } }),
    })
    expect(await screen.findByRole('heading', { name: 'Agro digital' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Agro digital' }))
    await userEvent.click(await screen.findByRole('link', { name: 'NovaSafra' }))
    expect(await screen.findByText('Verifique sua identidade para ver os dados das rodadas abertas.')).toBeInTheDocument()
    expect(screen.getAllByText('Sem rodada aberta no momento.').length).toBeGreaterThan(0)
  })
})
