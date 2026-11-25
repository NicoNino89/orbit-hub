import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'c1', nome: 'Ana Curadora', email: 'ana@orbithub.com', papeis: ['CURADOR'] },
}

const REVISAO = {
  id: 'r1', tipo: 'ALTERACAO', situacao: 'EM_ANALISE', enviadaEm: '2026-11-25T12:00:00Z', decididaEm: null, parecer: null,
  aguardandoSegundaAprovacao: false, itens: [],
}

const DETALHE = {
  revisao: REVISAO, startupId: 's1', startup: 'NovaSafra', cnpj: null, situacaoDaStartup: 'PUBLICADA', seloDaStartup: 'NAO_VERIFICADO',
  responsavel: null, prazo: '2026-11-28T12:00:00Z',
  checklist: [{ codigo: 'CAMPOS_OBRIGATORIOS', rotulo: 'Campos obrigatórios preenchidos' }, { codigo: 'CONTEUDO_ADEQUADO', rotulo: 'Conteúdo adequado' }],
  fundadores: [{ nome: 'Eva', papel: 'FUNDADOR_ADMINISTRADOR', selo: 'VERIFICADO' }],
  alteracoes: [{ campo: 'Nome', aprovado: 'Safra', novo: 'NovaSafra' }], anteriores: [],
}

const APRESENTACAO = {
  modo: 'COMPLETA', comoVerificado: true,
  cabecalho: { id: 's1', nome: 'NovaSafra', segmento: 'Agronegócio', estagio: null, localizacao: 'Mato Grosso', site: null, redes: [],
    temLogotipo: false, naoConstituida: true, selo: 'NAO_VERIFICADO' },
  paginas: [{ id: 'apresentacao', titulo: 'Apresentação', blocos: [] }],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-84, KAN-85
 * Título: Abrir como curadora
 * Objetivo: Renderizar o endereço com a sessão da curadora e as rotas simuladas.
 * Decisões: Sem papel ativo (conta interna).
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }), ...rotas })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Curadoria e edições (KAN-84, KAN-85)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-84
   * Título: Fila e decisão
   * Objetivo: Ver a fila com atraso e conflito, abrir a revisão, assumir, marcar o checklist, comentar uma seção e devolver.
   * Decisões: Confere o corpo enviado na decisão.
   */
  it('mostra a fila, assume e devolve com checklist e comentário', async () => {
    const { chamadas } = abrir('/admin/curadoria', {
      'GET /api/v1/admin/curadoria': () => ({ corpo: [{ id: 'r1', startupId: 's1', startup: 'NovaSafra', tipo: 'ALTERACAO', situacao: 'EM_ANALISE',
        enviadaEm: '2026-11-20T12:00:00Z', prazo: '2026-11-23T12:00:00Z', atrasado: true, responsavel: null, conflitoComigo: true,
        semCuradorLivre: false }] }),
      'GET /api/v1/admin/curadoria/r1': () => ({ corpo: DETALHE }),
      'GET /api/v1/admin/curadoria/startups/s1/apresentacao': () => ({ corpo: APRESENTACAO }),
      'POST /api/v1/admin/curadoria/r1/assumir': () => ({ corpo: REVISAO }),
      'POST /api/v1/admin/curadoria/r1/decisao': () => ({ corpo: { ...REVISAO, situacao: 'DEVOLVIDA' } }),
      'POST /api/v1/admin/curadoria/r1/impedimento': () => ({ corpo: REVISAO }),
    })
    const fila = await screen.findByRole('list', { name: 'Revisões abertas' })
    expect(fila).toHaveTextContent('Atrasada')
    expect(fila).toHaveTextContent('Você tem vínculo com esta startup')
    await userEvent.click(within(fila).getByRole('link', { name: 'NovaSafra' }))
    expect(await screen.findByText('Mato Grosso')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Alterações' })).toHaveTextContent('Safra → NovaSafra')
    await userEvent.click(screen.getByRole('button', { name: 'Assumir' }))
    expect(await screen.findByText('Revisão assumida.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Motivo do impedimento'), 'Sou sócia')
    await userEvent.click(screen.getByRole('button', { name: 'Declarar impedimento' }))
    expect(await screen.findByText('Impedimento registrado.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('checkbox', { name: 'Campos obrigatórios preenchidos' }))
    await userEvent.type(screen.getByLabelText('Comentário: Conteúdo adequado'), 'Tem promessa de retorno')
    await userEvent.type(screen.getByLabelText('Mercado'), 'Cite a fonte')
    await userEvent.type(screen.getByLabelText('Parecer'), 'Ajustes simples')
    await userEvent.click(screen.getByRole('button', { name: 'Devolver para ajustes' }))
    expect(await screen.findByText('Devolvida para ajustes.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/curadoria/r1/decisao')?.corpo).toEqual({
      resultado: 'DEVOLVIDA',
      checklist: [{ codigo: 'CAMPOS_OBRIGATORIOS', atendido: true, comentario: null },
        { codigo: 'CONTEUDO_ADEQUADO', atendido: false, comentario: 'Tem promessa de retorno' }],
      apontamentos: [{ secao: 'MERCADO', comentario: 'Cite a fonte' }],
      parecer: 'Ajustes simples',
    })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-84
   * Título: Recusa e erro
   * Objetivo: Mostrar o erro do servidor ao aprovar sem checklist e enviar a recusa por fraude.
   * Decisões: Erro simulado com status 422.
   */
  it('mostra o erro da aprovação e envia a recusa por fraude', async () => {
    abrir('/admin/curadoria/r1', {
      'GET /api/v1/admin/curadoria/r1': () => ({ corpo: DETALHE }),
      'GET /api/v1/admin/curadoria/startups/s1/apresentacao': () => ({ corpo: APRESENTACAO }),
      'POST /api/v1/admin/curadoria/r1/decisao': ({ corpo }) => ((corpo as { resultado: string }).resultado === 'APROVADA'
        ? { status: 422, corpo: { detail: 'Marque todos os itens do checklist.' } }
        : { corpo: { ...REVISAO, situacao: 'AGUARDANDO_APROVACAO', aguardandoSegundaAprovacao: true } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Aprovar' }))
    expect(await screen.findByText('Marque todos os itens do checklist.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Recusar por fraude' }))
    expect(await screen.findByText('Recusa enviada para a segunda aprovação.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-85
   * Título: Montar edição
   * Objetivo: Criar a edição, incluir e ordenar startups, publicar e ver o histórico.
   * Decisões: Detalhe simulado muda a cada composição.
   */
  it('cria, compõe, ordena e publica uma edição', async () => {
    const edicao = { id: 'e1', titulo: 'Agro digital', descricao: 'Do campo', inicio: '2026-11-25', fim: '2026-12-24', situacao: 'RASCUNHO',
      temCapa: false, quantidade: 0, curador: 'Ana Curadora', atualizadaEm: '2026-11-25T12:00:00Z' }
    const cartao = (id: string, nome: string) => ({ id, nome, segmento: 'Agronegócio', estagio: 'MVP', localizacao: null, temLogotipo: false })
    let composicao: string[] = ['s1', 's2']
    const nomes: Record<string, string> = { s1: 'Alfa', s2: 'Beta', s3: 'Gama' }
    const { chamadas } = abrir('/admin/edicoes', {
      'GET /api/v1/admin/edicoes': () => ({ corpo: [] }),
      'POST /api/v1/admin/edicoes': () => ({ corpo: edicao }),
      'GET /api/v1/admin/edicoes/e1': () => ({ corpo: { edicao, criterios: 'Agro', historico: [{ acao: 'CRIADA', detalhe: null, por: 'Ana Curadora', em: '2026-11-25T12:00:00Z' }],
        startups: composicao.map((id) => ({ startup: cartao(id, nomes[id]), visivel: id !== 's2' })) } }),
      'GET /api/v1/admin/edicoes/startups-elegiveis': () => ({ corpo: [cartao('s1', 'Alfa'), cartao('s3', 'Gama')] }),
      'PUT /api/v1/admin/edicoes/e1/startups': ({ corpo }) => {
        composicao = (corpo as { startups: string[] }).startups
        return { corpo: edicao }
      },
      'POST /api/v1/admin/edicoes/e1/publicacao': () => ({ corpo: { ...edicao, situacao: 'VIGENTE' } }),
    })
    expect(await screen.findByText('Nenhuma edição ainda.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Nova edição' }))
    await userEvent.type(screen.getByLabelText('Título'), 'Agro digital')
    await userEvent.type(screen.getByLabelText('Descrição'), 'Do campo')
    await userEvent.type(screen.getByLabelText('Critérios de inclusão'), 'Agro')
    await userEvent.type(screen.getByLabelText('Início'), '2026-11-25')
    await userEvent.click(screen.getByRole('button', { name: 'Criar edição' }))
    const lista = await screen.findByRole('region', { name: 'Startups da edição' })
    expect(lista).toHaveTextContent('oculta para o investidor')
    await userEvent.click(within(lista).getByRole('button', { name: 'Descer Alfa' }))
    expect(composicao).toEqual(['s2', 's1'])
    await userEvent.selectOptions(await screen.findByLabelText('Incluir startup'), 's3')
    expect(composicao).toEqual(['s2', 's1', 's3'])
    await userEvent.click(within(lista).getAllByRole('button', { name: 'Retirar' })[0])
    expect(composicao).toEqual(['s1', 's3'])
    await userEvent.click(screen.getByRole('button', { name: 'Publicar' }))
    expect(await screen.findByText('Edição publicada.')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Histórico da edição' })).toHaveTextContent('criada por Ana Curadora')
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/admin/edicoes')).toBe(true)
  })
})
