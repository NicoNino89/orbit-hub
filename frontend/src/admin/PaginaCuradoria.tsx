import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  apresentacaoCompleta, type Apresentacao, assumirRevisao, decidirRevisao, declararImpedimentoNaCuradoria, detalheDaCuradoria,
  type DecisaoDaCuradoria, type DetalheDaCuradoria, filaDaCuradoria, type ItemDaFilaDeCuradoria, NOMES_DA_SITUACAO_DA_REVISAO,
  NOMES_DO_TIPO_DA_REVISAO,
} from '../api/catalogo'
import { requisitar } from '../api/cliente'
import { NOMES_DAS_SECOES, type SecaoDaStartup } from '../api/startups'
import { ApresentacaoEmPaginas } from '../catalogo/ApresentacaoEmPaginas'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const NOMES_DO_SELO = { VERIFICADO: 'verificado', EM_ANALISE: 'em análise', NAO_VERIFICADO: 'não verificado' } as const
const SECOES: SecaoDaStartup[] = Object.keys(NOMES_DAS_SECOES) as SecaoDaStartup[]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-84
 * Título: Fila da curadoria
 * Objetivo: Mostrar à curadoria as revisões abertas por ordem de chegada, com prazo, responsável e conflitos.
 * Decisões: Mesmo padrão da fila de verificação; atrasada em destaque.
 */
export function PaginaFilaDaCuradoria() {
  const [fila, setFila] = useState<ItemDaFilaDeCuradoria[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    filaDaCuradoria().then((r) => { if (ativo) setFila(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Curadoria</h1>
        <p className="text-sm text-muted">Startups enviadas para publicação e alterações relevantes, da mais antiga para a mais nova.</p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {fila && fila.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">A fila está vazia.</p>}
      {fila && fila.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Revisões abertas">
          {fila.map((i) => (
            <li key={i.id} className={`flex flex-wrap items-center justify-between gap-2 rounded-lg border bg-surface p-4 text-sm ${i.atrasado ? 'border-crit' : 'border-line'}`}>
              <span>
                <Link to={`/admin/curadoria/${i.id}`} className={CLASSE_LINK}>{i.startup}</Link>
                <span className="block text-xs text-muted">
                  {NOMES_DO_TIPO_DA_REVISAO[i.tipo]} · {NOMES_DA_SITUACAO_DA_REVISAO[i.situacao]} · enviada em {FORMATO_DATA_HORA.format(new Date(i.enviadaEm))}
                </span>
              </span>
              <span className="flex flex-col items-end text-xs">
                <span className={i.atrasado ? 'font-semibold text-crit' : 'text-muted'}>
                  {i.atrasado ? 'Atrasada' : 'Prazo'}: {FORMATO_DATA_HORA.format(new Date(i.prazo))}
                </span>
                <span className="text-muted">{i.responsavel ? `Com ${i.responsavel}` : 'Sem responsável'}</span>
                {i.conflitoComigo && <span className="font-semibold text-warn">Você tem vínculo com esta startup</span>}
                {i.semCuradorLivre && <span className="font-semibold text-warn">Todos os curadores têm vínculo: fica com o administrador</span>}
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
 * Cartão Jira: KAN-66, KAN-84
 * Título: Análise da revisão
 * Objetivo: Mostrar ao curador a startup completa (três páginas), os selos, as alterações e o checklist, e registrar a
 *           decisão: aprovar, devolver com comentários por seção ou recusar por fraude (segunda aprovação).
 * Decisões: O checklist começa todo desmarcado; aprovar só com todos atendidos (o servidor também confere). Impedimento
 *           com motivo devolve a revisão à fila.
 */
export function PaginaRevisaoDaCuradoria() {
  const { id = '' } = useParams()
  const [detalhe, setDetalhe] = useState<DetalheDaCuradoria | null>(null)
  const [apresentacao, setApresentacao] = useState<Apresentacao | null>(null)
  const [marcas, setMarcas] = useState<Record<string, { atendido: boolean; comentario: string }>>({})
  const [apontamentos, setApontamentos] = useState<Partial<Record<SecaoDaStartup, string>>>({})
  const [parecer, setParecer] = useState('')
  const [motivo, setMotivo] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    detalheDaCuradoria(id).then((d) => {
      if (!ativo) return
      setDetalhe(d)
      apresentacaoCompleta(d.startupId).then((a) => { if (ativo) setApresentacao(a) }).catch(() => undefined)
    }).catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-84
   * Título: Agir
   * Objetivo: Rodar uma ação, mostrar o aviso e recarregar o detalhe.
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
  const r = detalhe.revisao
  const aberta = r.situacao === 'EM_ANALISE'
  const decisao = (resultado: DecisaoDaCuradoria['resultado']): DecisaoDaCuradoria => ({
    resultado,
    checklist: detalhe.checklist.map((c) => ({
      codigo: c.codigo, atendido: marcas[c.codigo]?.atendido ?? false, comentario: marcas[c.codigo]?.comentario || null,
    })),
    apontamentos: Object.entries(apontamentos).filter(([, t]) => t && t.trim())
      .map(([secao, comentario]) => ({ secao: secao as SecaoDaStartup, comentario: comentario ?? '' })),
    parecer,
  })

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <Link to="/admin/curadoria" className={`${CLASSE_LINK} text-sm`}>← Fila da curadoria</Link>
        <h1 className="font-display text-3xl font-semibold tracking-tight">{detalhe.startup}</h1>
        <p className="text-sm text-muted">
          {NOMES_DO_TIPO_DA_REVISAO[r.tipo]} · {NOMES_DA_SITUACAO_DA_REVISAO[r.situacao]} · prazo {FORMATO_DATA_HORA.format(new Date(detalhe.prazo))}
          {' · '}{detalhe.responsavel ? `com ${detalhe.responsavel}` : 'sem responsável'}
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 rounded-lg border border-line bg-surface p-4 text-sm">
        <dt className="text-muted">CNPJ</dt><dd>{detalhe.cnpj ?? 'Empresa ainda não constituída'}</dd>
        <dt className="text-muted">Selo da startup</dt><dd>{NOMES_DO_SELO[detalhe.seloDaStartup]}</dd>
        {detalhe.fundadores.map((f) => (
          <div key={(f.nome ?? '') + f.papel} className="contents">
            <dt className="text-muted">Fundador</dt><dd>{f.nome ?? '—'} · identidade {NOMES_DO_SELO[f.selo]}</dd>
          </div>
        ))}
      </dl>

      {detalhe.alteracoes.length > 0 && (
        <section aria-label="Alterações" className="flex flex-col gap-1 text-sm">
          <h2 className="font-semibold">Alterações em relação à versão aprovada</h2>
          <ul className="list-disc pl-5">
            {detalhe.alteracoes.map((a) => <li key={a.campo}>{a.campo}: {a.aprovado ?? '—'} → <span className="font-semibold">{a.novo ?? '—'}</span></li>)}
          </ul>
        </section>
      )}

      {apresentacao && <ApresentacaoEmPaginas apresentacao={apresentacao}
        carregarLogotipo={() => requisitar<Blob>(`/api/v1/admin/curadoria/startups/${detalhe.startupId}/logotipo`, { comoArquivo: true })} />}

      {aberta && (
        <div className="flex flex-wrap gap-3 text-sm">
          <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void agir(() => assumirRevisao(id), 'Revisão assumida.')}>Assumir</button>
          <input aria-label="Motivo do impedimento" placeholder="Motivo do impedimento" value={motivo} onChange={(e) => setMotivo(e.target.value)}
            className="rounded-md border border-line px-2 py-1" />
          <button type="button" className={CLASSE_LINK} disabled={!motivo.trim()}
            onClick={() => void agir(() => declararImpedimentoNaCuradoria(id, motivo), 'Impedimento registrado.')}>Declarar impedimento</button>
        </div>
      )}

      {aberta && (
        <form aria-label="Decisão" className="flex flex-col gap-4" onSubmit={(e) => e.preventDefault()}>
          <fieldset className="flex flex-col gap-2 rounded-lg border border-line p-3 text-sm">
            <legend className="px-1 font-semibold">Checklist</legend>
            {detalhe.checklist.map((c) => (
              <div key={c.codigo} className="flex flex-wrap items-center gap-2">
                <label className="flex items-center gap-2">
                  <input type="checkbox" checked={marcas[c.codigo]?.atendido ?? false}
                    onChange={(e) => setMarcas({ ...marcas, [c.codigo]: { comentario: marcas[c.codigo]?.comentario ?? '', atendido: e.target.checked } })} />
                  {c.rotulo}
                </label>
                <input aria-label={`Comentário: ${c.rotulo}`} placeholder="Comentário (opcional)" value={marcas[c.codigo]?.comentario ?? ''}
                  maxLength={1000} className="flex-1 rounded-md border border-line px-2 py-1"
                  onChange={(e) => setMarcas({ ...marcas, [c.codigo]: { atendido: marcas[c.codigo]?.atendido ?? false, comentario: e.target.value } })} />
              </div>
            ))}
          </fieldset>
          <fieldset className="flex flex-col gap-2 rounded-lg border border-line p-3 text-sm">
            <legend className="px-1 font-semibold">Comentários por seção (aparecem para o empreendedor na seção)</legend>
            {SECOES.map((s) => (
              <label key={s} className="flex flex-col gap-1">
                {NOMES_DAS_SECOES[s]}
                <textarea rows={1} maxLength={1000} value={apontamentos[s] ?? ''} className="rounded-md border border-line px-2 py-1"
                  onChange={(e) => setApontamentos({ ...apontamentos, [s]: e.target.value })} />
              </label>
            ))}
          </fieldset>
          <label className="flex flex-col gap-1 text-sm">
            Parecer
            <textarea rows={3} maxLength={2000} value={parecer} onChange={(e) => setParecer(e.target.value)} className="rounded-md border border-line px-2 py-1" />
          </label>
          <div className="flex flex-wrap gap-3 text-sm">
            <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void agir(() => decidirRevisao(id, decisao('APROVADA')), 'Aprovada.')}>Aprovar</button>
            <button type="button" className="rounded-md border border-warn px-3 py-1 font-semibold text-warn"
              onClick={() => void agir(() => decidirRevisao(id, decisao('DEVOLVIDA')), 'Devolvida para ajustes.')}>Devolver para ajustes</button>
            <button type="button" className="rounded-md bg-crit px-3 py-1 font-semibold text-white"
              onClick={() => void agir(() => decidirRevisao(id, decisao('RECUSADA')), 'Recusa enviada para a segunda aprovação.')}>Recusar por fraude</button>
          </div>
          <p className="text-xs text-muted">A recusa por fraude precisa do parecer e só vale depois da segunda aprovação.</p>
        </form>
      )}

      {r.aguardandoSegundaAprovacao && (
        <p className="text-sm">A recusa aguarda a <Link to="/admin/aprovacoes" className={CLASSE_LINK}>segunda aprovação</Link>.</p>
      )}
      {!aberta && r.parecer && <p className="text-sm"><span className="font-semibold">Parecer:</span> {r.parecer}</p>}
    </section>
  )
}
