import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const APRESENTACAO = {
  modo: 'CATALOGO', comoVerificado: true,
  cabecalho: { id: 's1', nome: 'Conecta', segmento: 'Fintech', estagio: 'MVP', localizacao: null, site: null, redes: [], temLogotipo: false,
    naoConstituida: false, selo: 'NAO_VERIFICADO' },
  paginas: [{ id: 'apresentacao', titulo: 'Apresentação', blocos: [] }],
}

const CONEXAO = {
  id: 'c1', startupId: 's1', startup: 'Conecta', situacao: 'PENDENTE', valorPreliminar: 100000, condicoes: 'Mútuo', pedido: 'APRESENTACAO',
  mensagem: 'Podemos conversar?', criadaEm: '2026-11-25T12:00:00Z', atualizadaEm: '2026-11-25T12:00:00Z', aguardaVoce: true, motivo: null,
  investidor: { pessoa: 'Leo', organizacao: 'Fundo Leo', tipo: 'Fundo de Venture Capital', ticketMinimo: 50000, ticketMaximo: 200000,
    lideranca: 'LIDERA', selo: 'VERIFICADO' },
  contatos: [], feitaPor: 'Leo',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-47, KAN-48, KAN-49, KAN-67
 * Título: Abrir com papel
 * Objetivo: Renderizar o endereço com a sessão e o papel ativo pedidos.
 * Decisões: Listas de motivos simuladas.
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
    'GET /api/v1/listas/MOTIVO_ENCERRAMENTO': () => ({ corpo: [{ codigo: 'OUTRO', rotulo: 'Outro motivo', grupo: null }] }),
    'GET /api/v1/listas/MOTIVO_RECUSA': () => ({ corpo: [{ codigo: 'TICKET_INCOMPATIVEL', rotulo: 'Ticket incompatível', grupo: null }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Conexões (S7)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:25
   * Cartão Jira: KAN-45, KAN-46
   * Título: Demonstrar interesse
   * Objetivo: Preencher, ver o aviso de sem compromisso, confirmar e ver a conexão pendente com a opção de retirar.
   * Decisões: A situação simulada muda depois do envio.
   */
  it('demonstra interesse com confirmação e mostra a conexão', async () => {
    let comConexao = false
    const { chamadas } = abrir('/catalogo/startups/s1', 'INVESTIDOR', {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: APRESENTACAO }),
      'GET /api/v1/catalogo/startups/s1/marcacoes': () => ({ corpo: { favorito: false, descartada: false } }),
      'GET /api/v1/catalogo/startups/s1/conexao': () => ({ corpo: comConexao
        ? { conexao: CONEXAO, podeManifestar: false, impedimento: null, precisaVerificar: false }
        : { conexao: null, podeManifestar: true, impedimento: null, precisaVerificar: false } }),
      'POST /api/v1/catalogo/startups/s1/interesse': () => { comConexao = true; return { status: 201, corpo: { id: 'c1', situacao: 'PENDENTE' } } },
      'POST /api/v1/conexoes/c1/retirada': () => ({ corpo: { id: 'c1', situacao: 'RETIRADA' } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Demonstrar interesse' }))
    const form = screen.getByRole('form', { name: 'Demonstrar interesse' })
    await userEvent.type(within(form).getByLabelText('Valor preliminar (R$, opcional)'), '100000')
    await userEvent.selectOptions(within(form).getByLabelText('Pedido'), 'APRESENTACAO')
    await userEvent.click(within(form).getByRole('button', { name: 'Continuar' }))
    expect(within(form).getByRole('alert')).toHaveTextContent('não representam compromisso')
    await userEvent.click(within(form).getByRole('button', { name: 'Confirmar interesse' }))
    expect(await screen.findByRole('region', { name: 'Sua conexão' })).toHaveTextContent('Pendente')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/catalogo/startups/s1/interesse')?.corpo)
      .toEqual({ valor: 100000, condicoes: '', pedido: 'APRESENTACAO', mensagem: '' })
    await userEvent.click(screen.getByRole('button', { name: 'Retirar interesse' }))
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/conexoes/c1/retirada')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:25
   * Cartão Jira: KAN-45
   * Título: Verificação exigida
   * Objetivo: Mostrar o motivo e o caminho para se verificar.
   * Decisões: Sem botão de interesse.
   */
  it('mostra o caminho de verificação quando a startup exige', async () => {
    abrir('/catalogo/startups/s1', 'INVESTIDOR', {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: APRESENTACAO }),
      'GET /api/v1/catalogo/startups/s1/conexao': () => ({ corpo: { conexao: null, podeManifestar: false,
        impedimento: 'Esta startup aceita interesse só de investidores verificados.', precisaVerificar: true } }),
    })
    expect(await screen.findByRole('link', { name: 'Verificar agora' })).toHaveAttribute('href', '/investidor/verificacao')
    expect(screen.queryByRole('button', { name: 'Demonstrar interesse' })).not.toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:25
   * Cartão Jira: KAN-49
   * Título: Painel do investidor
   * Objetivo: Ver a conexão em Match com contato, avançar, encerrar com motivo e ver o histórico.
   * Decisões: Lista simulada.
   */
  it('mostra o painel do investidor com ações', async () => {
    const match = { ...CONEXAO, situacao: 'MATCH', contatos: [{ nome: 'Eva', email: 'eva@exemplo.com' }], investidor: null }
    const { chamadas } = abrir('/conexoes', 'INVESTIDOR', {
      'GET /api/v1/conexoes': () => ({ corpo: [match] }),
      'POST /api/v1/conexoes/c1/avanco': () => ({ corpo: { id: 'c1', situacao: 'EM_ANALISE' } }),
      'POST /api/v1/conexoes/c1/encerramento': () => ({ corpo: { id: 'c1', situacao: 'ENCERRADA' } }),
      'GET /api/v1/conexoes/c1/historico': () => ({ corpo: [{ de: null, para: 'PENDENTE', por: 'Leo', lado: 'INVESTIDOR', em: '2026-11-25T12:00:00Z' }] }),
    })
    const grupo = await screen.findByRole('region', { name: 'Match' })
    expect(grupo).toHaveTextContent('Eva · eva@exemplo.com')
    expect(grupo).toHaveTextContent('Aguarda você')
    await userEvent.click(within(grupo).getByRole('button', { name: 'Passar para em análise' }))
    await userEvent.selectOptions(await within(grupo).findByLabelText('Motivo do encerramento com Conecta'), 'OUTRO')
    await userEvent.click(within(grupo).getByRole('button', { name: 'Encerrar' }))
    await userEvent.click(within(grupo).getByRole('button', { name: 'Histórico' }))
    expect(await screen.findByRole('list', { name: 'Histórico da conexão' })).toHaveTextContent('Pendente por Leo')
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conexoes/c1/encerramento')?.corpo).toEqual({ motivo: 'OUTRO' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 11:25
   * Cartão Jira: KAN-47, KAN-48, KAN-67
   * Título: Painel da startup
   * Objetivo: Ver a manifestação com o resumo do investidor, recusar com motivo, aceitar, bloquear, desbloquear e
   *           desligar "só verificados".
   * Decisões: Startup simulada com o editor.
   */
  it('aceita, recusa e bloqueia no painel da startup', async () => {
    const startup = { id: 's1', nome: 'Conecta', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: 'FINTECH', localizacao: 'SP', site: null,
      redes: {}, fundadores: [], situacao: 'PUBLICADA', constituida: false, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
      meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [] }
    const { chamadas } = abrir('/empreendedor/startups/s1', 'EMPREENDEDOR', {
      'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
      'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
      'GET /api/v1/startups/campos': () => ({ corpo: [] }),
      'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
      'GET /api/v1/startups/s1': () => ({ corpo: startup }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { percentual: 100, podeEnviar: true, pendencias: [], situacao: 'PUBLICADA', secoes: [] } }),
      'GET /api/v1/startups/s1/curadoria': () => ({ corpo: [] }),
      'GET /api/v1/startups/s1/conexoes': () => ({ corpo: [CONEXAO] }),
      'GET /api/v1/startups/s1/bloqueios': () => ({ corpo: { apenasVerificados: true, historico: [],
        ativos: [{ tipo: 'PESSOA', alvoId: 'u9', rotulo: 'Concorrente (c@x.com)', em: '2026-11-25T12:00:00Z' }] } }),
      'POST /api/v1/conexoes/c1/aceite': () => ({ corpo: { id: 'c1', situacao: 'MATCH' } }),
      'POST /api/v1/conexoes/c1/recusa': () => ({ corpo: { id: 'c1', situacao: 'RECUSADA' } }),
      'POST /api/v1/startups/s1/bloqueios': () => ({ status: 201, corpo: {} }),
      'DELETE /api/v1/startups/s1/bloqueios/PESSOA/u9': () => ({ status: 204 }),
      'PUT /api/v1/startups/s1/apenas-verificados': () => ({ corpo: { apenasVerificados: false } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Conexões' }))
    const lista = await screen.findByRole('list', { name: 'Manifestações recebidas' })
    expect(lista).toHaveTextContent('Fundo Leo (por Leo)')
    expect(lista).toHaveTextContent('lidera rodadas')
    await userEvent.selectOptions(within(lista).getByLabelText('Motivo da recusa (só para a plataforma)'), 'TICKET_INCOMPATIVEL')
    await userEvent.click(within(lista).getByRole('button', { name: 'Recusar' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conexoes/c1/recusa')?.corpo).toEqual({ motivo: 'TICKET_INCOMPATIVEL' })
    await userEvent.click(within(lista).getByRole('button', { name: 'Aceitar (Match)' }))
    const bloqueios = screen.getByRole('region', { name: 'Bloqueios' })
    await userEvent.type(within(bloqueios).getByLabelText('E-mail'), 'outro@x.com')
    await userEvent.click(within(bloqueios).getByRole('button', { name: 'Bloquear' }))
    await userEvent.click(within(bloqueios).getByRole('button', { name: 'Desbloquear' }))
    await userEvent.click(within(bloqueios).getByLabelText('Aceitar interesse só de investidores verificados'))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/bloqueios')?.corpo).toEqual({ tipo: 'PESSOA', identificador: 'outro@x.com' })
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/startups/s1/bloqueios/PESSOA/u9')).toBe(true)
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/apenas-verificados')?.corpo).toEqual({ apenasVerificados: false })
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/conexoes/c1/aceite')).toBe(true)
  })
})
