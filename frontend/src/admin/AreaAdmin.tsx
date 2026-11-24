import { NavLink, Outlet } from 'react-router'
import type { Papel } from '../api/contas'
import { useSessao } from '../sessao/contexto'
import { CLASSE_LINK } from '../ui/estilos'
import { MENU_BACKOFFICE, PAPEIS_INTERNOS } from './papeisInternos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Rota da equipe interna
 * Objetivo: Só mostrar as telas do backoffice para contas com papel interno.
 * Decisões: Sem papel interno, mostra a recusa (e não redireciona), para a pessoa entender o que houve.
 */
export function RotaInterna({ papeis = PAPEIS_INTERNOS }: { papeis?: Papel[] }) {
  const { estado } = useSessao()
  const permitido = estado.status === 'autenticado' && estado.usuario.papeis.some((p) => papeis.includes(p))
  if (!permitido) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-semibold">Área da equipe</h1>
        <p>Esta área é só para a equipe interna da Orbit Hub.</p>
      </section>
    )
  }
  return <Outlet />
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Moldura do backoffice
 * Objetivo: Mostrar o menu do backoffice e a tela escolhida.
 * Decisões: Menu lateral em telas largas e em linha no celular.
 */
export function AreaAdmin() {
  const { estado } = useSessao()
  const papeis = estado.status === 'autenticado' ? estado.usuario.papeis : []
  const itens = MENU_BACKOFFICE.filter((item) => item.papeis.some((p) => papeis.includes(p)))
  return (
    <div className="flex flex-col gap-6 md:flex-row">
      <nav aria-label="Backoffice" className="flex shrink-0 flex-row flex-wrap gap-2 md:w-48 md:flex-col">
        <span className="text-xs font-semibold uppercase tracking-wide text-muted">Backoffice</span>
        {itens.map((item) => (
          <NavLink key={item.caminho} to={item.caminho}
            className={({ isActive }) => `rounded-md px-3 py-2 text-sm font-semibold ${isActive ? 'bg-primary text-on-primary' : CLASSE_LINK}`}>
            {item.rotulo}
          </NavLink>
        ))}
      </nav>
      <div className="min-w-0 flex-1">
        <Outlet />
      </div>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Início do backoffice
 * Objetivo: Dar as boas-vindas e apontar as telas disponíveis.
 * Decisões: Sem indicadores ainda; o painel de operação é a KAN-82.
 */
export function InicioAdmin() {
  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Backoffice</h1>
      <p className="text-muted">Escolha uma área no menu. Toda ação feita aqui fica registrada na trilha de auditoria.</p>
    </section>
  )
}
