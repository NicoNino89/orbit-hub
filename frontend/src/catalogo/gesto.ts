/** Distância mínima, em pixels, para um arraste contar como virada de página. */
export const LIMIAR_GESTO = 48

export type Direcao = 'proxima' | 'anterior' | null

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Direção do gesto
 * Objetivo: Interpretar um arraste como próxima página, página anterior ou nada.
 * Decisões: Só vale se passar de 48 px e for mais horizontal que vertical, para não brigar com a
 *           rolagem da página no celular.
 */
export function direcaoDoGesto(dx: number, dy: number): Direcao {
  if (Math.abs(dx) < LIMIAR_GESTO || Math.abs(dx) <= Math.abs(dy)) return null
  return dx < 0 ? 'proxima' : 'anterior'
}
