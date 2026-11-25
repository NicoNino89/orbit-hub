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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Abrir a área do empreendedor
 * Objetivo: Renderizar o endereço com a sessão da empreendedora, as listas e as rotas informadas.
 * Decisões: Papel ativo de empreendedor.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'EMPREENDEDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [{ codigo: 'AGRONEGOCIO', rotulo: 'Agronegócio', grupo: null }] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [{ codigo: 'MT', rotulo: 'Mato Grosso', grupo: 'CENTRO_OESTE' }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Cadastro da startup (KAN-21)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:33
   * Cartão Jira: KAN-21
   * Título: Criar e editar
   * Objetivo: Conferir a criação, a abertura do editor com o selo de empresa não constituída e o salvamento com um
   *           fundador.
   * Decisões: O servidor simulado devolve a startup criada.
   */
  it('cria a startup e salva os dados cadastrais', async () => {
    const { chamadas } = abrir('/empreendedor/startups/nova', {
      'POST /api/v1/startups': () => ({ status: 201, corpo: STARTUP }),
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'PUT /api/v1/startups/s1/cadastro': ({ corpo }) => ({ corpo: { ...STARTUP, ...(corpo as object), atualizadaEm: '2026-11-24T18:05:00Z' } }),
    })
    const form = await screen.findByRole('form', { name: 'Dados cadastrais' })
    await userEvent.type(within(form).getByLabelText('Nome da startup'), 'NovaSafra')
    await within(form).findByRole('option', { name: 'Agronegócio' })
    await userEvent.selectOptions(within(form).getByLabelText('Segmento'), 'AGRONEGOCIO')
    await userEvent.selectOptions(within(form).getByLabelText('Localização'), 'MT')
    await userEvent.click(within(form).getByRole('button', { name: 'Criar startup' }))
    expect(await screen.findByRole('heading', { name: 'NovaSafra' })).toBeInTheDocument()
    expect(screen.getByText('Empresa ainda não constituída')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups')?.corpo).toMatchObject({ nome: 'NovaSafra', segmento: 'AGRONEGOCIO', localizacao: 'MT' })

    const editor = screen.getByRole('form', { name: 'Dados cadastrais' })
    await userEvent.click(within(editor).getByRole('button', { name: 'Adicionar fundador' }))
    const fundador = within(editor).getByRole('group', { name: 'Fundador 1' })
    await userEvent.type(within(fundador).getByLabelText('Nome'), 'Eva')
    await userEvent.type(within(fundador).getByLabelText('Participação (%)'), '60')
    await userEvent.click(within(editor).getByRole('button', { name: 'Salvar dados cadastrais' }))
    expect(await screen.findByText('Dados cadastrais salvos.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/cadastro')?.corpo)
      .toMatchObject({ fundadores: [{ nome: 'Eva', cargo: null, participacao: 60 }] })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Colaborador
   * Objetivo: Conferir que o colaborador sem a seção de dados cadastrais vê o cadastro bloqueado, sem botão de salvar
   *           nem envio de logotipo.
   * Decisões: Autorizado só em Mercado (KAN-65).
   */
  it('bloqueia o cadastro para o colaborador sem a seção', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'COLABORADOR', minhasSecoes: ['MERCADO'], cnpj: '11.222.333/0001-81', constituida: true } }),
    })
    expect(await screen.findByRole('heading', { name: 'NovaSafra' })).toBeInTheDocument()
    expect(screen.queryByText('Empresa ainda não constituída')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Nome da startup')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Salvar dados cadastrais' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Arquivo do logotipo')).not.toBeInTheDocument()
  })
})
