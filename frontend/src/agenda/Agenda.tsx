import { useEffect, useState } from 'react'
import {
  aceitarHorario, anotarReuniao, cancelarReuniao, contrapropor, conviteDaReuniao, type DadosDaProposta, type FormatoDeReuniao, minhasReunioes,
  NOMES_DA_SITUACAO_DA_REUNIAO, NOMES_DO_PROXIMO_PASSO, NOMES_DO_RESULTADO, NOMES_DO_TIPO, type ProximoPasso, proporReuniao, recusarReuniao,
  registrarReuniao, remarcarReuniao, responderPresenca, type ResultadoDaReuniao, type Reuniao, type TipoDeReuniao,
} from '../api/agenda'
import { opcoesDaLista } from '../api/organizacoes'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const DATA_HORA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const quando = (iso: string) => DATA_HORA.format(new Date(iso))
const paraIso = (local: string) => new Date(local).toISOString()

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
 * Cartão Jira: KAN-94, KAN-95
 * Título: Até três horários
 * Objetivo: Escolher de 1 a 3 horários, no fuso do navegador.
 * Decisões: Campos datetime-local; vazios são ignorados.
 */
function Horarios({ valor, aoMudar }: { valor: string[]; aoMudar: (v: string[]) => void }) {
  return (
    <div className="flex flex-wrap gap-2">
      {[0, 1, 2].map((i) => (
        <Campo key={i} rotulo={`Horário ${i + 1}`} type="datetime-local" value={valor[i] ?? ''}
          onChange={(e) => { const n = [...valor]; n[i] = e.target.value; aoMudar(n) }} />
      ))}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-94
 * Título: Propor reunião
 * Objetivo: Propor uma reunião na conexão: tipo, formato, link ou endereço, pauta, duração e até 3 horários.
 * Decisões: Fechado num botão para não poluir o painel.
 */
export function ProporReuniao({ conexaoId, aoPropor }: { conexaoId: string; aoPropor: (erro: string | null) => void }) {
  const [aberto, setAberto] = useState(false)
  const [d, setD] = useState<Omit<DadosDaProposta, 'opcoes' | 'membros'>>({ tipo: 'APRESENTACAO', formato: 'VIDEO', local: '', pauta: '', duracaoMinutos: 45 })
  const [horarios, setHorarios] = useState<string[]>([])
  if (!aberto) return <button type="button" className={CLASSE_LINK} onClick={() => setAberto(true)}>Propor reunião</button>
  return (
    <form aria-label="Propor reunião" className="flex flex-col gap-2 rounded-md border border-line p-3" onSubmit={(e) => {
      e.preventDefault()
      void tentar(() => proporReuniao(conexaoId, { ...d, opcoes: horarios.filter(Boolean).map(paraIso), membros: [] })).then((f) => {
        aoPropor(f)
        if (!f) setAberto(false)
      })
    }}>
      <div className="flex flex-wrap gap-2">
        <label className="flex flex-col gap-1">Tipo
          <select value={d.tipo} onChange={(e) => setD({ ...d, tipo: e.target.value as TipoDeReuniao })} className="rounded-md border border-line px-2 py-1">
            {(['APRESENTACAO', 'DUE_DILIGENCE', 'NEGOCIACAO'] as TipoDeReuniao[]).map((t) => <option key={t} value={t}>{NOMES_DO_TIPO[t]}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1">Formato
          <select value={d.formato} onChange={(e) => setD({ ...d, formato: e.target.value as FormatoDeReuniao })} className="rounded-md border border-line px-2 py-1">
            <option value="VIDEO">Vídeo</option><option value="PRESENCIAL">Presencial</option>
          </select>
        </label>
        <Campo rotulo={d.formato === 'VIDEO' ? 'Link da reunião' : 'Endereço'} value={d.local} onChange={(e) => setD({ ...d, local: e.target.value })} />
        <Campo rotulo="Duração (minutos)" type="number" value={String(d.duracaoMinutos)} onChange={(e) => setD({ ...d, duracaoMinutos: Number(e.target.value) })} />
      </div>
      <Campo rotulo="Pauta" value={d.pauta} maxLength={1000} onChange={(e) => setD({ ...d, pauta: e.target.value })} />
      <Horarios valor={horarios} aoMudar={setHorarios} />
      <div className="flex gap-3">
        <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar proposta</button>
        <button type="button" className={CLASSE_LINK} onClick={() => setAberto(false)}>Cancelar</button>
      </div>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-95, KAN-96, KAN-97, KAN-98
 * Título: Reunião na agenda
 * Objetivo: Mostrar a reunião e as ações possíveis agora: aceitar um horário, contrapropor, recusar, baixar o convite,
 *           remarcar, cancelar, responder presença, registrar o resultado e guardar a anotação privada.
 * Decisões: A mudança da conexão pelo próximo passo pede confirmação (caixa marcada).
 */
function CartaoDaReuniao({ r, motivos, aoMudar }: { r: Reuniao; motivos: { codigo: string; rotulo: string }[]; aoMudar: (e: string | null) => void }) {
  const [horarios, setHorarios] = useState<string[]>([])
  const [motivo, setMotivo] = useState('')
  const [texto, setTexto] = useState(r.minhaAnotacao ?? '')
  const [resultado, setResultado] = useState<ResultadoDaReuniao>('REALIZADA')
  const [passo, setPasso] = useState<ProximoPasso | ''>('')
  const [aplicar, setAplicar] = useState(false)
  const passou = r.inicio !== null && new Date(r.inicio).getTime() < Date.now()
  const agir = (acao: () => Promise<unknown>) => void tentar(acao).then(aoMudar)
  return (
    <li className={`flex flex-col gap-2 rounded-lg border bg-surface p-4 text-sm ${r.aguardaVoce ? 'border-primary' : 'border-line'}`}>
      <span className="flex flex-wrap justify-between gap-2">
        <strong>{NOMES_DO_TIPO[r.tipo]} · {r.startup}</strong>
        <span className="text-xs text-muted">{NOMES_DA_SITUACAO_DA_REUNIAO[r.situacao] ?? r.situacao}{r.tardia && ' · tardio'}{r.aguardaVoce && ' · aguarda você'}</span>
      </span>
      <span>{r.inicio ? quando(r.inicio) : 'Horário a combinar'} · {r.duracaoMinutos} min · {r.formato === 'VIDEO' ? 'vídeo' : 'presencial'} · {r.local}</span>
      <span className="text-muted">{r.pauta}</span>
      <span className="text-xs text-muted">Participantes: {r.participantes.map((p) => p.nome + (r.coletiva ? ` (${p.resposta.toLowerCase()})` : '')).join(', ')}</span>
      {r.motivo && <span className="text-xs">Motivo: {r.motivo}</span>}

      {r.situacao === 'PROPOSTA' && r.aguardaVoce && (
        <div className="flex flex-col gap-2">
          <div className="flex flex-wrap gap-2">
            {r.opcoes.map((o) => <button key={o.ordem} type="button" className={CLASSE_LINK} onClick={() => agir(() => aceitarHorario(r.id, o.ordem))}>Aceitar {quando(o.inicio)}</button>)}
          </div>
          <Horarios valor={horarios} aoMudar={setHorarios} />
          <button type="button" className={`${CLASSE_LINK} self-start`} disabled={!horarios.some(Boolean)}
            onClick={() => agir(() => contrapropor(r.id, horarios.filter(Boolean).map(paraIso)))}>Contrapropor estes horários</button>
          <span className="flex flex-wrap items-center gap-2">
            <select aria-label="Motivo da recusa" value={motivo} onChange={(e) => setMotivo(e.target.value)} className="rounded-md border border-line px-2 py-1">
              <option value="">Motivo…</option>
              {motivos.map((m) => <option key={m.codigo} value={m.codigo}>{m.rotulo}</option>)}
            </select>
            <button type="button" className={CLASSE_LINK} disabled={!motivo} onClick={() => agir(() => recusarReuniao(r.id, motivo))}>Recusar</button>
          </span>
        </div>
      )}
      {r.situacao === 'CONFIRMADA' && (
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className={CLASSE_LINK} onClick={() => void conviteDaReuniao(r.id).then((b) => {
            const u = URL.createObjectURL(b); const a = document.createElement('a'); a.href = u; a.download = 'reuniao.ics'; a.click(); URL.revokeObjectURL(u)
          }).catch(() => undefined)}>Baixar convite (.ics)</button>
          {r.coletiva && (['CONFIRMADO', 'TALVEZ', 'RECUSADO'] as const).map((x) => (
            <button key={x} type="button" className={CLASSE_LINK} onClick={() => agir(() => responderPresenca(r.id, x))}>{x === 'CONFIRMADO' ? 'Vou' : x === 'TALVEZ' ? 'Talvez' : 'Não vou'}</button>
          ))}
        </div>
      )}
      {(r.situacao === 'PROPOSTA' || r.situacao === 'CONFIRMADA') && !passou && (
        <div className="flex flex-wrap items-end gap-2">
          <Campo rotulo="Motivo (cancelar)" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
          <button type="button" className={CLASSE_LINK} disabled={!motivo.trim()} onClick={() => agir(() => cancelarReuniao(r.id, motivo))}>Cancelar reunião</button>
          {r.situacao === 'CONFIRMADA' && !r.coletiva && (
            <button type="button" className={CLASSE_LINK} disabled={!horarios.some(Boolean)}
              onClick={() => agir(() => remarcarReuniao(r.id, horarios.filter(Boolean).map(paraIso)))}>Remarcar para os horários acima</button>
          )}
        </div>
      )}
      {r.situacao === 'CONFIRMADA' && !r.coletiva && !passou && <Horarios valor={horarios} aoMudar={setHorarios} />}
      {r.situacao === 'CONFIRMADA' && passou && (
        <form aria-label="Registrar reunião" className="flex flex-wrap items-end gap-2" onSubmit={(e) => {
          e.preventDefault()
          agir(() => registrarReuniao(r.id, { resultado, proximoPasso: passo || null, aplicarNaConexao: aplicar, resumo: null }))
        }}>
          <label className="flex flex-col gap-1">Como foi
            <select value={resultado} onChange={(e) => setResultado(e.target.value as ResultadoDaReuniao)} className="rounded-md border border-line px-2 py-1">
              {(Object.keys(NOMES_DO_RESULTADO) as ResultadoDaReuniao[]).map((x) => <option key={x} value={x}>{NOMES_DO_RESULTADO[x]}</option>)}
            </select>
          </label>
          <label className="flex flex-col gap-1">Próximo passo
            <select value={passo} onChange={(e) => setPasso(e.target.value as ProximoPasso | '')} className="rounded-md border border-line px-2 py-1">
              <option value="">—</option>
              {(Object.keys(NOMES_DO_PROXIMO_PASSO) as ProximoPasso[]).map((x) => <option key={x} value={x}>{NOMES_DO_PROXIMO_PASSO[x]}</option>)}
            </select>
          </label>
          {passo && passo !== 'NOVA_REUNIAO' && (
            <label className="flex items-center gap-1"><input type="checkbox" checked={aplicar} onChange={(e) => setAplicar(e.target.checked)} />Confirmo a mudança na conexão</label>
          )}
          <button type="submit" className={CLASSE_LINK}>Registrar</button>
        </form>
      )}
      {r.registros.length > 0 && <span className="text-xs text-muted">Registros: {r.registros.map((g) => NOMES_DO_RESULTADO[g.resultado]).join(' · ')}</span>}
      <div className="flex flex-wrap items-end gap-2">
        <Campo rotulo="Minha anotação (privada)" value={texto} onChange={(e) => setTexto(e.target.value)} />
        <button type="button" className={CLASSE_LINK} disabled={!texto.trim()} onClick={() => agir(() => anotarReuniao(r.id, texto, false))}>Salvar anotação</button>
      </div>
    </li>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-95
 * Título: Agenda
 * Objetivo: Listar as reuniões futuras e passadas, por conexão, com as ações de cada uma.
 * Decisões: Futuras primeiro; horários no fuso do navegador.
 */
export function PaginaAgenda() {
  const [reunioes, setReunioes] = useState<Reuniao[] | null>(null)
  const [motivos, setMotivos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    let ativo = true
    minhasReunioes().then((r) => { if (ativo) setReunioes(r) }).catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Erro.') })
    return () => { ativo = false }
  }, [versao])
  useEffect(() => { opcoesDaLista('MOTIVO_RECUSA_REUNIAO').then(setMotivos).catch(() => undefined) }, [])
  const agora = Date.now()
  const futura = (r: Reuniao) => r.situacao === 'PROPOSTA' || (r.inicio !== null && new Date(r.inicio).getTime() >= agora)
  const grupos: [string, Reuniao[]][] = [['Próximas', (reunioes ?? []).filter(futura)], ['Anteriores', (reunioes ?? []).filter((r) => !futura(r))]]
  return (
    <section className="flex flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Agenda</h1>
      <p className="text-xs text-muted">Horários no seu fuso ({Intl.DateTimeFormat().resolvedOptions().timeZone}). Proponha reuniões a partir de Conexões.</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {reunioes && reunioes.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nenhuma reunião ainda.</p>}
      {grupos.filter(([, l]) => l.length > 0).map(([nome, lista]) => (
        <section key={nome} aria-label={nome} className="flex flex-col gap-2">
          <h2 className="font-semibold">{nome}</h2>
          <ul className="flex flex-col gap-2">
            {lista.map((r) => <CartaoDaReuniao key={r.id + r.situacao} r={r} motivos={motivos} aoMudar={(e) => { setErro(e); setVersao((v) => v + 1) }} />)}
          </ul>
        </section>
      ))}
    </section>
  )
}
