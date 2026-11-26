import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { ehPapelPublico, NOMES_DOS_PAPEIS, type Papel } from '../api/contas'
import { buscar, type ResultadoDaBusca } from '../api/inicio'
import { lerPerfil } from '../api/perfilDeInvestimento'
import { requisitar } from '../api/cliente'
import { SeletorDeAtuacao } from '../investidor/SeletorDeAtuacao'
import { Sino } from '../notificacoes/Sino'
import { useSessao } from '../sessao/contexto'
import { Foguete, Grafico } from '../ui/Ilustracoes'

interface ItemDoMenu { caminho: string; rotulo: string; icone: string }

const ITENS: Record<'INVESTIDOR' | 'EMPREENDEDOR', ItemDoMenu[]> = {
  INVESTIDOR: [
    { caminho: '/painel', rotulo: 'Início', icone: '⌂' }, { caminho: '/catalogo', rotulo: 'Explorar', icone: '◎' },
    { caminho: '/conexoes', rotulo: 'Minha Rede', icone: '⚇' }, { caminho: '/agenda', rotulo: 'Agenda', icone: '▦' },
    { caminho: '/notificacoes', rotulo: 'Notificações', icone: '◔' }, { caminho: '/investidor', rotulo: 'Meu perfil', icone: '◐' },
    { caminho: '/ajuda/chamados', rotulo: 'Ajuda', icone: '?' }, { caminho: '/conta', rotulo: 'Configurações', icone: '⚙' },
  ],
  EMPREENDEDOR: [
    { caminho: '/painel', rotulo: 'Início', icone: '⌂' }, { caminho: '/empreendedor', rotulo: 'Minha Startup', icone: '◆' },
    { caminho: '/catalogo', rotulo: 'Explorar', icone: '◎' }, { caminho: '/agenda', rotulo: 'Agenda', icone: '▦' },
    { caminho: '/notificacoes', rotulo: 'Notificações', icone: '◔' }, { caminho: '/ajuda/chamados', rotulo: 'Ajuda', icone: '?' },
    { caminho: '/conta', rotulo: 'Configurações', icone: '⚙' },
  ],
}

const CRIACAO: Record<'INVESTIDOR' | 'EMPREENDEDOR', { caminho: string; rotulo: string }[]> = {
  INVESTIDOR: [{ caminho: '/investidor/perfil', rotulo: 'Definir tese' }, { caminho: '/investidor/organizacoes', rotulo: 'Nova organização' },
    { caminho: '/ajuda/chamados', rotulo: 'Abrir chamado' }],
  EMPREENDEDOR: [{ caminho: '/empreendedor/startups/nova', rotulo: 'Nova startup' }, { caminho: '/agenda', rotulo: 'Ver reuniões' },
    { caminho: '/ajuda/chamados', rotulo: 'Abrir chamado' }],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-166
 * Título: Próximo passo do perfil
 * Objetivo: Descobrir se falta a tese (investidor) ou a startup (empreendedor), para o cartão "Comece agora".
 * Decisões: Em caso de erro, não mostra o cartão.
 */
function useProximoPasso(papel: Papel | null) {
  const [passo, setPasso] = useState<{ texto: string; caminho: string; botao: string } | null>(null)
  useEffect(() => {
    let ativo = true
    setPasso(null)
    if (papel === 'INVESTIDOR') {
      lerPerfil().then((s) => {
        if (ativo && !s.perfil) setPasso({ texto: 'Defina seu perfil e receba startups e conteúdos alinhados com seus interesses.', caminho: '/investidor/onboarding', botao: 'Configurar perfil' })
      }).catch(() => undefined)
    } else if (papel === 'EMPREENDEDOR') {
      requisitar<unknown[]>('/api/v1/startups').then((l) => {
        if (ativo && l.length === 0) setPasso({ texto: 'Cadastre sua startup para apresentá-la a investidores.', caminho: '/empreendedor/startups/nova', botao: 'Cadastrar startup' })
      }).catch(() => undefined)
    }
    return () => { ativo = false }
  }, [papel])
  return passo
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-166
 * Título: Menu lateral
 * Objetivo: Mostrar as áreas do papel ativo, o item atual destacado, o atalho da equipe e o cartão "Comece agora".
 * Decisões: Itens de fases futuras (Escola, Consultorias, Mensagens, Carteira) só entram quando existirem.
 */
export function MenuLateral({ aberto, aoNavegar }: { aberto: boolean; aoNavegar: () => void }) {
  const { estado, papelAtivo } = useSessao()
  const passo = useProximoPasso(papelAtivo)
  if (estado.status !== 'autenticado') return null
  const daEquipe = estado.usuario.papeis.some((p) => !ehPapelPublico(p))
  const itens = papelAtivo === 'INVESTIDOR' || papelAtivo === 'EMPREENDEDOR' ? ITENS[papelAtivo] : []
  return (
    <aside className={`${aberto ? 'flex' : 'hidden'} fixed inset-y-0 left-0 z-30 w-64 flex-col gap-6 overflow-y-auto border-r border-line bg-surface px-4 py-5 lg:sticky lg:top-0 lg:flex lg:h-svh`}>
      <Link to="/painel" className="flex items-center gap-2" onClick={aoNavegar}>
        <Grafico className="h-9 w-9" />
        <span className="flex flex-col leading-tight">
          <span className="font-display text-2xl font-bold tracking-tight">Orbit Hub</span>
          <span className="text-xs text-muted">Conectando ideias, impulsionando negócios</span>
        </span>
      </Link>
      <nav aria-label="Principal" className="flex flex-col gap-1">
        {itens.map((i) => (
          <NavLink key={i.caminho} to={i.caminho} end={i.caminho === '/painel'} onClick={aoNavegar}
            className={({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 font-medium ${isActive ? 'bg-primary-soft text-fg' : 'text-fg hover:bg-surface-2'}`}>
            <span aria-hidden="true" className="w-5 text-center text-lg">{i.icone}</span>{i.rotulo}
          </NavLink>
        ))}
        {daEquipe && <NavLink to="/admin" onClick={aoNavegar} className="flex items-center gap-3 rounded-lg px-3 py-2.5 font-semibold text-fg hover:bg-surface-2">
          <span aria-hidden="true" className="w-5 text-center text-lg">⚑</span>Backoffice</NavLink>}
      </nav>
      {passo && (
        <section aria-label="Comece agora" className="mt-auto flex flex-col items-center gap-2 rounded-xl bg-surface-2 p-4 text-center">
          <Foguete />
          <h2 className="font-semibold">Comece agora!</h2>
          <p className="text-sm text-muted">{passo.texto}</p>
          <Link to={passo.caminho} onClick={aoNavegar} className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-on-primary">{passo.botao} →</Link>
        </section>
      )}
    </aside>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-167
 * Título: Busca global
 * Objetivo: Buscar startups, organizações e artigos enquanto a pessoa digita, com os resultados agrupados.
 * Decisões: Espera 300 ms entre teclas; a partir de 2 caracteres.
 */
export function BuscaGlobal() {
  const [q, setQ] = useState('')
  const [r, setR] = useState<ResultadoDaBusca | null>(null)
  const navegar = useNavigate()
  const espera = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    if (espera.current) clearTimeout(espera.current)
    if (q.trim().length < 2) { setR(null); return }
    espera.current = setTimeout(() => { buscar(q.trim()).then(setR).catch(() => setR(null)) }, 300)
    return () => { if (espera.current) clearTimeout(espera.current) }
  }, [q])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-167
   * Título: Ir ao resultado
   * Objetivo: Abrir o resultado e limpar a busca.
   * Decisões: Navegação interna.
   */
  function ir(caminho: string) {
    setQ('')
    setR(null)
    navegar(caminho)
  }

  const vazio = r && r.startups.length + r.organizacoes.length + r.artigos.length === 0
  return (
    <div className="relative w-full max-w-2xl">
      <label className="flex items-center gap-2 rounded-full border border-line bg-surface px-4 py-2">
        <span aria-hidden="true" className="text-muted">⌕</span>
        <input type="search" aria-label="Buscar" placeholder="Buscar empresas, investidores, conteúdos…" value={q} onChange={(e) => setQ(e.target.value)}
          className="w-full bg-transparent outline-none" />
        <Link to="/catalogo" aria-label="Filtros do catálogo" className="text-muted">☰</Link>
      </label>
      {r && (
        <div role="listbox" aria-label="Resultados da busca" className="absolute z-40 mt-2 w-full rounded-xl border border-line bg-surface p-2 shadow-lg">
          {vazio && <p className="p-2 text-sm text-muted">Nada encontrado.</p>}
          {([['Empresas', r.startups, (id: string) => `/catalogo/startups/${id}`], ['Investidores', r.organizacoes, () => '/catalogo'],
            ['Ajuda', r.artigos, () => '/ajuda']] as const).map(([titulo, lista, destino]) => lista.length > 0 && (
            <div key={titulo}>
              <p className="px-2 pt-1 text-xs font-semibold uppercase text-muted">{titulo}</p>
              {lista.map((a) => (
                <button key={a.id} type="button" role="option" aria-selected="false" onClick={() => ir(destino(a.id))}
                  className="flex w-full justify-between rounded-lg px-2 py-1.5 text-left hover:bg-surface-2">
                  <span>{a.nome}</span>{a.detalhe && <span className="text-sm text-muted">{a.detalhe}</span>}
                </button>
              ))}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-166, KAN-167
 * Título: Barra superior
 * Objetivo: Mostrar a busca, a criação rápida, o papel ativo, a atuação, o sino, a conta e o botão de sair.
 * Decisões: No celular, um botão abre o menu lateral.
 */
export function BarraSuperior({ aoAbrirMenu }: { aoAbrirMenu: () => void }) {
  const { estado, papelAtivo, escolherPapel, sair } = useSessao()
  if (estado.status !== 'autenticado') return null
  const papeis = estado.usuario.papeis.filter(ehPapelPublico)
  const criacao = papelAtivo === 'INVESTIDOR' || papelAtivo === 'EMPREENDEDOR' ? CRIACAO[papelAtivo] : []
  return (
    <header className="sticky top-0 z-20 flex flex-wrap items-center gap-3 border-b border-line bg-bg/95 px-4 py-3 backdrop-blur">
      <button type="button" aria-label="Abrir menu" onClick={aoAbrirMenu} className="rounded-lg border border-line px-3 py-2 lg:hidden">☰</button>
      <BuscaGlobal />
      <div className="ml-auto flex flex-wrap items-center gap-3 text-sm">
        {criacao.length > 0 && (
          <details className="relative">
            <summary aria-label="Criar" className="flex h-9 w-9 cursor-pointer list-none items-center justify-center rounded-full border border-line text-xl">+</summary>
            <div className="absolute right-0 z-40 mt-2 flex w-48 flex-col rounded-xl border border-line bg-surface p-2 shadow-lg">
              {criacao.map((c) => <Link key={c.caminho + c.rotulo} to={c.caminho} className="rounded-lg px-2 py-1.5 hover:bg-surface-2">{c.rotulo}</Link>)}
            </div>
          </details>
        )}
        {papeis.length > 1 ? (
          <div role="group" aria-label="Atuando como" className="flex items-center gap-1 rounded-full bg-surface-2 p-1">
            {papeis.map((p) => (
              <button key={p} type="button" aria-pressed={p === papelAtivo} onClick={() => escolherPapel(p)}
                className={`rounded-full px-3 py-1 font-semibold ${p === papelAtivo ? 'bg-primary text-on-primary' : ''}`}>
                {NOMES_DOS_PAPEIS[p]}
              </button>
            ))}
          </div>
        ) : (
          papelAtivo && <span className="rounded-full bg-primary-soft px-3 py-1 font-semibold">{NOMES_DOS_PAPEIS[papelAtivo]}</span>
        )}
        {papelAtivo === 'INVESTIDOR' && <SeletorDeAtuacao usuario={estado.usuario.id} />}
        <Sino />
        <Link to="/conta" className="flex items-center gap-2">
          <span aria-hidden="true" className="flex h-9 w-9 items-center justify-center rounded-full bg-accent font-bold text-on-accent">
            {estado.usuario.nome.slice(0, 1).toUpperCase()}</span>
          <span className="hidden sm:inline">{estado.usuario.nome}</span>
        </Link>
        <button type="button" onClick={() => void sair()} className="rounded-full border border-line px-3 py-1 font-semibold">Sair</button>
      </div>
    </header>
  )
}
