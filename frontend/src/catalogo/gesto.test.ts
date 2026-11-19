import { describe, expect, it } from 'vitest'
import { direcaoDoGesto, LIMIAR_GESTO } from './gesto'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Testes da direção do gesto
 * Objetivo: Verificar a interpretação de arrastes.
 * Decisões: Valores logo acima e abaixo do limiar de 48 px.
 */
describe('direcaoDoGesto', () => {
  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Esquerda avança
   * Objetivo: Garantir que arrastar para a esquerda vá para a próxima página.
   * Decisões: Mesma convenção de livros e aplicativos de leitura.
   */
  it('arrastar para a esquerda vai para a próxima página', () => {
    expect(direcaoDoGesto(-LIMIAR_GESTO - 1, 0)).toBe('proxima')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Direita volta
   * Objetivo: Garantir que arrastar para a direita volte uma página.
   * Decisões: Inclui um pequeno desvio vertical, comum no dedo.
   */
  it('arrastar para a direita volta uma página', () => {
    expect(direcaoDoGesto(LIMIAR_GESTO + 1, 5)).toBe('anterior')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Toques curtos ignorados
   * Objetivo: Garantir que um movimento abaixo do limiar não vire a página.
   * Decisões: Evita virar sem querer ao tocar para ler.
   */
  it('ignora toques curtos', () => {
    expect(direcaoDoGesto(LIMIAR_GESTO - 1, 0)).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Gestos verticais ignorados
   * Objetivo: Garantir que gestos verticais ou na diagonal exata não virem a página.
   * Decisões: Empate (45°) conta como vertical, a favor da rolagem.
   */
  it('ignora gestos mais verticais que horizontais, para não brigar com a rolagem', () => {
    expect(direcaoDoGesto(-80, 120)).toBeNull()
    expect(direcaoDoGesto(80, -80)).toBeNull()
  })
})
