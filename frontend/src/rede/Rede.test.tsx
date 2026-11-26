import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import axe from 'axe-core'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const AGORA = '2026-11-25T12:00:00Z'
const CONEXAO = { id: 'c1', startupId: 's1', startup: 'AgroTech', situacao: 'MATCH', valorPreliminar: null, condicoes: null, pedido: 'NENHUM', mensagem: null,
  criadaEm: AGORA, atualizadaEm: AGORA, aguardaVoce: false, motivo: null, investidor: null, contatos: [{ nome: 'Eva', email: 'eva@x.com' }], feitaPor: null }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-169, KAN-173, KAN-174, KAN-175, KAN-176, KAN-181, KAN-183
 * Título: Abrir com papel
 * Objetivo: Renderizar o endereço com a sessão e as rotas simuladas.
 * Decisões: Sem papel, abre sem sessão (página pública).
 */
function abrir(endereco: string, papeis: Papel[], rotas: Parameters<typeof simularApi>[0]) {
  if (papeis.length > 0) {
    const sessao: SessaoApi = {
      tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
      usuario: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', papeis },
    }
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    localStorage.setItem(CHAVE_PAPEL_ATIVO, papeis[0])
    rotas = { 'POST /api/v1/auth/renovar': () => ({ corpo: sessao }), ...rotas }
  }
  const api = simularApi(rotas)
  const { container } = render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return { ...api, container }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-183
 * Título: Violações graves de acessibilidade
 * Objetivo: Rodar o axe e devolver as violações sérias ou críticas.
 * Decisões: Contraste fica de fora (o jsdom não calcula estilos); é conferido na revisão visual.
 */
async function violacoesGraves(container: HTMLElement) {
  const r = await axe.run(container, { rules: { 'color-contrast': { enabled: false } } })
  return r.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical').map((v) => `${v.id}: ${v.nodes.length}`)
}

describe('Novo visual – páginas (S13)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-175, KAN-176, KAN-183
   * Título: Minha Rede e conversa
   * Objetivo: Ver a conexão com contatos, abrir a conversa e enviar mensagem, sem violações graves de acessibilidade.
   * Decisões: Investidor.
   */
  it('mostra a rede e conversa na conexão', async () => {
    const { chamadas, container } = abrir('/rede', ['INVESTIDOR'], {
      'GET /api/v1/conexoes': () => ({ corpo: [CONEXAO] }),
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: [{ id: 'o1', nome: 'Fundo Um', meuPapel: 'ADMINISTRADOR' }] }),
      'GET /api/v1/mensagens/contagem': () => ({ corpo: { naoLidas: 2 } }),
      'GET /api/v1/conexoes/c1/mensagens': () => ({ corpo: { conexaoId: 'c1', podeEscrever: true, mensagens: [{ autor: 'Eva', lado: 'STARTUP', texto: 'Olá!', em: AGORA, minha: false }] } }),
      'POST /api/v1/conexoes/c1/mensagens': () => ({ corpo: { conexaoId: 'c1', podeEscrever: true, mensagens: [] } }),
    })
    const conexao = await screen.findByRole('link', { name: 'AgroTech' })
    expect(screen.getByText('Eva (eva@x.com)')).toBeInTheDocument()
    expect(await screen.findByLabelText('2 mensagens não lidas')).toBeInTheDocument()
    expect(screen.getByText('Fundo Um · administrador')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Mensagens' }))
    expect(await screen.findByText('Olá!')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Mensagem'), 'Podemos falar amanhã?')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conexoes/c1/mensagens')?.corpo).toEqual({ texto: 'Podemos falar amanhã?' })
    await userEvent.type(screen.getByLabelText('Buscar na rede'), 'xyz')
    expect(conexao).not.toBeInTheDocument()
    expect(await violacoesGraves(container)).toEqual([])
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-174
   * Título: Minha Carteira
   * Objetivo: Somar só os confirmados e listar as rodadas; exportar CSV.
   * Decisões: Download simulado.
   */
  it('mostra a carteira do investidor', async () => {
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
    abrir('/investidor/carteira', ['INVESTIDOR'], {
      'GET /api/v1/investidores/carteira': () => ({ corpo: {
        investimentos: [{ startupId: 's1', startup: 'AgroTech', data: '2026-10-01', instrumento: 'MUTUO_CONVERSIVEL', valor: 50000, situacao: 'CONFIRMADO' },
          { startupId: 's2', startup: 'InovaAI', data: null, instrumento: null, valor: 10000, situacao: 'EM_DISPUTA' }],
        rodadas: [{ startupId: 's3', startup: 'HealthLife', situacao: 'ABERTA', prazo: '2026-12-30', intencao: 20000, entrouEm: AGORA }] } }),
    })
    expect(await screen.findByText('R$ 50.000')).toBeInTheDocument()
    expect(screen.getByText(/em disputa/)).toBeInTheDocument()
    expect(screen.getByText('HealthLife')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Exportar CSV' }))
    expect(URL.createObjectURL).toHaveBeenCalled()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-169
   * Título: Início do empreendedor
   * Objetivo: Ver a startup com situação e progresso, as pendências e as próximas reuniões.
   * Decisões: Uma startup e uma reunião confirmada no futuro.
   */
  it('mostra a página inicial do empreendedor', async () => {
    abrir('/painel', ['EMPREENDEDOR'], {
      'GET /api/v1/inicio/banners': () => ({ corpo: [] }),
      'GET /api/v1/startups/minhas': () => ({ corpo: [{ id: 's1', nome: 'AgroTech', situacao: 'PUBLICADA', constituida: true, meuPapel: 'FUNDADOR_ADMINISTRADOR' }] }),
      'GET /api/v1/startups': () => ({ corpo: [{}] }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { percentual: 80, podeEnviar: true, pendencias: [], secoes: [], situacao: 'PUBLICADA' } }),
      'GET /api/v1/startups/s1/conexoes': () => ({ corpo: [{ ...CONEXAO, situacao: 'PENDENTE' }] }),
      'GET /api/v1/reunioes': () => ({ corpo: [{ id: 'r1', situacao: 'CONFIRMADA', inicio: '2999-01-01T12:00:00Z', pauta: 'Apresentação', aguardaVoce: true }] }),
    })
    const startup = await screen.findByRole('region', { name: 'Minha startup' })
    expect(within(startup).getByText('AgroTech')).toBeInTheDocument()
    expect(await screen.findByLabelText('Cadastro 80% completo')).toBeInTheDocument()
    expect(await screen.findByText('1 interesse(s) aguardando resposta')).toBeInTheDocument()
    expect(screen.getByText('1 reunião(ões) aguardando você')).toBeInTheDocument()
    expect(screen.getByText(/Apresentação ·/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-173
   * Título: Explorar em grade
   * Objetivo: Alternar o catálogo para a grade de cartões.
   * Decisões: Investidor.
   */
  it('mostra o catálogo em grade', async () => {
    abrir('/catalogo', ['INVESTIDOR'], {
      'GET /api/v1/catalogo/startups': () => ({ corpo: { lista: 'PERSONALIZADA', verificado: true, comPerfil: true, itens: [
        { startup: { id: 's1', nome: 'AgroTech', segmento: 'Agro', estagio: 'MVP', localizacao: null, temLogotipo: false, temCapa: false, chamada: null },
          criterios: [], rodadaAberta: false, favorito: true, publicadaEm: null }] } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Grade' }))
    const grade = screen.getByRole('list', { name: 'Grade de startups' })
    expect(within(grade).getByRole('article', { name: 'AgroTech' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-181, KAN-183
   * Título: Página pública
   * Objetivo: Mostrar a proposta, o como funciona e os selos de confiança, sem violações graves de acessibilidade.
   * Decisões: Sem sessão.
   */
  it('mostra a página pública acessível', async () => {
    const { container } = abrir('/', [], { 'GET /api/v1/sobre': () => ({ corpo: { nome: 'x', versao: '1', ambiente: 'local' } }) })
    expect(await screen.findByText('Investidores que acreditam.')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Como funciona' })).toBeInTheDocument()
    expect(await violacoesGraves(container)).toEqual([])
  })
})
