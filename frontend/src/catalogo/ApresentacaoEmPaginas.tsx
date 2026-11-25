import { useEffect, useState } from 'react'
import type { Apresentacao, PaginaDaApresentacao } from '../api/catalogo'
import { Folheador } from './Folheador'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-36, KAN-29
 * Título: Apresentação da startup em três páginas
 * Objetivo: Mostrar a apresentação (Apresentação, Resumo executivo, Conheça o empreendimento) no folheador do catálogo,
 *           a mesma para o investidor, a pré-visualização da equipe e a conferência do curador.
 * Decisões: A capa da primeira página traz nome, logotipo, segmento, estágio, localização e selos. Só aparecem os blocos
 *           que vieram da API (os não autorizados já foram omitidos lá). O aviso de que a plataforma não recomenda
 *           investimento fecha a última página.
 */
export function ApresentacaoEmPaginas({ apresentacao, carregarLogotipo }: {
  apresentacao: Apresentacao
  carregarLogotipo?: () => Promise<Blob>
}) {
  const [logo, setLogo] = useState<string | null>(null)
  const { cabecalho: c } = apresentacao

  useEffect(() => {
    if (!c.temLogotipo || !carregarLogotipo) return
    let endereco: string | null = null
    let ativo = true
    carregarLogotipo().then((arquivo) => {
      if (!ativo) return
      endereco = URL.createObjectURL(arquivo)
      setLogo(endereco)
    }).catch(() => undefined)
    return () => {
      ativo = false
      if (endereco) URL.revokeObjectURL(endereco)
    }
  }, [c.id, c.temLogotipo, carregarLogotipo])

  const total = apresentacao.paginas.length
  return (
    <Folheador paginas={apresentacao.paginas} rotulo={`Apresentação da ${c.nome}`}
      renderizar={(p: PaginaDaApresentacao) => {
        const indice = apresentacao.paginas.indexOf(p)
        return (
          <article className="flex h-full flex-col gap-4 overflow-y-auto p-6 sm:p-8">
            <span className="font-mono text-xs uppercase tracking-widest text-muted">{c.nome} · página {indice + 1} de {total}</span>
            {indice === 0 && (
              <header className="flex flex-wrap items-center gap-4">
                {logo ? <img src={logo} alt={`Logotipo de ${c.nome}`} className="h-16 w-16 rounded-lg border border-line object-contain" />
                  : <div aria-hidden="true" className="flex h-16 w-16 items-center justify-center rounded-lg bg-primary text-2xl font-bold text-on-primary">{c.nome[0]}</div>}
                <div className="flex flex-col gap-1">
                  <h3 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">{c.nome}</h3>
                  <p className="flex flex-wrap gap-2 text-xs">
                    <span className="rounded-full bg-surface-2 px-3 py-1">{c.segmento}</span>
                    {c.estagio && <span className="rounded-full bg-surface-2 px-3 py-1">{c.estagio}</span>}
                    {c.localizacao && <span className="rounded-full bg-surface-2 px-3 py-1">{c.localizacao}</span>}
                    {c.selo === 'VERIFICADO' && <span className="rounded-full bg-ok-soft px-3 py-1 font-semibold text-ok">Startup verificada</span>}
                    {c.naoConstituida && <span className="rounded-full bg-warn-soft px-3 py-1 font-semibold text-warn">Empresa ainda não constituída</span>}
                  </p>
                  {c.site && <a href={c.site} target="_blank" rel="noreferrer noopener" className="text-sm underline">{c.site}</a>}
                </div>
              </header>
            )}
            <h3 className="font-display text-2xl font-semibold tracking-tight">{p.titulo}</h3>
            {p.blocos.length === 0 && <p className="text-sm text-muted">Nada público nesta página ainda.</p>}
            <div className="grid gap-4 sm:grid-cols-2">
              {p.blocos.map((b) => (
                <section key={b.titulo} aria-label={b.titulo} className="flex flex-col gap-1">
                  <h4 className="text-xs font-semibold uppercase tracking-wide text-muted">{b.titulo}</h4>
                  <dl className="flex flex-col gap-1 text-sm">
                    {b.itens.map((i) => (
                      <div key={i.rotulo + i.valor}>
                        <dt className="sr-only">{i.rotulo}</dt>
                        <dd><span className="text-muted">{i.rotulo}: </span><span className="whitespace-pre-line">{i.valor}</span></dd>
                      </div>
                    ))}
                  </dl>
                  {b.avisos.map((a) => <p key={a} className="text-xs italic text-muted">{a}</p>)}
                </section>
              ))}
            </div>
            {indice === total - 1 && (
              <p className="mt-auto text-xs text-muted">
                O Orbit Hub conecta as partes e não faz recomendação de investimento nem intermedeia ofertas.
              </p>
            )}
          </article>
        )
      }} />
  )
}
