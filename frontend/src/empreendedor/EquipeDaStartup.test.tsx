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

const EQUIPE = [
  { usuarioId: 'e1', nome: 'Eva Empreendedora', papel: 'FUNDADOR_ADMINISTRADOR', secoes: [], entrouEm: '2026-11-24T18:00:00Z' },
  { usuarioId: 'l1', nome: 'Lia Colaboradora', papel: 'COLABORADOR', secoes: ['MERCADO'], entrouEm: '2026-11-24T19:00:00Z' },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
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

describe('Equipe da startup (KAN-65)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Convidar colaborador e trocar papel
   * Objetivo: Conferir a aba da equipe com as seções do colaborador, o convite com seções e a troca de papel.
   * Decisões: O servidor simulado aceita tudo.
   */
  it('convida um colaborador com seções e troca o papel de um membro', async () => {
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/s1/equipe': () => ({ corpo: EQUIPE }),
      'GET /api/v1/startups/s1/convites': () => ({ corpo: [] }),
      'POST /api/v1/startups/s1/convites': () => ({ status: 201, corpo: {} }),
      'PUT /api/v1/startups/s1/equipe/l1': () => ({ status: 204 }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Equipe' }))
    expect(await screen.findByText(/edita: Mercado/)).toBeInTheDocument()

    const convite = screen.getByRole('form', { name: 'Enviar convite' })
    await userEvent.type(screen.getByLabelText('E-mail'), 'rui@exemplo.com')
    await userEvent.selectOptions(within(convite).getByLabelText('Papel'), 'COLABORADOR')
    await userEvent.click(within(convite).getByLabelText('Tração e resultados'))
    await userEvent.click(within(convite).getByRole('button', { name: 'Enviar convite' }))
    expect(await screen.findByText('Convite enviado para rui@exemplo.com.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/convites')?.corpo)
      .toEqual({ email: 'rui@exemplo.com', papel: 'COLABORADOR', secoes: ['TRACAO'] })

    const lia = screen.getByText('Lia Colaboradora').closest('li') as HTMLElement
    await userEvent.click(within(lia).getByRole('button', { name: 'Mudar papel' }))
    const escolha = within(lia).getByRole('form', { name: 'Salvar papel de Lia Colaboradora' })
    await userEvent.selectOptions(within(escolha).getByLabelText('Papel'), 'FUNDADOR')
    await userEvent.click(within(escolha).getByRole('button', { name: 'Salvar papel' }))
    expect(await screen.findByText('Papel alterado.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/equipe/l1')?.corpo).toEqual({ papel: 'FUNDADOR', secoes: [] })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Último administrador não sai
   * Objetivo: Conferir que a saída pede confirmação e mostra a recusa do servidor.
   * Decisões: Erro 422 com o código ultimo_administrador.
   */
  it('explica que o último administrador não pode sair', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/s1/equipe': () => ({ corpo: EQUIPE }),
      'GET /api/v1/startups/s1/convites': () => ({ corpo: [] }),
      'DELETE /api/v1/startups/s1/equipe/e1': () => ({
        status: 422, corpo: { detail: 'A startup precisa de pelo menos um fundador administrador. Indique outro antes.', codigo: 'ultimo_administrador' },
      }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Equipe' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Sair desta equipe' }))
    await userEvent.click(within(screen.getByRole('group', { name: 'Confirmar saída' })).getByRole('button', { name: 'Sair da equipe' }))
    expect(await screen.findByText(/precisa de pelo menos um fundador administrador/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Colaborador com a seção
   * Objetivo: Conferir que o colaborador autorizado nos dados cadastrais edita e não vê as ações de administrador.
   * Decisões: Sem convites para quem não é administrador.
   */
  it('libera o cadastro para o colaborador autorizado e esconde a gestão da equipe', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'COLABORADOR', minhasSecoes: ['CADASTRO'] } }),
      'GET /api/v1/startups/s1/equipe': () => ({ corpo: EQUIPE }),
    })
    expect(await screen.findByRole('button', { name: 'Salvar dados cadastrais' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Equipe' }))
    expect(await screen.findByText('Lia Colaboradora')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mudar papel' })).not.toBeInTheDocument()
    expect(screen.queryByRole('form', { name: 'Enviar convite' })).not.toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Aceitar o convite
   * Objetivo: Conferir que o convite mostra a startup, o papel e as seções, e abre a startup ao aceitar.
   * Decisões: Token na query string.
   */
  it('mostra e aceita o convite para a startup', async () => {
    const { chamadas } = abrir('/startups/convite?token=tk', {
      'POST /api/v1/startups/convites/consulta': () => ({
        corpo: { startup: 'NovaSafra', papel: 'COLABORADOR', secoes: ['MERCADO'], email: 'eva@exemplo.com', expiraEm: '2026-12-08T15:00:00Z' },
      }),
      'POST /api/v1/startups/convites/aceite': () => ({ corpo: { id: 's1', nome: 'NovaSafra' } }),
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'COLABORADOR', minhasSecoes: ['MERCADO'] } }),
    })
    expect(await screen.findByText(/Seções que você poderá editar: Mercado/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Aceitar e entrar' }))
    expect(await screen.findByRole('heading', { name: 'NovaSafra' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/convites/aceite')?.corpo).toEqual({ token: 'tk' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Convite sem papel de empreendedor
   * Objetivo: Conferir que, sem o papel, a tela explica e leva à conta.
   * Decisões: Erro 422 com o código sem_papel_empreendedor.
   */
  it('explica o aceite sem papel de empreendedor', async () => {
    abrir('/startups/convite?token=tk', {
      'POST /api/v1/startups/convites/consulta': () => ({
        corpo: { startup: 'NovaSafra', papel: 'FUNDADOR', secoes: [], email: 'eva@exemplo.com', expiraEm: '2026-12-08T15:00:00Z' },
      }),
      'POST /api/v1/startups/convites/aceite': () => ({
        status: 422, corpo: { detail: 'Ative o papel de empreendedor na sua conta para entrar na equipe.', codigo: 'sem_papel_empreendedor' },
      }),
    })
    expect(await screen.findByText('NovaSafra')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Aceitar e entrar' }))
    expect(await screen.findByText(/Ative o papel de empreendedor/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Ativar o papel de empreendedor/ })).toHaveAttribute('href', '/conta')
  })
})
