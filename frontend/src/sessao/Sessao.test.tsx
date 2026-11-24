import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { simularApi } from '../test/apiSimulada'
import { CHAVE_SESSAO_ATIVA } from './contexto'

const SESSAO: SessaoApi = {
  tokenAcesso: 'token-1',
  expiraEm: '2026-11-24T05:00:00Z',
  sessaoExpiraEm: '2999-01-01T00:00:00Z',
  usuario: { id: 'u1', nome: 'Ana Souza', email: 'ana@exemplo.com', papeis: ['EMPREENDEDOR', 'INVESTIDOR'] },
  termosPendentes: false,
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Abrir a aplicação
 * Objetivo: Renderizar as rotas reais no endereço informado.
 * Decisões: MemoryRouter.
 */
function abrir(endereco: string) {
  return render(
    <MemoryRouter initialEntries={[endereco]}>
      <Rotas />
    </MemoryRouter>,
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Preencher e enviar o login
 * Objetivo: Digitar e-mail e senha e clicar em Entrar.
 * Decisões: Valores fixos.
 */
async function fazerLogin() {
  await userEvent.type(screen.getByLabelText('E-mail'), 'ana@exemplo.com')
  await userEvent.type(screen.getByLabelText('Senha'), 'uma-senha-bem-longa')
  await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('Sessão (login, área logada e logout)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Rota protegida leva ao login e volta
   * Objetivo: Garantir que, sem sessão, /painel leva ao login e, depois de entrar, volta ao painel.
   * Decisões: Confere também que as credenciais foram enviadas.
   */
  it('leva ao login e volta para a página pedida depois de entrar', async () => {
    const { chamadas } = simularApi({ 'POST /api/v1/auth/entrar': () => ({ corpo: SESSAO }) })
    abrir('/painel')
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    await fazerLogin()

    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/auth/entrar')?.corpo)
      .toEqual({ email: 'ana@exemplo.com', senha: 'uma-senha-bem-longa' })
    expect(localStorage.getItem(CHAVE_SESSAO_ATIVA)).toBe('1')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-12
   * Título: Papel ativo com dois papéis
   * Objetivo: Garantir que a interface mostra em qual papel a pessoa atua e permite trocar.
   * Decisões: Começa no primeiro papel da conta; a troca muda o próximo passo.
   */
  it('mostra e troca o papel ativo quando a conta tem os dois', async () => {
    simularApi({ 'POST /api/v1/auth/entrar': () => ({ corpo: SESSAO }) })
    abrir('/entrar')
    await fazerLogin()

    const empreendedor = await screen.findByRole('button', { name: 'Empreendedor' })
    expect(empreendedor).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText(/Cadastre sua startup/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Investidor' }))
    expect(screen.getByRole('button', { name: 'Investidor' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByText(/Defina sua tese de investimento/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Erros do login
   * Objetivo: Garantir a mensagem genérica e, para conta pendente, a oferta de reenvio do link.
   * Decisões: Primeiro 401, depois 403 conta_pendente.
   */
  it('mostra a mensagem da API e oferece reenvio para conta pendente', async () => {
    let tentativa = 0
    simularApi({
      'POST /api/v1/auth/entrar': () => (++tentativa === 1
        ? { status: 401, corpo: { detail: 'E-mail ou senha incorretos.', codigo: 'credenciais_invalidas' } }
        : { status: 403, corpo: { detail: 'Confirme seu e-mail para entrar.', codigo: 'conta_pendente' } }),
      'POST /api/v1/contas/confirmacao/reenvio': () => ({ status: 202 }),
    })
    abrir('/entrar')
    await fazerLogin()
    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha incorretos.')

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar o link de confirmação' }))
    expect(await screen.findByText(/enviamos um novo link/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-92
   * Título: Aceite no próximo acesso
   * Objetivo: Garantir que, com termos pendentes, a tela de aceite vem antes do painel e libera depois.
   * Decisões: Botão só habilita com as duas caixas marcadas; depois do aceite, a sessão é renovada.
   */
  it('pede o aceite dos termos antes de liberar o painel', async () => {
    const { chamadas } = simularApi({
      'POST /api/v1/auth/entrar': () => ({ corpo: { ...SESSAO, termosPendentes: true } }),
      'GET /api/v1/conta/aceites/pendentes': () => ({ corpo: [
        { id: 'd1', tipo: 'TERMOS_DE_USO', versao: 2, titulo: 'Termos de Uso', aceite: 'TODOS_NO_ACESSO' },
        { id: 'd2', tipo: 'POLITICA_DE_PRIVACIDADE', versao: 1, titulo: 'Política de Privacidade', aceite: 'TODOS_NO_ACESSO' },
        { id: 'd3', tipo: 'TERMO_DE_CONFIDENCIALIDADE', versao: 1, titulo: 'Termo', aceite: 'INVESTIDOR_ANTES_DO_NIVEL_2' },
      ] }),
      'POST /api/v1/conta/aceites': () => ({ status: 204 }),
      'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    })
    abrir('/painel')
    await fazerLogin()

    expect(await screen.findByRole('heading', { name: 'Atualizamos nossos termos' })).toBeInTheDocument()
    const botao = screen.getByRole('button', { name: 'Aceitar e continuar' })
    expect(await screen.findAllByRole('checkbox')).toHaveLength(2)
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de Uso/ }))
    expect(botao).toBeDisabled()
    await userEvent.click(screen.getByRole('checkbox', { name: /Política de Privacidade/ }))
    await userEvent.click(botao)

    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conta/aceites')?.corpo).toEqual({ documentos: ['d1', 'd2'] })
    expect(chamadas.find((c) => c.chave === 'GET /api/v1/conta/aceites/pendentes')?.cabecalhos.Authorization)
      .toBe('Bearer token-1')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Sair
   * Objetivo: Garantir que sair encerra no servidor, apaga a marca de sessão e volta ao login.
   * Decisões: A rota protegida redireciona para o login depois de sair.
   */
  it('sai e volta para o login', async () => {
    const { chamadas } = simularApi({
      'POST /api/v1/auth/entrar': () => ({ corpo: SESSAO }),
      'POST /api/v1/auth/sair': () => ({ status: 204 }),
    })
    abrir('/painel')
    await fazerLogin()
    await userEvent.click(await screen.findByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/auth/sair')).toBe(true)
    expect(localStorage.getItem(CHAVE_SESSAO_ATIVA)).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Restaurar a sessão ao recarregar
   * Objetivo: Garantir que, com a marca de sessão ativa, a sessão volta pelo cookie sem novo login.
   * Decisões: Simula a página recarregada direto no painel.
   */
  it('restaura a sessão ao recarregar a página', async () => {
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }) })
    abrir('/painel')
    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Destino externo recusado
   * Objetivo: Garantir que "voltar" com endereço externo não é seguido depois do login.
   * Decisões: //site-falso.exemplo cai no painel.
   */
  it('ignora destino externo no parâmetro voltar', async () => {
    simularApi({ 'POST /api/v1/auth/entrar': () => ({ corpo: SESSAO }) })
    abrir('/entrar?voltar=%2F%2Fsite-falso.exemplo')
    await fazerLogin()
    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
  })
})
