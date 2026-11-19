import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Inicio } from './Inicio'

function respostaJson(corpo: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(corpo), { status, headers: { 'Content-Type': 'application/json' } }))
}

describe('Inicio', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('mostra a versão e o ambiente quando a API responde', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respostaJson({ nome: 'Orbit Hub API', versao: '0.1.0', ambiente: 'homologacao' })))

    render(<Inicio />)

    expect(screen.getByRole('status')).toHaveTextContent('Consultando a API')
    expect(await screen.findByText('No ar')).toBeInTheDocument()
    expect(screen.getByText('0.1.0')).toBeInTheDocument()
    expect(screen.getByText('homologacao')).toBeInTheDocument()
  })

  it('avisa quando a API está fora do ar', async () => {
    vi.stubGlobal('fetch', vi.fn(() => respostaJson({}, 503)))

    render(<Inicio />)

    expect(await screen.findByRole('alert')).toHaveTextContent('A API respondeu 503')
  })

  it('consulta o endpoint de informações da API', async () => {
    const fetchFalso = vi.fn(() => respostaJson({ nome: 'x', versao: '1', ambiente: 'local' }))
    vi.stubGlobal('fetch', fetchFalso)

    render(<Inicio />)
    await screen.findByText('No ar')

    expect(fetchFalso).toHaveBeenCalledWith('/api/v1/sobre', expect.anything())
  })
})
