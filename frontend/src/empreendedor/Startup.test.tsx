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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:13
   * Cartão Jira: KAN-21
   * Título: Logotipo
   * Objetivo: Conferir que o logotipo salvo aparece e que o envio de um novo mostra o aviso.
   * Decisões: Endereço local do arquivo simulado (o jsdom não cria).
   */
  it('mostra o logotipo e envia um novo', async () => {
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:logo'), revokeObjectURL: vi.fn() }))
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, temLogotipo: true } }),
      'GET /api/v1/startups/s1/logotipo': () => ({ corpo: {} }),
      'PUT /api/v1/startups/s1/logotipo': () => ({ corpo: { ...STARTUP, temLogotipo: true, atualizadaEm: '2026-11-24T18:10:00Z' } }),
    })
    expect(await screen.findByRole('img', { name: 'Logotipo de NovaSafra' })).toHaveAttribute('src', 'blob:logo')
    await userEvent.upload(screen.getByLabelText('Arquivo do logotipo'), new File(['png'], 'logo.png', { type: 'image/png' }))
    expect(await screen.findByText('Logotipo atualizado.')).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/startups/s1/logotipo')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:13
   * Cartão Jira: KAN-21
   * Título: Recusas do servidor
   * Objetivo: Conferir que as mensagens do servidor aparecem ao criar, ao salvar, ao enviar o logotipo e ao abrir uma
   *           startup que não existe.
   * Decisões: Erros 422 e 404 simulados.
   */
  it('mostra as recusas do servidor', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'PUT /api/v1/startups/s1/cadastro': () => ({ status: 422, corpo: { detail: 'Este CNPJ já está cadastrado.', codigo: 'cnpj_repetido' } }),
      'PUT /api/v1/startups/s1/logotipo': () => ({ status: 422, corpo: { detail: 'Envie PNG, JPG ou WebP.', codigo: 'formato_invalido' } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Salvar dados cadastrais' }))
    expect(await screen.findByText('Este CNPJ já está cadastrado.')).toBeInTheDocument()
    await userEvent.upload(screen.getByLabelText('Arquivo do logotipo'), new File(['x'], 'logo.png', { type: 'image/png' }))
    expect(await screen.findByText('Envie PNG, JPG ou WebP.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:13
   * Cartão Jira: KAN-21
   * Título: Criação recusada e startup inexistente
   * Objetivo: Conferir a mensagem ao criar com erro e ao abrir uma startup de fora da equipe.
   * Decisões: Erros 422 e 404 simulados.
   */
  it('mostra o erro ao criar e ao abrir startup de fora', async () => {
    abrir('/empreendedor/startups/nova', {
      'POST /api/v1/startups': () => ({ status: 422, corpo: { detail: 'Informe o nome da startup.', codigo: 'nome_obrigatorio' } }),
    })
    const form = await screen.findByRole('form', { name: 'Dados cadastrais' })
    await userEvent.type(within(form).getByLabelText('Nome da startup'), 'X')
    await within(form).findByRole('option', { name: 'Agronegócio' })
    await userEvent.selectOptions(within(form).getByLabelText('Segmento'), 'AGRONEGOCIO')
    await userEvent.selectOptions(within(form).getByLabelText('Localização'), 'MT')
    await userEvent.click(within(form).getByRole('button', { name: 'Criar startup' }))
    expect(await screen.findByText('Informe o nome da startup.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:13
   * Cartão Jira: KAN-21
   * Título: Startup de fora
   * Objetivo: Conferir a mensagem quando a startup não é da equipe.
   * Decisões: 404 simulado.
   */
  it('avisa quando a startup não é da equipe', async () => {
    abrir('/empreendedor/startups/s9', {
      'GET /api/v1/startups/s9': () => ({ status: 404, corpo: { detail: 'Startup não encontrada.' } }),
    })
    expect(await screen.findByText('Startup não encontrada.')).toBeInTheDocument()
  })
})
