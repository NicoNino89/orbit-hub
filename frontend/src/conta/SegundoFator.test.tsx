import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { simularApi, type RespostaSimulada } from '../test/apiSimulada'

const SESSAO_DA_EQUIPE: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'b1', nome: 'Bia Admin', email: 'bia@orbithub.com', papeis: ['ADMIN'] },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Abrir o login com a API simulada
 * Objetivo: Renderizar a tela de login com o desafio de segundo fator na resposta do login.
 * Decisões: A resposta de confirmação pode ser trocada por teste.
 */
function abrir(etapa: 'CONFIGURAR' | 'CODIGO', confirmacao: () => RespostaSimulada = () => ({ corpo: SESSAO_DA_EQUIPE })) {
  const api = simularApi({
    'POST /api/v1/auth/entrar': () => ({ corpo: { segundoFator: etapa, desafio: 'd1', expiraEm: '2999-01-01T00:00:00Z' } }),
    'POST /api/v1/auth/segundo-fator/configuracao': () => ({
      corpo: { segredo: 'GEZD GNBV GY3T QOJQ', endereco: 'otpauth://totp/Orbit%20Hub:bia?secret=GEZDGNBVGY3TQOJQ' },
    }),
    'POST /api/v1/auth/segundo-fator': confirmacao,
  })
  render(<MemoryRouter initialEntries={['/entrar']}><Rotas /></MemoryRouter>)
  return api
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Preencher a senha
 * Objetivo: Fazer o primeiro passo do login.
 * Decisões: Dados fictícios.
 */
async function entrarComSenha() {
  const usuario = userEvent.setup()
  await usuario.type(screen.getByLabelText('E-mail'), 'bia@orbithub.com')
  await usuario.type(screen.getByLabelText('Senha'), 'Senha-da-equipe-2026')
  await usuario.click(screen.getByRole('button', { name: 'Entrar' }))
  return usuario
}

describe('Segundo fator da equipe (KAN-75)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Primeiro acesso configura o aplicativo
   * Objetivo: Conferir que a senha leva à configuração, que a chave aparece e que o código abre a sessão.
   * Decisões: O código vai com o desafio recebido no login.
   */
  it('configura o aplicativo no primeiro acesso e entra com o código', async () => {
    const { chamadas } = abrir('CONFIGURAR')
    const usuario = await entrarComSenha()
    expect(await screen.findByRole('heading', { name: 'Verificação em duas etapas' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Código de 6 dígitos')).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Gerar a chave' }))
    expect(await screen.findByLabelText('Chave do aplicativo autenticador')).toHaveTextContent('GEZD GNBV GY3T QOJQ')
    expect(screen.getByRole('link', { name: 'Abrir no aplicativo (no celular)' }))
      .toHaveAttribute('href', 'otpauth://totp/Orbit%20Hub:bia?secret=GEZDGNBVGY3TQOJQ')

    await usuario.type(screen.getByLabelText('Código de 6 dígitos'), '12a3456')
    await usuario.click(screen.getByRole('button', { name: 'Confirmar' }))
    expect(await screen.findByRole('heading', { name: /Olá, Bia/ })).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/auth/segundo-fator')?.corpo).toEqual({ desafio: 'd1', codigo: '123456' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Código errado e bloqueio
   * Objetivo: Conferir que o código errado mostra a mensagem e que o bloqueio desabilita o envio.
   * Decisões: Mensagens vindas da API.
   */
  it('mostra o erro do código e trava o envio quando bloqueia', async () => {
    let tentativas = 0
    abrir('CODIGO', () => {
      tentativas += 1
      return tentativas === 1
        ? { status: 422, corpo: { detail: 'Código incorreto ou expirado.', codigo: 'codigo_invalido' } }
        : { status: 429, corpo: { detail: 'Muitas tentativas seguidas.', codigo: 'login_bloqueado' } }
    })
    const usuario = await entrarComSenha()
    await usuario.type(await screen.findByLabelText('Código de 6 dígitos'), '000000')
    await usuario.click(screen.getByRole('button', { name: 'Confirmar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Código incorreto ou expirado.')
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeEnabled()

    await usuario.click(screen.getByRole('button', { name: 'Confirmar' }))
    expect(await screen.findByText('Muitas tentativas seguidas.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeDisabled()

    await usuario.click(screen.getByRole('button', { name: 'Voltar e entrar com outra conta' }))
    expect(await screen.findByLabelText('Senha')).toHaveValue('')
  })
})
