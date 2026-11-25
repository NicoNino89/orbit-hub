import { useCallback, useEffect, useState } from 'react'
import { opcoesDaLista } from '../api/organizacoes'
import {
  acaoNaRodada, criarRodada, editarRodada, encerrarRodada, NOMES_DA_SITUACAO_DA_RODADA, type ResultadoDaRodada, type Rodada,
  rodadasDaStartup, type TermoDoCatalogo, termosDasRodadas, termosDoInstrumento, type TermosDaRodada,
} from '../api/rodadas'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

type Opcao = { codigo: string; rotulo: string }
const VAZIA: TermosDaRodada = { estagio: null, instrumento: null, valorAlvo: null, valorMinimo: null, valorMaximo: null, prazo: null, especificos: {} }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Painel da rodada
 * Objetivo: Mostrar a rodada em andamento com os termos, deixar o fundador administrador criar, editar, abrir, pôr em
 *           negociação e encerrar, e listar as rodadas anteriores.
 * Decisões: Sem CNPJ, explica que não há rodada. Avisa que valuation e termos específicos são Nível 2 e que a rodada não
 *           é compromisso de investimento. Participantes e líder chegam com o Match (S7): até lá, "sem líder definido".
 */
export function PainelDeRodada({ startupId, admin, constituida }: { startupId: string; admin: boolean; constituida: boolean }) {
  const [rodadas, setRodadas] = useState<Rodada[] | null>(null)
  const [catalogo, setCatalogo] = useState<TermoDoCatalogo[]>([])
  const [estagios, setEstagios] = useState<Opcao[]>([])
  const [instrumentos, setInstrumentos] = useState<Opcao[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    rodadasDaStartup(startupId).then((r) => { if (ativo) setRodadas(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [startupId, versao])

  useEffect(() => {
    termosDasRodadas().then(setCatalogo).catch(() => setCatalogo([]))
    opcoesDaLista('ESTAGIO_RODADA').then(setEstagios).catch(() => setEstagios([]))
    opcoesDaLista('INSTRUMENTO').then(setInstrumentos).catch(() => setInstrumentos([]))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:51
   * Cartão Jira: KAN-59
   * Título: Agir
   * Objetivo: Rodar uma ação, mostrar o aviso e recarregar as rodadas.
   * Decisões: Erro do servidor vai para o alerta.
   */
  const agir = useCallback(async (acao: () => Promise<unknown>, sucesso: string) => {
    try {
      await acao()
      setErro(null)
      setAviso(sucesso)
      setVersao((v) => v + 1)
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }, [])

  if (!constituida) {
    return <p className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">Cadastre o CNPJ da startup nos dados cadastrais para abrir uma rodada. Sem pessoa jurídica, nenhum instrumento de investimento funciona.</p>
  }
  if (!rodadas) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando rodadas…</p>
  const atual = rodadas.find((r) => r.situacao !== 'ENCERRADA') ?? null
  const anteriores = rodadas.filter((r) => r.situacao === 'ENCERRADA')
  const nome = (lista: Opcao[], codigo: string | null) => lista.find((o) => o.codigo === codigo)?.rotulo ?? codigo ?? '—'

  return (
    <section aria-label="Rodada de investimento" className="flex flex-col gap-4">
      <p className="rounded-md bg-surface-2 px-3 py-2 text-sm">
        A rodada é a fonte única dos termos de captação. Valuation e termos do instrumento são Nível 2, e os dados gerais só aparecem
        para investidores verificados. Rodada e intenções não representam compromisso de investimento.
      </p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      {atual ? (
        <div className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4">
          <p className="flex flex-wrap items-center gap-2 text-sm">
            <span className="rounded bg-surface-2 px-2 py-0.5 text-xs font-semibold">{NOMES_DA_SITUACAO_DA_RODADA[atual.situacao]}</span>
            {atual.situacao !== 'RASCUNHO' && atual.semLider && <span className="text-muted">Rodada sem líder definido</span>}
          </p>
          {admin && atual.situacao !== 'EM_NEGOCIACAO' ? (
            <FormularioDaRodada key={atual.atualizadaEm} inicial={atual} catalogo={catalogo} estagios={estagios} instrumentos={instrumentos}
              rotulo="Salvar termos" aoSalvar={(t) => agir(() => editarRodada(atual.id, t), 'Termos salvos.')} />
          ) : (
            <ResumoDaRodada rodada={atual} catalogo={catalogo} estagio={nome(estagios, atual.estagio)} instrumento={nome(instrumentos, atual.instrumento)} />
          )}
          {admin && <AcoesDaRodada key={atual.situacao} rodada={atual} agir={agir} />}
        </div>
      ) : admin ? (
        <div className="flex flex-col gap-2">
          <h2 className="font-semibold">Nova rodada</h2>
          <FormularioDaRodada inicial={VAZIA} catalogo={catalogo} estagios={estagios} instrumentos={instrumentos}
            rotulo="Criar rascunho da rodada" aoSalvar={(t) => agir(() => criarRodada(startupId, t), 'Rascunho da rodada criado.')} />
        </div>
      ) : <p className="text-sm text-muted">Sem rodada em andamento.</p>}

      {anteriores.length > 0 && (
        <section aria-label="Rodadas anteriores" className="flex flex-col gap-1 text-sm">
          <h2 className="font-semibold">Rodadas anteriores</h2>
          <ul className="flex flex-col gap-1 text-muted">
            {anteriores.map((r) => (
              <li key={r.id}>{nome(estagios, r.estagio)} · {nome(instrumentos, r.instrumento)} · {r.resultado === 'CONCLUIDA' ? 'Concluída' : 'Cancelada'}</li>
            ))}
          </ul>
        </section>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Formulário da rodada
 * Objetivo: Editar os termos comuns e os específicos do instrumento escolhido.
 * Decisões: Os termos específicos mudam com o instrumento; os de outro instrumento são descartados ao trocar.
 */
function FormularioDaRodada({ inicial, catalogo, estagios, instrumentos, rotulo, aoSalvar }: {
  inicial: TermosDaRodada
  catalogo: TermoDoCatalogo[]
  estagios: Opcao[]
  instrumentos: Opcao[]
  rotulo: string
  aoSalvar: (t: TermosDaRodada) => Promise<void>
}) {
  const [t, setT] = useState<TermosDaRodada>({ ...inicial, especificos: { ...inicial.especificos } })
  const termos = termosDoInstrumento(catalogo, t.instrumento)
  const numero = (v: string) => (v.trim() === '' ? null : Number(v.replace(',', '.')))
  return (
    <form aria-label="Termos da rodada" className="flex flex-col gap-3"
      onSubmit={(e) => {
        e.preventDefault()
        const especificos = Object.fromEntries(termos.map((x) => [x.termo, t.especificos[x.termo] ?? '']).filter(([, v]) => v !== ''))
        void aoSalvar({ ...t, especificos })
      }}>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-semibold">Estágio da rodada</span>
          <select value={t.estagio ?? ''} onChange={(e) => setT({ ...t, estagio: e.target.value || null })} className="rounded-md border border-line bg-surface px-3 py-2">
            <option value="">Escolha</option>
            {estagios.map((o) => <option key={o.codigo} value={o.codigo}>{o.rotulo}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-semibold">Instrumento</span>
          <select value={t.instrumento ?? ''} onChange={(e) => setT({ ...t, instrumento: e.target.value || null, especificos: {} })}
            className="rounded-md border border-line bg-surface px-3 py-2">
            <option value="">Escolha</option>
            {instrumentos.map((o) => <option key={o.codigo} value={o.codigo}>{o.rotulo}</option>)}
          </select>
        </label>
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
        <Campo rotulo="Valor alvo (R$)" inputMode="decimal" value={t.valorAlvo ?? ''} onChange={(e) => setT({ ...t, valorAlvo: numero(e.target.value) })} />
        <Campo rotulo="Valor mínimo (R$)" inputMode="decimal" value={t.valorMinimo ?? ''} onChange={(e) => setT({ ...t, valorMinimo: numero(e.target.value) })} />
        <Campo rotulo="Valor máximo (R$)" inputMode="decimal" value={t.valorMaximo ?? ''} onChange={(e) => setT({ ...t, valorMaximo: numero(e.target.value) })} />
      </div>
      <Campo rotulo="Prazo da captação" type="date" value={t.prazo ?? ''} onChange={(e) => setT({ ...t, prazo: e.target.value || null })} />
      {termos.length > 0 && (
        <fieldset className="flex flex-col gap-3 rounded-md border border-line p-3">
          <legend className="px-1 text-sm font-semibold">Termos do instrumento (Nível 2)</legend>
          {termos.map((x) => (
            <Campo key={x.termo} rotulo={x.rotulo} value={t.especificos[x.termo] ?? ''} inputMode={x.tipo === 'TEXTO' ? undefined : 'decimal'}
              onChange={(e) => setT({ ...t, especificos: { ...t.especificos, [x.termo]: e.target.value } })} />
          ))}
        </fieldset>
      )}
      <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>{rotulo}</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Resumo da rodada
 * Objetivo: Mostrar os termos da rodada para quem só acompanha (ou durante a negociação).
 * Decisões: Valores em reais.
 */
function ResumoDaRodada({ rodada, catalogo, estagio, instrumento }: { rodada: Rodada; catalogo: TermoDoCatalogo[]; estagio: string; instrumento: string }) {
  const reais = (v: number | null) => (v == null ? '—' : v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }))
  return (
    <dl className="grid gap-x-4 gap-y-1 text-sm sm:grid-cols-2">
      <dt className="font-semibold">Estágio</dt><dd>{estagio}</dd>
      <dt className="font-semibold">Instrumento</dt><dd>{instrumento}</dd>
      <dt className="font-semibold">Alvo, mínimo e máximo</dt><dd>{reais(rodada.valorAlvo)} · {reais(rodada.valorMinimo)} · {reais(rodada.valorMaximo)}</dd>
      <dt className="font-semibold">Prazo</dt><dd>{rodada.prazo ?? '—'}</dd>
      {termosDoInstrumento(catalogo, rodada.instrumento).map((x) => (
        <div key={x.termo} className="contents"><dt className="font-semibold">{x.rotulo}</dt><dd>{rodada.especificos[x.termo] ?? '—'}</dd></div>
      ))}
    </dl>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Ações da rodada
 * Objetivo: Abrir, marcar em negociação e encerrar a rodada (concluída ou cancelada, com motivo).
 * Decisões: Encerrar pede confirmação; rascunho só pode ser cancelado.
 */
function AcoesDaRodada({ rodada, agir }: { rodada: Rodada; agir: (acao: () => Promise<unknown>, sucesso: string) => Promise<void> }) {
  const [encerrando, setEncerrando] = useState(false)
  const [resultado, setResultado] = useState<ResultadoDaRodada>(rodada.situacao === 'RASCUNHO' ? 'CANCELADA' : 'CONCLUIDA')
  const [motivo, setMotivo] = useState('')
  return (
    <div className="flex flex-col gap-2 border-t border-line pt-3 text-sm">
      <div className="flex flex-wrap gap-3">
        {rodada.situacao === 'RASCUNHO' && (
          <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void agir(() => acaoNaRodada(rodada.id, 'abertura'), 'Rodada aberta.')}>Abrir rodada</button>
        )}
        {rodada.situacao === 'ABERTA' && (
          <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => acaoNaRodada(rodada.id, 'negociacao'), 'Rodada em negociação.')}>Marcar em negociação</button>
        )}
        {!encerrando && <button type="button" className={CLASSE_LINK} onClick={() => setEncerrando(true)}>Encerrar rodada</button>}
      </div>
      {encerrando && (
        <div role="group" aria-label="Encerrar rodada" className="flex flex-col gap-2 rounded-md border border-line p-3">
          <label className="flex flex-col gap-1">
            <span className="font-semibold">Resultado</span>
            <select value={resultado} onChange={(e) => setResultado(e.target.value as ResultadoDaRodada)} className="self-start rounded-md border border-line bg-surface px-3 py-2">
              {rodada.situacao !== 'RASCUNHO' && <option value="CONCLUIDA">Concluída</option>}
              <option value="CANCELADA">Cancelada</option>
            </select>
          </label>
          <Campo rotulo="Motivo (opcional)" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={250} />
          <div className="flex gap-3">
            <button type="button" className="rounded-md bg-crit px-3 py-1 font-semibold text-white"
              onClick={() => void agir(() => encerrarRodada(rodada.id, resultado, motivo), 'Rodada encerrada.')}>Confirmar encerramento</button>
            <button type="button" className={CLASSE_LINK} onClick={() => setEncerrando(false)}>Cancelar</button>
          </div>
        </div>
      )}
    </div>
  )
}
