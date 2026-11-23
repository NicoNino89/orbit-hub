import { useSyncExternalStore } from 'react'

const CONSULTA = '(prefers-reduced-motion: reduce)'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Assinar mudanças de movimento reduzido
 * Objetivo: Avisar o React quando a preferência de movimento do sistema mudar.
 * Decisões: Devolve a função de cancelamento exigida pelo useSyncExternalStore.
 */
function assinar(aoMudar: () => void) {
  const mql = window.matchMedia(CONSULTA)
  mql.addEventListener('change', aoMudar)
  return () => mql.removeEventListener('change', aoMudar)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Hook de movimento reduzido
 * Objetivo: Informar se o sistema pede movimento reduzido, acompanhando mudanças em tempo real.
 * Decisões: useSyncExternalStore em vez de useEffect + useState, sem render a mais nem estado
 *           desatualizado; no servidor assume falso (com animação).
 */
export function useMovimentoReduzido(): boolean {
  return useSyncExternalStore(
    assinar,
    () => window.matchMedia(CONSULTA).matches,
    () => false,
  )
}
