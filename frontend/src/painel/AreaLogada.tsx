import { useState } from 'react'
import { Link, Outlet } from 'react-router'
import { ehPapelPublico } from '../api/contas'
import { PaginaInicialDoInvestidor } from '../inicio/Inicio'
import { useSessao } from '../sessao/contexto'
import { BarraSuperior, MenuLateral } from './Moldura'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:35
 * Cartão Jira: KAN-166, KAN-167, KAN-165
 * Título: Área logada
 * Objetivo: Montar a moldura da área logada: menu lateral, barra superior e o conteúdo.
 * Decisões: Menu fixo em telas largas; no celular abre como gaveta sobre o conteúdo.
 */
export function AreaLogada() {
  const { estado } = useSessao()
  const [menu, setMenu] = useState(false)
  if (estado.status !== 'autenticado') return null
  return (
    <div className="flex min-h-svh bg-bg">
      <MenuLateral aberto={menu} aoNavegar={() => setMenu(false)} />
      {menu && <button type="button" aria-label="Fechar menu" onClick={() => setMenu(false)} className="fixed inset-0 z-20 bg-overlay lg:hidden" />}
      <div className="flex min-w-0 flex-1 flex-col">
        <BarraSuperior aoAbrirMenu={() => setMenu(true)} />
        <main className="mx-auto w-full max-w-7xl px-4 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:35
 * Cartão Jira: KAN-168, KAN-18
 * Título: Página inicial
 * Objetivo: Mostrar a página inicial do papel ativo.
 * Decisões: Investidor tem a página nova (KAN-168); o empreendedor ganha a dele na S13 (KAN-169). Conta só da equipe é
 *           levada ao backoffice.
 */
export function Painel() {
  const { estado, papelAtivo } = useSessao()
  if (estado.status !== 'autenticado') return null
  if (papelAtivo === 'INVESTIDOR') return <PaginaInicialDoInvestidor />
  const primeiroNome = estado.usuario.nome.split(' ')[0]
  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-bold tracking-tight">Olá, {primeiroNome}</h1>
      <div className="rounded-xl border border-line bg-surface p-5">
        <h2 className="mb-1 font-semibold">Próximo passo</h2>
        {papelAtivo === 'EMPREENDEDOR' && (
          <p className="text-muted">
            Cadastre sua startup para apresentá-la a investidores.{' '}
            <Link to="/empreendedor" className="font-semibold text-primary">Abrir a área do empreendedor</Link>
          </p>
        )}
        {papelAtivo === null && (
          <p className="text-muted">
            Sua conta não tem papel público.{' '}
            {estado.usuario.papeis.some((p) => !ehPapelPublico(p)) && (
              <Link to="/admin" className="font-semibold text-primary">Abrir o backoffice</Link>
            )}
          </p>
        )}
      </div>
    </section>
  )
}
