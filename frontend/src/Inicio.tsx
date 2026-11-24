import { useEffect, useState } from 'react'
import { buscarSobre, type Sobre } from './api/cliente'

type EstadoApi =
  | { tipo: 'carregando' }
  | { tipo: 'ok'; sobre: Sobre }
  | { tipo: 'erro'; mensagem: string }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Página inicial da S0
 * Objetivo: Mostrar que o front-end está no ar e se ele enxerga a API, com versão e ambiente.
 * Decisões: Consulta a API ao montar e cancela a chamada ao desmontar (AbortController); as telas do produto
 *           chegam a partir da S1.
 */
export function Inicio() {
  const [api, setApi] = useState<EstadoApi>({ tipo: 'carregando' })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Consultar a API ao abrir a página
   * Objetivo: Buscar versão e ambiente da API e guardar o resultado ou o erro.
   * Decisões: Erro de uma chamada já cancelada é ignorado, para não piscar "indisponível" ao sair da tela.
   */
  useEffect(() => {
    const controle = new AbortController()
    buscarSobre(controle.signal)
      .then((sobre) => setApi({ tipo: 'ok', sobre }))
      .catch((erro: unknown) => {
        if (controle.signal.aborted) return
        setApi({ tipo: 'erro', mensagem: erro instanceof Error ? erro.message : 'Falha desconhecida' })
      })
    return () => controle.abort()
  }, [])

  return (
    <main className="mx-auto flex min-h-svh max-w-3xl flex-col justify-center gap-8 px-4 py-12">
      <header className="flex items-center gap-3">
        <img src="/favicon.svg" alt="" width={40} height={40} />
        <span className="text-xl font-bold tracking-tight">Orbit Hub</span>
      </header>

      <div className="flex flex-col gap-3">
        <span className="text-xs font-semibold uppercase tracking-widest text-muted">Fundação · S0</span>
        <h1 className="font-display text-4xl font-semibold tracking-tight sm:text-5xl">
          A plataforma está sendo construída
        </h1>
        <p className="max-w-prose text-muted">
          Esta página confirma que o front-end está publicado e consegue falar com a API.
        </p>
      </div>

      <section aria-labelledby="titulo-status" className="rounded-lg border border-line bg-surface p-5">
        <h2 id="titulo-status" className="mb-3 text-sm font-semibold">Status da API</h2>
        <StatusApi estado={api} />
      </section>

      <a href="/poc-catalogo" className="self-start rounded-md bg-primary px-4 py-2 font-semibold text-on-primary">
        Ver a prova de conceito do catálogo
      </a>

      <footer className="border-t border-line pt-4 text-sm text-muted">
        <a href="/termos" className="text-primary">Termos, políticas e avisos</a>
      </footer>
    </main>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Status da API
 * Objetivo: Exibir o estado da consulta à API: carregando, no ar (versão e ambiente) ou indisponível.
 * Decisões: role="status" no carregamento e role="alert" no erro, para o leitor de tela anunciar a mudança.
 */
function StatusApi({ estado }: { estado: EstadoApi }) {
  if (estado.tipo === 'carregando') {
    return <p role="status" className="text-muted">Consultando a API…</p>
  }
  if (estado.tipo === 'erro') {
    return (
      <p role="alert" className="rounded-md bg-crit-soft px-3 py-2 text-crit">
        API indisponível: {estado.mensagem}
      </p>
    )
  }
  const { sobre } = estado
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1 text-sm">
      <dt className="text-muted">Situação</dt>
      <dd className="font-semibold text-ok">No ar</dd>
      <dt className="text-muted">Versão</dt>
      <dd className="font-mono">{sobre.versao}</dd>
      <dt className="text-muted">Ambiente</dt>
      <dd className="font-mono">{sobre.ambiente}</dd>
    </dl>
  )
}
