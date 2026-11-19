import { useSyncExternalStore } from 'react'

const CONSULTA = '(prefers-reduced-motion: reduce)'

function assinar(aoMudar: () => void) {
  const mql = window.matchMedia(CONSULTA)
  mql.addEventListener('change', aoMudar)
  return () => mql.removeEventListener('change', aoMudar)
}

/** Verdadeiro quando o sistema pede movimento reduzido. Acompanha mudanças em tempo real. */
export function useMovimentoReduzido(): boolean {
  return useSyncExternalStore(
    assinar,
    () => window.matchMedia(CONSULTA).matches,
    () => false,
  )
}
