import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import { simularApi } from '../test/apiSimulada'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Abrir a tela
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

describe('Recuperação de senha', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Pedido sempre confirmado
   * Objetivo: Garantir que o pedido envia o e-mail e mostra a mesma confirmação, mesmo com falha da API.
   * Decisões: API responde 500.
   */
  it('mostra a mesma confirmação ao pedir o link', async () => {
    const { chamadas } = simularApi({ 'POST /api/v1/contas/senha/esqueci': () => ({ status: 500 }) })
    abrir('/esqueci-senha')
    await userEvent.type(screen.getByLabelText('E-mail'), 'ana@exemplo.com')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar link' }))

    expect(await screen.findByText(/Se houver uma conta ativa/)).toBeInTheDocument()
    expect(chamadas[0].corpo).toEqual({ email: 'ana@exemplo.com' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Senha nova com o link
   * Objetivo: Garantir que a senha repetida errada não é enviada e que a certa troca a senha.
   * Decisões: Confere o corpo enviado com o token do endereço.
   */
  it('troca a senha com o token do link', async () => {
    const { chamadas } = simularApi({ 'POST /api/v1/contas/senha/redefinir': () => ({ status: 204 }) })
    abrir('/redefinir-senha?token=tk1')
    await userEvent.type(screen.getByLabelText('Senha nova'), 'senha-nova-bem-longa')
    await userEvent.type(screen.getByLabelText('Repita a senha nova'), 'outra-coisa')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar senha nova' }))
    expect(screen.getByText('As duas senhas precisam ser iguais.')).toBeInTheDocument()
    expect(chamadas).toHaveLength(0)

    await userEvent.clear(screen.getByLabelText('Repita a senha nova'))
    await userEvent.type(screen.getByLabelText('Repita a senha nova'), 'senha-nova-bem-longa')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar senha nova' }))

    expect(await screen.findByRole('heading', { name: 'Senha alterada' })).toBeInTheDocument()
    expect(chamadas[0].corpo).toEqual({ token: 'tk1', senha: 'senha-nova-bem-longa' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Senha fraca e link inválido
   * Objetivo: Garantir a mensagem de senha fraca no campo e a tela de link inválido.
   * Decisões: Primeiro 422 senha_fraca, depois 422 link_invalido.
   */
  it('mostra senha fraca no campo e trata link inválido', async () => {
    let tentativa = 0
    simularApi({
      'POST /api/v1/contas/senha/redefinir': () => (++tentativa === 1
        ? { status: 422, corpo: { detail: 'Essa senha é muito comum. Escolha outra.', codigo: 'senha_fraca' } }
        : { status: 422, corpo: { detail: 'Link inválido.', codigo: 'link_invalido' } }),
    })
    abrir('/redefinir-senha?token=tk1')
    await userEvent.type(screen.getByLabelText('Senha nova'), '1234567890')
    await userEvent.type(screen.getByLabelText('Repita a senha nova'), '1234567890')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar senha nova' }))
    expect(await screen.findByText('Essa senha é muito comum. Escolha outra.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Salvar senha nova' }))
    expect(await screen.findByRole('heading', { name: 'Link inválido ou expirado' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Pedir novo link' })).toHaveAttribute('href', '/esqueci-senha')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Endereço sem token
   * Objetivo: Garantir que sem token a tela já pede um link novo.
   * Decisões: Nenhuma chamada.
   */
  it('trata endereço sem token como link inválido', () => {
    simularApi({})
    abrir('/redefinir-senha')
    expect(screen.getByRole('heading', { name: 'Link inválido ou expirado' })).toBeInTheDocument()
  })
})
