import { describe, expect, it } from 'vitest'
import { direcaoDoGesto, LIMIAR_GESTO } from './gesto'

describe('direcaoDoGesto', () => {
  it('arrastar para a esquerda vai para a próxima página', () => {
    expect(direcaoDoGesto(-LIMIAR_GESTO - 1, 0)).toBe('proxima')
  })

  it('arrastar para a direita volta uma página', () => {
    expect(direcaoDoGesto(LIMIAR_GESTO + 1, 5)).toBe('anterior')
  })

  it('ignora toques curtos', () => {
    expect(direcaoDoGesto(LIMIAR_GESTO - 1, 0)).toBeNull()
  })

  it('ignora gestos mais verticais que horizontais, para não brigar com a rolagem', () => {
    expect(direcaoDoGesto(-80, 120)).toBeNull()
    expect(direcaoDoGesto(80, -80)).toBeNull()
  })
})
