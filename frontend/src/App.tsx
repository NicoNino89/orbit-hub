import { useSyncExternalStore } from 'react'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { Inicio } from './Inicio'

function assinarHash(aoMudar: () => void) {
  window.addEventListener('hashchange', aoMudar)
  return () => window.removeEventListener('hashchange', aoMudar)
}

/**
 * Rotas mínimas da S0, por hash, para funcionar igual no Static Web Apps e no nginx.
 * O roteador definitivo entra junto com as primeiras telas do produto (S1).
 */
export default function App() {
  const hash = useSyncExternalStore(assinarHash, () => window.location.hash, () => '')
  return hash === '#/poc-catalogo' ? <PocCatalogo /> : <Inicio />
}
