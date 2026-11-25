import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const APRESENTACAO = {
  modo: 'CATALOGO', comoVerificado: true,
  cabecalho: { id: 's1', nome: 'Sigilosa', segmento: 'Fintech', estagio: 'MVP', localizacao: null, site: null, redes: [], temLogotipo: false,
    naoConstituida: false, selo: 'NAO_VERIFICADO' },
  paginas: [{ id: 'apresentacao', titulo: 'Apresentação', blocos: [] }],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-145
 * Título: Abrir com papel
 * Objetivo: Renderizar o endereço com a sessão e o papel pedidos.
 * Decisões: Lista de categorias simulada.
 */
function abrir(endereco: string, papel: 'INVESTIDOR' | 'EMPREENDEDOR', rotas: Parameters<typeof simularApi>[0]) {
  const sessao: SessaoApi = {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Pessoa', email: 'p@exemplo.com', papeis: [papel] },
  }
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, papel)
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao }),
    'GET /api/v1/listas/CATEGORIA_DATAROOM': () => ({ corpo: [{ codigo: 'FINANCEIRO', rotulo: 'Financeiro', grupo: null }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Nível 2 e Data Room (S8)', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:x')
    URL.revokeObjectURL = vi.fn()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 12:04
   * Cartão Jira: KAN-30, KAN-31, KAN-33, KAN-63, KAN-145
   * Título: Aba Confidencial
   * Objetivo: Enviar documento, trocar a regra, liberar, renovar e revogar, e ver o histórico.
   * Decisões: Startup simulada no editor.
   */
  it('gerencia o Data Room e os acessos na aba Confidencial', async () => {
    const startup = { id: 's1', nome: 'Sigilosa', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: 'FINTECH', localizacao: 'SP', site: null,
      redes: {}, fundadores: [], situacao: 'PUBLICADA', constituida: true, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
      meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [] }
    const { chamadas } = abrir('/empreendedor/startups/s1', 'EMPREENDEDOR', {
      'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
      'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
      'GET /api/v1/startups/campos': () => ({ corpo: [] }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'GET /api/v1/startups/s1': () => ({ corpo: startup }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { percentual: 100, podeEnviar: true, pendencias: [], situacao: 'PUBLICADA', secoes: [] } }),
      'GET /api/v1/startups/s1/curadoria': () => ({ corpo: [] }),
      'GET /api/v1/startups/s1/nivel2': () => ({ corpo: { regra: 'TERMO_PLATAFORMA', ndaEnviado: false, validadePadraoDias: 90, conexoes: [
        { conexaoId: 'c1', investidor: 'Leo', situacao: 'MATCH', verificado: true, termoAceito: true, ndaInvestidor: false, ndaStartup: false,
          falta: null, acesso: null },
        { conexaoId: 'c2', investidor: 'Ana', situacao: 'EM_ANALISE', verificado: false, termoAceito: true, ndaInvestidor: false, ndaStartup: false,
          falta: null, acesso: { id: 'a2', conexaoId: 'c2', liberadoEm: '2026-11-20T12:00:00Z', validoAte: '2027-02-18T12:00:00Z' } },
      ] } }),
      'GET /api/v1/startups/s1/nivel2/historico': () => ({ corpo: [{ tipo: 'DOWNLOAD', investidor: 'Ana', por: 'Ana', detalhe: null,
        em: '2026-11-25T12:00:00Z', documento: 'Balanço' }] }),
      'GET /api/v1/startups/s1/dataroom': () => ({ corpo: [{ id: 'd1', categoria: 'FINANCEIRO', categoriaRotulo: 'Financeiro', nome: 'Balanço',
        descricao: null, tipo: 'application/pdf', tamanho: 10, versao: 2, atual: true, enviadoPor: 'Eva', enviadoEm: '2026-11-25T12:00:00Z' }] }),
      'POST /api/v1/startups/s1/dataroom': () => ({ status: 201, corpo: {} }),
      'DELETE /api/v1/startups/s1/dataroom/d1': () => ({ status: 204 }),
      'PUT /api/v1/startups/s1/nivel2/regra': () => ({ corpo: { regra: 'NDA_BILATERAL' } }),
      'POST /api/v1/startups/s1/nivel2/acessos': () => ({ status: 201, corpo: {} }),
      'POST /api/v1/startups/s1/nivel2/acessos/a2/renovacao': () => ({ corpo: {} }),
      'POST /api/v1/startups/s1/nivel2/acessos/a2/revogacao': () => ({ status: 204 }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Confidencial' }))
    const aba = await screen.findByRole('region', { name: 'Confidencial' })
    expect(within(aba).getByRole('region', { name: 'Histórico de acessos' })).toHaveTextContent('Download · Ana · Balanço')
    const form = within(aba).getByRole('form', { name: 'Enviar documento' })
    await userEvent.selectOptions(await within(form).findByLabelText('Categoria'), 'FINANCEIRO')
    await userEvent.type(within(form).getByLabelText('Nome do documento'), 'DRE')
    await userEvent.upload(within(form).getByLabelText('Arquivo do documento'), new File(['%PDF'], 'dre.pdf', { type: 'application/pdf' }))
    await userEvent.click(within(form).getByRole('button', { name: 'Enviar' }))
    await userEvent.click(within(aba).getByRole('button', { name: 'Remover' }))
    await userEvent.click(within(aba).getByLabelText(/NDA bilateral/))
    await userEvent.type(within(aba).getByLabelText('Dias para Leo'), '30')
    await userEvent.click(within(aba).getByRole('button', { name: 'Liberar informações confidenciais' }))
    await userEvent.click(within(aba).getByRole('button', { name: 'Renovar' }))
    await userEvent.click(within(aba).getByRole('button', { name: 'Revogar' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/nivel2/acessos')?.corpo).toEqual({ conexaoId: 'c1', dias: 30 })
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/nivel2/regra')?.corpo).toEqual({ regra: 'NDA_BILATERAL' })
    for (const chave of ['POST /api/v1/startups/s1/dataroom', 'DELETE /api/v1/startups/s1/dataroom/d1',
      'POST /api/v1/startups/s1/nivel2/acessos/a2/renovacao', 'POST /api/v1/startups/s1/nivel2/acessos/a2/revogacao']) {
      expect(chamadas.some((c) => c.chave === chave)).toBe(true)
    }
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 12:04
   * Cartão Jira: KAN-32, KAN-63, KAN-145
   * Título: Área confidencial do investidor
   * Objetivo: Confirmar o NDA, abrir as informações confidenciais e baixar um documento pelo link temporário.
   * Decisões: O download chama primeiro o link e depois o arquivo.
   */
  it('mostra a área confidencial ao investidor autorizado', async () => {
    const { chamadas } = abrir('/catalogo/startups/s1', 'INVESTIDOR', {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: APRESENTACAO }),
      'GET /api/v1/catalogo/startups/s1/nivel2/situacao': () => ({ corpo: { temAcesso: true, validoAte: '2027-02-18T12:00:00Z', regra: 'NDA_BILATERAL',
        termoAceito: true, conexaoId: 'c1', ndaDisponivel: true, ndaConfirmado: false } }),
      'POST /api/v1/conexoes/c1/nda': () => ({ status: 204 }),
      'GET /api/v1/catalogo/startups/s1/nivel2': () => ({ corpo: { informacoes: { ...APRESENTACAO, modo: 'NIVEL2', paginas: [
        { id: 'resumo', titulo: 'Resumo executivo', blocos: [{ titulo: 'Modelo de negócio', itens: [{ rotulo: 'Fontes', valor: 'Mensalidades' }], avisos: [] }] }] },
        documentos: [{ id: 'd1', categoria: 'Financeiro', nome: 'Balanço', descricao: null, tipo: 'application/pdf', tamanho: 10, versao: 1,
          enviadoEm: '2026-11-25T12:00:00Z' }], validoAte: '2027-02-18T12:00:00Z' } }),
      'POST /api/v1/catalogo/startups/s1/nivel2/documentos/d1/link': () => ({ corpo: { token: 'tk', expiraEm: '2026-11-25T12:10:00Z' } }),
      'GET /api/v1/dataroom/links/tk': () => ({ corpo: {} }),
    })
    const area = await screen.findByRole('region', { name: 'Informações confidenciais' })
    await userEvent.click(within(area).getByRole('button', { name: 'Confirmo que li e aceito o NDA' }))
    await userEvent.click(within(area).getByRole('button', { name: 'Ver informações confidenciais' }))
    expect(await within(area).findAllByText('Mensalidades')).not.toHaveLength(0)
    await userEvent.click(within(area).getByRole('button', { name: 'Baixar' }))
    await vi.waitFor(() => expect(chamadas.some((c) => c.chave === 'GET /api/v1/dataroom/links/tk')).toBe(true))
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/conexoes/c1/nda')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 12:04
   * Cartão Jira: KAN-63
   * Título: Termo pendente
   * Objetivo: Pedir o aceite do termo quando a versão atual não foi aceita.
   * Decisões: Sem acesso liberado.
   */
  it('pede o aceite do termo e avisa que não há acesso', async () => {
    abrir('/catalogo/startups/s1', 'INVESTIDOR', {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: APRESENTACAO }),
      'GET /api/v1/catalogo/startups/s1/nivel2/situacao': () => ({ corpo: { temAcesso: false, validoAte: null, regra: 'TERMO_PLATAFORMA',
        termoAceito: false, conexaoId: 'c1', ndaDisponivel: false, ndaConfirmado: false } }),
    })
    const area = await screen.findByRole('region', { name: 'Informações confidenciais' })
    expect(within(area).getByRole('link', { name: 'Termo de Confidencialidade' })).toBeInTheDocument()
    expect(area).toHaveTextContent('ainda não liberou')
  })
})
