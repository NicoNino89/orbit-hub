import { useEffect, useState } from 'react'
import {
  avaliarIncidente, comunicarIncidente, convidarOrganizacao, definirLiderDoTime, devolverCasosDoMembro, encerrarIncidente, filaDePrivacidade, incidentes,
  minhasAusencias, NOMES_DA_SITUACAO_DO_PEDIDO, NOMES_DO_PEDIDO, painelDeOperacao, proporExclusaoDoTitular, proporTransferencia, registrarAusencia,
  registrarIncidente, removerAusencia, responderPedido, timesInternos, type Ausencia, type Incidente, type PainelDeOperacao, type PedidoDoTitular,
  type Time,
} from '../api/privacidade'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-82
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
 * Hora: 20:54
 * Cartão Jira: KAN-82
 * Título: Número ou traço
 * Objetivo: Mostrar o indicador ou um traço quando não há base.
 * Decisões: Sufixo opcional.
 */
function valor(n: number | null, sufixo = '') {
  return n === null ? '—' : `${n}${sufixo}`
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-82, KAN-91
 * Título: Painel de operação
 * Objetivo: Mostrar as filas (abertos, idade, perto do prazo, vencidos, por responsável), os indicadores do mês e do
 *           ecossistema, e deixar qualquer pessoa da equipe registrar a suspeita de incidente.
 * Decisões: Início do backoffice.
 */
export function PainelDeOperacaoAdmin() {
  const [p, setP] = useState<PainelDeOperacao | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [incidente, setIncidente] = useState({ titulo: '', descricao: '' })
  const [aviso, setAviso] = useState<string | null>(null)
  useEffect(() => {
    painelDeOperacao().then(setP).catch((e: unknown) => setErro(mensagemDe(e)))
  }, [])
  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Painel de operação</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {p && (
        <>
          <table aria-label="Filas" className="text-left text-sm">
            <thead><tr><th>Fila</th><th>Abertos</th><th>Idade média</th><th>Perto do prazo</th><th>Vencidos</th><th>Por responsável</th></tr></thead>
            <tbody>
              {p.filas.map((f) => (
                <tr key={f.fila} className={f.vencidos > 0 ? 'text-crit' : ''}>
                  <td>{f.nome}</td><td>{f.abertos}</td><td>{f.idadeMediaHoras} h</td><td>{f.pertoDoPrazo}</td><td>{f.vencidos}</td>
                  <td>{Object.entries(f.porResponsavel).map(([n, q]) => `${n}: ${q}`).join(' · ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <section aria-label="Indicadores do mês" className="grid grid-cols-2 gap-2 text-sm sm:grid-cols-5">
            <p className={CAIXA}>Casos no mês<br /><strong>{p.indicadores.casosNoMes}</strong></p>
            <p className={CAIXA}>Horas para resolver<br /><strong>{valor(p.indicadores.horasMediasParaResolver)}</strong></p>
            <p className={CAIXA}>No prazo<br /><strong>{valor(p.indicadores.percentualNoPrazo, '%')}</strong></p>
            <p className={CAIXA}>Recursos aceitos<br /><strong>{valor(p.indicadores.percentualDeRecursosAceitos, '%')}</strong></p>
            <p className={CAIXA}>Satisfação<br /><strong>{valor(p.indicadores.satisfacao)}</strong></p>
          </section>
          <section aria-label="Ecossistema" className="grid grid-cols-2 gap-2 text-sm sm:grid-cols-6">
            <p className={CAIXA}>Novos cadastros<br /><strong>{p.ecossistema.novosCadastros}</strong></p>
            <p className={CAIXA}>Startups publicadas<br /><strong>{p.ecossistema.startupsPublicadas}</strong></p>
            <p className={CAIXA}>Matches no mês<br /><strong>{p.ecossistema.matches}</strong></p>
            <p className={CAIXA}>Reuniões realizadas<br /><strong>{p.ecossistema.reunioesRealizadas}</strong></p>
            <p className={CAIXA}>Rodadas abertas<br /><strong>{p.ecossistema.rodadasAbertas}</strong></p>
            <p className={CAIXA}>Investimentos<br /><strong>{p.ecossistema.investimentosConcretizados}</strong></p>
          </section>
        </>
      )}
      <form aria-label="Registrar incidente" onSubmit={(e) => {
        e.preventDefault()
        registrarIncidente(incidente).then(() => { setAviso('Incidente registrado. O encarregado foi avisado.'); setIncidente({ titulo: '', descricao: '' }) })
          .catch((x: unknown) => setErro(mensagemDe(x)))
      }} className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Suspeita de incidente com dados pessoais</h2>
        <Campo rotulo="Título do incidente" value={incidente.titulo} onChange={(e) => setIncidente({ ...incidente, titulo: e.target.value })} />
        <label className="flex flex-col gap-1 text-sm font-semibold">O que aconteceu
          <textarea value={incidente.descricao} onChange={(e) => setIncidente({ ...incidente, descricao: e.target.value })} rows={2} className={CAIXA} />
        </label>
        {aviso && <p role="status" className="text-sm">{aviso}</p>}
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Registrar</button>
      </form>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90, KAN-91
 * Título: Privacidade (encarregado)
 * Objetivo: Responder os pedidos do titular, propor a exclusão fora do fluxo e conduzir os incidentes.
 * Decisões: Um texto por pedido; ações do incidente em linha.
 */
export function PaginaPrivacidade() {
  const [pedidos, setPedidos] = useState<PedidoDoTitular[]>([])
  const [lista, setLista] = useState<Incidente[]>([])
  const [textos, setTextos] = useState<Record<string, string>>({})
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    filaDePrivacidade().then(setPedidos).catch((e: unknown) => setErro(mensagemDe(e)))
    incidentes().then(setLista).catch(() => setLista([]))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-90
   * Título: Agir
   * Objetivo: Executar a ação e recarregar.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<unknown>) {
    setErro(null)
    try {
      await acao()
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  const texto = (id: string) => textos[id] ?? ''
  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Privacidade</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <section aria-label="Pedidos do titular" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Pedidos do titular</h2>
        {pedidos.length === 0 && <p className="text-muted">Nenhum pedido em aberto.</p>}
        {pedidos.map((p) => (
          <article key={p.id} className={`${CAIXA} flex flex-col gap-2 ${p.vencido ? 'border-crit' : ''}`}>
            <p className="font-semibold">{NOMES_DO_PEDIDO[p.tipo]} · {p.titular} · {NOMES_DA_SITUACAO_DO_PEDIDO[p.situacao]} · prazo {FORMATO_DATA_HORA.format(new Date(p.prazo))}</p>
            {p.detalhes && <p className="text-sm">{p.detalhes}</p>}
            {p.situacao !== 'AGUARDANDO_APROVACAO' && (
              <>
                <Campo rotulo="Resposta ou justificativa" value={texto(p.id)} onChange={(e) => setTextos({ ...textos, [p.id]: e.target.value })} />
                <div className="flex flex-wrap gap-3">
                  <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void agir(() => responderPedido(p.id, texto(p.id), false))}>Responder</button>
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => responderPedido(p.id, texto(p.id), true))}>Recusar</button>
                  {p.tipo === 'ELIMINACAO' && (
                    <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => proporExclusaoDoTitular(p.id, texto(p.id)))}>
                      Propor exclusão da conta
                    </button>
                  )}
                </div>
              </>
            )}
          </article>
        ))}
      </section>
      <section aria-label="Incidentes" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Incidentes</h2>
        {lista.map((i) => (
          <article key={i.id} className={`${CAIXA} flex flex-col gap-2 ${i.comunicacaoAtrasada ? 'border-crit' : ''}`}>
            <p className="font-semibold">{i.titulo} · {i.situacao.toLowerCase()}{i.risco ? ` · risco ${i.risco.toLowerCase()}` : ''}
              {' · '}comunicar até {FORMATO_DATA_HORA.format(new Date(i.prazoComunicacao))}</p>
            <p className="text-sm">{i.descricao}</p>
            {i.situacao !== 'ENCERRADO' && (
              <>
                <Campo rotulo={`Texto para ${i.titulo}`} value={texto(i.id)} onChange={(e) => setTextos({ ...textos, [i.id]: e.target.value })}
                  dica="Contenção, lições aprendidas ou ações preventivas, conforme o botão." />
                <div className="flex flex-wrap gap-3 text-sm">
                  {(['BAIXO', 'MEDIO', 'ALTO'] as const).map((r) => (
                    <button key={r} type="button" className={CLASSE_LINK}
                      onClick={() => void agir(() => avaliarIncidente(i.id, { risco: r, contencao: texto(i.id) || undefined }))}>Risco {r.toLowerCase()}</button>
                  ))}
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => comunicarIncidente(i.id, 'ANPD'))}>Comunicado à ANPD</button>
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => comunicarIncidente(i.id, 'TITULARES'))}>Comunicado aos titulares</button>
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => encerrarIncidente(i.id, texto(i.id), texto(i.id)))}>Encerrar</button>
                </div>
              </>
            )}
          </article>
        ))}
      </section>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-76
 * Título: Times
 * Objetivo: Mostrar os times com líder, membros e ausências; deixar o administrador escolher o líder e devolver os
 *           casos de um membro; deixar cada pessoa marcar as próprias ausências.
 * Decisões: Datas no formato do navegador (AAAA-MM-DD).
 */
export function PaginaTimes() {
  const { estado } = useSessao()
  const admin = estado.status === 'autenticado' && estado.usuario.papeis.includes('ADMIN')
  const [times, setTimes] = useState<Time[]>([])
  const [ausencias, setAusencias] = useState<Ausencia[]>([])
  const [nova, setNova] = useState({ inicio: '', fim: '', motivo: '' })
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    timesInternos().then(setTimes).catch((e: unknown) => setErro(mensagemDe(e)))
    minhasAusencias().then(setAusencias).catch(() => setAusencias([]))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-76
   * Título: Agir
   * Objetivo: Executar a ação e recarregar.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<unknown>, texto?: string) {
    setErro(null)
    try {
      await acao()
      if (texto) setAviso(texto)
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Times</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="text-sm">{aviso}</p>}
      {times.map((t) => (
        <article key={t.time} aria-label={t.nome} className={`${CAIXA} flex flex-col gap-2`}>
          <p className="font-semibold">{t.nome} · líder: {t.lider ?? 'administradores'} · distribuição {t.distribuicao === 'RODIZIO' ? 'por rodízio' : 'manual'}</p>
          <ul className="text-sm">
            {t.membros.map((m) => (
              <li key={m.id} className="flex flex-wrap gap-3">
                {m.nome}{m.ausenteHoje ? ' (ausente hoje)' : ''}
                {admin && t.liderId !== m.id && (
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => definirLiderDoTime(t.time, m.id))}>Tornar líder</button>
                )}
                {admin && (
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => devolverCasosDoMembro(m.id), 'Casos devolvidos à fila.')}>
                    Devolver casos à fila
                  </button>
                )}
              </li>
            ))}
          </ul>
        </article>
      ))}
      <form aria-label="Minhas ausências" onSubmit={(e) => { e.preventDefault(); void agir(() => registrarAusencia(nova.inicio, nova.fim, nova.motivo)) }}
        className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Minhas ausências</h2>
        <Campo rotulo="Início" type="date" value={nova.inicio} onChange={(e) => setNova({ ...nova, inicio: e.target.value })} />
        <Campo rotulo="Fim" type="date" value={nova.fim} onChange={(e) => setNova({ ...nova, fim: e.target.value })} />
        <Campo rotulo="Motivo" value={nova.motivo} onChange={(e) => setNova({ ...nova, motivo: e.target.value })} />
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Registrar ausência</button>
        <ul className="text-sm">
          {ausencias.map((a) => (
            <li key={a.id}>{a.inicio} a {a.fim}{a.motivo ? ` · ${a.motivo}` : ''}{' '}
              <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => removerAusencia(a.id))}>Remover</button></li>
          ))}
        </ul>
      </form>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-77
 * Título: Titularidade
 * Objetivo: Criar a organização convidada e propor a transferência de titularidade.
 * Decisões: Usado na página do atendimento, para N2 e administrador.
 */
export function TitularidadeNoAtendimento() {
  const [org, setOrg] = useState({ nome: '', tipo: 'FUNDO_VC', cnpj: '', emailResponsavel: '' })
  const [tr, setTr] = useState<{ alvo: 'STARTUP' | 'ORGANIZACAO'; alvoId: string; emailNovoTitular: string; comprovacao: string }>(
    { alvo: 'STARTUP', alvoId: '', emailNovoTitular: '', comprovacao: '' })
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-77
   * Título: Agir
   * Objetivo: Executar a ação e mostrar o aviso.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<unknown>, texto: string) {
    setErro(null)
    try {
      await acao()
      setAviso(texto)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <section aria-label="Titularidade" className="flex flex-col gap-4">
      <h2 className="text-xl font-semibold">Cadastro assistido e titularidade</h2>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="text-sm">{aviso}</p>}
      <form aria-label="Convite institucional" onSubmit={(e) => { e.preventDefault(); void agir(() => convidarOrganizacao(org), 'Organização criada e responsável convidado.') }}
        className="flex flex-col gap-2">
        <Campo rotulo="Nome da organização" value={org.nome} onChange={(e) => setOrg({ ...org, nome: e.target.value })} />
        <Campo rotulo="CNPJ" value={org.cnpj} onChange={(e) => setOrg({ ...org, cnpj: e.target.value })} />
        <Campo rotulo="E-mail do responsável" value={org.emailResponsavel} onChange={(e) => setOrg({ ...org, emailResponsavel: e.target.value })} />
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Criar e convidar</button>
      </form>
      <form aria-label="Transferência de titularidade" onSubmit={(e) => { e.preventDefault(); void agir(() => proporTransferencia(tr), 'Transferência enviada para a segunda aprovação.') }}
        className="flex flex-col gap-2">
        <select aria-label="O que transferir" value={tr.alvo} onChange={(e) => setTr({ ...tr, alvo: e.target.value as 'STARTUP' | 'ORGANIZACAO' })} className={CAIXA}>
          <option value="STARTUP">Startup</option><option value="ORGANIZACAO">Organização</option>
        </select>
        <Campo rotulo="Identificador da startup ou organização" value={tr.alvoId} onChange={(e) => setTr({ ...tr, alvoId: e.target.value })} />
        <Campo rotulo="E-mail do novo titular (membro ativo)" value={tr.emailNovoTitular} onChange={(e) => setTr({ ...tr, emailNovoTitular: e.target.value })} />
        <Campo rotulo="Comprovação conferida" value={tr.comprovacao} onChange={(e) => setTr({ ...tr, comprovacao: e.target.value })} />
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Propor transferência</button>
      </form>
    </section>
  )
}
