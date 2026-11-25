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
  usuario: { id: 'i1', nome: 'Leo Investidor', email: 'leo@exemplo.com', papeis: ['INVESTIDOR'] },
}

const cartao = (id: string, nome: string) => ({ id, nome, segmento: 'Agronegócio', estagio: 'MVP', localizacao: 'Mato Grosso', temLogotipo: false })

const RESULTADO = {
  lista: 'PERSONALIZADA', verificado: true, comPerfil: true,
  itens: [
    { startup: cartao('s1', 'Alfa'), criterios: ['Atua em Agronegócio, um dos seus segmentos'], rodadaAberta: true, favorito: false, publicadaEm: null },
    { startup: cartao('s2', 'Beta'), criterios: [], rodadaAberta: false, favorito: true, publicadaEm: null },
  ],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-35, KAN-37, KAN-38, KAN-39, KAN-41, KAN-42, KAN-43, KAN-44
 * Título: Abrir como investidor
 * Objetivo: Renderizar o endereço com a sessão do investidor e as rotas simuladas.
 * Decisões: Listas dos filtros vazias.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [{ codigo: 'FINTECH', rotulo: 'Fintech', grupo: null }] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
    'GET /api/v1/listas/ESTAGIO_RODADA': () => ({ corpo: [{ codigo: 'SEED', rotulo: 'Seed', grupo: null }] }),
    'GET /api/v1/listas/INSTRUMENTO': () => ({ corpo: [] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Catálogo (S6)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:03
   * Cartão Jira: KAN-35, KAN-37, KAN-38, KAN-39, KAN-41, KAN-44
   * Título: Folhear, filtrar e marcar
   * Objetivo: Ver a seleção com critérios e progresso, avançar (posição lembrada), filtrar, favoritar, descartar e
   *           trocar para novas oportunidades.
   * Decisões: Confere a consulta enviada com os filtros.
   */
  it('folheia, guarda a posição, filtra e marca startups', async () => {
    const { chamadas, fetchSimulado } = abrir('/catalogo', {
      'GET /api/v1/catalogo/startups': () => ({ corpo: RESULTADO }),
      'PUT /api/v1/catalogo/favoritos/s1': () => ({ status: 204 }),
      'PUT /api/v1/catalogo/descartadas/s1': () => ({ status: 204 }),
    })
    expect(await screen.findByText('Atua em Agronegócio, um dos seus segmentos')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Próxima página' }))
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(localStorage.getItem('orbit_catalogo_posicao')).toContain(':1')

    await userEvent.click(screen.getByText('Filtros'))
    const form = screen.getByRole('form', { name: 'Filtros' })
    await userEvent.click(await within(form).findByLabelText('Fintech'))
    await userEvent.click(await within(form).findByLabelText('Seed'))
    await userEvent.type(within(form).getByLabelText('Captação mínima'), '100000')
    await userEvent.click(within(form).getByRole('button', { name: 'Aplicar filtros' }))
    const urls = fetchSimulado.mock.calls.map((c) => String(c[0]))
    await vi.waitFor(() => expect(fetchSimulado.mock.calls.map((c) => String(c[0])).some((u) => u.includes('segmento=FINTECH')
      && u.includes('estagio=SEED') && u.includes('captacaoMinima=100000'))).toBe(true))
    expect(urls.length).toBeGreaterThan(0)

    const alfa = (await screen.findAllByRole('article'))[0]
    await userEvent.click(within(alfa).getByRole('button', { name: '☆ Salvar nos favoritos' }))
    await userEvent.click(within(alfa).getByRole('button', { name: 'Descartar' }))
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/catalogo/favoritos/s1')).toBe(true)
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/catalogo/descartadas/s1')).toBe(true)

    await userEvent.click(screen.getByRole('button', { name: 'Novas oportunidades' }))
    await vi.waitFor(() => expect(fetchSimulado.mock.calls.some((c) => String(c[0]).includes('lista=NOVAS'))).toBe(true))
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:03
   * Cartão Jira: KAN-38, KAN-39
   * Título: Sem resultado e sem perfil
   * Objetivo: Orientar a ajustar os filtros e a definir a tese; esconder os filtros de rodada do não verificado.
   * Decisões: Resultado vazio simulado.
   */
  it('orienta quando não há resultado nem tese', async () => {
    abrir('/catalogo', {
      'GET /api/v1/catalogo/startups': () => ({ corpo: { lista: 'PERSONALIZADA', verificado: false, comPerfil: false, itens: [] } }),
    })
    expect(await screen.findByText('Nenhuma startup encontrada. Ajuste ou limpe os filtros.')).toBeInTheDocument()
    expect(screen.getByText(/Sem tese de investimento/)).toBeInTheDocument()
    await userEvent.click(screen.getByText('Filtros'))
    expect(screen.getByText(/aparecem para investidores verificados/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Limpar' }))
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:03
   * Cartão Jira: KAN-42, KAN-43, KAN-44
   * Título: Minhas listas
   * Objetivo: Ver favoritos com indisponível, limpar o histórico e desfazer um descarte.
   * Decisões: Listas simuladas por tipo.
   */
  it('mostra favoritos, histórico e descartadas', async () => {
    const em = '2026-11-25T12:00:00Z'
    const { chamadas } = abrir('/catalogo/minhas', {
      'GET /api/v1/catalogo/minhas/FAVORITOS': () => ({ corpo: [{ startup: cartao('s1', 'Alfa'), disponivel: false, em }] }),
      'GET /api/v1/catalogo/minhas/HISTORICO': () => ({ corpo: [{ startup: cartao('s2', 'Beta'), disponivel: true, em }] }),
      'GET /api/v1/catalogo/minhas/DESCARTADAS': () => ({ corpo: [{ startup: cartao('s3', 'Gama'), disponivel: true, em }] }),
      'DELETE /api/v1/catalogo/minhas/HISTORICO': () => ({ status: 204 }),
      'DELETE /api/v1/catalogo/descartadas/s3': () => ({ status: 204 }),
      'DELETE /api/v1/catalogo/favoritos/s1': () => ({ status: 204 }),
    })
    expect(await screen.findByText('(indisponível)')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Remover' }))
    await userEvent.click(screen.getByRole('button', { name: 'Visualizadas' }))
    expect(await screen.findByRole('link', { name: 'Beta' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Limpar histórico' }))
    await userEvent.click(screen.getByRole('button', { name: 'Descartadas' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Desfazer' }))
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/catalogo/minhas/HISTORICO')).toBe(true)
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/catalogo/descartadas/s3')).toBe(true)
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/catalogo/favoritos/s1')).toBe(true)
  })
})
