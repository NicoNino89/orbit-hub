import { describe, expect, it } from 'vitest'
import { calcularDobra, cantoNoInstante, refletir, type Ponto } from './dobra'

const L = 600
const A = 400

function area(p: Ponto[]) {
  let s = 0
  for (let i = 0; i < p.length; i++) {
    const a = p[i]
    const b = p[(i + 1) % p.length]
    s += a.x * b.y - b.x * a.y
  }
  return Math.abs(s) / 2
}

describe('dobra', () => {
  it('não dobra nada no início', () => {
    expect(calcularDobra(0, L, A)).toBeNull()
  })

  it('não dobra quando a folha ainda não tem tamanho', () => {
    expect(calcularDobra(0.5, 0, 0)).toBeNull()
  })

  it('leva o canto inferior direito até o ponto da virada', () => {
    const t = 0.4
    const d = calcularDobra(t, L, A)!
    const destino = refletir(d.matriz, { x: L, y: A })
    const esperado = cantoNoInstante(t, L, A)
    expect(destino.x).toBeCloseTo(esperado.x, 6)
    expect(destino.y).toBeCloseTo(esperado.y, 6)
  })

  it('divide a folha em frente e aba sem perder área', () => {
    for (const t of [0.05, 0.3, 0.5, 0.8]) {
      const d = calcularDobra(t, L, A)!
      expect(area(d.frente) + area(d.aba)).toBeCloseTo(L * A, 3)
      expect(area(d.aba)).toBeGreaterThan(0)
    }
  })

  it('a aba cresce conforme a página vira', () => {
    const areas = [0.1, 0.3, 0.5, 0.7, 0.9].map((t) => area(calcularDobra(t, L, A)!.aba))
    for (let i = 1; i < areas.length; i++) expect(areas[i]).toBeGreaterThan(areas[i - 1])
  })

  it('no fim a folha virou por inteiro', () => {
    const d = calcularDobra(1, L, A)!
    expect(area(d.frente)).toBeCloseTo(0, 3)
  })

  it('a dobra começa na diagonal, como o canto de uma revista', () => {
    const d = calcularDobra(0.1, L, A)!
    // normal com componente vertical: a linha da dobra não é vertical
    expect(Math.abs(d.normal.y)).toBeGreaterThan(0.2)
  })

  it('o canto nunca se solta da lombada', () => {
    for (let t = 0; t <= 1; t += 0.05) {
      const p = cantoNoInstante(t, L, A)
      expect(Math.hypot(p.x, p.y - A)).toBeLessThanOrEqual(L + 1e-6)
    }
  })
})
