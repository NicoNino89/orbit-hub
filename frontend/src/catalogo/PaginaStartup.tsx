import { startupPorSlug, type Pagina } from './dados-ficticios'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Página de startup do catálogo
 * Objetivo: Montar o conteúdo de uma página: capa, problema e solução, ou rodada e critérios.
 * Decisões: Layout editorial do kit de interface (Newsreader nos títulos, mono nos números); mostra só os
 *           critérios da tese que combinaram, nunca percentual, e o aviso de que a plataforma não recomenda
 *           investimento.
 */
export function PaginaStartup({ pagina }: { pagina: Pagina }) {
  const s = startupPorSlug(pagina.startup)

  if (pagina.tipo === 'capa') {
    return (
      <article className="relative flex h-full flex-col gap-4 overflow-hidden bg-primary p-6 text-on-primary sm:p-8">
        <span className="font-mono text-xs uppercase tracking-widest opacity-75">Edição de demonstração · {s.setor}</span>
        <h3 className="font-display text-4xl font-semibold leading-none tracking-tight sm:text-6xl">{s.nome}</h3>
        <p className="max-w-md text-base opacity-90 sm:text-lg">{s.frase}</p>
        <div className="mt-auto flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-white/15 px-3 py-1">{s.cidade}</span>
          <span className="rounded-full bg-white/15 px-3 py-1">{s.estagio}</span>
        </div>
        <span aria-hidden="true" className="absolute -right-16 -bottom-16 size-64 rounded-full border border-white/25" />
        <span aria-hidden="true" className="absolute right-12 bottom-12 size-16 rounded-full bg-accent opacity-90" />
      </article>
    )
  }

  if (pagina.tipo === 'negocio') {
    return (
      <article className="flex h-full flex-col gap-4 p-6 sm:p-8">
        <span className="font-mono text-xs uppercase tracking-widest text-muted">{s.nome} · página 2 de 3</span>
        <h3 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Problema e solução</h3>
        <div className="grid gap-4 sm:grid-cols-2">
          <Bloco rotulo="Problema">{s.problema}</Bloco>
          <Bloco rotulo="Solução">{s.solucao}</Bloco>
        </div>
        <Bloco rotulo="Tração">
          <ul className="list-disc pl-5">
            {s.tracao.map((t) => <li key={t}>{t}</li>)}
          </ul>
        </Bloco>
      </article>
    )
  }

  return (
    <article className="flex h-full flex-col gap-4 p-6 sm:p-8">
      <span className="font-mono text-xs uppercase tracking-widest text-muted">{s.nome} · página 3 de 3</span>
      <h3 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Rodada e critérios</h3>
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt className="text-muted">Instrumento</dt><dd>{s.rodada.instrumento}</dd>
        <dt className="text-muted">Captação</dt><dd className="font-mono">{s.rodada.alvo}</dd>
        <dt className="text-muted">Aporte mínimo</dt><dd className="font-mono">{s.rodada.minimo}</dd>
        <dt className="text-muted">Liderança</dt><dd>{s.rodada.lider}</dd>
      </dl>
      <Bloco rotulo="Critérios da sua tese que combinaram">
        <ul className="flex flex-col gap-1">
          {s.criteriosQueCombinaram.map((c) => (
            <li key={c} className="flex gap-2"><span aria-hidden="true" className="text-ok">✓</span>{c}</li>
          ))}
        </ul>
      </Bloco>
      <p className="mt-auto text-xs text-muted">
        O Orbit Hub conecta as partes e não faz recomendação de investimento nem intermedeia ofertas.
      </p>
    </article>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Bloco rotulado
 * Objetivo: Exibir um rótulo pequeno em caixa alta acima de um conteúdo.
 * Decisões: Componente local, sem exportar: só a página de startup usa esse padrão por enquanto.
 */
function Bloco({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1 text-sm">
      <span className="text-xs font-semibold uppercase tracking-widest text-muted">{rotulo}</span>
      <div>{children}</div>
    </div>
  )
}
