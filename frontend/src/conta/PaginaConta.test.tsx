import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Conta } from '../api/conta'
import { simularApi } from '../test/apiSimulada'
import { CHAVE_SESSAO_ATIVA } from '../sessao/contexto'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'u1', nome: 'Ana Souza', email: 'ana@exemplo.com', papeis: ['EMPREENDEDOR'] },
}
const CONTA: Conta = { id: 'u1', nome: 'Ana Souza', email: 'ana@exemplo.com', telefone: null, temFoto: false, papeis: ['EMPREENDEDOR'] }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Abrir a tela de conta logado
 * Objetivo: Simular a API com as rotas da conta e abrir /conta com sessão restaurada.
 * Decisões: As rotas extras do teste sobrescrevem as padrão.
 */
async function abrirConta(extras: Parameters<typeof simularApi>[0] = {}) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/conta': () => ({ corpo: CONTA }),
    'GET /api/v1/conta/historico': () => ({ corpo: [{ evento: 'PAPEL_ATIVADO', detalhe: 'EMPREENDEDOR', ocorridoEm: '2026-11-24T04:00:00Z' }] }),
    'GET /api/v1/documentos-legais': () => ({ corpo: [{ id: 'termo-c', tipo: 'TERMO_DE_CONFIDENCIALIDADE' }] }),
    ...extras,
  })
  render(
    <MemoryRouter initialEntries={['/conta']}>
      <Rotas />
    </MemoryRouter>,
  )
  await screen.findByRole('heading', { name: 'Minha conta' })
  return api
}

describe('PaginaConta', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Editar dados e ver histórico
   * Objetivo: Garantir o envio de nome e telefone e a exibição do histórico.
   * Decisões: Confere o corpo do PUT.
   */
  it('salva nome e telefone e mostra o histórico', async () => {
    const { chamadas } = await abrirConta({
      'PUT /api/v1/conta': ({ corpo }) => ({ corpo: { ...CONTA, ...(corpo as object) } }),
    })
    expect(screen.getByText(/Papel ativado: Empreendedor/)).toBeInTheDocument()

    const nome = screen.getByLabelText('Nome')
    await userEvent.clear(nome)
    await userEvent.type(nome, 'Ana Lima')
    await userEvent.type(screen.getByLabelText('Telefone'), '(11) 99999-0000')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar dados' }))

    expect(await screen.findByText('Dados salvos.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/conta')?.corpo).toEqual({ nome: 'Ana Lima', telefone: '(11) 99999-0000' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Ativar investidor
   * Objetivo: Garantir que ativar investidor exige a caixa da confidencialidade e envia o aceite.
   * Decisões: Depois da ativação, a sessão é renovada e a pessoa vai ao painel.
   */
  it('ativa o papel de investidor com o Termo de Confidencialidade', async () => {
    const { chamadas } = await abrirConta({
      'POST /api/v1/conta/papeis/INVESTIDOR': () => ({ corpo: { proximoPasso: '/investidor/onboarding' } }),
    })
    const botao = screen.getByRole('button', { name: 'Ativar investidor' })
    expect(botao).toBeDisabled()
    await userEvent.click(screen.getByRole('checkbox', { name: /Termo de Confidencialidade/ }))
    await userEvent.click(botao)

    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conta/papeis/INVESTIDOR')?.corpo).toEqual({ aceites: ['termo-c'] })
    expect(chamadas.filter((c) => c.chave === 'POST /api/v1/auth/renovar').length).toBeGreaterThanOrEqual(2)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Pendência ao desativar
   * Objetivo: Garantir que a mensagem de pendência da API aparece ao tentar desativar.
   * Decisões: Conta com os dois papéis.
   */
  it('mostra a pendência ao desativar um papel', async () => {
    await abrirConta({
      'GET /api/v1/conta': () => ({ corpo: { ...CONTA, papeis: ['EMPREENDEDOR', 'INVESTIDOR'] } }),
      'DELETE /api/v1/conta/papeis/EMPREENDEDOR': () => ({ status: 422, corpo: {
        detail: 'Não é possível desativar este papel agora: 1 rodada aberta.', codigo: 'papel_com_pendencias' } }),
    })
    await userEvent.click(screen.getByRole('button', { name: 'Desativar empreendedor' }))
    expect(await screen.findByText(/1 rodada aberta/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Troca de e-mail e de senha
   * Objetivo: Garantir o pedido de troca de e-mail e a troca de senha com repetição conferida.
   * Decisões: A senha atual errada aparece no campo.
   */
  it('pede troca de e-mail e troca a senha', async () => {
    let tentativaSenha = 0
    const { chamadas } = await abrirConta({
      'POST /api/v1/conta/email': () => ({ status: 202 }),
      'PUT /api/v1/conta/senha': () => (++tentativaSenha === 1
        ? { status: 422, corpo: { detail: 'A senha atual está incorreta.', codigo: 'senha_atual_incorreta' } }
        : { status: 204 }),
    })
    const secaoEmail = screen.getByRole('region', { name: 'Trocar e-mail' })
    await userEvent.type(within(secaoEmail).getByLabelText('Novo e-mail'), 'novo@exemplo.com')
    await userEvent.type(within(secaoEmail).getByLabelText(/Senha atual/), 'senha-atual-longa')
    await userEvent.click(within(secaoEmail).getByRole('button', { name: 'Enviar link de confirmação' }))
    expect(await screen.findByText(/Enviamos um link para/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/conta/email')?.corpo)
      .toEqual({ novoEmail: 'novo@exemplo.com', senhaAtual: 'senha-atual-longa' })

    const secaoSenha = screen.getByRole('region', { name: 'Trocar senha' })
    await userEvent.type(within(secaoSenha).getByLabelText('Senha atual'), 'errada-123456')
    await userEvent.type(within(secaoSenha).getByLabelText('Senha nova'), 'nova-senha-bem-longa')
    await userEvent.type(within(secaoSenha).getByLabelText('Repita a senha nova'), 'nova-senha-bem-longa')
    await userEvent.click(within(secaoSenha).getByRole('button', { name: 'Trocar senha' }))
    expect(await within(secaoSenha).findByText('A senha atual está incorreta.')).toBeInTheDocument()

    await userEvent.click(within(secaoSenha).getByRole('button', { name: 'Trocar senha' }))
    expect(await screen.findByText(/Os outros aparelhos foram desconectados/)).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Enviar foto
   * Objetivo: Garantir que a foto escolhida é enviada como formulário e que o erro da API aparece.
   * Decisões: A API recusa o formato.
   */
  it('envia a foto e mostra a recusa da API', async () => {
    const { chamadas } = await abrirConta({
      'PUT /api/v1/conta/foto': () => ({ status: 422, corpo: { detail: 'Use uma imagem JPEG, PNG ou WebP.', codigo: 'foto_invalida' } }),
    })
    const arquivo = new File(['texto'], 'foto.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('Trocar foto'), { target: { files: [arquivo] } })

    expect(await screen.findByText('Use uma imagem JPEG, PNG ou WebP.')).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/conta/foto')).toBe(true)
  })
})

describe('PaginaNovoEmail', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Confirmar novo e-mail
   * Objetivo: Garantir a confirmação com o token do link e a mensagem de falha.
   * Decisões: Dois endereços: um que confirma e um com e-mail já tomado.
   */
  it('confirma o novo e-mail e mostra a falha da API', async () => {
    simularApi({
      'POST /api/v1/contas/email/confirmacao': ({ corpo }) => ((corpo as { token: string }).token === 'ok'
        ? { status: 204 }
        : { status: 422, corpo: { detail: 'Já existe uma conta com este e-mail.', codigo: 'email_em_uso' } }),
    })
    const { unmount } = render(<MemoryRouter initialEntries={['/confirmar-novo-email?token=ok']}><Rotas /></MemoryRouter>)
    expect(await screen.findByRole('heading', { name: 'E-mail alterado' })).toBeInTheDocument()
    unmount()

    render(<MemoryRouter initialEntries={['/confirmar-novo-email?token=tomado']}><Rotas /></MemoryRouter>)
    expect(await screen.findByText('Já existe uma conta com este e-mail.')).toBeInTheDocument()
  })
})
