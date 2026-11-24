import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import { simularApi } from '../test/apiSimulada'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Abrir a confirmação
 * Objetivo: Renderizar /confirmar-email com o endereço informado.
 * Decisões: MemoryRouter com as rotas reais.
 */
function abrir(endereco: string) {
  return render(
    <MemoryRouter initialEntries={[endereco]}>
      <Rotas />
    </MemoryRouter>,
  )
}

describe('PaginaConfirmacao', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Link válido
   * Objetivo: Garantir que o token do endereço é enviado uma vez e a tela confirma.
   * Decisões: Confere o corpo enviado e o número de chamadas.
   */
  it('confirma o e-mail com o token do link', async () => {
    const { chamadas } = simularApi({ 'POST /api/v1/contas/confirmacao': () => ({ status: 204 }) })
    abrir('/confirmar-email?token=abc123')

    expect(await screen.findByRole('heading', { name: 'E-mail confirmado' })).toBeInTheDocument()
    const envios = chamadas.filter((c) => c.chave === 'POST /api/v1/contas/confirmacao')
    expect(envios).toHaveLength(1)
    expect(envios[0].corpo).toEqual({ token: 'abc123' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Link inválido e reenvio
   * Objetivo: Garantir a mensagem de link inválido e o pedido de link novo.
   * Decisões: API responde 422 link_invalido.
   */
  it('oferece um link novo quando o token não vale', async () => {
    const { chamadas } = simularApi({
      'POST /api/v1/contas/confirmacao': () => ({ status: 422, corpo: { detail: 'Link inválido', codigo: 'link_invalido' } }),
      'POST /api/v1/contas/confirmacao/reenvio': () => ({ status: 202 }),
    })
    abrir('/confirmar-email?token=velho')

    expect(await screen.findByRole('heading', { name: 'Link inválido ou expirado' })).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('E-mail da conta'), 'ana@exemplo.com')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar novo link' }))

    expect(await screen.findByText(/enviamos um novo link/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/contas/confirmacao/reenvio')?.corpo)
      .toEqual({ email: 'ana@exemplo.com' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Endereço sem token
   * Objetivo: Garantir que sem token a tela já mostra o link inválido, sem chamar a API.
   * Decisões: Nenhuma chamada de confirmação.
   */
  it('trata endereço sem token como link inválido', async () => {
    const { chamadas } = simularApi({})
    abrir('/confirmar-email')
    expect(await screen.findByRole('heading', { name: 'Link inválido ou expirado' })).toBeInTheDocument()
    expect(chamadas).toHaveLength(0)
  })
})
