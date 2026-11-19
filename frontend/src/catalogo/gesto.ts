/** Distância mínima, em pixels, para um arraste contar como virada de página. */
export const LIMIAR_GESTO = 48

export type Direcao = 'proxima' | 'anterior' | null

/**
 * Interpreta um arraste. Só vale se for mais horizontal que vertical,
 * para não brigar com a rolagem da página no celular.
 */
export function direcaoDoGesto(dx: number, dy: number): Direcao {
  if (Math.abs(dx) < LIMIAR_GESTO || Math.abs(dx) <= Math.abs(dy)) return null
  return dx < 0 ? 'proxima' : 'anterior'
}
