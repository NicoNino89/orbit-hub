import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  type Apresentacao, apresentacaoNoCatalogo, type CartaoDeStartup, descartar, edicaoVigente, type EdicaoNoCatalogo, edicoesVigentes, favoritar,
  logotipoNoCatalogo, marcacoesDaStartup,
} from '../api/catalogo'
import { requisitar } from '../api/cliente'
import { PainelDeInteresse } from '../conexoes/Conexoes'
import { AreaConfidencial } from '../dataroom/Nivel2'
import { useSessao } from '../sessao/contexto'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'
import { ApresentacaoEmPaginas } from './ApresentacaoEmPaginas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-36
 * Título: Startup no catálogo
 * Objetivo: Mostrar a apresentação de uma startup publicada a quem navega no catálogo.
 * Decisões: Só com login (rota protegida). Os dados da rodada dependem do selo do investidor (servidor). Botões de
 *           favoritar e descartar (KAN-41, KAN-44), sem aviso ao empreendedor.
 */
export function PaginaStartupNoCatalogo() {
  const { id = '' } = useParams()
  const [apresentacao, setApresentacao] = useState<Apresentacao | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const logotipo = useCallback(() => logotipoNoCatalogo(id), [id])
  const { papelAtivo: papel } = useSessao()
  const [marcas, setMarcas] = useState<{ favorito: boolean; descartada: boolean } | null>(null)
  const lerMarcas = useCallback(() => { marcacoesDaStartup(id).then(setMarcas).catch(() => undefined) }, [id])
  useEffect(() => { lerMarcas() }, [lerMarcas])

  useEffect(() => {
    let ativo = true
    apresentacaoNoCatalogo(id).then((a) => { if (ativo) setApresentacao(a) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id])

  if (!apresentacao) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  return (
    <section className="flex flex-col gap-4">
      <Link to="/catalogo" className={`${CLASSE_LINK} text-sm`}>← Catálogo</Link>
      {marcas && (
        <div className="flex flex-wrap gap-3 text-sm">
          <button type="button" aria-pressed={marcas.favorito} className={CLASSE_LINK}
            onClick={() => void favoritar(id, !marcas.favorito).then(lerMarcas)}>
            {marcas.favorito ? '★ Nos favoritos' : '☆ Salvar nos favoritos'}
          </button>
          <button type="button" className={CLASSE_LINK} onClick={() => void descartar(id, !marcas.descartada).then(lerMarcas)}>
            {marcas.descartada ? 'Desfazer descarte' : 'Descartar'}
          </button>
        </div>
      )}
      {papel === 'INVESTIDOR' && <PainelDeInteresse startupId={id} />}
      {!apresentacao.comoVerificado && (
        <p className="rounded-md bg-surface-2 px-3 py-2 text-sm text-muted">
          Verifique sua identidade para ver os dados das rodadas abertas.
        </p>
      )}
      <ApresentacaoEmPaginas apresentacao={apresentacao} carregarLogotipo={logotipo} />
      {papel === 'INVESTIDOR' && <AreaConfidencial startupId={id} />}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Cartões das startups
 * Objetivo: Listar as startups de uma edição, na ordem do curador, com link para a apresentação.
 * Decisões: Localização só quando pública.
 */
function CartoesDeStartups({ startups }: { startups: CartaoDeStartup[] }) {
  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {startups.map((s) => (
        <li key={s.id} className="flex flex-col gap-1 rounded-lg border border-line bg-surface p-4 text-sm">
          <Link to={`/catalogo/startups/${s.id}`} className={`${CLASSE_LINK} text-base`}>{s.nome}</Link>
          <span className="text-muted">{[s.segmento, s.estagio, s.localizacao].filter(Boolean).join(' · ')}</span>
        </li>
      ))}
    </ul>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Capa da edição
 * Objetivo: Mostrar a imagem da capa, quando houver.
 * Decisões: Arquivo protegido por login.
 */
function CapaDaEdicao({ edicao }: { edicao: EdicaoNoCatalogo }) {
  const [capa, setCapa] = useState<string | null>(null)
  useEffect(() => {
    if (!edicao.temCapa) return
    let endereco: string | null = null
    let ativo = true
    requisitar<Blob>(`/api/v1/catalogo/edicoes/${edicao.id}/capa`, { comoArquivo: true }).then((arquivo) => {
      if (!ativo) return
      endereco = URL.createObjectURL(arquivo)
      setCapa(endereco)
    }).catch(() => undefined)
    return () => {
      ativo = false
      if (endereco) URL.revokeObjectURL(endereco)
    }
  }, [edicao.id, edicao.temCapa])
  return capa ? <img src={capa} alt={`Capa da edição ${edicao.titulo}`} className="max-h-48 w-full rounded-lg object-cover" /> : null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edições do catálogo
 * Objetivo: Listar as edições temáticas no ar, cada uma com as suas startups.
 * Decisões: A navegação completa do catálogo (filtros, seleção personalizada) chega na S6.
 */
export function PaginaEdicoesDoCatalogo() {
  const [edicoes, setEdicoes] = useState<EdicaoNoCatalogo[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    edicoesVigentes().then((e) => { if (ativo) setEdicoes(e) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Edições do catálogo</h1>
        <p className="text-sm text-muted">Seleções temáticas montadas pela curadoria do Orbit Hub.</p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {edicoes && edicoes.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nenhuma edição no ar agora.</p>}
      {edicoes?.map((e) => (
        <section key={e.id} aria-label={e.titulo} className="flex flex-col gap-3">
          <CapaDaEdicao edicao={e} />
          <h2 className="font-display text-2xl font-semibold"><Link to={`/catalogo/edicoes/${e.id}`}>{e.titulo}</Link></h2>
          <p className="text-sm text-muted">{e.descricao}</p>
          <CartoesDeStartups startups={e.startups} />
        </section>
      ))}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edição do catálogo
 * Objetivo: Mostrar uma edição no ar com as startups na ordem do curador.
 * Decisões: Fora do ar mostra o erro do servidor.
 */
export function PaginaEdicaoDoCatalogo() {
  const { id = '' } = useParams()
  const [edicao, setEdicao] = useState<EdicaoNoCatalogo | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    edicaoVigente(id).then((e) => { if (ativo) setEdicao(e) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id])

  if (!edicao) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  return (
    <section className="flex flex-col gap-4">
      <Link to="/catalogo/edicoes" className={`${CLASSE_LINK} text-sm`}>← Edições do catálogo</Link>
      <CapaDaEdicao edicao={edicao} />
      <h1 className="font-display text-3xl font-semibold tracking-tight">{edicao.titulo}</h1>
      <p className="text-sm text-muted">{edicao.descricao}</p>
      <CartoesDeStartups startups={edicao.startups} />
    </section>
  )
}
