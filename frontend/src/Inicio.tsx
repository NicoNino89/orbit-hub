import { useEffect, useState } from 'react'
import { buscarSobre, type Sobre } from './api/cliente'
import { FundoDoBanner, Grafico } from './ui/Ilustracoes'

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
    <main className="min-h-svh bg-bg">
      <header className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5">
        <span className="flex items-center gap-2 text-xl font-bold tracking-tight"><Grafico className="h-8 w-8" />Orbit Hub</span>
        <nav aria-label="Acesso" className="flex gap-3">
          <a href="/entrar" className="rounded-lg border border-line px-4 py-2 font-semibold">Entrar</a>
          <a href="/cadastro" className="rounded-lg bg-primary px-4 py-2 font-semibold text-on-primary">Criar conta</a>
        </nav>
      </header>
      <section className="relative mx-auto max-w-6xl overflow-hidden rounded-3xl px-4 text-white">
        <div className="relative overflow-hidden rounded-3xl">
          <FundoDoBanner />
          <div className="relative flex flex-col gap-4 p-10 md:p-16">
            <h1 className="max-w-2xl text-4xl font-bold leading-tight md:text-5xl">Empresas que inspiram. <span className="text-accent">Investidores que acreditam.</span></h1>
            <p className="max-w-xl text-lg text-white/90">Uma rede para conectar empreendedores, investidores e conhecimento, com verificação, curadoria e confidencialidade.</p>
            <div className="flex flex-wrap gap-3">
              <a href="/cadastro" className="rounded-lg bg-accent px-5 py-2 font-semibold text-on-accent">Começar agora →</a>
              <a href="/ajuda" className="rounded-lg border border-white/60 px-5 py-2 font-semibold">Como funciona</a>
            </div>
          </div>
        </div>
      </section>
      <section aria-label="Como funciona" className="mx-auto grid max-w-6xl gap-4 px-4 py-10 md:grid-cols-2">
        <article className="rounded-2xl border border-line bg-surface p-6">
          <h2 className="text-xl font-bold">Para investidores</h2>
          <p className="mt-2 text-muted">Defina sua tese, receba startups compatíveis, manifeste interesse e, com o Match, acesse informações confidenciais liberadas pela startup.</p>
        </article>
        <article className="rounded-2xl border border-line bg-surface p-6">
          <h2 className="text-xl font-bold">Para empreendedores</h2>
          <p className="mt-2 text-muted">Cadastre a startup, passe pela curadoria, escolha o que é público e converse com investidores verificados, inclusive na sua rodada.</p>
        </article>
      </section>
      <section aria-label="Confiança" className="mx-auto grid max-w-6xl gap-4 px-4 pb-10 sm:grid-cols-3">
        {[['Verificação', 'Investidores e startups com identidade conferida.'], ['Curadoria', 'Cada startup é revisada antes de ir ao catálogo.'],
          ['Confidencialidade', 'Dados sensíveis só com liberação da startup e termo aceito.']].map(([t, d]) => (
          <div key={t} className="rounded-2xl bg-primary-soft p-5"><h3 className="font-bold">{t}</h3><p className="text-sm">{d}</p></div>
        ))}
      </section>
      <footer className="mx-auto flex max-w-6xl flex-col gap-4 border-t border-line px-4 py-6 text-sm text-muted">
        <section aria-labelledby="titulo-status" className="flex flex-wrap items-center gap-3">
          <h2 id="titulo-status" className="font-semibold">Status da API</h2>
          <StatusApi estado={api} />
        </section>
        <nav aria-label="Rodapé" className="flex flex-wrap gap-4">
          <a href="/termos" className="text-primary">Termos, políticas e avisos</a>
          <a href="/ajuda" className="text-primary">Central de ajuda</a>
          <a href="/poc-catalogo" className="text-primary">Prova de conceito do catálogo</a>
        </nav>
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
