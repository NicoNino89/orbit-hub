import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { DocumentoLegal } from '../api/documentos'
import { simularApi } from '../test/apiSimulada'

const VIGENTES: Partial<DocumentoLegal>[] = [
  { id: 'id-termos', tipo: 'TERMOS_DE_USO', aceite: 'TODOS_NO_ACESSO', titulo: 'Termos de Uso' },
  { id: 'id-privacidade', tipo: 'POLITICA_DE_PRIVACIDADE', aceite: 'TODOS_NO_ACESSO', titulo: 'Política de Privacidade' },
  { id: 'id-confidencialidade', tipo: 'TERMO_DE_CONFIDENCIALIDADE', aceite: 'INVESTIDOR_ANTES_DO_NIVEL_2', titulo: 'Termo' },
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Abrir o cadastro
 * Objetivo: Renderizar a rota /cadastro.
 * Decisões: MemoryRouter com as rotas reais.
 */
function abrirCadastro() {
  return render(
    <MemoryRouter initialEntries={['/cadastro']}>
      <Rotas />
    </MemoryRouter>,
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Preencher o básico
 * Objetivo: Preencher nome, e-mail e senha.
 * Decisões: Valores válidos.
 */
async function preencherBasico() {
  await userEvent.type(screen.getByLabelText('Nome'), 'Ana Souza')
  await userEvent.type(screen.getByLabelText('E-mail'), 'ana@exemplo.com')
  await userEvent.type(screen.getByLabelText('Senha'), 'uma-senha-bem-longa')
}

describe('PaginaCadastro', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Investidor e empreendedor com confidencialidade
   * Objetivo: Garantir que os dois papéis e os três aceites são enviados e que a tela pede a confirmação.
   * Decisões: A caixa de confidencialidade só aparece depois de marcar investidor.
   */
  it('cadastra com os dois papéis e pede a confirmação do e-mail', async () => {
    const { chamadas } = simularApi({
      'GET /api/v1/documentos-legais': () => ({ corpo: VIGENTES }),
      'POST /api/v1/contas': () => ({
        status: 201,
        corpo: { id: 'c1', status: 'PENDENTE_CONFIRMACAO', papeis: ['EMPREENDEDOR', 'INVESTIDOR'],
          confirmacaoPendente: true, proximosPassos: [] },
      }),
    })
    abrirCadastro()
    await preencherBasico()
    expect(screen.queryByText(/Termo de Confidencialidade/)).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('checkbox', { name: /Investidor/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Empreendedor/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de Uso/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Termo de Confidencialidade/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('heading', { name: 'Confirme seu e-mail' })).toBeInTheDocument()
    const envio = chamadas.find((c) => c.chave === 'POST /api/v1/contas')
    expect(envio?.corpo).toEqual({
      nome: 'Ana Souza', email: 'ana@exemplo.com', senha: 'uma-senha-bem-longa',
      papeis: ['INVESTIDOR', 'EMPREENDEDOR'], aceites: ['id-termos', 'id-privacidade', 'id-confidencialidade'],
    })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Validação antes de enviar
   * Objetivo: Garantir que, sem papel e sem aceite, nada é enviado e os avisos aparecem.
   * Decisões: Confere que não houve POST.
   */
  it('pede papel e aceite antes de enviar', async () => {
    const { chamadas } = simularApi({ 'GET /api/v1/documentos-legais': () => ({ corpo: VIGENTES }) })
    abrirCadastro()
    await preencherBasico()
    await userEvent.click(await screen.findByRole('button', { name: 'Criar conta' }))

    expect(screen.getByText('Escolha pelo menos um papel.')).toBeInTheDocument()
    expect(screen.getByText(/aceite os Termos de Uso e a Política de Privacidade/)).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/contas')).toBe(false)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Erros da API
   * Objetivo: Garantir o erro de campo (400) no campo e o erro de regra (422) no alerta.
   * Decisões: Primeiro senha inválida no campo; depois e-mail em uso.
   */
  it('mostra os erros devolvidos pela API', async () => {
    let tentativa = 0
    simularApi({
      'GET /api/v1/documentos-legais': () => ({ corpo: VIGENTES }),
      'POST /api/v1/contas': () => (++tentativa === 1
        ? { status: 400, corpo: { detail: 'Confira os campos destacados.', campos: { senha: 'A senha precisa ter de 10 a 128 caracteres.' } } }
        : { status: 422, corpo: { detail: 'Já existe uma conta com este e-mail.', codigo: 'email_em_uso' } }),
    })
    abrirCadastro()
    await preencherBasico()
    await userEvent.click(screen.getByRole('checkbox', { name: /Empreendedor/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de Uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByText('A senha precisa ter de 10 a 128 caracteres.')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toHaveAttribute('aria-invalid', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma conta com este e-mail.')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Conta já ativa
   * Objetivo: Garantir que, sem confirmação pendente, a tela leva para o login.
   * Decisões: Homologação sem provedor de e-mail.
   */
  it('leva para o login quando a conta já nasce ativa', async () => {
    simularApi({
      'GET /api/v1/documentos-legais': () => ({ corpo: VIGENTES }),
      'POST /api/v1/contas': () => ({ status: 201, corpo: { id: 'c1', status: 'ATIVO', papeis: ['EMPREENDEDOR'],
        confirmacaoPendente: false, proximosPassos: [] } }),
    })
    abrirCadastro()
    await preencherBasico()
    await userEvent.click(screen.getByRole('checkbox', { name: /Empreendedor/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de Uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('heading', { name: 'Conta criada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/entrar')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Reenvio da confirmação
   * Objetivo: Garantir que o pedido de reenvio é feito com o e-mail digitado.
   * Decisões: Depois do cadastro pendente.
   */
  it('reenvia o link de confirmação', async () => {
    const { chamadas } = simularApi({
      'GET /api/v1/documentos-legais': () => ({ corpo: VIGENTES }),
      'POST /api/v1/contas': () => ({ status: 201, corpo: { id: 'c1', status: 'PENDENTE_CONFIRMACAO',
        papeis: ['EMPREENDEDOR'], confirmacaoPendente: true, proximosPassos: [] } }),
      'POST /api/v1/contas/confirmacao/reenvio': () => ({ status: 202 }),
    })
    abrirCadastro()
    await preencherBasico()
    await userEvent.click(screen.getByRole('checkbox', { name: /Empreendedor/ }))
    await userEvent.click(screen.getByRole('checkbox', { name: /Termos de Uso/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))
    await userEvent.click(await screen.findByRole('button', { name: /Reenviar o link/ }))

    expect(await screen.findByText(/um novo link foi enviado/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/contas/confirmacao/reenvio')?.corpo)
      .toEqual({ email: 'ana@exemplo.com' })
  })
})
