import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { favoritar, navegarNoCatalogo, SEM_FILTROS, type ItemDoCatalogo, type ListaDoCatalogo } from '../api/catalogo'
import { bannersVigentes, capaNoCatalogo, imagemDoBanner, type Banner } from '../api/inicio'
import { opcoesDaLista } from '../api/organizacoes'
import { lerPerfil, salvarPerfil, type SituacaoDoPerfil } from '../api/perfilDeInvestimento'
import { useSessao } from '../sessao/contexto'
import { Alvo, CapaPadrao, FundoDoBanner, Trofeu } from '../ui/Ilustracoes'

const CARTAO = 'rounded-2xl border border-line bg-surface p-5 shadow-sm'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-170, KAN-171
 * Título: Imagem protegida
 * Objetivo: Baixar uma imagem com o token e mostrá-la por um endereço local.
 * Decisões: Sem imagem (404), devolve nulo e quem chama mostra a ilustração padrão.
 */
function useImagem(chave: string | null, carregar: () => Promise<Blob>) {
  const [url, setUrl] = useState<string | null>(null)
  useEffect(() => {
    if (!chave) { setUrl(null); return }
    let ativo = true
    let endereco: string | null = null
    carregar().then((b) => {
      if (!ativo || typeof URL.createObjectURL !== 'function') return
      endereco = URL.createObjectURL(b)
      setUrl(endereco)
    }).catch(() => setUrl(null))
    return () => { ativo = false; if (endereco) URL.revokeObjectURL(endereco) }
  }, [chave]) // carregar muda a cada render; a chave identifica a imagem
  return url
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-170, KAN-41, KAN-62
 * Título: Cartão de oportunidade
 * Objetivo: Mostrar a startup com capa, nome, chamada, localização e o favorito, como no mockup.
 * Decisões: Sem capa, ilustração do segmento. Nunca mostra valuation nem a situação da curadoria (KAN-28).
 */
export function CartaoDeOportunidade({ item, aoFavoritar }: { item: ItemDoCatalogo; aoFavoritar?: (favorito: boolean) => void }) {
  const s = item.startup
  const capa = useImagem(s.temCapa ? `capa-${s.id}` : null, () => capaNoCatalogo(s.id))
  return (
    <article aria-label={s.nome} className="relative flex h-64 w-60 shrink-0 flex-col justify-end overflow-hidden rounded-2xl bg-fg text-white shadow-md">
      <div className="absolute inset-0">
        {capa ? <img src={capa} alt={`Capa da ${s.nome}`} className="h-full w-full object-cover" /> : <CapaPadrao segmento={s.segmento} nome={s.nome} />}
      </div>
      <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent" />
      {item.rodadaAberta && <span className="absolute left-3 top-3 rounded-full bg-accent px-2 py-0.5 text-xs font-semibold text-on-accent">Rodada aberta</span>}
      <button type="button" aria-pressed={item.favorito} aria-label={item.favorito ? `Tirar ${s.nome} dos favoritos` : `Salvar ${s.nome} nos favoritos`}
        onClick={() => { const novo = !item.favorito; void favoritar(s.id, novo).then(() => aoFavoritar?.(novo)) }}
        className="absolute right-3 top-3 rounded-full bg-white/90 px-2 py-0.5 text-sm text-fg">{item.favorito ? '★' : '☆'}</button>
      <Link to={`/catalogo/startups/${s.id}`} className="relative flex flex-col gap-1 p-4">
        <span className="text-lg font-bold leading-tight">{s.nome}</span>
        <span className="line-clamp-2 text-sm text-white/85">{s.chamada ?? s.segmento}</span>
        <span className="mt-1 flex gap-3 text-xs text-white/80">
          <span>{s.segmento}</span>{s.localizacao && <span>⌖ {s.localizacao}</span>}
        </span>
      </Link>
    </article>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-165, KAN-183
 * Título: Carrossel
 * Objetivo: Rolar cartões na horizontal com setas, toque ou teclado.
 * Decisões: Sem rolagem automática.
 */
export function Carrossel({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  const trilho = useRef<HTMLDivElement>(null)
  const rolar = (lado: number) => trilho.current?.scrollBy?.({ left: lado * 260, behavior: 'smooth' })
  return (
    <div className="relative">
      <div ref={trilho} role="region" aria-label={rotulo} tabIndex={0} className="flex snap-x gap-4 overflow-x-auto pb-2">{children}</div>
      <button type="button" aria-label="Anteriores" onClick={() => rolar(-1)} className="absolute -left-3 top-1/2 hidden h-9 w-9 -translate-y-1/2 rounded-full border border-line bg-surface shadow md:block">‹</button>
      <button type="button" aria-label="Próximos" onClick={() => rolar(1)} className="absolute -right-3 top-1/2 hidden h-9 w-9 -translate-y-1/2 rounded-full border border-line bg-surface shadow md:block">›</button>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-171
 * Título: Banner rotativo
 * Objetivo: Mostrar os banners institucionais vigentes, com troca automática, pausa e indicadores.
 * Decisões: Respeita "reduzir movimento"; título com a parte de destaque em âmbar.
 */
export function BannerRotativo({ banners }: { banners: Banner[] }) {
  const [i, setI] = useState(0)
  const [pausado, setPausado] = useState(false)
  useEffect(() => {
    const reduzir = typeof window.matchMedia === 'function' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (banners.length < 2 || pausado || reduzir) return
    const t = setInterval(() => setI((x) => (x + 1) % banners.length), 7000)
    return () => clearInterval(t)
  }, [banners.length, pausado])
  const b = banners[i]
  const imagem = useImagem(b?.temImagem ? `banner-${b.id}` : null, () => imagemDoBanner(b.id))
  if (!b) return null
  return (
    <section aria-label="Destaques da plataforma" aria-roledescription="carrossel" onMouseEnter={() => setPausado(true)} onMouseLeave={() => setPausado(false)}
      className="relative overflow-hidden rounded-2xl text-white">
      {imagem ? <img src={imagem} alt={b.textoAlt ?? ''} className="absolute inset-0 h-full w-full object-cover opacity-60" /> : <FundoDoBanner />}
      <div className="relative flex min-h-56 flex-col justify-center gap-3 p-8">
        <h2 className="max-w-xl text-3xl font-bold leading-tight md:text-4xl">{b.titulo}{b.destaque && <> <span className="text-accent">{b.destaque}</span></>}</h2>
        {b.texto && <p className="max-w-xl text-lg text-white/90">{b.texto}</p>}
        {b.botao && b.link && <Link to={b.link} className="self-start rounded-lg bg-accent px-5 py-2 font-semibold text-on-accent">{b.botao} →</Link>}
      </div>
      {banners.length > 1 && (
        <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-2">
          {banners.map((x, n) => <button key={x.id} type="button" aria-label={`Destaque ${n + 1}`} aria-current={n === i}
            onClick={() => setI(n)} className={`h-2.5 w-2.5 rounded-full border border-white ${n === i ? 'bg-white' : ''}`} />)}
        </div>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-172, KAN-39
 * Título: Painel do perfil de investidor
 * Objetivo: Mostrar o tipo, o perfil de risco e os interesses (segmentos da tese) como etiquetas removíveis, com
 *           "+ Adicionar interesse"; sem tese, a chamada para defini-la.
 * Decisões: Mudar interesses grava o perfil inteiro (a mesma rota da tese); quem só observa a organização não edita.
 */
export function PainelDoPerfil({ aoMudar }: { aoMudar: () => void }) {
  const [s, setS] = useState<SituacaoDoPerfil | null>(null)
  const [segmentos, setSegmentos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [riscos, setRiscos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [tipos, setTipos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [novo, setNovo] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    lerPerfil().then(setS).catch(() => setS({ daOrganizacao: false, podeEditar: false, tipoFixo: null, perfil: null, atualizadoPor: null, atualizadoEm: null }))
    opcoesDaLista('SEGMENTO').then(setSegmentos).catch(() => setSegmentos([]))
    opcoesDaLista('PERFIL_RISCO').then(setRiscos).catch(() => setRiscos([]))
    opcoesDaLista('TIPO_INVESTIDOR').then(setTipos).catch(() => setTipos([]))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-172
   * Título: Gravar interesses
   * Objetivo: Salvar os segmentos novos e atualizar a seleção.
   * Decisões: Pelo menos um segmento (regra da tese).
   */
  async function gravar(lista: string[]) {
    if (!s?.perfil) return
    setErro(null)
    try {
      setS(await salvarPerfil({ ...s.perfil, segmentos: lista }))
      setNovo('')
      aoMudar()
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Não foi possível salvar.')
    }
  }

  if (!s) return null
  if (!s.perfil) {
    return (
      <section aria-label="Seu perfil, seu foco" className={`${CARTAO} flex flex-col gap-3 bg-primary-soft`}>
        <div className="flex items-center gap-3"><Alvo /><h2 className="text-xl font-bold">Seu perfil, seu foco</h2></div>
        <p>Defina sua tese de investimento e receba indicações personalizadas de empresas e conteúdos.</p>
        <Link to="/investidor/onboarding" className="self-start rounded-lg bg-primary px-4 py-2 font-semibold text-on-primary">Definir tese →</Link>
      </section>
    )
  }
  const p = s.perfil
  const nome = (lista: { codigo: string; rotulo: string }[], c: string | null) => lista.find((x) => x.codigo === c)?.rotulo ?? c
  return (
    <section aria-label="Seu perfil de investidor" className={`${CARTAO} flex flex-col gap-3`}>
      <div>
        <h2 className="font-semibold">Seu perfil de investidor</h2>
        <p className="text-sm text-muted">{nome(tipos, p.tipo)}{p.complementares.perfilRisco ? ` · Perfil: ${nome(riscos, p.complementares.perfilRisco)}` : ''}</p>
      </div>
      <Link to="/investidor/perfil" className="rounded-lg border border-line py-2 text-center font-medium">Editar perfil</Link>
      <h3 className="font-semibold">Seus interesses</h3>
      <ul className="flex flex-wrap gap-2">
        {p.segmentos.map((c) => (
          <li key={c} className="flex items-center gap-1 rounded-full border border-line bg-surface-2 px-3 py-1 text-sm">
            {nome(segmentos, c)}
            {s.podeEditar && p.segmentos.length > 1 && (
              <button type="button" aria-label={`Remover ${nome(segmentos, c)}`} onClick={() => void gravar(p.segmentos.filter((x) => x !== c))}>×</button>
            )}
          </li>
        ))}
      </ul>
      {s.podeEditar && (
        <form aria-label="Adicionar interesse" onSubmit={(e) => { e.preventDefault(); if (novo) void gravar([...p.segmentos, novo]) }} className="flex gap-2">
          <select aria-label="Novo interesse" value={novo} onChange={(e) => setNovo(e.target.value)} className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-2 py-1 text-sm">
            <option value="">+ Adicionar interesse</option>
            {segmentos.filter((x) => !p.segmentos.includes(x.codigo)).map((x) => <option key={x.codigo} value={x.codigo}>{x.rotulo}</option>)}
          </select>
          <button type="submit" className="rounded-lg bg-primary px-3 text-sm font-semibold text-on-primary">Adicionar</button>
        </form>
      )}
      {erro && <p role="alert" className="text-sm text-crit">{erro}</p>}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-171
 * Título: Chamada lateral
 * Objetivo: Mostrar uma chamada institucional (como "Conquiste seu espaço – Participe!").
 * Decisões: Cartão escuro com troféu.
 */
function ChamadaLateral({ b }: { b: Banner }) {
  return (
    <section aria-label={b.titulo} className="flex items-center gap-4 rounded-2xl bg-fg p-5 text-white">
      <Trofeu className="h-14 w-14 shrink-0" />
      <div className="flex flex-col gap-2">
        <p className="font-semibold">{b.titulo}{b.texto ? ` ${b.texto}` : ''}</p>
        {b.botao && b.link && <Link to={b.link} className="self-start rounded-lg bg-accent px-3 py-1 text-sm font-semibold text-on-accent">{b.botao}</Link>}
      </div>
    </section>
  )
}

const ABAS: { lista: ListaDoCatalogo; rotulo: string }[] = [{ lista: 'PERSONALIZADA', rotulo: 'Para você' }, { lista: 'NOVAS', rotulo: 'Empresas' }]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-168
 * Título: Página inicial do investidor
 * Objetivo: Reunir banner, abas (Para você, Empresas), carrossel de empresas em destaque, painel do perfil e chamadas.
 * Decisões: Abas de módulos futuros (Investidores, Consultorias, Educação, Tendências) só entram quando existirem.
 */
export function PaginaInicialDoInvestidor() {
  const { papelAtivo } = useSessao()
  const [banners, setBanners] = useState<Banner[]>([])
  const [aba, setAba] = useState<ListaDoCatalogo>('PERSONALIZADA')
  const [itens, setItens] = useState<ItemDoCatalogo[] | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    bannersVigentes(papelAtivo).then(setBanners).catch(() => setBanners([]))
  }, [papelAtivo])
  useEffect(() => {
    setItens(null)
    navegarNoCatalogo(aba, SEM_FILTROS).then((r) => setItens(r.itens.slice(0, 12))).catch(() => setItens([]))
  }, [aba, versao])
  const principais = banners.filter((b) => b.tipo === 'BANNER')
  const chamadas = banners.filter((b) => b.tipo === 'CHAMADA')
  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
      <div className="flex min-w-0 flex-col gap-6">
        <BannerRotativo banners={principais} />
        <div role="tablist" aria-label="Seções" className="flex flex-wrap gap-2">
          {ABAS.map((a) => (
            <button key={a.lista} type="button" role="tab" aria-selected={aba === a.lista} onClick={() => setAba(a.lista)}
              className={`rounded-full px-4 py-1.5 font-medium ${aba === a.lista ? 'bg-primary text-on-primary' : 'bg-surface-2'}`}>{a.rotulo}</button>
          ))}
        </div>
        <section aria-labelledby="t-destaques" className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 id="t-destaques" className="text-xl font-bold">Empresas em destaque</h2>
            <Link to="/catalogo" className="font-semibold">Ver todas →</Link>
          </div>
          {itens === null && <p className="text-muted">Carregando…</p>}
          {itens?.length === 0 && <p className="text-muted">Nenhuma empresa por aqui ainda. Volte em breve ou explore o catálogo.</p>}
          {itens && itens.length > 0 && (
            <Carrossel rotulo="Empresas em destaque">
              {itens.map((i) => <CartaoDeOportunidade key={i.startup.id} item={i} aoFavoritar={() => setVersao((v) => v + 1)} />)}
            </Carrossel>
          )}
        </section>
      </div>
      <aside aria-label="Seu painel" className="flex flex-col gap-4">
        <PainelDoPerfil aoMudar={() => setVersao((v) => v + 1)} />
        {chamadas.map((b) => <ChamadaLateral key={b.id} b={b} />)}
      </aside>
    </div>
  )
}
