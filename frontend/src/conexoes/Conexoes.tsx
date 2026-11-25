import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import {
  agirNaConexao, bloquear, bloqueiosDaStartup, type ConexaoDoPainel, type ConexaoParaInvestidor, conexaoComAStartup, conexoesDaStartup,
  definirApenasVerificados, demonstrarInteresse, desbloquear, historicoDaConexao, minhasConexoes, type MudancaDaConexao,
  NOMES_DA_SITUACAO_DA_CONEXAO, NOMES_DO_PEDIDO, type PainelDeBloqueios, type PedidoDaConexao, type SituacaoDaConexao,
} from '../api/catalogo'
import { opcoesDaLista } from '../api/organizacoes'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const REAIS = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const SEM_COMPROMISSO = 'A manifestação de interesse e o Match não representam compromisso nem garantia de investimento.'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-49
 * Título: Executar com erro
 * Objetivo: Rodar uma ação e devolver a mensagem de erro, se houver.
 * Decisões: Usado pelos painéis.
 */
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
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46
 * Título: Demonstrar interesse
 * Objetivo: Na apresentação da startup, mostrar a conexão existente ou o formulário de interesse (valor e condições
 *           preliminares, pedido de informações ou apresentação, mensagem), com o aviso de que não há compromisso.
 * Decisões: Confirmação em dois passos. Não verificado, em startup que exige, vê o caminho para se verificar.
 */
export function PainelDeInteresse({ startupId }: { startupId: string }) {
  const [estado, setEstado] = useState<ConexaoParaInvestidor | null>(null)
  const [aberto, setAberto] = useState(false)
  const [confirmar, setConfirmar] = useState(false)
  const [valor, setValor] = useState('')
  const [condicoes, setCondicoes] = useState('')
  const [pedido, setPedido] = useState<PedidoDaConexao>('NENHUM')
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const ler = useCallback(() => { conexaoComAStartup(startupId).then(setEstado).catch(() => setEstado(null)) }, [startupId])
  useEffect(() => { ler() }, [ler])

  if (!estado) return null
  if (estado.conexao) {
    const c = estado.conexao
    return (
      <section aria-label="Sua conexão" className="flex flex-wrap items-center gap-3 rounded-lg border border-line bg-surface p-3 text-sm">
        <span>Sua conexão: <strong>{NOMES_DA_SITUACAO_DA_CONEXAO[c.situacao]}</strong></span>
        {c.situacao === 'PENDENTE' && (
          <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => agirNaConexao(c.id, 'retirada')).then((e) => { setErro(e); ler() })}>
            Retirar interesse
          </button>
        )}
        <Link to="/conexoes" className={CLASSE_LINK}>Ver em Conexões</Link>
        {erro && <AlertaErro>{erro}</AlertaErro>}
      </section>
    )
  }
  if (!estado.podeManifestar) {
    return (
      <p className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
        {estado.impedimento} {estado.precisaVerificar && <Link to="/investidor/verificacao" className={CLASSE_LINK}>Verificar agora</Link>}
      </p>
    )
  }
  if (!aberto) {
    return <button type="button" className={`${CLASSE_BOTAO_PRIMARIO} self-start`} onClick={() => setAberto(true)}>Demonstrar interesse</button>
  }
  return (
    <form aria-label="Demonstrar interesse" className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4 text-sm"
      onSubmit={(e) => {
        e.preventDefault()
        if (!confirmar) { setConfirmar(true); return }
        void tentar(() => demonstrarInteresse(startupId, { valor: valor ? Number(valor) : null, condicoes, pedido, mensagem }))
          .then((falha) => { setErro(falha); if (!falha) ler() })
      }}>
      <Campo rotulo="Valor preliminar (R$, opcional)" type="number" min={1} value={valor} onChange={(e) => setValor(e.target.value)} />
      <Campo rotulo="Condições preliminares (opcional)" value={condicoes} maxLength={1000} onChange={(e) => setCondicoes(e.target.value)} />
      <label className="flex flex-col gap-1">
        Pedido
        <select value={pedido} onChange={(e) => setPedido(e.target.value as PedidoDaConexao)} className="rounded-md border border-line px-2 py-1">
          {(Object.keys(NOMES_DO_PEDIDO) as PedidoDaConexao[]).map((p) => <option key={p} value={p}>{NOMES_DO_PEDIDO[p]}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1">
        Mensagem curta (opcional)
        <textarea rows={2} maxLength={1000} value={mensagem} onChange={(e) => setMensagem(e.target.value)} className="rounded-md border border-line px-2 py-1" />
      </label>
      {confirmar && <p role="alert" className="rounded-md bg-warn-soft px-3 py-2 text-warn">{SEM_COMPROMISSO} Confirme para enviar.</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <div className="flex gap-3">
        <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>{confirmar ? 'Confirmar interesse' : 'Continuar'}</button>
        <button type="button" className={CLASSE_LINK} onClick={() => { setAberto(false); setConfirmar(false) }}>Cancelar</button>
      </div>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-49
 * Título: Ações de uma conexão
 * Objetivo: Mostrar o que a parte pode fazer agora: avançar, encerrar com motivo e ver o histórico.
 * Decisões: Aceitar e recusar ficam no painel da startup (fundador administrador).
 */
function AcoesDaConexao({ c, aoMudar }: { c: ConexaoDoPainel; aoMudar: (erro: string | null) => void }) {
  const [motivos, setMotivos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [motivo, setMotivo] = useState('')
  const [historico, setHistorico] = useState<MudancaDaConexao[] | null>(null)
  const proxima: Partial<Record<SituacaoDaConexao, SituacaoDaConexao>> = { MATCH: 'EM_ANALISE', EM_ANALISE: 'EM_NEGOCIACAO' }
  const podeEncerrar = c.situacao === 'MATCH' || c.situacao === 'EM_ANALISE' || c.situacao === 'EM_NEGOCIACAO'
  useEffect(() => {
    if (!podeEncerrar) return
    opcoesDaLista('MOTIVO_ENCERRAMENTO').then(setMotivos).catch(() => undefined)
  }, [podeEncerrar])
  const para = proxima[c.situacao]
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-3">
        {para && (
          <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => agirNaConexao(c.id, 'avanco', { para })).then(aoMudar)}>
            Passar para {NOMES_DA_SITUACAO_DA_CONEXAO[para].toLowerCase()}
          </button>
        )}
        {podeEncerrar && (
          <span className="flex flex-wrap items-center gap-2">
            <select aria-label={`Motivo do encerramento com ${c.startup}`} value={motivo} onChange={(e) => setMotivo(e.target.value)}
              className="rounded-md border border-line px-2 py-1">
              <option value="">Motivo…</option>
              {motivos.map((m) => <option key={m.codigo} value={m.codigo}>{m.rotulo}</option>)}
            </select>
            <button type="button" className={CLASSE_LINK} disabled={!motivo}
              onClick={() => void tentar(() => agirNaConexao(c.id, 'encerramento', { motivo })).then(aoMudar)}>Encerrar</button>
          </span>
        )}
        <button type="button" className={CLASSE_LINK} onClick={() => void historicoDaConexao(c.id).then(setHistorico).catch(() => undefined)}>Histórico</button>
      </div>
      {historico && (
        <ol aria-label="Histórico da conexão" className="text-xs text-muted">
          {historico.map((h) => (
            <li key={h.em + h.para}>
              {FORMATO_DATA_HORA.format(new Date(h.em))}: {h.de ? `${NOMES_DA_SITUACAO_DA_CONEXAO[h.de]} → ` : ''}{NOMES_DA_SITUACAO_DA_CONEXAO[h.para]}
              {h.por ? ` por ${h.por}` : ' (plataforma)'}
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-46, KAN-47
 * Título: Detalhes do interesse
 * Objetivo: Mostrar valor, condições, pedido, mensagem e contatos da conexão.
 * Decisões: Contatos só a partir do Match (servidor).
 */
function DetalhesDoInteresse({ c }: { c: ConexaoDoPainel }) {
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-3 text-xs">
      {c.valorPreliminar !== null && <><dt className="text-muted">Valor preliminar</dt><dd>{REAIS.format(c.valorPreliminar)}</dd></>}
      {c.condicoes && <><dt className="text-muted">Condições</dt><dd>{c.condicoes}</dd></>}
      <dt className="text-muted">Pedido</dt><dd>{NOMES_DO_PEDIDO[c.pedido]}</dd>
      {c.mensagem && <><dt className="text-muted">Mensagem</dt><dd>{c.mensagem}</dd></>}
      {c.motivo && <><dt className="text-muted">Seu motivo</dt><dd>{c.motivo}</dd></>}
      {c.contatos.map((k) => <span key={k.email ?? k.nome} className="contents"><dt className="text-muted">Contato</dt><dd>{k.nome} · {k.email}</dd></span>)}
    </dl>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-49
 * Título: Painel de conexões do investidor
 * Objetivo: Listar as conexões da pessoa ou da organização por situação, com pendências, contatos e ações.
 * Decisões: Agrupadas por situação; "aguarda você" destacado.
 */
export function PaginaConexoes() {
  const [itens, setItens] = useState<ConexaoDoPainel[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    let ativo = true
    minhasConexoes().then((i) => { if (ativo) setItens(i) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])
  const situacoes = [...new Set((itens ?? []).map((i) => i.situacao))]
  return (
    <section className="flex flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Conexões</h1>
      <p className="text-xs text-muted">{SEM_COMPROMISSO}</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {itens && itens.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">Nenhuma conexão ainda. <Link to="/catalogo" className={CLASSE_LINK}>Abrir o catálogo</Link></p>}
      {situacoes.map((s) => (
        <section key={s} aria-label={NOMES_DA_SITUACAO_DA_CONEXAO[s]} className="flex flex-col gap-2">
          <h2 className="font-semibold">{NOMES_DA_SITUACAO_DA_CONEXAO[s]}</h2>
          <ul className="flex flex-col gap-2">
            {(itens ?? []).filter((i) => i.situacao === s).map((c) => (
              <li key={c.id} className={`flex flex-col gap-2 rounded-lg border bg-surface p-4 text-sm ${c.aguardaVoce ? 'border-primary' : 'border-line'}`}>
                <span className="flex flex-wrap items-center justify-between gap-2">
                  <Link to={`/catalogo/startups/${c.startupId}`} className={CLASSE_LINK}>{c.startup}</Link>
                  <span className="text-xs text-muted">
                    {c.aguardaVoce && <strong className="mr-2 text-primary">Aguarda você</strong>}
                    atualizada em {FORMATO_DATA_HORA.format(new Date(c.atualizadaEm))}{c.feitaPor && ` · por ${c.feitaPor}`}
                  </span>
                </span>
                <DetalhesDoInteresse c={c} />
                <AcoesDaConexao c={c} aoMudar={(e) => { setErro(e); setVersao((v) => v + 1) }} />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-47, KAN-48, KAN-49, KAN-67
 * Título: Conexões da startup
 * Objetivo: Mostrar à equipe as manifestações recebidas (com resumo do investidor e filtro por situação), deixar o
 *           fundador administrador aceitar ou recusar, avançar e encerrar, e gerenciar bloqueios e a exigência de
 *           investidor verificado.
 * Decisões: Recusa com motivo opcional, que o investidor não vê.
 */
export function ConexoesDaStartup({ startupId, admin }: { startupId: string; admin: boolean }) {
  const [filtro, setFiltro] = useState<SituacaoDaConexao | ''>('')
  const [itens, setItens] = useState<ConexaoDoPainel[] | null>(null)
  const [bloqueios, setBloqueios] = useState<PainelDeBloqueios | null>(null)
  const [motivosRecusa, setMotivosRecusa] = useState<{ codigo: string; rotulo: string }[]>([])
  const [recusa, setRecusa] = useState<Record<string, string>>({})
  const [tipoBloqueio, setTipoBloqueio] = useState<'PESSOA' | 'ORGANIZACAO'>('PESSOA')
  const [alvo, setAlvo] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const recarregar = (e: string | null) => { setErro(e); setVersao((v) => v + 1) }

  useEffect(() => {
    let ativo = true
    conexoesDaStartup(startupId, filtro).then((i) => { if (ativo) setItens(i) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    bloqueiosDaStartup(startupId).then((b) => { if (ativo) setBloqueios(b) }).catch(() => undefined)
    return () => { ativo = false }
  }, [startupId, filtro, versao])
  useEffect(() => { opcoesDaLista('MOTIVO_RECUSA').then(setMotivosRecusa).catch(() => undefined) }, [])

  return (
    <section aria-label="Conexões" className="flex flex-col gap-4 text-sm">
      <p className="text-xs text-muted">{SEM_COMPROMISSO} O Match não libera as informações confidenciais.</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <label className="flex items-center gap-2">
        Situação
        <select value={filtro} onChange={(e) => setFiltro(e.target.value as SituacaoDaConexao | '')} className="rounded-md border border-line px-2 py-1">
          <option value="">Todas</option>
          {(['PENDENTE', 'MATCH', 'RECUSADA'] as SituacaoDaConexao[]).map((s) => <option key={s} value={s}>{NOMES_DA_SITUACAO_DA_CONEXAO[s]}</option>)}
        </select>
      </label>
      {itens && itens.length === 0 && <p className="text-muted">Nenhuma manifestação nesta situação.</p>}
      <ul aria-label="Manifestações recebidas" className="flex flex-col gap-2">
        {(itens ?? []).map((c) => {
          const inv = c.investidor
          return (
            <li key={c.id} className={`flex flex-col gap-2 rounded-lg border bg-surface p-4 ${c.aguardaVoce ? 'border-primary' : 'border-line'}`}>
              <span className="flex flex-wrap items-center justify-between gap-2">
                <strong>{inv?.organizacao ? `${inv.organizacao} (por ${inv.pessoa})` : inv?.pessoa}</strong>
                <span className="text-xs text-muted">{NOMES_DA_SITUACAO_DA_CONEXAO[c.situacao]} · {FORMATO_DATA_HORA.format(new Date(c.criadaEm))}</span>
              </span>
              {inv && (
                <p className="text-xs text-muted">
                  {[inv.tipo, inv.selo === 'VERIFICADO' ? 'verificado' : 'não verificado',
                    inv.ticketMinimo !== null && inv.ticketMaximo !== null ? `ticket ${REAIS.format(inv.ticketMinimo)} a ${REAIS.format(inv.ticketMaximo)}` : null,
                    inv.lideranca === 'LIDERA' || inv.lideranca === 'AMBOS' ? 'lidera rodadas' : null].filter(Boolean).join(' · ')}
                </p>
              )}
              <DetalhesDoInteresse c={c} />
              {admin && c.situacao === 'PENDENTE' && (
                <div className="flex flex-wrap items-center gap-3">
                  <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void tentar(() => agirNaConexao(c.id, 'aceite')).then(recarregar)}>Aceitar (Match)</button>
                  <select aria-label="Motivo da recusa (só para a plataforma)" value={recusa[c.id] ?? ''} onChange={(e) => setRecusa({ ...recusa, [c.id]: e.target.value })}
                    className="rounded-md border border-line px-2 py-1">
                    <option value="">Motivo (opcional)</option>
                    {motivosRecusa.map((m) => <option key={m.codigo} value={m.codigo}>{m.rotulo}</option>)}
                  </select>
                  <button type="button" className={CLASSE_LINK}
                    onClick={() => void tentar(() => agirNaConexao(c.id, 'recusa', { motivo: recusa[c.id] || null })).then(recarregar)}>Recusar</button>
                </div>
              )}
              {c.situacao !== 'PENDENTE' && <AcoesDaConexao c={c} aoMudar={recarregar} />}
            </li>
          )
        })}
      </ul>

      {bloqueios && (
        <section aria-label="Bloqueios" className="flex flex-col gap-2 rounded-lg border border-line p-3">
          <h2 className="font-semibold">Quem pode se conectar</h2>
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={bloqueios.apenasVerificados} disabled={!admin}
              onChange={(e) => void tentar(() => definirApenasVerificados(startupId, e.target.checked)).then(recarregar)} />
            Aceitar interesse só de investidores verificados
          </label>
          <p className="text-xs text-muted">Bloqueados deixam de ver a startup e não podem demonstrar interesse. Eles não são avisados.</p>
          {admin && (
            <form aria-label="Bloquear" className="flex flex-wrap items-end gap-2"
              onSubmit={(e) => { e.preventDefault(); void tentar(() => bloquear(startupId, tipoBloqueio, alvo)).then((f) => { recarregar(f); if (!f) setAlvo('') }) }}>
              <select aria-label="Tipo do bloqueio" value={tipoBloqueio} onChange={(e) => setTipoBloqueio(e.target.value as 'PESSOA' | 'ORGANIZACAO')}
                className="rounded-md border border-line px-2 py-1">
                <option value="PESSOA">Pessoa (e-mail)</option>
                <option value="ORGANIZACAO">Organização (CNPJ)</option>
              </select>
              <Campo rotulo={tipoBloqueio === 'PESSOA' ? 'E-mail' : 'CNPJ'} value={alvo} onChange={(e) => setAlvo(e.target.value)} />
              <button type="submit" className={CLASSE_LINK} disabled={!alvo.trim()}>Bloquear</button>
            </form>
          )}
          <ul aria-label="Bloqueados" className="flex flex-col gap-1">
            {bloqueios.ativos.map((b) => (
              <li key={b.tipo + b.alvoId} className="flex items-center gap-2">
                {b.rotulo}
                {admin && <button type="button" className={CLASSE_LINK}
                  onClick={() => void tentar(() => desbloquear(startupId, b.tipo, b.alvoId)).then(recarregar)}>Desbloquear</button>}
              </li>
            ))}
          </ul>
          {bloqueios.historico.length > 0 && (
            <ol className="text-xs text-muted">
              {bloqueios.historico.map((h) => <li key={h.em + h.rotulo}>{FORMATO_DATA_HORA.format(new Date(h.em))}: {h.acao.toLowerCase()} de {h.rotulo}{h.por && ` por ${h.por}`}</li>)}
            </ol>
          )}
        </section>
      )}
      {!admin && <p className="text-xs text-muted">Só o fundador administrador aceita, recusa e bloqueia.</p>}
    </section>
  )
}
