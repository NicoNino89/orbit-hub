import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { requisitar } from '../api/cliente'
import { NOMES_DA_SITUACAO_DA_CONEXAO, type ConexaoDoPainel } from '../api/catalogo'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const CARTAO = 'rounded-2xl border border-line bg-surface p-5'

interface Mensagem { autor: string | null; lado: string; texto: string; em: string; minha: boolean }
interface Conversa { conexaoId: string; podeEscrever: boolean; mensagens: Mensagem[] }
interface Investimento { startupId: string; startup: string; data: string | null; instrumento: string | null; valor: number | null; situacao: string }
interface Participacao { startupId: string; startup: string; situacao: string; prazo: string | null; intencao: number | null; entrouEm: string }

const real = (v: number | null) => (v === null ? '—' : v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }))

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-176
 * Título: Conversa da conexão
 * Objetivo: Mostrar e enviar mensagens entre a startup e o investidor depois do Match.
 * Decisões: Fechada, a conversa fica só leitura; o aviso lembra que documentos confidenciais vão no Data Room.
 */
export function ConversaDaConexao({ conexaoId }: { conexaoId: string }) {
  const [c, setC] = useState<Conversa | null>(null)
  const [texto, setTexto] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    requisitar<Conversa>(`/api/v1/conexoes/${conexaoId}/mensagens`).then(setC).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Falhou.'))
  }, [conexaoId])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-176
   * Título: Enviar
   * Objetivo: Enviar a mensagem e atualizar a conversa.
   * Decisões: Limpa o campo.
   */
  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      setC(await requisitar<Conversa>(`/api/v1/conexoes/${conexaoId}/mensagens`, { metodo: 'POST', corpo: { texto } }))
      setTexto('')
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Falhou.')
    }
  }

  return (
    <section aria-label="Conversa" className="flex flex-col gap-2 rounded-xl border border-line p-3">
      <ol className="flex max-h-72 flex-col gap-2 overflow-y-auto">
        {c?.mensagens.length === 0 && <li className="text-sm text-muted">Nenhuma mensagem ainda.</li>}
        {c?.mensagens.map((m) => (
          <li key={m.em + m.texto} className={`max-w-[80%] rounded-xl px-3 py-2 text-sm ${m.minha ? 'self-end bg-primary-soft' : 'self-start bg-surface-2'}`}>
            <span className="block text-xs text-muted">{m.autor} · {new Date(m.em).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</span>
            {m.texto}
          </li>
        ))}
      </ol>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {c?.podeEscrever ? (
        <form aria-label="Nova mensagem" onSubmit={(e) => void enviar(e)} className="flex gap-2">
          <label className="sr-only" htmlFor={`msg-${conexaoId}`}>Mensagem</label>
          <input id={`msg-${conexaoId}`} value={texto} maxLength={2000} onChange={(e) => setTexto(e.target.value)}
            className="min-w-0 flex-1 rounded-lg border border-line bg-surface px-3 py-2" placeholder="Escreva… (documentos vão no Data Room)" />
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar</button>
        </form>
      ) : c && <p className="text-xs text-muted">Conversa encerrada: só leitura.</p>}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-174
 * Título: Minha Carteira
 * Objetivo: Mostrar os investimentos registrados, as rodadas de que participo e o total confirmado, com exportação CSV.
 * Decisões: Só o confirmado soma; em disputa e não confirmados ficam marcados.
 */
export function PaginaCarteira() {
  const [dados, setDados] = useState<{ investimentos: Investimento[]; rodadas: Participacao[] } | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    requisitar<{ investimentos: Investimento[]; rodadas: Participacao[] }>('/api/v1/investidores/carteira').then(setDados)
      .catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Falhou.'))
  }, [])
  const confirmados = dados?.investimentos.filter((i) => i.situacao === 'CONFIRMADO') ?? []
  const total = confirmados.reduce((s, i) => s + (i.valor ?? 0), 0)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:58
   * Cartão Jira: KAN-174
   * Título: Exportar CSV
   * Objetivo: Baixar a lista de investimentos para controle próprio.
   * Decisões: Separador ponto e vírgula (Excel em português).
   */
  function exportar() {
    if (!dados || typeof URL.createObjectURL !== 'function') return
    const linhas = [['Startup', 'Data', 'Instrumento', 'Valor', 'Situação'], ...dados.investimentos.map((i) => [i.startup, i.data ?? '', i.instrumento ?? '',
      String(i.valor ?? ''), i.situacao])]
    const u = URL.createObjectURL(new Blob([linhas.map((l) => l.join(';')).join('\n')], { type: 'text/csv' }))
    const a = document.createElement('a')
    a.href = u
    a.download = 'minha-carteira.csv'
    a.click()
    URL.revokeObjectURL(u)
  }

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight">Minha Carteira</h1>
          <p className="text-sm text-muted">Investimentos são combinados fora da plataforma. Isto não é extrato financeiro nem recomendação.</p>
        </div>
        <button type="button" onClick={exportar} className={CLASSE_LINK}>Exportar CSV</button>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <div className="grid gap-3 sm:grid-cols-2">
        <p className={CARTAO}>Total confirmado<br /><strong className="text-2xl">{real(total)}</strong></p>
        <p className={CARTAO}>Startups investidas<br /><strong className="text-2xl">{new Set(confirmados.map((i) => i.startupId)).size}</strong></p>
      </div>
      <section aria-label="Investimentos" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Investimentos</h2>
        {dados?.investimentos.length === 0 && <p className="text-muted">Nenhum investimento registrado ainda.</p>}
        {dados?.investimentos.map((i) => (
          <p key={`${i.startupId}-${i.data}-${i.valor}`} className={`${CARTAO} flex flex-wrap justify-between gap-2`}>
            <Link to={`/catalogo/startups/${i.startupId}`} className="font-semibold">{i.startup}</Link>
            <span>{i.data ?? '—'} · {i.instrumento ?? '—'} · {real(i.valor)} · {i.situacao === 'CONFIRMADO' ? 'confirmado' : i.situacao.toLowerCase().replace('_', ' ')}</span>
          </p>
        ))}
      </section>
      <section aria-label="Rodadas de que participo" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Rodadas de que participo</h2>
        {dados?.rodadas.length === 0 && <p className="text-muted">Nenhuma rodada em andamento.</p>}
        {dados?.rodadas.map((r) => (
          <p key={`${r.startupId}-${r.entrouEm}`} className={`${CARTAO} flex flex-wrap justify-between gap-2`}>
            <Link to={`/catalogo/startups/${r.startupId}`} className="font-semibold">{r.startup}</Link>
            <span>{r.situacao.toLowerCase().replace('_', ' ')} · prazo {r.prazo ?? '—'} · minha intenção {real(r.intencao)}</span>
          </p>
        ))}
      </section>
      <Link to="/conexoes" className={CLASSE_LINK}>Ver as conexões em andamento →</Link>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-175
 * Título: Minha Rede
 * Objetivo: Reunir as conexões a partir do Match (com contatos e a conversa) e as organizações de que participo.
 * Decisões: Contatos só depois do Match (KAN-48). "Seguindo" entra na fase 3 (KAN-136).
 */
export function PaginaMinhaRede() {
  const [conexoes, setConexoes] = useState<ConexaoDoPainel[]>([])
  const [orgs, setOrgs] = useState<{ id: string; nome: string; meuPapel: string }[]>([])
  const [filtro, setFiltro] = useState('')
  const [aberta, setAberta] = useState<string | null>(null)
  useEffect(() => {
    requisitar<ConexaoDoPainel[]>('/api/v1/conexoes').then(setConexoes).catch(() => setConexoes([]))
    requisitar<{ id: string; nome: string; meuPapel: string }[]>('/api/v1/organizacoes/minhas').then(setOrgs).catch(() => setOrgs([]))
  }, [])
  const rede = conexoes.filter((c) => ['MATCH', 'EM_ANALISE', 'EM_NEGOCIACAO', 'INVESTIU'].includes(c.situacao))
    .filter((c) => !filtro || c.startup.toLowerCase().includes(filtro.toLowerCase()))
  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-bold tracking-tight">Minha Rede</h1>
      <label className="flex flex-col gap-1 text-sm font-semibold">Buscar na rede
        <input value={filtro} onChange={(e) => setFiltro(e.target.value)} className="rounded-lg border border-line bg-surface px-3 py-2" />
      </label>
      <section aria-label="Conexões" className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Conexões</h2>
        {rede.length === 0 && <p className="text-muted">Ninguém na sua rede ainda. Os contatos aparecem a partir do Match.</p>}
        {rede.map((c) => (
          <article key={c.id} className={`${CARTAO} flex flex-col gap-2`}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <Link to={`/catalogo/startups/${c.startupId}`} className="text-lg font-semibold">{c.startup}</Link>
              <span className="rounded-full bg-surface-2 px-3 py-0.5 text-sm">{NOMES_DA_SITUACAO_DA_CONEXAO[c.situacao]}</span>
            </div>
            {c.contatos.length > 0 && <p className="text-sm">{c.contatos.map((p) => `${p.nome ?? ''} (${p.email ?? ''})`).join(' · ')}</p>}
            <div className="flex gap-3 text-sm">
              <button type="button" className={CLASSE_LINK} onClick={() => setAberta(aberta === c.id ? null : c.id)}>{aberta === c.id ? 'Fechar conversa' : 'Mensagens'}</button>
              <Link to="/agenda" className={CLASSE_LINK}>Agenda</Link>
            </div>
            {aberta === c.id && <ConversaDaConexao conexaoId={c.id} />}
          </article>
        ))}
      </section>
      <section aria-label="Organizações" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Organizações</h2>
        {orgs.length === 0 && <p className="text-muted">Você não participa de organizações.</p>}
        {orgs.map((o) => (
          <Link key={o.id} to={`/investidor/organizacoes/${o.id}`} className={CARTAO}>{o.nome} · {o.meuPapel.toLowerCase()}</Link>
        ))}
      </section>
    </section>
  )
}
