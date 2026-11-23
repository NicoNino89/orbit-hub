import { describe, expect, it } from 'vitest'
import { calcularDobra, cantoNoInstante, refletir, type Ponto } from './dobra'

const L = 600
const A = 400

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Área de polígono
 * Objetivo: Calcular a área de um polígono pela fórmula do laço (shoelace).
 * Decisões: Usada para provar que frente e aba somam a folha inteira.
 */
function area(p: Ponto[]) {
  let s = 0
  for (let i = 0; i < p.length; i++) {
    const a = p[i]
    const b = p[(i + 1) % p.length]
    s += a.x * b.y - b.x * a.y
  }
  return Math.abs(s) / 2
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Testes da geometria da dobra
 * Objetivo: Verificar a matemática da virada sem navegador.
 * Decisões: Folha de 600 x 400 (3:2), o formato do computador.
 */
describe('dobra', () => {
  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Sem dobra no início
   * Objetivo: Garantir que t = 0 não produza dobra.
   * Decisões: null, em vez de polígonos vazios, para o componente não recortar a folha.
   */
  it('não dobra nada no início', () => {
    expect(calcularDobra(0, L, A)).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Folha sem tamanho
   * Objetivo: Garantir que tamanho zero não produza dobra nem divisão por zero.
   * Decisões: Caso real antes do primeiro layout e no jsdom.
   */
  it('não dobra quando a folha ainda não tem tamanho', () => {
    expect(calcularDobra(0.5, 0, 0)).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Reflexo leva o canto ao destino
   * Objetivo: Garantir que a matriz leve o canto exatamente ao ponto da virada.
   * Decisões: Prova a matriz de reflexo usada no CSS.
   */
  it('leva o canto inferior direito até o ponto da virada', () => {
    const t = 0.4
    const d = calcularDobra(t, L, A)!
    const destino = refletir(d.matriz, { x: L, y: A })
    const esperado = cantoNoInstante(t, L, A)
    expect(destino.x).toBeCloseTo(esperado.x, 6)
    expect(destino.y).toBeCloseTo(esperado.y, 6)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Área conservada
   * Objetivo: Garantir que frente e aba somem a área da folha em vários instantes.
   * Decisões: Pega erros no recorte de polígono.
   */
  it('divide a folha em frente e aba sem perder área', () => {
    for (const t of [0.05, 0.3, 0.5, 0.8]) {
      const d = calcularDobra(t, L, A)!
      expect(area(d.frente) + area(d.aba)).toBeCloseTo(L * A, 3)
      expect(area(d.aba)).toBeGreaterThan(0)
    }
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Aba crescente
   * Objetivo: Garantir que a aba só aumente ao longo da virada.
   * Decisões: Uma aba que diminuísse no meio seria vista como tremida.
   */
  it('a aba cresce conforme a página vira', () => {
    const areas = [0.1, 0.3, 0.5, 0.7, 0.9].map((t) => area(calcularDobra(t, L, A)!.aba))
    for (let i = 1; i < areas.length; i++) expect(areas[i]).toBeGreaterThan(areas[i - 1])
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Folha virada no fim
   * Objetivo: Garantir que em t = 1 não reste frente.
   * Decisões: Garante que nada da página antiga fique à vista no fim.
   */
  it('no fim a folha virou por inteiro', () => {
    const d = calcularDobra(1, L, A)!
    expect(area(d.frente)).toBeCloseTo(0, 3)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Dobra diagonal
   * Objetivo: Garantir que a linha da dobra comece inclinada, como canto de revista.
   * Decisões: Evita voltar ao efeito de porta reprovado no teste humano.
   */
  it('a dobra começa na diagonal, como o canto de uma revista', () => {
    const d = calcularDobra(0.1, L, A)!
    // normal com componente vertical: a linha da dobra não é vertical
    expect(Math.abs(d.normal.y)).toBeGreaterThan(0.2)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Canto preso à lombada
   * Objetivo: Garantir que o canto nunca fique mais longe que uma largura do pé da lombada.
   * Decisões: Amostra a virada inteira de 5% em 5%.
   */
  it('o canto nunca se solta da lombada', () => {
    for (let t = 0; t <= 1; t += 0.05) {
      const p = cantoNoInstante(t, L, A)
      expect(Math.hypot(p.x, p.y - A)).toBeLessThanOrEqual(L + 1e-6)
    }
  })
})
