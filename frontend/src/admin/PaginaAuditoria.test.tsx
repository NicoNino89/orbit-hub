import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'
import { inicioDoDia } from './datas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:05
 * Cartão Jira: KAN-80
 * Título: Sessão de teste
 * Objetivo: Montar a resposta de renovação de sessão com os papéis pedidos.
 * Decisões: Token e validade fictícios.
 */
function sessao(papeis: Papel[]): SessaoApi {
  return {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis },
  }
}

const REGISTROS = {
  itens: [
    {
      id: 'r1', atorId: 'a1', atorNome: 'Bia Admin', acao: 'DOCUMENTO_LEGAL_PUBLICADO', objetoTipo: 'documento_legal',
      objetoId: 'd1', resultado: 'SUCESSO', statusHttp: 201, detalhe: 'Tipo TERMOS_DE_USO, versão 2', justificativa: null,
      ocorridoEm: '2026-11-24T13:00:00Z',
    },
    {
      id: 'r2', atorId: 'a1', atorNome: 'Bia Admin', acao: 'NIVEL2_ACESSO_INTERNO', objetoTipo: 'startup',
      objetoId: 's1', resultado: 'FALHA', statusHttp: 422, detalhe: 'Caso denuncia c1',
      justificativa: 'Conferir os documentos citados', ocorridoEm: '2026-11-24T12:00:00Z',
    },
  ],
  pagina: 0, tamanho: 50, total: 2,
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:05
 * Cartão Jira: KAN-80
 * Título: Abrir a tela logado
 * Objetivo: Renderizar as rotas num endereço com a API simulada.
 * Decisões: Sessão ativa no armazenamento local, como depois de um login.
 */
function abrir(endereco: string, papeis: Papel[]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao(papeis) }),
    'GET /api/v1/admin/auditoria': () => ({ corpo: REGISTROS }),
    'GET /api/v1/admin/auditoria/acoes': () => ({ corpo: ['DOCUMENTO_LEGAL_PUBLICADO', 'NIVEL2_ACESSO_INTERNO'] }),
    'GET /api/v1/admin/auditoria/atores': () => ({ corpo: [{ id: 'a1', nome: 'Bia Admin' }] }),
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Trilha de auditoria (KAN-80)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:05
   * Cartão Jira: KAN-80
   * Título: Lista os registros
   * Objetivo: Conferir que a tela mostra autor, ação, resultado e justificativa, sem ações de edição.
   * Decisões: A falha mostra o status HTTP.
   */
  it('mostra os registros com resultado e justificativa, só para leitura', async () => {
    abrir('/admin/auditoria', ['ADMIN'])
    const tabela = await screen.findByRole('table')
    const linhas = within(tabela).getAllByRole('row')
    expect(linhas).toHaveLength(3)
    expect(within(linhas[1]).getByText('DOCUMENTO_LEGAL_PUBLICADO')).toBeInTheDocument()
    expect(within(linhas[1]).getByText('Sucesso')).toBeInTheDocument()
    expect(within(linhas[2]).getByText('Falha (422)')).toBeInTheDocument()
    expect(within(linhas[2]).getByText('Justificativa: Conferir os documentos citados')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /apagar|editar|excluir/i })).not.toBeInTheDocument()
    expect(screen.getByText('2 registro(s)')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:05
   * Cartão Jira: KAN-80
   * Título: Filtros por pessoa, ação e período
   * Objetivo: Conferir que os filtros escolhidos vão para a consulta, com as datas em horário de Brasília.
   * Decisões: O "até" é inclusivo: vira o início do dia seguinte.
   */
  it('envia os filtros escolhidos para a API', async () => {
    const { fetchSimulado } = abrir('/admin/auditoria', ['ADMIN'])
    const usuario = userEvent.setup()
    await screen.findByRole('table')
    await usuario.selectOptions(await screen.findByLabelText('Pessoa'), await screen.findByRole('option', { name: 'Bia Admin' }))
    await usuario.selectOptions(screen.getByLabelText('Tipo de ação'), 'NIVEL2_ACESSO_INTERNO')
    await usuario.type(screen.getByLabelText('De'), '2026-11-01')
    await usuario.type(screen.getByLabelText('Até'), '2026-11-24')
    await usuario.click(screen.getByRole('button', { name: 'Filtrar' }))

    const urls = fetchSimulado.mock.calls.map((c) => String(c[0])).filter((u) => u.includes('/api/v1/admin/auditoria?'))
    const ultima = new URL(urls.at(-1) ?? '', 'http://localhost')
    expect(ultima.searchParams.get('ator')).toBe('a1')
    expect(ultima.searchParams.get('acao')).toBe('NIVEL2_ACESSO_INTERNO')
    expect(ultima.searchParams.get('de')).toBe('2026-11-01T03:00:00.000Z')
    expect(ultima.searchParams.get('ate')).toBe('2026-11-25T03:00:00.000Z')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:05
   * Cartão Jira: KAN-80
   * Título: Só para a equipe
   * Objetivo: Conferir que quem não é da equipe não vê o backoffice nem chama a API da trilha.
   * Decisões: O servidor também recusa; aqui é a camada da interface.
   */
  it('não abre o backoffice para quem não é da equipe', async () => {
    const { chamadas } = abrir('/admin/auditoria', ['INVESTIDOR'])
    expect(await screen.findByRole('heading', { name: 'Área da equipe' })).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave.startsWith('GET /api/v1/admin/'))).toBe(false)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:05
   * Cartão Jira: KAN-80
   * Título: Link do backoffice
   * Objetivo: Conferir que a conta da equipe vê o link do backoffice no cabeçalho.
   * Decisões: O link fica no cabeçalho da área logada.
   */
  it('mostra o link do backoffice para a equipe', async () => {
    abrir('/painel', ['ADMIN'])
    expect(await screen.findByRole('link', { name: 'Backoffice' })).toHaveAttribute('href', '/admin')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 11:05
   * Cartão Jira: KAN-80
   * Título: Conversão das datas
   * Objetivo: Conferir a conversão do dia de Brasília para o instante em UTC.
   * Decisões: Inclui a virada de mês.
   */
  it('converte o dia de Brasília para o instante da API', () => {
    expect(inicioDoDia('2026-11-24')).toBe('2026-11-24T03:00:00.000Z')
    expect(inicioDoDia('2026-11-30', 1)).toBe('2026-12-01T03:00:00.000Z')
  })
})
