import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import { simularApi, type RespostaSimulada } from '../test/apiSimulada'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:20
 * Cartão Jira: KAN-75
 * Título: Abrir o convite
 * Objetivo: Renderizar a tela do convite com a resposta simulada do aceite.
 * Decisões: Token fixo no endereço.
 */
function abrir(resposta: () => RespostaSimulada, endereco = '/equipe/convite?token=abc') {
  const api = simularApi({ 'POST /api/v1/equipe/convite': resposta })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Convite da equipe (KAN-75)', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:20
   * Cartão Jira: KAN-75
   * Título: Aceite com senha
   * Objetivo: Conferir que as senhas precisam ser iguais e que o aceite avisa do segundo fator.
   * Decisões: O token do endereço vai junto.
   */
  it('cria a senha e avisa do aplicativo autenticador', async () => {
    const { chamadas } = abrir(() => ({ status: 204 }))
    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText('Senha'), 'Senha-da-equipe-2026')
    await usuario.type(screen.getByLabelText('Repita a senha'), 'Outra-senha-2026')
    await usuario.click(screen.getByRole('button', { name: 'Criar senha' }))
    expect(screen.getByText('As duas senhas precisam ser iguais.')).toBeInTheDocument()
    expect(chamadas).toHaveLength(0)

    await usuario.clear(screen.getByLabelText('Repita a senha'))
    await usuario.type(screen.getByLabelText('Repita a senha'), 'Senha-da-equipe-2026')
    await usuario.click(screen.getByRole('button', { name: 'Criar senha' }))
    expect(await screen.findByRole('status')).toHaveTextContent('aplicativo autenticador')
    expect(chamadas[0].corpo).toEqual({ token: 'abc', senha: 'Senha-da-equipe-2026' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:20
   * Cartão Jira: KAN-75
   * Título: Convite vencido
   * Objetivo: Conferir a orientação quando o link não vale mais ou falta o token.
   * Decisões: Pede um convite novo ao administrador.
   */
  it('orienta a pedir convite novo quando o link não vale', async () => {
    abrir(() => ({ status: 422, corpo: { detail: 'Link inválido.', codigo: 'link_invalido' } }))
    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText('Senha'), 'Senha-da-equipe-2026')
    await usuario.type(screen.getByLabelText('Repita a senha'), 'Senha-da-equipe-2026')
    await usuario.click(screen.getByRole('button', { name: 'Criar senha' }))
    expect(await screen.findByRole('heading', { name: 'Convite inválido ou expirado' })).toBeInTheDocument()
  })
})
