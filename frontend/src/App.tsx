import { useEffect, useState } from 'react'
import { buscarSobre, type Sobre } from './api/cliente'

type EstadoApi =
  | { tipo: 'carregando' }
  | { tipo: 'ok'; sobre: Sobre }
  | { tipo: 'erro'; mensagem: string }

/**
 * Página inicial da S0: mostra que o front-end está no ar e se ele enxerga a API.
 * As telas do produto chegam a partir da S1.
 */
export default function App() {
  const [api, setApi] = useState<EstadoApi>({ tipo: 'carregando' })

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
    </main>
  )
}

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
