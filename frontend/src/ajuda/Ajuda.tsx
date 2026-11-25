import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import {
  abrirChamado, abrirChamadoPublico, artigosDaAjuda, avaliarChamado, confirmarChamadoPublico, meuChamado, meusChamados, NOMES_DA_SITUACAO_DO_CHAMADO,
  responderChamado, servicosDaPlataforma, type Artigo, type Chamado, type ResumoDoChamado, type ServicoDaPlataforma,
} from '../api/atendimento'
import {
  meusRecursos, minhasDenuncias, minhasSancoes, NOMES_DA_ORIGEM, NOMES_DA_SANCAO, recorrer, type MinhaDenuncia, type MinhaSancao, type OrigemDoRecurso,
  type Recurso,
} from '../api/moderacao'
import { opcoesDaLista } from '../api/organizacoes'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89
 * Título: Data e hora
 * Objetivo: Mostrar datas no fuso de quem vê.
 * Decisões: Formato curto do português do Brasil.
 */
function quando(iso: string) {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89
 * Título: Mensagem de erro
 * Objetivo: Tirar o texto de um erro desconhecido.
 * Decisões: Texto padrão quando não há mensagem.
 */
function mensagemDe(e: unknown) {
  return e instanceof Error ? e.message : 'Não foi possível concluir. Tente de novo.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89
 * Título: Central de ajuda
 * Objetivo: Mostrar, com ou sem login, o catálogo de serviços, os artigos e o caminho para pedir ajuda (chamado com
 *           conta ou formulário público para quem não consegue entrar).
 * Decisões: Quem está logado vai para "Meus chamados"; quem não está preenche o formulário público.
 */
export function PaginaAjuda() {
  const { estado } = useSessao()
  const [servicos, setServicos] = useState<ServicoDaPlataforma[]>([])
  const [artigos, setArtigos] = useState<Artigo[]>([])
  useEffect(() => {
    servicosDaPlataforma().then(setServicos).catch(() => setServicos([]))
    artigosDaAjuda().then(setArtigos).catch(() => setArtigos([]))
  }, [])
  const logado = estado.status === 'autenticado'
  return (
    <main className="mx-auto flex max-w-4xl flex-col gap-8 px-4 py-10">
      <header className="flex flex-col gap-2">
        <Link to={logado ? '/painel' : '/'} className="font-bold tracking-tight">Orbit Hub</Link>
        <h1 className="font-display text-3xl font-semibold tracking-tight">Central de ajuda</h1>
        <p className="text-muted">Guias, serviços da plataforma e atendimento.</p>
      </header>
      <section aria-labelledby="t-artigos" className="flex flex-col gap-2">
        <h2 id="t-artigos" className="text-xl font-semibold">Perguntas frequentes</h2>
        {artigos.map((a) => (
          <details key={a.id} className={CAIXA}>
            <summary className="cursor-pointer font-semibold">{a.titulo}</summary>
            <p className="mt-2 whitespace-pre-line text-sm">{a.conteudo}</p>
          </details>
        ))}
      </section>
      <section aria-labelledby="t-servicos" className="flex flex-col gap-2">
        <h2 id="t-servicos" className="text-xl font-semibold">Serviços da plataforma</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead><tr className="border-b border-line"><th className="py-2">Serviço</th><th>Para quem</th><th>Quem presta</th><th>Canal</th><th>Prazo</th></tr></thead>
            <tbody>
              {servicos.map((s) => (
                <tr key={s.codigo} className="border-b border-line align-top">
                  <td className="py-2 font-semibold">{s.nome}</td><td>{s.publico}</td><td>{s.prestador}</td><td>{s.canal}</td><td>{s.prazo}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section aria-labelledby="t-contato" className="flex flex-col gap-2">
        <h2 id="t-contato" className="text-xl font-semibold">Falar com o atendimento</h2>
        {logado
          ? <p>Abra e acompanhe os seus pedidos em <Link to="/ajuda/chamados" className={CLASSE_LINK}>Meus chamados</Link>.</p>
          : <FormularioPublico />}
      </section>
    </main>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89
 * Título: Formulário sem conta
 * Objetivo: Receber o pedido de quem não consegue entrar e avisar que o link de confirmação foi enviado.
 * Decisões: A mesma resposta para qualquer e-mail.
 */
function FormularioPublico() {
  const [categorias, setCategorias] = useState<{ codigo: string; rotulo: string }[]>([])
  const [d, setD] = useState({ nome: '', email: '', categoria: 'ACESSO', descricao: '' })
  const [enviado, setEnviado] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    opcoesDaLista('CATEGORIA_CHAMADO').then((l) => setCategorias(l.filter((c) => c.codigo !== 'DISPUTA'))).catch(() => setCategorias([]))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Enviar
   * Objetivo: Enviar o pedido sem conta.
   * Decisões: Mostra o erro de validação.
   */
  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await abrirChamadoPublico(d)
      setEnviado(true)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  if (enviado) return <p role="status">Enviamos um link para {d.email}. Confirme o e-mail para o seu pedido entrar na fila.</p>
  return (
    <form aria-label="Pedir ajuda sem conta" onSubmit={(e) => void enviar(e)} className="flex flex-col gap-3">
      <p className="text-sm text-muted">Não consegue entrar? Conte o que aconteceu. Vamos confirmar o seu e-mail antes de atender.</p>
      <Campo rotulo="Seu nome" value={d.nome} onChange={(e) => setD({ ...d, nome: e.target.value })} />
      <Campo rotulo="E-mail para resposta" type="email" value={d.email} onChange={(e) => setD({ ...d, email: e.target.value })} />
      <label className="flex flex-col gap-1 text-sm font-semibold">Assunto
        <select value={d.categoria} onChange={(e) => setD({ ...d, categoria: e.target.value })} className={CAIXA}>
          {categorias.map((c) => <option key={c.codigo} value={c.codigo}>{c.rotulo}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-sm font-semibold">O que aconteceu
        <textarea value={d.descricao} onChange={(e) => setD({ ...d, descricao: e.target.value })} rows={4} className={CAIXA} />
      </label>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar pedido</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89
 * Título: Confirmar chamado sem conta
 * Objetivo: Confirmar o e-mail pelo link e mostrar o protocolo.
 * Decisões: Uma tentativa ao abrir a página.
 */
export function PaginaConfirmarChamado() {
  const [params] = useSearchParams()
  const [protocolo, setProtocolo] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    confirmarChamadoPublico(params.get('token') ?? '').then((r) => setProtocolo(r.protocolo)).catch((e: unknown) => setErro(mensagemDe(e)))
  }, [params])
  return (
    <MolduraConta titulo="Pedido de ajuda">
      {protocolo && <p role="status">Pedido confirmado. Protocolo {protocolo.slice(0, 8)}. Responderemos por e-mail.</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
    </MolduraConta>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89, KAN-99, KAN-101, KAN-86, KAN-103
 * Título: Meus pedidos
 * Objetivo: Reunir os chamados (com o formulário de novo chamado ou disputa), as denúncias feitas, as sanções
 *           recebidas (com o recurso) e os recursos apresentados.
 * Decisões: Uma página com seções; as notificações apontam para cada uma.
 */
export function PaginaMeusPedidos() {
  const [chamados, setChamados] = useState<ResumoDoChamado[]>([])
  const [denuncias, setDenuncias] = useState<MinhaDenuncia[]>([])
  const [sancoes, setSancoes] = useState<MinhaSancao[]>([])
  const [recursos, setRecursos] = useState<Recurso[]>([])
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    meusChamados().then(setChamados).catch(() => setChamados([]))
    minhasDenuncias().then(setDenuncias).catch(() => setDenuncias([]))
    minhasSancoes().then(setSancoes).catch(() => setSancoes([]))
    meusRecursos().then(setRecursos).catch(() => setRecursos([]))
  }, [versao])
  return (
    <section className="flex flex-col gap-8">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Ajuda e atendimento</h1>
        <p className="text-sm text-muted">Veja também a <Link to="/ajuda" className={CLASSE_LINK}>central de ajuda</Link>.</p>
      </header>
      <section aria-label="Meus chamados" className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Meus chamados</h2>
        <NovoChamado aoAbrir={() => setVersao((v) => v + 1)} />
        <ul className="flex flex-col gap-2">
          {chamados.map((c) => (
            <li key={c.id} className={CAIXA}>
              <Link to={`/ajuda/chamados/${c.id}`} className={CLASSE_LINK}>{c.categoria}</Link>
              {' · '}{NOMES_DA_SITUACAO_DO_CHAMADO[c.situacao] ?? c.situacao}{c.souOutraParte ? ' · você é a outra parte' : ''}
              <span className="text-sm text-muted"> · {quando(c.criadoEm)}</span>
            </li>
          ))}
          {chamados.length === 0 && <li className="text-sm text-muted">Nenhum chamado.</li>}
        </ul>
      </section>
      <section aria-label="Minhas sanções" className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Decisões da moderação</h2>
        {sancoes.length === 0 && <p className="text-sm text-muted">Nenhuma decisão sobre você.</p>}
        {sancoes.map((s) => (
          <article key={s.sancao.id} className={CAIXA}>
            <p className="font-semibold">{NOMES_DA_SANCAO[s.sancao.tipo] ?? s.sancao.tipo}{s.sancao.dias ? ` de ${s.sancao.dias} dias` : ''}
              {s.sancao.alvoTipo === 'STARTUP' ? ' (startup)' : ''}</p>
            <p className="text-sm">Motivo: {s.motivo} · {quando(s.sancao.inicio)}{s.sancao.situacao === 'REVOGADA' ? ' · revogada no recurso' : ''}</p>
            {s.podeRecorrer && <RecorrerDaDecisao origemTipo="SANCAO" origemId={s.sancao.id} ate={s.recorrerAte} aoRecorrer={() => setVersao((v) => v + 1)} />}
          </article>
        ))}
      </section>
      <section aria-label="Meus recursos" className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Recursos</h2>
        {recursos.length === 0 && <p className="text-sm text-muted">Nenhum recurso.</p>}
        {recursos.map((r) => (
          <article key={r.id} className={CAIXA}>
            <p className="font-semibold">{NOMES_DA_ORIGEM[r.origemTipo]} · {r.situacao === 'PENDENTE' ? `em análise até ${quando(r.prazo)}`
              : r.situacao === 'DEFERIDO' ? 'aceito' : 'negado'}</p>
            {r.motivo && <p className="text-sm">Motivo: {r.motivo}</p>}
          </article>
        ))}
      </section>
      <section aria-label="Minhas denúncias" className="flex flex-col gap-3">
        <h2 className="text-xl font-semibold">Denúncias que fiz</h2>
        {denuncias.length === 0 && <p className="text-sm text-muted">Nenhuma denúncia.</p>}
        <ul className="flex flex-col gap-1 text-sm">
          {denuncias.map((d) => (
            <li key={d.protocolo}>Protocolo {d.protocolo.slice(0, 8)} · {d.categoria} · {d.analisada ? 'analisada' : 'em análise'}</li>
          ))}
        </ul>
      </section>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89, KAN-103
 * Título: Novo chamado
 * Objetivo: Abrir um chamado com categoria e descrição; na disputa, pedir o e-mail da outra parte não é possível, então
 *           pede o identificador do registro em disputa.
 * Decisões: A disputa vem com a outra parte já preenchida a partir do registro (ver DisputaDoRegistro).
 */
function NovoChamado({ aoAbrir }: { aoAbrir: () => void }) {
  const [categorias, setCategorias] = useState<{ codigo: string; rotulo: string }[]>([])
  const [categoria, setCategoria] = useState('')
  const [descricao, setDescricao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  useEffect(() => {
    opcoesDaLista('CATEGORIA_CHAMADO').then((l) => setCategorias(l.filter((c) => c.codigo !== 'DISPUTA'))).catch(() => setCategorias([]))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Abrir
   * Objetivo: Enviar o chamado.
   * Decisões: Limpa o formulário depois.
   */
  async function abrir(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await abrirChamado({ categoria, descricao })
      setDescricao('')
      setAviso('Chamado aberto. Você recebe a resposta nas notificações.')
      aoAbrir()
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <form aria-label="Novo chamado" onSubmit={(e) => void abrir(e)} className="flex flex-col gap-2">
      <label className="flex flex-col gap-1 text-sm font-semibold">Assunto
        <select value={categoria} onChange={(e) => setCategoria(e.target.value)} className={CAIXA}>
          <option value="">Escolha…</option>
          {categorias.map((c) => <option key={c.codigo} value={c.codigo}>{c.rotulo}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-sm font-semibold">Descrição
        <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} rows={3} className={CAIXA} />
      </label>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="text-sm">{aviso}</p>}
      <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Abrir chamado</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86
 * Título: Recorrer da decisão
 * Objetivo: Apresentar o recurso com os argumentos e, se quiser, os anexos (links), até o prazo.
 * Decisões: Usado nas sanções, na verificação recusada e na curadoria recusada.
 */
export function RecorrerDaDecisao({ origemTipo, origemId, ate, aoRecorrer }: { origemTipo: OrigemDoRecurso; origemId: string; ate?: string
  aoRecorrer?: () => void }) {
  const [aberto, setAberto] = useState(false)
  const [argumentos, setArgumentos] = useState('')
  const [anexos, setAnexos] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [feito, setFeito] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-86
   * Título: Enviar recurso
   * Objetivo: Enviar o recurso.
   * Decisões: Uma vez por decisão (a API recusa o segundo).
   */
  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await recorrer({ origemTipo, origemId, argumentos, anexos: anexos || undefined })
      setFeito(true)
      aoRecorrer?.()
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  if (feito) return <p role="status" className="text-sm">Recurso enviado. Outra pessoa do time vai analisar.</p>
  if (!aberto) {
    return <button type="button" onClick={() => setAberto(true)} className={CLASSE_LINK}>Recorrer{ate ? ` (até ${quando(ate)})` : ''}</button>
  }
  return (
    <form aria-label="Recorrer" onSubmit={(e) => void enviar(e)} className="mt-2 flex flex-col gap-2">
      <label className="flex flex-col gap-1 text-sm font-semibold">Seus argumentos
        <textarea value={argumentos} onChange={(e) => setArgumentos(e.target.value)} rows={3} className={CAIXA} />
      </label>
      <Campo rotulo="Novos documentos (links, opcional)" value={anexos} onChange={(e) => setAnexos(e.target.value)} />
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar recurso</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89, KAN-103
 * Título: Chamado
 * Objetivo: Mostrar as mensagens, responder e, depois de resolvido, avaliar o atendimento.
 * Decisões: A outra parte da disputa responde, mas não avalia.
 */
export function PaginaChamado() {
  const { id = '' } = useParams()
  const [c, setC] = useState<Chamado | null>(null)
  const [texto, setTexto] = useState('')
  const [nota, setNota] = useState(5)
  const [comentario, setComentario] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    meuChamado(id).then(setC).catch((e: unknown) => setErro(mensagemDe(e)))
  }, [id])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Agir no chamado
   * Objetivo: Executar a resposta ou a avaliação e atualizar a tela.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<Chamado>) {
    setErro(null)
    try {
      setC(await acao())
      setTexto('')
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  if (!c) return erro ? <AlertaErro>{erro}</AlertaErro> : <p>Carregando…</p>
  const fechado = c.situacao === 'FECHADO'
  return (
    <section className="flex flex-col gap-4">
      <Link to="/ajuda/chamados" className={CLASSE_LINK}>← Meus chamados</Link>
      <h1 className="font-display text-2xl font-semibold">{c.categoria}</h1>
      <p className="text-sm">{NOMES_DA_SITUACAO_DO_CHAMADO[c.situacao] ?? c.situacao} · aberto em {quando(c.criadoEm)}
        {c.outraParte ? ` · outra parte: ${c.outraParte}` : ''}{c.resultadoDisputa ? ` · resultado: ${c.resultadoDisputa === 'ACORDO' ? 'acordo' : 'em disputa'}` : ''}</p>
      <p className={`${CAIXA} whitespace-pre-line`}>{c.descricao}</p>
      <ol aria-label="Mensagens" className="flex flex-col gap-2">
        {c.mensagens.map((m) => (
          <li key={m.em + m.texto} className={`${CAIXA} ${m.equipe ? 'bg-primary-soft' : ''}`}>
            <p className="text-xs text-muted">{m.autor ?? 'Você'} · {quando(m.em)}</p>
            <p className="whitespace-pre-line">{m.texto}</p>
          </li>
        ))}
      </ol>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {!fechado && (
        <form aria-label="Responder" onSubmit={(e) => { e.preventDefault(); void agir(() => responderChamado(id, texto)) }} className="flex flex-col gap-2">
          <label className="flex flex-col gap-1 text-sm font-semibold">Sua mensagem
            <textarea value={texto} onChange={(e) => setTexto(e.target.value)} rows={3} className={CAIXA} />
          </label>
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar</button>
        </form>
      )}
      {c.situacao === 'RESOLVIDO' && !c.souOutraParte && (
        <form aria-label="Avaliar atendimento" onSubmit={(e) => { e.preventDefault(); void agir(() => avaliarChamado(id, nota, comentario)) }}
          className="flex flex-col gap-2">
          <label className="flex flex-col gap-1 text-sm font-semibold">Nota (1 a 5)
            <select value={nota} onChange={(e) => setNota(Number(e.target.value))} className={CAIXA}>
              {[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n}</option>)}
            </select>
          </label>
          <Campo rotulo="Comentário (opcional)" value={comentario} onChange={(e) => setComentario(e.target.value)} />
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Avaliar e fechar</button>
        </form>
      )}
      {c.nota && <p className="text-sm">Sua avaliação: {c.nota} de 5.</p>}
    </section>
  )
}
