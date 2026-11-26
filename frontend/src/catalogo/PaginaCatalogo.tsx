import { useEffect, useMemo, useState } from 'react'
import { Link, NavLink } from 'react-router'
import { CartaoDeOportunidade } from '../inicio/Inicio'
import {
  descartar, favoritar, type FiltrosDoCatalogo, type ItemDoCatalogo, limparHistorico, type ListaDoCatalogo, type Marcada,
  minhasMarcacoes, navegarNoCatalogo, type ResultadoDoCatalogo, SEM_FILTROS, consultaDosFiltros, type TipoDeMarcacao,
} from '../api/catalogo'
import { opcoesDaLista } from '../api/organizacoes'
import { NOMES_DA_MATURIDADE } from '../api/perfilDeInvestimento'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { Folheador } from './Folheador'

type Opcao = { codigo: string; rotulo: string }
const CHAVE_POSICAO = 'orbit_catalogo_posicao'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-37
 * Título: Posição lembrada
 * Objetivo: Ler e guardar a última página visitada de cada lista com os mesmos filtros.
 * Decisões: No navegador (localStorage), por lista e filtros; é só conforto de navegação, sem dado sensível.
 */
function posicaoSalva(chave: string): number {
  try {
    const mapa = JSON.parse(localStorage.getItem(CHAVE_POSICAO) ?? '{}') as Record<string, number>
    return mapa[chave] ?? 0
  } catch {
    return 0
  }
}

function salvarPosicao(chave: string, indice: number) {
  try {
    const mapa = JSON.parse(localStorage.getItem(CHAVE_POSICAO) ?? '{}') as Record<string, number>
    localStorage.setItem(CHAVE_POSICAO, JSON.stringify({ ...mapa, [chave]: indice }))
  } catch {
    localStorage.setItem(CHAVE_POSICAO, JSON.stringify({ [chave]: indice }))
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38
 * Título: Escolha múltipla
 * Objetivo: Mostrar opções marcáveis de um filtro.
 * Decisões: Caixas de seleção num grupo com legenda.
 */
function Multipla({ rotulo, opcoes, valor, aoMudar }: { rotulo: string; opcoes: Opcao[]; valor: string[]; aoMudar: (v: string[]) => void }) {
  return (
    <fieldset className="flex max-h-40 flex-col gap-1 overflow-y-auto rounded-md border border-line p-2 text-sm">
      <legend className="px-1 font-semibold">{rotulo}</legend>
      {opcoes.map((o) => (
        <label key={o.codigo} className="flex items-center gap-2">
          <input type="checkbox" checked={valor.includes(o.codigo)}
            onChange={(e) => aoMudar(e.target.checked ? [...valor, o.codigo] : valor.filter((v) => v !== o.codigo))} />
          {o.rotulo}
        </label>
      ))}
    </fieldset>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38
 * Título: Filtros do catálogo
 * Objetivo: Combinar segmento, maturidade, localização e rodada aberta e, para investidor verificado, estágio,
 *           instrumento e faixa de captação.
 * Decisões: Os filtros só valem ao clicar em "Aplicar"; "Limpar" volta ao catálogo inteiro.
 */
function PainelDeFiltros({ verificado, aplicados, aoAplicar }: {
  verificado: boolean
  aplicados: FiltrosDoCatalogo
  aoAplicar: (f: FiltrosDoCatalogo) => void
}) {
  const [f, setF] = useState(aplicados)
  const [opcoes, setOpcoes] = useState<Record<string, Opcao[]>>({})
  useEffect(() => {
    let ativo = true
    for (const lista of ['SEGMENTO', 'LOCALIZACAO', 'ESTAGIO_RODADA', 'INSTRUMENTO']) {
      opcoesDaLista(lista).then((o) => { if (ativo) setOpcoes((atual) => ({ ...atual, [lista]: o })) }).catch(() => undefined)
    }
    return () => { ativo = false }
  }, [])
  const maturidades = Object.entries(NOMES_DA_MATURIDADE).map(([codigo, rotulo]) => ({ codigo, rotulo }))

  return (
    <details className="rounded-lg border border-line bg-surface p-3">
      <summary className="cursor-pointer text-sm font-semibold">Filtros</summary>
      <form aria-label="Filtros" className="mt-3 flex flex-col gap-3" onSubmit={(e) => { e.preventDefault(); aoAplicar(f) }}>
        <div className="grid gap-3 md:grid-cols-3">
          <Multipla rotulo="Segmento" opcoes={opcoes.SEGMENTO ?? []} valor={f.segmento} aoMudar={(v) => setF({ ...f, segmento: v })} />
          <Multipla rotulo="Maturidade do negócio" opcoes={maturidades} valor={f.maturidade} aoMudar={(v) => setF({ ...f, maturidade: v })} />
          <Multipla rotulo="Localização" opcoes={opcoes.LOCALIZACAO ?? []} valor={f.localizacao} aoMudar={(v) => setF({ ...f, localizacao: v })} />
        </div>
        <label className="flex items-center gap-2 text-sm">
          Rodada aberta
          <select value={f.comRodada} onChange={(e) => setF({ ...f, comRodada: e.target.value as FiltrosDoCatalogo['comRodada'] })}
            className="rounded-md border border-line px-2 py-1">
            <option value="">Tanto faz</option>
            <option value="true">Com rodada aberta</option>
            <option value="false">Sem rodada aberta</option>
          </select>
        </label>
        {verificado ? (
          <div className="grid gap-3 md:grid-cols-3">
            <Multipla rotulo="Estágio da rodada" opcoes={opcoes.ESTAGIO_RODADA ?? []} valor={f.estagio} aoMudar={(v) => setF({ ...f, estagio: v })} />
            <Multipla rotulo="Instrumento" opcoes={opcoes.INSTRUMENTO ?? []} valor={f.instrumento} aoMudar={(v) => setF({ ...f, instrumento: v })} />
            <fieldset className="flex flex-col gap-1 rounded-md border border-line p-2 text-sm">
              <legend className="px-1 font-semibold">Faixa de captação (R$)</legend>
              <input type="number" min={0} aria-label="Captação mínima" placeholder="Mínimo" value={f.captacaoMinima}
                onChange={(e) => setF({ ...f, captacaoMinima: e.target.value })} className="rounded-md border border-line px-2 py-1" />
              <input type="number" min={0} aria-label="Captação máxima" placeholder="Máximo" value={f.captacaoMaxima}
                onChange={(e) => setF({ ...f, captacaoMaxima: e.target.value })} className="rounded-md border border-line px-2 py-1" />
            </fieldset>
          </div>
        ) : <p className="text-xs text-muted">Filtros de rodada (estágio, instrumento e faixa) aparecem para investidores verificados.</p>}
        <div className="flex gap-3">
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Aplicar filtros</button>
          <button type="button" className={CLASSE_LINK} onClick={() => { setF(SEM_FILTROS); aoAplicar(SEM_FILTROS) }}>Limpar</button>
        </div>
      </form>
    </details>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-35, KAN-39, KAN-41, KAN-44
 * Título: Página de uma startup no catálogo
 * Objetivo: Mostrar, numa página do folheador, a startup com os critérios da tese que combinaram e as ações de abrir,
 *           favoritar e descartar.
 * Decisões: Só os critérios atendidos, nunca percentual. Ocupa a tela inteira no celular (classe do folheador).
 */
function PaginaDeStartup({ item, aoMudar }: { item: ItemDoCatalogo; aoMudar: () => void }) {
  const s = item.startup
  return (
    <article className="flex h-full flex-col gap-4 overflow-y-auto p-6 sm:p-8">
      <span className="font-mono text-xs uppercase tracking-widest text-muted">{s.segmento}</span>
      <h3 className="font-display text-3xl font-semibold tracking-tight sm:text-5xl">{s.nome}</h3>
      <p className="flex flex-wrap gap-2 text-xs">
        {s.estagio && <span className="rounded-full bg-surface-2 px-3 py-1">{s.estagio}</span>}
        {s.localizacao && <span className="rounded-full bg-surface-2 px-3 py-1">{s.localizacao}</span>}
        <span className="rounded-full bg-surface-2 px-3 py-1">{item.rodadaAberta ? 'Rodada aberta' : 'Sem rodada aberta'}</span>
      </p>
      {item.criterios.length > 0 && (
        <section aria-label="Critérios da sua tese que combinaram" className="flex flex-col gap-1 text-sm">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-muted">Critérios da sua tese que combinaram</h4>
          <ul className="flex flex-col gap-1">
            {item.criterios.map((c) => <li key={c} className="flex gap-2"><span aria-hidden="true" className="text-ok">✓</span>{c}</li>)}
          </ul>
        </section>
      )}
      <div className="mt-auto flex flex-wrap items-center gap-3 text-sm">
        <Link to={`/catalogo/startups/${s.id}`} className={CLASSE_BOTAO_PRIMARIO}>Ver apresentação</Link>
        <button type="button" aria-pressed={item.favorito} className={CLASSE_LINK}
          onClick={() => void favoritar(s.id, !item.favorito).then(aoMudar)}>
          {item.favorito ? '★ Nos favoritos' : '☆ Salvar nos favoritos'}
        </button>
        <button type="button" className={CLASSE_LINK} onClick={() => void descartar(s.id, true).then(aoMudar)}>Descartar</button>
      </div>
      <p className="text-xs text-muted">O Orbit Hub conecta as partes e não faz recomendação de investimento nem intermedeia ofertas.</p>
    </article>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-35, KAN-37, KAN-38, KAN-39, KAN-40
 * Título: Catálogo
 * Objetivo: Folhear a seleção personalizada ou as novas oportunidades, com filtros, posição e progresso, e chegar às
 *           edições e às listas pessoais.
 * Decisões: Uma startup por página do folheador (mouse, teclado e toque já vêm dele). A posição volta à última visitada
 *           de cada lista e filtros. Sem resultado, orienta a ajustar os filtros.
 */
export function PaginaCatalogo() {
  const [lista, setLista] = useState<ListaDoCatalogo>('PERSONALIZADA')
  const [filtros, setFiltros] = useState<FiltrosDoCatalogo>(SEM_FILTROS)
  const [resultado, setResultado] = useState<ResultadoDoCatalogo | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [grade, setGrade] = useState(false)
  const chave = useMemo(() => consultaDosFiltros(lista, filtros), [lista, filtros])

  useEffect(() => {
    let ativo = true
    navegarNoCatalogo(lista, filtros).then((r) => { if (ativo) { setResultado(r); setErro(null) } })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [lista, filtros, versao])

  const paginas = (resultado?.itens ?? []).map((i) => ({ ...i, id: i.startup.id, titulo: i.startup.nome }))
  return (
    <section className="flex flex-col gap-4">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h1 className="font-display text-3xl font-bold tracking-tight">Explorar</h1>
          <nav aria-label="Listas do catálogo" className="flex flex-wrap gap-2 text-sm">
            {([['PERSONALIZADA', 'Seleção personalizada'], ['NOVAS', 'Novas oportunidades']] as const).map(([valor, nome]) => (
              <button key={valor} type="button" aria-pressed={lista === valor} onClick={() => setLista(valor)}
                className={lista === valor ? 'rounded-md bg-primary px-3 py-1 font-semibold text-on-primary' : 'rounded-md border border-line px-3 py-1'}>
                {nome}
              </button>
            ))}
            <Link to="/catalogo/edicoes" className="rounded-md border border-line px-3 py-1">Edições temáticas</Link>
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm">
          <div role="group" aria-label="Visualização" className="flex rounded-full bg-surface-2 p-1">
            <button type="button" aria-pressed={!grade} onClick={() => setGrade(false)} className={`rounded-full px-3 py-1 ${!grade ? 'bg-primary text-on-primary' : ''}`}>Página a página</button>
            <button type="button" aria-pressed={grade} onClick={() => setGrade(true)} className={`rounded-full px-3 py-1 ${grade ? 'bg-primary text-on-primary' : ''}`}>Grade</button>
          </div>
          <Link to="/catalogo/minhas" className={CLASSE_LINK}>Favoritos, histórico e descartadas</Link>
        </div>
      </header>
      {resultado && lista === 'PERSONALIZADA' && !resultado.comPerfil && (
        <p className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
          Sem tese de investimento, mostramos as mais recentes. <Link to="/investidor/onboarding" className={CLASSE_LINK}>Definir a tese</Link>
        </p>
      )}
      <PainelDeFiltros key={chave} verificado={resultado?.verificado ?? false} aplicados={filtros} aoAplicar={setFiltros} />
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {resultado && paginas.length === 0 && (
        <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nenhuma startup encontrada. Ajuste ou limpe os filtros.</p>
      )}
      {grade && paginas.length > 0 && (
        <div role="list" aria-label="Grade de startups" className="grid grid-cols-[repeat(auto-fill,minmax(15rem,1fr))] gap-4">
          {resultado?.itens.map((i) => <div role="listitem" key={i.startup.id}><CartaoDeOportunidade item={i} aoFavoritar={() => setVersao((v) => v + 1)} /></div>)}
        </div>
      )}
      {!grade && paginas.length > 0 && (
        <Folheador key={`${chave}-${paginas.length}`} paginas={paginas} rotulo="Startups do catálogo" comProgresso
          inicial={posicaoSalva(chave)} aoMudar={(i) => salvarPosicao(chave, i)}
          renderizar={(p) => <PaginaDeStartup item={p} aoMudar={() => setVersao((v) => v + 1)} />} />
      )}
    </section>
  )
}

const NOMES_DAS_MINHAS: Record<TipoDeMarcacao, string> = { FAVORITOS: 'Favoritos', HISTORICO: 'Visualizadas', DESCARTADAS: 'Descartadas' }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-42, KAN-43, KAN-44
 * Título: Minhas listas do catálogo
 * Objetivo: Listar favoritos (com data em que foram salvos), visualizadas (com limpar) e descartadas (com desfazer).
 * Decisões: Startup que saiu do catálogo aparece como indisponível, sem link. Tudo é privado da pessoa.
 */
export function PaginaMinhasDoCatalogo() {
  const [tipo, setTipo] = useState<TipoDeMarcacao>('FAVORITOS')
  const [itens, setItens] = useState<Marcada[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    minhasMarcacoes(tipo).then((m) => { if (ativo) setItens(m) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [tipo, versao])

  const recarregar = () => setVersao((v) => v + 1)
  return (
    <section className="flex flex-col gap-4">
      <Link to="/catalogo" className={`${CLASSE_LINK} text-sm`}>← Catálogo</Link>
      <h1 className="font-display text-3xl font-semibold tracking-tight">Minhas listas</h1>
      <nav aria-label="Minhas listas" className="flex flex-wrap gap-2 text-sm">
        {(Object.keys(NOMES_DAS_MINHAS) as TipoDeMarcacao[]).map((t) => (
          <button key={t} type="button" aria-pressed={tipo === t} onClick={() => setTipo(t)}
            className={tipo === t ? 'rounded-md bg-primary px-3 py-1 font-semibold text-on-primary' : 'rounded-md border border-line px-3 py-1'}>
            {NOMES_DAS_MINHAS[t]}
          </button>
        ))}
      </nav>
      <p className="text-xs text-muted">Só você vê estas listas. Favoritar ou descartar não avisa o empreendedor.</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {tipo === 'HISTORICO' && itens && itens.length > 0 && (
        <button type="button" className={`${CLASSE_LINK} self-start text-sm`} onClick={() => void limparHistorico().then(recarregar)}>Limpar histórico</button>
      )}
      {itens && itens.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nada por aqui ainda.</p>}
      {itens && itens.length > 0 && (
        <ul aria-label={NOMES_DAS_MINHAS[tipo]} className="flex flex-col gap-2">
          {itens.map((m) => (
            <li key={m.startup.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-4 text-sm">
              <span>
                {m.disponivel ? <Link to={`/catalogo/startups/${m.startup.id}`} className={CLASSE_LINK}>{m.startup.nome}</Link>
                  : <span className="font-semibold">{m.startup.nome} <span className="text-xs text-warn">(indisponível)</span></span>}
                <span className="block text-xs text-muted">
                  {[m.startup.segmento, m.startup.estagio].filter(Boolean).join(' · ')} · {tipo === 'FAVORITOS' ? 'salva' : tipo === 'HISTORICO' ? 'vista' : 'descartada'} em {FORMATO_DATA_HORA.format(new Date(m.em))}
                </span>
              </span>
              {tipo === 'FAVORITOS' && <button type="button" className={CLASSE_LINK} onClick={() => void favoritar(m.startup.id, false).then(recarregar)}>Remover</button>}
              {tipo === 'DESCARTADAS' && <button type="button" className={CLASSE_LINK} onClick={() => void descartar(m.startup.id, false).then(recarregar)}>Desfazer</button>}
            </li>
          ))}
        </ul>
      )}
      <NavLink to="/catalogo/edicoes" className={`${CLASSE_LINK} text-sm`}>Edições temáticas</NavLink>
    </section>
  )
}
