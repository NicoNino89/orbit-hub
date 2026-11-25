import { useCallback, useEffect, useState } from 'react'
import { FORMATO_DATA_HORA } from '../admin/datas'
import {
  atualizarIntencao, cancelarRodada, concluirRodada, convidarCoInvestidor, convidarLider, decidirConvidado, definirPolitica, entrarNaRodada,
  NOMES_DA_POLITICA, NOMES_DA_SITUACAO_DA_RODADA, painelDaRodada, type PainelDaRodada, pedirEncerramento, type Politica, prorrogarRodada,
  responderInvestimento, responderLideranca, rodadaParaInvestidor, type RodadaParaInvestidor,
} from '../api/rodadas'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const REAIS = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const SEM_COMPROMISSO = 'Rodada, intenções e liderança não representam compromisso de investimento.'
const real = (v: number | null) => (v === null ? '—' : REAIS.format(v))

async function tentar(acao: () => Promise<unknown>): Promise<string | null> {
  try {
    await acao()
    return null
  } catch (e) {
    return e instanceof Error ? e.message : 'Não foi possível concluir.'
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109
 * Título: Condução da rodada (startup)
 * Objetivo: Mostrar à equipe os participantes, o total das intenções frente aos valores, o excesso de demanda e o líder;
 *           e deixar o fundador administrador aprovar convidados, convidar o líder, mudar a política, prorrogar, cancelar
 *           e concluir registrando o resultado.
 * Decisões: Na conclusão, cada participante pode ser marcado como investidor, com valor opcional.
 */
export function ConducaoDaRodada({ rodadaId, admin }: { rodadaId: string; startupId: string; admin: boolean }) {
  const [p, setP] = useState<PainelDaRodada | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [prazo, setPrazo] = useState('')
  const [motivo, setMotivo] = useState('')
  const [captado, setCaptado] = useState('')
  const [instrumento, setInstrumento] = useState('')
  const [fechamento, setFechamento] = useState('')
  const [investiram, setInvestiram] = useState<Record<string, string>>({})
  const [versao, setVersao] = useState(0)
  const pronto = (f: string | null) => { setErro(f); setVersao((v) => v + 1) }
  useEffect(() => {
    let ativo = true
    painelDaRodada(rodadaId).then((x) => { if (ativo) setP(x) }).catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Erro.') })
    return () => { ativo = false }
  }, [rodadaId, versao])
  if (!p) return erro ? <AlertaErro>{erro}</AlertaErro> : null
  const emCurso = p.situacao === 'ABERTA' || p.situacao === 'EM_NEGOCIACAO' || p.situacao === 'AGUARDANDO_RESULTADO'

  return (
    <section aria-label="Condução da rodada" className="flex flex-col gap-3 text-sm">
      <p className="text-xs text-muted">{SEM_COMPROMISSO}</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <dl className="grid grid-cols-[auto_1fr] gap-x-3">
        <dt className="text-muted">Intenções</dt>
        <dd>
          {real(p.somaIntencoes)} · alvo {real(p.valorAlvo)} · mínimo {real(p.valorMinimo)} · máximo {real(p.valorMaximo)}
          {p.excessoDeDemanda && <strong className="ml-2 text-warn">Excesso de demanda</strong>}
        </dd>
        <dt className="text-muted">Líder</dt><dd>{p.lider ?? 'Rodada sem líder definido'}{p.liderConvidado && ` · convite pendente: ${p.liderConvidado}`}</dd>
        <dt className="text-muted">Prazo</dt><dd>{p.prazo ?? '—'} · {p.prorrogacoes} prorrogação(ões){p.encerramentoPedido && ' · o líder pediu o encerramento'}</dd>
        {p.valorCaptado !== null && <><dt className="text-muted">Captado</dt><dd>{real(p.valorCaptado)} · {p.instrumentoUtilizado} · {p.dataFechamento}</dd></>}
      </dl>
      <label className="flex items-center gap-2">
        Política de alocação
        <select value={p.politica} disabled={!admin} onChange={(e) => void tentar(() => definirPolitica(rodadaId, e.target.value as Politica)).then(pronto)}
          className="rounded-md border border-line px-2 py-1">
          {(Object.keys(NOMES_DA_POLITICA) as Politica[]).map((x) => <option key={x} value={x}>{NOMES_DA_POLITICA[x]}</option>)}
        </select>
      </label>
      <ul aria-label="Participantes da rodada" className="flex flex-col gap-1">
        {p.participantes.map((x) => (
          <li key={x.id} className="flex flex-wrap items-center gap-2 rounded-md border border-line px-3 py-2">
            <span className="flex-1">{x.investidor} · {x.situacao === 'AGUARDANDO_APROVACAO' ? 'convidado pelo líder' : real(x.intencao)}{x.condicoes && ` · ${x.condicoes}`}</span>
            {admin && x.situacao === 'AGUARDANDO_APROVACAO' && (
              <>
                <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => decidirConvidado(rodadaId, x.id, true)).then(pronto)}>Aprovar</button>
                <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => decidirConvidado(rodadaId, x.id, false)).then(pronto)}>Recusar</button>
              </>
            )}
            {admin && emCurso && x.situacao === 'ATIVO' && (
              <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => convidarLider(rodadaId, x.id)).then(pronto)}>Convidar para liderar</button>
            )}
            {admin && emCurso && x.situacao === 'ATIVO' && (
              <label className="flex items-center gap-1 text-xs">
                <input type="checkbox" aria-label={`Investiu: ${x.investidor}`} checked={x.id in investiram}
                  onChange={(e) => { const n = { ...investiram }; if (e.target.checked) n[x.id] = ''; else delete n[x.id]; setInvestiram(n) }} />
                investiu
                {x.id in investiram && <input type="number" aria-label={`Valor de ${x.investidor}`} value={investiram[x.id]} placeholder="valor"
                  onChange={(e) => setInvestiram({ ...investiram, [x.id]: e.target.value })} className="w-24 rounded-md border border-line px-1" />}
              </label>
            )}
          </li>
        ))}
      </ul>
      {p.investimentos.length > 0 && (
        <ul aria-label="Investimentos registrados" className="text-xs text-muted">
          {p.investimentos.map((i) => <li key={i.id}>{i.investidor} · {real(i.valor)} · {i.situacao.toLowerCase().replace('_', ' ')}</li>)}
        </ul>
      )}
      {admin && emCurso && (
        <div className="flex flex-col gap-3 rounded-lg border border-line p-3">
          <div className="flex flex-wrap items-end gap-2">
            <Campo rotulo="Novo prazo" type="date" value={prazo} onChange={(e) => setPrazo(e.target.value)} />
            <button type="button" className={CLASSE_LINK} disabled={!prazo} onClick={() => void tentar(() => prorrogarRodada(rodadaId, prazo)).then(pronto)}>Prorrogar</button>
          </div>
          <form aria-label="Concluir rodada" className="flex flex-wrap items-end gap-2" onSubmit={(e) => {
            e.preventDefault()
            void tentar(() => concluirRodada(rodadaId, { valorCaptado: Number(captado), instrumento, dataFechamento: fechamento,
              investidores: Object.entries(investiram).map(([participanteId, v]) => ({ participanteId, valor: v ? Number(v) : null })) })).then(pronto)
          }}>
            <Campo rotulo="Valor captado (R$)" type="number" value={captado} onChange={(e) => setCaptado(e.target.value)} />
            <Campo rotulo="Instrumento utilizado" value={instrumento} onChange={(e) => setInstrumento(e.target.value)} />
            <Campo rotulo="Data do fechamento" type="date" value={fechamento} onChange={(e) => setFechamento(e.target.value)} />
            <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Concluir e registrar o resultado</button>
          </form>
          <div className="flex flex-wrap items-end gap-2">
            <Campo rotulo="Motivo do cancelamento" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
            <button type="button" className={CLASSE_LINK} disabled={!motivo.trim()} onClick={() => void tentar(() => cancelarRodada(rodadaId, motivo)).then(pronto)}>
              Cancelar a rodada
            </button>
          </div>
        </div>
      )}
      <p className="text-xs text-muted">Situação: {NOMES_DA_SITUACAO_DA_RODADA[p.situacao]}</p>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109
 * Título: Rodada para o investidor
 * Objetivo: Na página da startup, mostrar a rodada aberta: líder e política, entrar com intenção, atualizar, aceitar a
 *           liderança; ao líder, os participantes, a soma, convidar co-investidor e pedir o encerramento; e confirmar
 *           ou contestar o investimento registrado.
 * Decisões: Sem rodada aberta, nada aparece.
 */
export function RodadaDoInvestidor({ startupId }: { startupId: string }) {
  const [r, setR] = useState<RodadaParaInvestidor | null>(null)
  const [intencao, setIntencao] = useState('')
  const [condicoes, setCondicoes] = useState('')
  const [email, setEmail] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const ler = useCallback(() => { rodadaParaInvestidor(startupId).then(setR).catch(() => setR(null)) }, [startupId])
  useEffect(() => { ler() }, [ler])
  if (!r) return null
  const pronto = (f: string | null) => { setErro(f); ler() }
  const valor = intencao ? Number(intencao) : null

  return (
    <section aria-label="Rodada" className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-4 text-sm">
      <h2 className="font-semibold">Rodada · {NOMES_DA_SITUACAO_DA_RODADA[r.situacao]}</h2>
      <p className="text-xs text-muted">{SEM_COMPROMISSO}</p>
      <p>Líder: {r.lider ?? 'rodada sem líder definido'} · Alocação: {NOMES_DA_POLITICA[r.politica]}</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {r.convidadoParaLiderar && (
        <div className="flex gap-3">
          <span>A startup convidou você para liderar.</span>
          <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => responderLideranca(r.id, true)).then(pronto)}>Aceitar liderança</button>
          <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => responderLideranca(r.id, false)).then(pronto)}>Recusar</button>
        </div>
      )}
      {(r.podeEntrar || r.minhaParticipacao) && (
        <form aria-label="Minha intenção" className="flex flex-wrap items-end gap-2" onSubmit={(e) => {
          e.preventDefault()
          void tentar(() => (r.minhaParticipacao ? atualizarIntencao(r.id, valor, condicoes) : entrarNaRodada(r.id, valor, condicoes))).then(pronto)
        }}>
          {r.minhaParticipacao && <span>Sua intenção: {real(r.minhaParticipacao.intencao)}</span>}
          <Campo rotulo="Intenção (R$)" type="number" value={intencao} onChange={(e) => setIntencao(e.target.value)} />
          <Campo rotulo="Condições preliminares" value={condicoes} onChange={(e) => setCondicoes(e.target.value)} />
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>{r.minhaParticipacao ? 'Atualizar intenção' : 'Entrar na rodada'}</button>
        </form>
      )}
      {r.souLider && (
        <div className="flex flex-col gap-2 rounded-md border border-line p-2">
          <p>Soma das intenções: {real(r.somaIntencoes)}</p>
          <ul aria-label="Participantes (líder)" className="text-xs">{r.participantes.map((x) => <li key={x.id}>{x.investidor}</li>)}</ul>
          <form aria-label="Convidar co-investidor" className="flex flex-wrap items-end gap-2"
            onSubmit={(e) => { e.preventDefault(); void tentar(() => convidarCoInvestidor(r.id, email)).then((f) => { pronto(f); if (!f) setEmail('') }) }}>
            <Campo rotulo="E-mail do co-investidor" value={email} onChange={(e) => setEmail(e.target.value)} />
            <button type="submit" className={CLASSE_LINK} disabled={!email.trim()}>Convidar</button>
          </form>
          <button type="button" className={`${CLASSE_LINK} self-start`} onClick={() => void tentar(() => pedirEncerramento(r.id)).then(pronto)}>
            Pedir o encerramento da rodada
          </button>
        </div>
      )}
      {r.meusInvestimentos.map((i) => (
        <div key={i.id} className="flex flex-wrap items-center gap-3">
          <span>Investimento registrado: {real(i.valor)} · {i.situacao.toLowerCase().replace('_', ' ')} · {FORMATO_DATA_HORA.format(new Date(i.criadoEm))}</span>
          {i.situacao === 'PENDENTE' && (
            <>
              <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => responderInvestimento(i.id, true)).then(pronto)}>Confirmar</button>
              <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => responderInvestimento(i.id, false)).then(pronto)}>Contestar</button>
            </>
          )}
        </div>
      ))}
    </section>
  )
}
