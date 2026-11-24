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
  usuario: { id: 'a1', nome: 'Ana Investidora', email: 'ana@exemplo.com', papeis: ['INVESTIDOR'] },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Abrir a central
 * Objetivo: Renderizar a central com um aviso não lido, um lido e as preferências.
 * Decisões: O contador começa em 1 e cai para 0 depois da leitura.
 */
function abrir() {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  let lido = false
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/notificacoes/contagem': () => ({ corpo: { naoLidas: lido ? 0 : 1 } }),
    'GET /api/v1/notificacoes': () => ({
      corpo: {
        pagina: 0, total: 2, naoLidas: lido ? 0 : 1,
        itens: [
          { id: 'n1', tipo: 'MATCH', titulo: 'Match com a NovaSafra', mensagem: 'A NovaSafra aceitou o seu interesse.',
            link: '/investidor', criadaEm: '2026-11-24T15:00:00Z', lida: lido },
          { id: 'n2', tipo: 'SEGURANCA_CONTA', titulo: 'Senha alterada', mensagem: 'A senha foi trocada.', link: null,
            criadaEm: '2026-11-23T15:00:00Z', lida: true },
        ],
      },
    }),
    'POST /api/v1/notificacoes/n1/leitura': () => { lido = true; return { status: 204 } },
    'GET /api/v1/notificacoes/preferencias': () => ({
      corpo: {
        resumoDiario: false,
        tipos: [
          { tipo: 'MATCH', grupo: 'Conexões', descricao: 'Match confirmado', obrigatoria: false, canal: 'AMBOS' },
          { tipo: 'SEGURANCA_CONTA', grupo: 'Conta', descricao: 'Segurança da conta (senha e e-mail)', obrigatoria: true, canal: 'AMBOS' },
        ],
      },
    }),
    'PUT /api/v1/notificacoes/preferencias': ({ corpo }) => ({
      corpo: {
        resumoDiario: true,
        tipos: [
          { tipo: 'MATCH', grupo: 'Conexões', descricao: 'Match confirmado', obrigatoria: false,
            canal: (corpo as { canais: Record<string, string> }).canais.MATCH },
          { tipo: 'SEGURANCA_CONTA', grupo: 'Conta', descricao: 'Segurança da conta (senha e e-mail)', obrigatoria: true, canal: 'AMBOS' },
        ],
      },
    }),
  })
  render(<MemoryRouter initialEntries={['/notificacoes']}><Rotas /></MemoryRouter>)
  return api
}

describe('Central de notificações (KAN-146)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:36
   * Cartão Jira: KAN-146
   * Título: Sino e leitura
   * Objetivo: Conferir o contador no sino, a lista e que marcar como lido zera o sino.
   * Decisões: O sino recebe o evento da central.
   */
  it('mostra o contador e marca como lido', async () => {
    abrir()
    expect(await screen.findByRole('link', { name: 'Avisos: 1 não lido(s)' })).toBeInTheDocument()
    const lista = await screen.findByRole('list', { name: 'Avisos' })
    expect(within(lista).getByText('Match com a NovaSafra')).toBeInTheDocument()
    expect(screen.getByText('1 não lido(s)')).toBeInTheDocument()
    await userEvent.click(within(lista).getByRole('button', { name: 'Marcar como lido' }))
    expect(await screen.findByText('Tudo lido.')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Avisos' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:36
   * Cartão Jira: KAN-146
   * Título: Preferências
   * Objetivo: Conferir que o obrigatório fica travado e que a escolha e o resumo vão para a API.
   * Decisões: Só os não obrigatórios são enviados.
   */
  it('salva as preferências sem mexer nos obrigatórios', async () => {
    const { chamadas } = abrir()
    await userEvent.click(await screen.findByRole('tab', { name: 'Como receber' }))
    const seguranca = await screen.findByRole('combobox', { name: /Segurança da conta/ })
    expect(seguranca).toBeDisabled()
    await userEvent.selectOptions(screen.getByRole('combobox', { name: /Match confirmado/ }), 'EMAIL')
    await userEvent.click(screen.getByRole('checkbox', { name: /resumo diário/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Salvar preferências' }))
    expect(await screen.findByText('Preferências salvas.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/notificacoes/preferencias')?.corpo)
      .toEqual({ canais: { MATCH: 'EMAIL' }, resumoDiario: true })
  })
})
