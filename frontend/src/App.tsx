import { useSyncExternalStore } from 'react'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { Inicio } from './Inicio'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Assinar mudanças de endereço
 * Objetivo: Avisar o React quando o hash da URL mudar.
 * Decisões: Devolve a função de cancelamento exigida pelo useSyncExternalStore.
 */
function assinarHash(aoMudar: () => void) {
  window.addEventListener('hashchange', aoMudar)
  return () => window.removeEventListener('hashchange', aoMudar)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Rotas da S0
 * Objetivo: Exibir a página inicial ou a prova de conceito do catálogo, conforme o endereço.
 * Decisões: Rotas por hash, para funcionar em qualquer servidor estático sem regra de reescrita.
 *           O roteador definitivo entra junto com as primeiras telas do produto (S1).
 */
export default function App() {
  const hash = useSyncExternalStore(assinarHash, () => window.location.hash, () => '')
  return hash === '#/poc-catalogo' ? <PocCatalogo /> : <Inicio />
}
