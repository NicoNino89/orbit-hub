import { PAGINAS } from './dados-ficticios'
import { Folheador } from './Folheador'
import { PaginaStartup } from './PaginaStartup'

/** Tela da prova de conceito KAN-58: três startups fictícias, três páginas cada. */
export function PocCatalogo() {
  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-6 px-4 py-8">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <div>
          <span className="text-xs font-semibold uppercase tracking-widest text-muted">Prova de conceito · KAN-58</span>
          <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Catálogo com virada de páginas</h1>
        </div>
        <a href="#/" className="text-sm text-primary">Voltar ao início</a>
      </header>

      <p className="max-w-prose text-sm text-muted">
        Use as setas, o teclado (← →, Home, End), clique nos pontos ou arraste a folha pelo canto, com o mouse ou o dedo.
        Com o movimento reduzido ligado no sistema, as páginas trocam sem animação. Conteúdo fictício.
      </p>

      <Folheador
        paginas={PAGINAS}
        rotulo="Edição de demonstração do catálogo"
        renderizar={(p) => <PaginaStartup pagina={p} />}
      />
    </main>
  )
}
