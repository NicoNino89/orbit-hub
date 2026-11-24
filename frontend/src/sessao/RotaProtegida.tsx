import { Navigate, Outlet, useLocation } from 'react-router'
import { AceiteDeTermos } from './AceiteDeTermos'
import { useSessao } from './contexto'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Rota que exige login
 * Objetivo: Mostrar a área logada só para quem tem sessão, levando os demais ao login.
 * Decisões: O endereço atual vai em "voltar", para a pessoa retornar à mesma página depois de entrar
 *           (KAN-15). Com termos pendentes, a tela de aceite aparece antes de qualquer página (KAN-92).
 *           Esconder a tela é só conforto: as regras de acesso continuam no servidor (KAN-34).
 */
export function RotaProtegida() {
  const { estado } = useSessao()
  const local = useLocation()

  if (estado.status === 'carregando') {
    return <p role="status" className="p-8 text-muted">Carregando sua sessão…</p>
  }
  if (estado.status === 'anonimo') {
    const voltar = encodeURIComponent(local.pathname + local.search)
    return <Navigate to={`/entrar?voltar=${voltar}`} replace />
  }
  if (estado.termosPendentes) {
    return <AceiteDeTermos />
  }
  return <Outlet />
}
