import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_ATUACAO } from '../sessao/atuacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'a1', nome: 'Ana Investidora', email: 'ana@exemplo.com', papeis: ['INVESTIDOR'] },
}

const AURORA = {
  id: 'o1', nome: 'Fundo Aurora', tipo: 'FUNDO_VC', cnpj: '11.222.333/0001-81', site: null, descricao: null,
  meuPapel: 'ADMINISTRADOR',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Abrir uma tela logada
 * Objetivo: Renderizar o endereço com a sessão de investidora e as rotas simuladas.
 * Decisões: Papel ativo de investidor.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/TIPO_INVESTIDOR': () => ({
      corpo: [{ codigo: 'ANJO', rotulo: 'Investidor-anjo', grupo: null }, { codigo: 'FUNDO_VC', rotulo: 'Fundo de Venture Capital', grupo: null }],
    }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Organizações investidoras (KAN-60)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Criar organização
   * Objetivo: Conferir a lista vazia, os tipos sem investidor-anjo e o envio do cadastro, que abre a organização.
   * Decisões: O servidor devolve a organização criada.
   */
  it('cria uma organização', async () => {
    let criada = false
    const { chamadas } = abrir('/investidor/organizacoes', {
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: criada ? [AURORA] : [] }),
      'POST /api/v1/organizacoes': () => { criada = true; return { status: 201, corpo: AURORA } },
      'GET /api/v1/organizacoes/o1': () => ({ corpo: { organizacao: AURORA, membros: [] } }),
      'GET /api/v1/organizacoes/o1/convites': () => ({ corpo: [] }),
    })
    expect(await screen.findByText(/ainda não participa de nenhuma organização/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Criar organização' }))
    const form = screen.getByRole('form', { name: 'Nova organização' })
    await screen.findByRole('option', { name: 'Fundo de Venture Capital' })
    expect(within(form).queryByRole('option', { name: 'Investidor-anjo' })).not.toBeInTheDocument()
    await userEvent.type(within(form).getByLabelText('Nome'), 'Fundo Aurora')
    await userEvent.selectOptions(within(form).getByLabelText('Tipo'), 'FUNDO_VC')
    await userEvent.type(within(form).getByLabelText('CNPJ'), '11.222.333/0001-81')
    await userEvent.click(within(form).getByRole('button', { name: 'Criar organização' }))
    expect(await screen.findByRole('heading', { name: 'Fundo Aurora' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/organizacoes')?.corpo)
      .toMatchObject({ nome: 'Fundo Aurora', tipo: 'FUNDO_VC', cnpj: '11.222.333/0001-81' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Administrar e sair
   * Objetivo: Conferir convite, troca de papel e a saída com confirmação na própria tela.
   * Decisões: A saída volta para a lista.
   */
  it('convida, troca papel e sai com confirmação', async () => {
    const { chamadas } = abrir('/investidor/organizacoes/o1', {
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: [AURORA] }),
      'GET /api/v1/organizacoes/o1': () => ({
        corpo: { organizacao: AURORA, membros: [
          { usuarioId: 'a1', nome: 'Ana Investidora', papel: 'ADMINISTRADOR', entrouEm: '2026-11-24T15:00:00Z' },
          { usuarioId: 'b1', nome: 'Bia', papel: 'MEMBRO', entrouEm: '2026-11-24T16:00:00Z' },
        ] },
      }),
      'GET /api/v1/organizacoes/o1/convites': () => ({ corpo: [] }),
      'POST /api/v1/organizacoes/o1/convites': () => ({ status: 201, corpo: {} }),
      'PUT /api/v1/organizacoes/o1/membros/b1': () => ({ status: 204 }),
      'DELETE /api/v1/organizacoes/o1/membros/a1': () => ({ status: 204 }),
    })
    const convites = await screen.findByRole('region', { name: 'Convites' })
    await userEvent.type(within(convites).getByLabelText('E-mail'), 'caio@exemplo.com')
    await userEvent.click(within(convites).getByRole('button', { name: 'Enviar convite' }))
    expect(await screen.findByText('Convite enviado para caio@exemplo.com.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/organizacoes/o1/convites')?.corpo)
      .toEqual({ email: 'caio@exemplo.com', papel: 'MEMBRO' })

    await userEvent.selectOptions(screen.getByLabelText('Papel de Bia'), 'OBSERVADOR')
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/organizacoes/o1/membros/b1')?.corpo).toEqual({ papel: 'OBSERVADOR' })

    await userEvent.click(screen.getByRole('button', { name: 'Sair desta organização' }))
    const confirmar = screen.getByRole('group', { name: 'Confirmar saída' })
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/organizacoes/o1/membros/a1')).toBe(false)
    await userEvent.click(within(confirmar).getByRole('button', { name: 'Sair da organização' }))
    expect(await screen.findByRole('heading', { name: 'Organizações' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Convite sem papel de investidor
   * Objetivo: Conferir que o convite mostra a organização e, sem o papel, explica e leva à conta.
   * Decisões: Erro 422 com o código sem_papel_investidor.
   */
  it('explica o aceite sem papel de investidor', async () => {
    abrir('/organizacoes/convite?token=tk', {
      'POST /api/v1/organizacoes/convites/consulta': () => ({
        corpo: { organizacao: 'Fundo Aurora', tipo: 'FUNDO_VC', papel: 'MEMBRO', email: 'ana@exemplo.com', expiraEm: '2026-12-08T15:00:00Z' },
      }),
      'POST /api/v1/organizacoes/convites/aceite': () => ({
        status: 422, corpo: { detail: 'Ative o papel de investidor na sua conta para entrar na organização.', codigo: 'sem_papel_investidor' },
      }),
    })
    expect(await screen.findByText('Fundo Aurora')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Aceitar e entrar' }))
    expect(await screen.findByText(/Ative o papel de investidor/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Ativar o papel de investidor/ })).toHaveAttribute('href', '/conta')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Seletor de atuação
   * Objetivo: Conferir que escolher a organização manda o cabeçalho X-Orbit-Organizacao e que uma escolha antiga de uma
   *           organização que sumiu volta para "por mim".
   * Decisões: A área do investidor recarrega com a nova atuação.
   */
  it('escolhe a atuação e manda o cabeçalho', async () => {
    localStorage.setItem(CHAVE_ATUACAO, JSON.stringify({ usuario: 'a1', organizacao: 'saiu', nome: 'Antiga' }))
    const { chamadas } = abrir('/investidor', {
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: [AURORA] }),
      'GET /api/v1/investidores/eu': ({ cabecalhos }) => ({
        corpo: { teseCadastrada: false, proximoPasso: '/investidor/onboarding',
          atuandoPor: cabecalhos['X-Orbit-Organizacao'] === 'o1' ? { organizacao: 'o1', nome: 'Fundo Aurora', papel: 'ADMINISTRADOR' } : null },
      }),
    })
    const seletor = await screen.findByRole('combobox', { name: 'Atuando por' })
    expect(seletor).toHaveValue('')
    expect(localStorage.getItem(CHAVE_ATUACAO)).toBeNull()
    await userEvent.selectOptions(seletor, 'o1')
    expect(await screen.findByText('Fundo Aurora', { selector: 'strong' })).toBeInTheDocument()
    expect(chamadas.filter((c) => c.chave === 'GET /api/v1/investidores/eu').at(-1)?.cabecalhos['X-Orbit-Organizacao']).toBe('o1')
  })
})
