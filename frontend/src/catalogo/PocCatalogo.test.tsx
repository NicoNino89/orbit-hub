import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { PAGINAS, STARTUPS, startupPorSlug } from './dados-ficticios'
import { PocCatalogo } from './PocCatalogo'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Testes da prova de conceito
 * Objetivo: Verificar o conteúdo e as regras de domínio da tela da PoC.
 * Decisões: Inclui as regras da plataforma, não só o funcionamento visual.
 */
describe('PocCatalogo', () => {
  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Três startups, três páginas
   * Objetivo: Garantir o critério do cartão: 3 startups com capa, negócio e rodada.
   * Decisões: Verifica também a ordem das páginas.
   */
  it('tem três startups fictícias com três páginas cada', () => {
    expect(STARTUPS).toHaveLength(3)
    expect(PAGINAS).toHaveLength(9)
    for (const s of STARTUPS) {
      expect(PAGINAS.filter((p) => p.startup === s.slug).map((p) => p.tipo)).toEqual(['capa', 'negocio', 'rodada'])
    }
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Percorrer o catálogo inteiro
   * Objetivo: Garantir que as nove páginas sejam alcançáveis até a última rodada.
   * Decisões: Navega pelos botões, como um usuário faria.
   */
  it('percorre as nove páginas até a rodada da última startup', async () => {
    const usuario = userEvent.setup()
    render(<PocCatalogo />)
    const proxima = screen.getByRole('button', { name: 'Próxima página' })
    for (let i = 0; i < 8; i++) await usuario.click(proxima)

    expect(screen.getByText(/^Página 9 de 9/)).toHaveTextContent('Trama Logística: Rodada e critérios')
    expect(screen.getAllByText('Critérios da sua tese que combinaram').length).toBeGreaterThan(0)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Sem percentual e sem recomendação
   * Objetivo: Garantir que o conteúdo não mostre percentual de compatibilidade nem linguagem de recomendação.
   * Decisões: Regra da plataforma (CVM 88): ela conecta as partes e mostra só critérios que combinaram.
   */
  it('nunca mostra percentual de compatibilidade nem linguagem de recomendação (regra da plataforma)', () => {
    const { container } = render(<PocCatalogo />)
    const texto = container.textContent ?? ''
    expect(texto).not.toMatch(/\d+\s*%/)
    expect(texto).not.toMatch(/recomendamos|invista|compatibilidade de/i)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Startup inexistente
   * Objetivo: Garantir erro claro quando uma página cita uma startup que não existe.
   * Decisões: A mensagem traz o slug, para o erro ser achado rápido.
   */
  it('falha de forma clara se a página citar uma startup inexistente', () => {
    expect(() => startupPorSlug('nao-existe')).toThrow('nao-existe')
  })
})
