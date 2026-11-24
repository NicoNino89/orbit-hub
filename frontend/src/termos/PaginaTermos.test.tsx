import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { DocumentoLegal } from '../api/documentos'

const TERMOS_V2: DocumentoLegal = {
  id: 't2', tipo: 'TERMOS_DE_USO', versao: 2, titulo: 'Termos de Uso', conteudo: 'Texto da versão 2.',
  vigenteDesde: '2026-11-20T03:00:00Z', aceite: 'TODOS_NO_ACESSO',
}
const TERMOS_V1: DocumentoLegal = { ...TERMOS_V2, id: 't1', versao: 1, conteudo: 'Texto da versão 1.', vigenteDesde: '2026-10-01T03:00:00Z' }
const PRIVACIDADE: DocumentoLegal = {
  id: 'p1', tipo: 'POLITICA_DE_PRIVACIDADE', versao: 1, titulo: 'Política de Privacidade',
  conteudo: 'Como tratamos seus dados.', vigenteDesde: '2026-11-20T03:00:00Z', aceite: 'TODOS_NO_ACESSO',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: API simulada dos documentos
 * Objetivo: Responder às rotas de documentos vigentes e de versões.
 * Decisões: Responde pelo endereço chamado; qualquer outra rota falha com 500.
 */
function simularApi(falharVigentes = false) {
  const fetchSimulado = vi.fn(async (url: string) => {
    const json = (corpo: unknown, status = 200) =>
      new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } })
    if (url.endsWith('/api/v1/documentos-legais')) {
      return falharVigentes ? json({ detail: 'Serviço indisponível.' }, 503) : json([TERMOS_V2, PRIVACIDADE])
    }
    if (url.endsWith('/TERMOS_DE_USO/versoes')) return json([TERMOS_V2, TERMOS_V1])
    if (url.endsWith('/POLITICA_DE_PRIVACIDADE/versoes')) return json([PRIVACIDADE])
    return json({}, 500)
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return fetchSimulado
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Montar a página
 * Objetivo: Renderizar as rotas no endereço informado.
 * Decisões: MemoryRouter, sem depender da URL do jsdom.
 */
function abrir(endereco: string) {
  return render(
    <MemoryRouter initialEntries={[endereco]}>
      <Rotas />
    </MemoryRouter>,
  )
}

describe('PaginaTermos', () => {
  afterEach(() => vi.unstubAllGlobals())

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Documento vigente por padrão
   * Objetivo: Garantir que /termos mostra o primeiro documento vigente com versão e data.
   * Decisões: Data no formato brasileiro.
   */
  it('mostra o primeiro documento vigente com versão e data', async () => {
    simularApi()
    abrir('/termos')
    expect(await screen.findByText('Texto da versão 2.')).toBeInTheDocument()
    expect(screen.getByText(/Versão 2 · em vigor desde 20\/11\/2026/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Termos de Uso' })).toHaveAttribute('aria-current', 'page')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Documento pelo endereço
   * Objetivo: Garantir que /termos/privacidade abre a Política de Privacidade.
   * Decisões: Confere o texto e o item marcado na navegação.
   */
  it('abre o documento indicado no endereço', async () => {
    simularApi()
    abrir('/termos/privacidade')
    expect(await screen.findByText('Como tratamos seus dados.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Política de Privacidade' })).toHaveAttribute('aria-current', 'page')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Versão anterior consultável
   * Objetivo: Garantir que a versão 1 pode ser lida, com aviso de que não é a vigente.
   * Decisões: Escolhe a versão na lista lateral.
   */
  it('permite ler uma versão anterior com aviso', async () => {
    simularApi()
    abrir('/termos/uso')
    const versao1 = await screen.findByRole('button', { name: /Versão 1/ })
    await userEvent.click(versao1)

    expect(screen.getByText('Texto da versão 1.')).toBeInTheDocument()
    expect(screen.getByRole('note')).toHaveTextContent('Esta é uma versão anterior. A versão em vigor é a 2.')
    expect(versao1).toHaveAttribute('aria-pressed', 'true')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: API indisponível
   * Objetivo: Garantir que a falha aparece como alerta com a mensagem da API.
   * Decisões: Resposta 503 com Problem Details.
   */
  it('mostra o erro quando a API falha', async () => {
    simularApi(true)
    abrir('/termos')
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Serviço indisponível.'))
  })
})
