import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import {
  arquivarEdicao, type CartaoDeStartup, comporEdicao, criarEdicao, type DadosDaEdicao, detalheDaEdicao, type DetalheDaEdicao, type Edicao,
  editarEdicao, listarEdicoes, NOMES_DA_SITUACAO_DA_EDICAO, publicarEdicao, startupsElegiveis, trocarCapaDaEdicao,
} from '../api/catalogo'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' })
const VAZIA: DadosDaEdicao = { titulo: '', descricao: '', criterios: '', inicio: '', fim: null }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Formulário da edição
 * Objetivo: Preencher título, descrição, critérios de inclusão e vigência.
 * Decisões: Fim opcional: o servidor usa a frequência padrão das edições.
 */
function FormularioDaEdicao({ inicial, rotulo, aoSalvar }: { inicial: DadosDaEdicao; rotulo: string; aoSalvar: (d: DadosDaEdicao) => void }) {
  const [d, setD] = useState(inicial)
  return (
    <form className="flex flex-col gap-3" onSubmit={(e) => { e.preventDefault(); aoSalvar(d) }}>
      <Campo rotulo="Título" value={d.titulo} maxLength={120} onChange={(e) => setD({ ...d, titulo: e.target.value })} />
      <Campo rotulo="Descrição" value={d.descricao} maxLength={1000} onChange={(e) => setD({ ...d, descricao: e.target.value })} />
      <Campo rotulo="Critérios de inclusão" value={d.criterios} maxLength={1000} onChange={(e) => setD({ ...d, criterios: e.target.value })} />
      <div className="flex flex-wrap gap-3">
        <Campo rotulo="Início" type="date" value={d.inicio} onChange={(e) => setD({ ...d, inicio: e.target.value })} />
        <Campo rotulo="Fim (opcional)" type="date" value={d.fim ?? ''} onChange={(e) => setD({ ...d, fim: e.target.value || null })} />
      </div>
      <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>{rotulo}</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edições temáticas
 * Objetivo: Listar as edições e criar uma nova.
 * Decisões: A nova abre direto na montagem.
 */
export function PaginaEdicoes() {
  const navegar = useNavigate()
  const [edicoes, setEdicoes] = useState<Edicao[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [criando, setCriando] = useState(false)

  useEffect(() => {
    let ativo = true
    listarEdicoes().then((e) => { if (ativo) setEdicoes(e) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-wrap items-end justify-between gap-2">
        <div className="flex flex-col gap-1">
          <h1 className="font-display text-3xl font-semibold tracking-tight">Edições temáticas</h1>
          <p className="text-sm text-muted">Seleções montadas pela curadoria. Publicadas e arquivadas pela vigência (rotina R10).</p>
        </div>
        {!criando && <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => setCriando(true)}>Nova edição</button>}
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {criando && (
        <FormularioDaEdicao inicial={VAZIA} rotulo="Criar edição" aoSalvar={(d) => {
          criarEdicao(d).then((e) => void navegar(`/admin/edicoes/${e.id}`))
            .catch((falha: unknown) => setErro(falha instanceof Error ? falha.message : 'Não foi possível criar.'))
        }} />
      )}
      {edicoes && edicoes.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nenhuma edição ainda.</p>}
      {edicoes && edicoes.length > 0 && (
        <ul aria-label="Edições" className="flex flex-col gap-2">
          {edicoes.map((e) => (
            <li key={e.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-4 text-sm">
              <Link to={`/admin/edicoes/${e.id}`} className={CLASSE_LINK}>{e.titulo}</Link>
              <span className="text-muted">
                {NOMES_DA_SITUACAO_DA_EDICAO[e.situacao]} · {FORMATO_DATA.format(new Date(e.inicio))} a {FORMATO_DATA.format(new Date(e.fim))} · {e.quantidade} startup(s)
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Montagem da edição
 * Objetivo: Editar os dados, a capa, a composição e a ordem, publicar, arquivar e ver o histórico com o curador.
 * Decisões: Ordem por botões subir/descer; cada mudança da composição é salva na hora. Startup oculta (saiu do catálogo
 *           ou das edições) aparece marcada e pode ser retirada.
 */
export function PaginaEdicao() {
  const { id = '' } = useParams()
  const [detalhe, setDetalhe] = useState<DetalheDaEdicao | null>(null)
  const [elegiveis, setElegiveis] = useState<CartaoDeStartup[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    detalheDaEdicao(id).then((d) => { if (ativo) setDetalhe(d) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    startupsElegiveis().then((s) => { if (ativo) setElegiveis(s) }).catch(() => undefined)
    return () => { ativo = false }
  }, [id, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-85
   * Título: Agir
   * Objetivo: Rodar uma ação e recarregar.
   * Decisões: Erro do servidor vai para o alerta.
   */
  async function agir(acao: () => Promise<unknown>, mensagem: string) {
    try {
      await acao()
      setErro(null)
      setAviso(mensagem)
      setVersao((v) => v + 1)
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }

  if (!detalhe) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const e = detalhe.edicao
  const ids = detalhe.startups.map((s) => s.startup.id)
  const arquivada = e.situacao === 'ARQUIVADA'
  const mover = (de: number, para: number) => {
    const nova = [...ids]
    const [item] = nova.splice(de, 1)
    nova.splice(para, 0, item)
    return nova
  }

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <Link to="/admin/edicoes" className={`${CLASSE_LINK} text-sm`}>← Edições</Link>
        <h1 className="font-display text-3xl font-semibold tracking-tight">{e.titulo}</h1>
        <p className="text-sm text-muted">
          {NOMES_DA_SITUACAO_DA_EDICAO[e.situacao]} · {FORMATO_DATA.format(new Date(e.inicio))} a {FORMATO_DATA.format(new Date(e.fim))}
          {e.curador && ` · curadoria de ${e.curador}`}
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      {!arquivada && (
        <div className="flex flex-wrap gap-3 text-sm">
          {e.situacao === 'RASCUNHO' && (
            <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void agir(() => publicarEdicao(id), 'Edição publicada.')}>Publicar</button>
          )}
          <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => arquivarEdicao(id), 'Edição arquivada.')}>Arquivar</button>
          <label className={CLASSE_LINK}>
            {e.temCapa ? 'Trocar capa' : 'Enviar capa'} (PNG, JPG ou WebP)
            <input type="file" accept="image/png,image/jpeg,image/webp" className="sr-only" aria-label="Arquivo da capa"
              onChange={(ev) => {
                const arquivo = ev.target.files?.[0]
                ev.target.value = ''
                if (arquivo) void agir(() => trocarCapaDaEdicao(id, arquivo), 'Capa atualizada.')
              }} />
          </label>
        </div>
      )}

      <section aria-label="Startups da edição" className="flex flex-col gap-2 text-sm">
        <h2 className="font-semibold">Startups, na ordem de exibição</h2>
        {detalhe.startups.length === 0 && <p className="text-muted">Nenhuma startup ainda.</p>}
        <ol className="flex flex-col gap-1">
          {detalhe.startups.map((s, i) => (
            <li key={s.startup.id} className="flex flex-wrap items-center gap-2 rounded-md border border-line px-3 py-2">
              <span className="flex-1">
                {i + 1}. {s.startup.nome} <span className="text-muted">· {s.startup.segmento}{s.startup.estagio && ` · ${s.startup.estagio}`}</span>
                {!s.visivel && <span className="ml-2 text-xs font-semibold text-warn">oculta para o investidor</span>}
              </span>
              {!arquivada && (
                <>
                  <button type="button" aria-label={`Subir ${s.startup.nome}`} disabled={i === 0}
                    onClick={() => void agir(() => comporEdicao(id, mover(i, i - 1)), 'Ordem salva.')}>↑</button>
                  <button type="button" aria-label={`Descer ${s.startup.nome}`} disabled={i === ids.length - 1}
                    onClick={() => void agir(() => comporEdicao(id, mover(i, i + 1)), 'Ordem salva.')}>↓</button>
                  <button type="button" className={CLASSE_LINK}
                    onClick={() => void agir(() => comporEdicao(id, ids.filter((x) => x !== s.startup.id)), 'Startup retirada.')}>Retirar</button>
                </>
              )}
            </li>
          ))}
        </ol>
        {!arquivada && elegiveis.filter((s) => !ids.includes(s.id)).length > 0 && (
          <label className="flex flex-col gap-1">
            Incluir startup publicada
            <select aria-label="Incluir startup" value="" className="rounded-md border border-line px-2 py-1"
              onChange={(ev) => { if (ev.target.value) void agir(() => comporEdicao(id, [...ids, ev.target.value]), 'Startup incluída.') }}>
              <option value="">Escolha…</option>
              {elegiveis.filter((s) => !ids.includes(s.id)).map((s) => <option key={s.id} value={s.id}>{s.nome} · {s.segmento}</option>)}
            </select>
          </label>
        )}
      </section>

      {!arquivada && (
        <details className="text-sm">
          <summary className="cursor-pointer font-semibold">Editar dados da edição</summary>
          <FormularioDaEdicao key={e.atualizadaEm} rotulo="Salvar dados"
            inicial={{ titulo: e.titulo, descricao: e.descricao, criterios: detalhe.criterios, inicio: e.inicio, fim: e.fim }}
            aoSalvar={(d) => void agir(() => editarEdicao(id, d), 'Dados salvos.')} />
        </details>
      )}

      <section aria-label="Histórico da edição" className="flex flex-col gap-1 text-sm">
        <h2 className="font-semibold">Histórico</h2>
        <ol className="flex flex-col gap-1 text-muted">
          {detalhe.historico.map((h) => (
            <li key={h.em + h.acao}>
              {FORMATO_DATA_HORA.format(new Date(h.em))}: {h.acao.toLowerCase().replaceAll('_', ' ')}
              {h.por ? ` por ${h.por}` : ' (rotina)'}{h.detalhe && ` · ${h.detalhe}`}
            </li>
          ))}
        </ol>
      </section>
    </section>
  )
}
