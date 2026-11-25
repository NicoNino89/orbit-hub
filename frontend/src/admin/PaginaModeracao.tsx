import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  acessosAoNivel2, aplicarPreventiva, assumirCaso, casoDeModeracao, decidirCaso, filaDeModeracao, filaDeRecursos, julgarRecurso, NOMES_DA_DECISAO,
  NOMES_DA_ORIGEM, NOMES_DA_PRIORIDADE, NOMES_DA_SANCAO, registrarParecer, type AcessoConfidencial, type CasoDeModeracao, type DecisaoDaModeracao,
  type ItemDaFilaDeModeracao, type MedidaPreventiva, type Recurso,
} from '../api/moderacao'
import { opcoesDaLista } from '../api/organizacoes'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-100
 * Título: Mensagem de erro
 * Objetivo: Tirar o texto de um erro desconhecido.
 * Decisões: Texto padrão quando não há mensagem.
 */
function mensagemDe(e: unknown) {
  return e instanceof Error ? e.message : 'Não foi possível concluir.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-100
 * Título: Fila de moderação
 * Objetivo: Mostrar os casos em aberto, dos críticos aos normais, com prazo, denúncias e responsável.
 * Decisões: Prazo vencido em destaque.
 */
export function PaginaFilaDeModeracao() {
  const [fila, setFila] = useState<ItemDaFilaDeModeracao[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    filaDeModeracao().then(setFila).catch((e: unknown) => setErro(mensagemDe(e)))
  }, [])
  return (
    <section className="flex flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Moderação</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {fila?.length === 0 && <p className="text-muted">Nenhum caso em aberto.</p>}
      <ul className="flex flex-col gap-2">
        {fila?.map((c) => (
          <li key={c.id} className={`${CAIXA} ${c.vencido ? 'border-crit' : ''}`}>
            <Link to={`/admin/moderacao/${c.id}`} className={CLASSE_LINK}>{c.objeto}</Link>
            {' · '}<strong>{NOMES_DA_PRIORIDADE[c.prioridade] ?? c.prioridade}</strong>{' · '}{c.categoria}{' · '}{c.denuncias} denúncia(s)
            <span className="text-sm text-muted"> · prazo {FORMATO_DATA_HORA.format(new Date(c.prazo))}{c.vencido ? ' (vencido)' : ''}
              {c.responsavel ? ` · com ${c.responsavel}` : ''}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-100, KAN-101, KAN-102
 * Título: Caso de moderação
 * Objetivo: Mostrar as denúncias e o histórico do denunciado e deixar assumir, aplicar medida preventiva, levantar os
 *           acessos ao Nível 2, registrar o parecer jurídico e decidir.
 * Decisões: O jurídico só vê e dá o parecer; as ações de moderação aparecem para moderador e administrador.
 */
export function PaginaCasoDeModeracao() {
  const { id = '' } = useParams()
  const { estado } = useSessao()
  const papeis = estado.status === 'autenticado' ? estado.usuario.papeis : []
  const modera = papeis.includes('ADMIN') || papeis.includes('MODERADOR')
  const juridico = papeis.includes('ADMIN') || papeis.includes('JURIDICO')
  const [c, setC] = useState<CasoDeModeracao | null>(null)
  const [motivos, setMotivos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [preventiva, setPreventiva] = useState<{ tipo: MedidaPreventiva; motivo: string; alvoId: string }>({ tipo: 'SUSPENDER_CONTA', motivo: '', alvoId: '' })
  const [decisao, setDecisao] = useState<{ decisao: DecisaoDaModeracao; motivo: string; observacao: string; dias: string; alvoId: string }>(
    { decisao: 'ESCALA', motivo: '', observacao: '', dias: '7', alvoId: '' })
  const [justificativa, setJustificativa] = useState('')
  const [acessos, setAcessos] = useState<AcessoConfidencial[] | null>(null)
  const [parecer, setParecer] = useState('')
  useEffect(() => {
    casoDeModeracao(id).then((x) => { setC(x); setParecer(x.parecerJuridico ?? '') }).catch((e: unknown) => setErro(mensagemDe(e)))
    opcoesDaLista('MOTIVO_DECISAO_MODERACAO').then(setMotivos).catch(() => setMotivos([]))
  }, [id])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-100
   * Título: Agir no caso
   * Objetivo: Executar uma ação e atualizar o caso.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<CasoDeModeracao>) {
    setErro(null)
    try {
      setC(await acao())
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  if (!c) return erro ? <AlertaErro>{erro}</AlertaErro> : <p>Carregando…</p>
  const aberto = c.situacao !== 'DECIDIDO'
  return (
    <section className="flex flex-col gap-5">
      <Link to="/admin/moderacao" className={CLASSE_LINK}>← Fila</Link>
      <h1 className="font-display text-2xl font-semibold">{c.objeto}</h1>
      <p className="text-sm">{NOMES_DA_PRIORIDADE[c.prioridade]} · {c.categoria} · {c.situacao.replace('_', ' ').toLowerCase()}
        {c.responsavel ? ` · com ${c.responsavel}` : ''}{c.decisao ? ` · decisão: ${NOMES_DA_DECISAO[c.decisao as DecisaoDaModeracao] ?? c.decisao}` : ''}</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <section aria-label="Denúncias" className="flex flex-col gap-2">
        <h2 className="font-semibold">Denúncias</h2>
        {c.denuncias.map((d) => (
          <article key={d.id} className={CAIXA}>
            <p className="text-xs text-muted">{d.denunciante ?? 'Conta removida'} · {d.categoria} · {FORMATO_DATA_HORA.format(new Date(d.criadaEm))}
              {d.semAcaoAntes > 0 ? ` · ${d.semAcaoAntes} denúncia(s) anterior(es) sem ação` : ''}</p>
            <p className="whitespace-pre-line">{d.descricao}</p>
            {d.evidencias && <p className="text-sm">Evidências: {d.evidencias}</p>}
          </article>
        ))}
      </section>
      <section aria-label="Histórico do denunciado" className="flex flex-col gap-1 text-sm">
        <h2 className="font-semibold">Histórico do denunciado</h2>
        {c.historico.length === 0 && c.anteriores.length === 0 && <p className="text-muted">Sem sanções nem casos anteriores.</p>}
        {c.historico.map((s) => <p key={s.id}>{NOMES_DA_SANCAO[s.tipo] ?? s.tipo} · {FORMATO_DATA_HORA.format(new Date(s.inicio))} · {s.situacao.toLowerCase()}</p>)}
        {c.anteriores.map((a) => <p key={a.id}>Caso anterior: {a.categoria} · {a.decisao}</p>)}
        {c.medidas.map((s) => <p key={s.id}>Medida deste caso: {NOMES_DA_SANCAO[s.tipo] ?? s.tipo} · {s.situacao.toLowerCase()}</p>)}
      </section>
      {aberto && modera && (
        <button type="button" onClick={() => void agir(() => assumirCaso(id))} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Assumir</button>
      )}
      {aberto && modera && (
        <form aria-label="Medida preventiva" onSubmit={(e) => { e.preventDefault(); void agir(() => aplicarPreventiva(id, preventiva.tipo, preventiva.motivo, preventiva.alvoId)) }}
          className="flex flex-col gap-2">
          <h2 className="font-semibold">Medida preventiva</h2>
          <select aria-label="Tipo de medida" value={preventiva.tipo} onChange={(e) => setPreventiva({ ...preventiva, tipo: e.target.value as MedidaPreventiva })} className={CAIXA}>
            <option value="SUSPENDER_CONTA">Suspender a conta</option>
            <option value="DESPUBLICAR">Despublicar a startup</option>
            <option value="SUSPENDER_NIVEL2">Tirar os acessos ao Nível 2</option>
          </select>
          <Campo rotulo="Conta ou startup (identificador, se não for o denunciado)" value={preventiva.alvoId} onChange={(e) => setPreventiva({ ...preventiva, alvoId: e.target.value })} />
          <Campo rotulo="Motivo da medida" value={preventiva.motivo} onChange={(e) => setPreventiva({ ...preventiva, motivo: e.target.value })} />
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Aplicar medida</button>
        </form>
      )}
      {c.objetoTipo === 'STARTUP' && (
        <form aria-label="Acessos ao Nível 2" onSubmit={(e) => { e.preventDefault(); acessosAoNivel2(id, justificativa).then(setAcessos).catch((x: unknown) => setErro(mensagemDe(x))) }}
          className="flex flex-col gap-2">
          <h2 className="font-semibold">Quem acessou o Nível 2</h2>
          <Campo rotulo="Justificativa da consulta" value={justificativa} onChange={(e) => setJustificativa(e.target.value)} />
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Levantar acessos</button>
          {acessos && (acessos.length === 0 ? <p className="text-sm text-muted">Ninguém acessou.</p> : (
            <ul className="text-sm">
              {acessos.map((a) => <li key={a.pessoaId + a.liberadoEm}>{a.pessoa} · desde {FORMATO_DATA_HORA.format(new Date(a.liberadoEm))}
                {a.encerradoEm ? ` até ${FORMATO_DATA_HORA.format(new Date(a.encerradoEm))}` : ' (ativo)'} · {a.links} link(s) de documento</li>)}
            </ul>
          ))}
        </form>
      )}
      {juridico && aberto && (
        <form aria-label="Parecer jurídico" onSubmit={(e) => { e.preventDefault(); void agir(() => registrarParecer(id, parecer)) }} className="flex flex-col gap-2">
          <label className="flex flex-col gap-1 text-sm font-semibold">Parecer jurídico
            <textarea value={parecer} onChange={(e) => setParecer(e.target.value)} rows={3} className={CAIXA} />
          </label>
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Salvar parecer</button>
        </form>
      )}
      {!juridico && c.parecerJuridico && <p className={CAIXA}>Parecer jurídico: {c.parecerJuridico}</p>}
      {aberto && modera && (
        <form aria-label="Decidir o caso" onSubmit={(e) => {
          e.preventDefault()
          void agir(() => decidirCaso(id, { decisao: decisao.decisao, motivo: decisao.motivo, observacao: decisao.observacao || undefined,
            alvoId: decisao.alvoId || undefined, dias: decisao.decisao === 'SUSPENSAO' ? Number(decisao.dias) : undefined }))
        }} className="flex flex-col gap-2">
          <h2 className="font-semibold">Decisão</h2>
          <select aria-label="Decisão" value={decisao.decisao} onChange={(e) => setDecisao({ ...decisao, decisao: e.target.value as DecisaoDaModeracao })} className={CAIXA}>
            {(Object.keys(NOMES_DA_DECISAO) as DecisaoDaModeracao[]).map((d) => <option key={d} value={d}>{NOMES_DA_DECISAO[d]}</option>)}
          </select>
          {decisao.decisao === 'SUSPENSAO' && (
            <select aria-label="Duração" value={decisao.dias} onChange={(e) => setDecisao({ ...decisao, dias: e.target.value })} className={CAIXA}>
              {['7', '30', '90'].map((d) => <option key={d} value={d}>{d} dias</option>)}
            </select>
          )}
          <select aria-label="Motivo" value={decisao.motivo} onChange={(e) => setDecisao({ ...decisao, motivo: e.target.value })} className={CAIXA}>
            <option value="">Motivo…</option>
            {motivos.map((m) => <option key={m.codigo} value={m.codigo}>{m.rotulo}</option>)}
          </select>
          {!c.alvoPadrao && <Campo rotulo="Conta que recebe a decisão (identificador)" value={decisao.alvoId} onChange={(e) => setDecisao({ ...decisao, alvoId: e.target.value })} />}
          <Campo rotulo="Observação" value={decisao.observacao} onChange={(e) => setDecisao({ ...decisao, observacao: e.target.value })} />
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Decidir</button>
        </form>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86
 * Título: Fila de recursos
 * Objetivo: Mostrar os recursos que a pessoa pode julgar e deixar deferir ou indeferir com o motivo.
 * Decisões: Os recursos contra decisão da própria pessoa não aparecem.
 */
export function PaginaRecursos() {
  const [fila, setFila] = useState<Recurso[] | null>(null)
  const [motivos, setMotivos] = useState<Record<string, string>>({})
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    filaDeRecursos().then(setFila).catch((e: unknown) => setErro(mensagemDe(e)))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-86
   * Título: Julgar
   * Objetivo: Enviar o julgamento e recarregar a fila.
   * Decisões: Motivo obrigatório (a API confere).
   */
  async function julgar(r: Recurso, deferir: boolean) {
    setErro(null)
    try {
      await julgarRecurso(r.id, deferir, motivos[r.id] ?? '')
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Recursos</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {fila?.length === 0 && <p className="text-muted">Nenhum recurso para julgar.</p>}
      {fila?.map((r) => (
        <article key={r.id} className={`${CAIXA} flex flex-col gap-2`}>
          <p className="font-semibold">{NOMES_DA_ORIGEM[r.origemTipo]} · {r.autor} · prazo {FORMATO_DATA_HORA.format(new Date(r.prazo))}</p>
          <p className="whitespace-pre-line">{r.argumentos}</p>
          {r.anexos && <p className="text-sm">Anexos: {r.anexos}</p>}
          <Campo rotulo="Motivo do julgamento" value={motivos[r.id] ?? ''} onChange={(e) => setMotivos({ ...motivos, [r.id]: e.target.value })} />
          <div className="flex gap-3">
            <button type="button" onClick={() => void julgar(r, true)} className={CLASSE_BOTAO_PRIMARIO}>Aceitar recurso</button>
            <button type="button" onClick={() => void julgar(r, false)} className={CLASSE_LINK}>Negar recurso</button>
          </div>
        </article>
      ))}
    </section>
  )
}
