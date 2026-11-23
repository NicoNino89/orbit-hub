import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { PAGINAS, STARTUPS, startupPorSlug } from './dados-ficticios'
import { PocCatalogo } from './PocCatalogo'

describe('PocCatalogo', () => {
  it('tem três startups fictícias com três páginas cada', () => {
    expect(STARTUPS).toHaveLength(3)
    expect(PAGINAS).toHaveLength(9)
    for (const s of STARTUPS) {
      expect(PAGINAS.filter((p) => p.startup === s.slug).map((p) => p.tipo)).toEqual(['capa', 'negocio', 'rodada'])
    }
  })

  it('percorre as nove páginas até a rodada da última startup', async () => {
    const usuario = userEvent.setup()
    render(<PocCatalogo />)
    const proxima = screen.getByRole('button', { name: 'Próxima página' })
    for (let i = 0; i < 8; i++) await usuario.click(proxima)

    expect(screen.getByText(/^Página 9 de 9/)).toHaveTextContent('Trama Logística: Rodada e critérios')
    expect(screen.getAllByText('Critérios da sua tese que combinaram').length).toBeGreaterThan(0)
  })

  it('nunca mostra percentual de compatibilidade nem linguagem de recomendação (regra da plataforma)', () => {
    const { container } = render(<PocCatalogo />)
    const texto = container.textContent ?? ''
    expect(texto).not.toMatch(/\d+\s*%/)
    expect(texto).not.toMatch(/recomendamos|invista|compatibilidade de/i)
  })

  it('falha de forma clara se a página citar uma startup inexistente', () => {
    expect(() => startupPorSlug('nao-existe')).toThrow('nao-existe')
  })
})
