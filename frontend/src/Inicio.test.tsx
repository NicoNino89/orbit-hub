import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Inicio } from './Inicio'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Resposta JSON simulada
 * Objetivo: Criar uma resposta HTTP com corpo JSON e status escolhido.
 * Decisões: Usa a classe Response real, para o cliente ler a resposta como em produção.
 */
function respostaJson(corpo: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } }))
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Testes da página inicial
 * Objetivo: Verificar o status da API exibido na página inicial.
 * Decisões: fetch substituído por dublê em cada teste; nenhuma chamada real à rede.
 */
describe('Inicio', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: API no ar
   * Objetivo: Garantir que a página passe de "consultando" para versão e ambiente.
   * Decisões: Confere também o estado intermediário de carregamento.
   */
  it('mostra a versão e o ambiente quando a API responde', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respostaJson({ nome: 'Orbit Hub API', versao: '0.1.0', ambiente: 'homologacao' })))

    render(<Inicio />)

    expect(screen.getByRole('status')).toHaveTextContent('Consultando a API')
    expect(await screen.findByText('No ar')).toBeInTheDocument()
    expect(screen.getByText('0.1.0')).toBeInTheDocument()
    expect(screen.getByText('homologacao')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: API fora do ar
   * Objetivo: Garantir o alerta com o status HTTP quando a API falha.
   * Decisões: Usa 503, a resposta típica de API em manutenção ou subindo.
   */
  it('avisa quando a API está fora do ar', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respostaJson({}, 503)))

    render(<Inicio />)

    expect(await screen.findByRole('alert')).toHaveTextContent('A API respondeu 503')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Endpoint consultado
   * Objetivo: Garantir que a página chame /api/v1/sobre.
   * Decisões: Verifica o endereço relativo, que o proxy ou VITE_API_URL completam.
   */
  it('consulta o endpoint de informações da API', async () => {
    const fetchFalso = vi.fn(() => respostaJson({ nome: 'x', versao: '1', ambiente: 'local' }))
    vi.stubGlobal('fetch', fetchFalso)

    render(<Inicio />)
    await screen.findByText('No ar')

    expect(fetchFalso).toHaveBeenCalledWith('/api/v1/sobre', expect.anything())
  })
})
