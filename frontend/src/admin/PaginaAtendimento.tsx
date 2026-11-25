import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  assumirChamado, chamadoDaEquipe, criarRespostaPadrao, escalarChamado, filaDeAtendimento, indicadoresDoAtendimento, NOMES_DA_SITUACAO_DO_CHAMADO,
  resolverChamado, responderComoEquipe, respostasPadrao, salvarArtigo, todosOsArtigos, type Artigo, type Chamado, type IndicadorDoAtendimento,
  type ItemDaFilaDeAtendimento, type NivelDeAtendimento, type RespostaPadrao,
} from '../api/atendimento'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

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
  return e instanceof Error ? e.message : 'Não foi possível concluir.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89
 * Título: Atendimento
 * Objetivo: Mostrar a fila dos níveis da pessoa, os indicadores por serviço, as respostas padrão e os artigos da
 *           central de ajuda.
 * Decisões: Respostas padrão só o líder (N2 ou administrador) cria.
 */
export function PaginaAtendimento() {
  const { estado } = useSessao()
  const papeis = estado.status === 'autenticado' ? estado.usuario.papeis : []
  const lider = papeis.includes('ADMIN') || papeis.includes('SUPORTE_N2')
  const [fila, setFila] = useState<ItemDaFilaDeAtendimento[]>([])
  const [indicadores, setIndicadores] = useState<IndicadorDoAtendimento[]>([])
  const [respostas, setRespostas] = useState<RespostaPadrao[]>([])
  const [artigos, setArtigos] = useState<Artigo[]>([])
  const [nova, setNova] = useState({ titulo: '', texto: '' })
  const [artigo, setArtigo] = useState<{ id?: string; titulo: string; conteudo: string; publicado: boolean }>({ titulo: '', conteudo: '', publicado: false })
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    filaDeAtendimento().then(setFila).catch((e: unknown) => setErro(mensagemDe(e)))
    indicadoresDoAtendimento().then(setIndicadores).catch(() => setIndicadores([]))
    respostasPadrao().then(setRespostas).catch(() => setRespostas([]))
    todosOsArtigos().then(setArtigos).catch(() => setArtigos([]))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Salvar
   * Objetivo: Executar o cadastro e recarregar.
   * Decisões: Mostra o erro da API.
   */
  async function salvar(acao: () => Promise<unknown>) {
    setErro(null)
    try {
      await acao()
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Atendimento</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <section aria-label="Fila de chamados" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Fila</h2>
        {fila.length === 0 && <p className="text-muted">Nenhum chamado em aberto nos seus níveis.</p>}
        {fila.map((c) => (
          <p key={c.id} className={`${CAIXA} ${c.vencido ? 'border-crit' : ''}`}>
            <Link to={`/admin/atendimento/${c.id}`} className={CLASSE_LINK}>{c.categoria}</Link> · {c.contato} · {c.nivel} · {NOMES_DA_SITUACAO_DO_CHAMADO[c.situacao]}
            <span className="text-sm text-muted"> · prazo {FORMATO_DATA_HORA.format(new Date(c.prazo))}{c.vencido ? ' (vencido)' : ''}{c.responsavel ? ` · com ${c.responsavel}` : ''}</span>
          </p>
        ))}
      </section>
      <section aria-label="Indicadores" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Indicadores por serviço</h2>
        <table className="text-left text-sm">
          <thead><tr><th>Serviço</th><th>Chamados</th><th>No prazo</th><th>Satisfação</th></tr></thead>
          <tbody>
            {indicadores.map((i) => (
              <tr key={i.servico}><td>{i.servico}</td><td>{i.chamados}</td><td>{i.chamados ? Math.round((100 * i.noPrazo) / i.chamados) : 0}%</td>
                <td>{i.satisfacao === null ? '—' : `${i.satisfacao.toFixed(1)} (${i.avaliacoes})`}</td></tr>
            ))}
          </tbody>
        </table>
      </section>
      <section aria-label="Respostas padrão" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Respostas padrão</h2>
        {respostas.map((r) => <details key={r.id} className={CAIXA}><summary>{r.titulo}</summary><p className="whitespace-pre-line text-sm">{r.texto}</p></details>)}
        {lider && (
          <form aria-label="Nova resposta padrão" onSubmit={(e) => { e.preventDefault(); void salvar(() => criarRespostaPadrao(nova.titulo, nova.texto)) }} className="flex flex-col gap-2">
            <Campo rotulo="Título da resposta" value={nova.titulo} onChange={(e) => setNova({ ...nova, titulo: e.target.value })} />
            <Campo rotulo="Texto da resposta" value={nova.texto} onChange={(e) => setNova({ ...nova, texto: e.target.value })} />
            <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Incluir resposta</button>
          </form>
        )}
      </section>
      <section aria-label="Central de ajuda" className="flex flex-col gap-2">
        <h2 className="text-xl font-semibold">Artigos da central de ajuda</h2>
        {artigos.map((a) => (
          <p key={a.id} className={CAIXA}>
            <button type="button" className={CLASSE_LINK} onClick={() => setArtigo({ id: a.id, titulo: a.titulo, conteudo: a.conteudo, publicado: a.publicado })}>{a.titulo}</button>
            {a.publicado ? '' : ' · rascunho'}
          </p>
        ))}
        <form aria-label="Artigo" onSubmit={(e) => { e.preventDefault(); void salvar(() => salvarArtigo(artigo)) }} className="flex flex-col gap-2">
          <Campo rotulo="Título do artigo" value={artigo.titulo} onChange={(e) => setArtigo({ ...artigo, titulo: e.target.value })} />
          <label className="flex flex-col gap-1 text-sm font-semibold">Conteúdo
            <textarea value={artigo.conteudo} onChange={(e) => setArtigo({ ...artigo, conteudo: e.target.value })} rows={4} className={CAIXA} />
          </label>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={artigo.publicado}
            onChange={(e) => setArtigo({ ...artigo, publicado: e.target.checked })} />Publicado</label>
          <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>{artigo.id ? 'Salvar artigo' : 'Incluir artigo'}</button>
        </form>
      </section>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89, KAN-103
 * Título: Chamado para a equipe
 * Objetivo: Mostrar o chamado com as notas internas e deixar assumir, responder (com resposta padrão), anotar, deixar
 *           aguardando o usuário, escalar e resolver (com o resultado da disputa).
 * Decisões: A resposta padrão só preenche o texto; quem atende ajusta antes de enviar.
 */
export function PaginaChamadoDaEquipe() {
  const { id = '' } = useParams()
  const [c, setC] = useState<Chamado | null>(null)
  const [respostas, setRespostas] = useState<RespostaPadrao[]>([])
  const [texto, setTexto] = useState('')
  const [interna, setInterna] = useState(false)
  const [aguardar, setAguardar] = useState(false)
  const [nivel, setNivel] = useState<NivelDeAtendimento>('N2')
  const [motivo, setMotivo] = useState('')
  const [resultado, setResultado] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    chamadoDaEquipe(id).then(setC).catch((e: unknown) => setErro(mensagemDe(e)))
    respostasPadrao().then(setRespostas).catch(() => setRespostas([]))
  }, [id])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Agir no chamado
   * Objetivo: Executar a ação e atualizar a tela.
   * Decisões: Limpa o texto depois de enviar.
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
  const disputa = c.categoriaCodigo === 'DISPUTA'
  return (
    <section className="flex flex-col gap-4">
      <Link to="/admin/atendimento" className={CLASSE_LINK}>← Fila</Link>
      <h1 className="font-display text-2xl font-semibold">{c.categoria}</h1>
      <p className="text-sm">{c.contato}{c.emailContato ? ` (${c.emailContato})` : ''} · {c.nivel} · {NOMES_DA_SITUACAO_DO_CHAMADO[c.situacao]}
        {c.outraParte ? ` · outra parte: ${c.outraParte}` : ''}{c.escalonamento ? ` · escalonamento: ${c.escalonamento}` : ''}</p>
      <p className={`${CAIXA} whitespace-pre-line`}>{c.descricao}</p>
      <ol aria-label="Mensagens" className="flex flex-col gap-2">
        {c.mensagens.map((m) => (
          <li key={m.em + m.texto} className={`${CAIXA} ${m.interna ? 'border-dashed' : ''}`}>
            <p className="text-xs text-muted">{m.autor} · {FORMATO_DATA_HORA.format(new Date(m.em))}{m.interna ? ' · nota interna' : ''}</p>
            <p className="whitespace-pre-line">{m.texto}</p>
          </li>
        ))}
      </ol>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {c.situacao !== 'FECHADO' && (
        <>
          <button type="button" onClick={() => void agir(() => assumirChamado(id))} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Assumir</button>
          <form aria-label="Responder" onSubmit={(e) => { e.preventDefault(); void agir(() => responderComoEquipe(id, texto, interna, aguardar)) }} className="flex flex-col gap-2">
            <select aria-label="Resposta padrão" onChange={(e) => setTexto(respostas.find((r) => r.id === e.target.value)?.texto ?? texto)} className={CAIXA}>
              <option value="">Resposta padrão…</option>
              {respostas.map((r) => <option key={r.id} value={r.id}>{r.titulo}</option>)}
            </select>
            <label className="flex flex-col gap-1 text-sm font-semibold">Mensagem
              <textarea value={texto} onChange={(e) => setTexto(e.target.value)} rows={3} className={CAIXA} />
            </label>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={interna} onChange={(e) => setInterna(e.target.checked)} />Nota interna</label>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={aguardar} onChange={(e) => setAguardar(e.target.checked)} />Aguardar o usuário</label>
            <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Enviar</button>
          </form>
          <form aria-label="Escalar" onSubmit={(e) => { e.preventDefault(); void agir(() => escalarChamado(id, nivel, motivo)) }} className="flex flex-col gap-2">
            <select aria-label="Nível" value={nivel} onChange={(e) => setNivel(e.target.value as NivelDeAtendimento)} className={CAIXA}>
              <option value="N2">N2</option><option value="ESPECIALISTA">Especialista</option>
            </select>
            <Campo rotulo="Motivo do escalonamento" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
            <button type="submit" className={`${CLASSE_LINK} self-start`}>Escalar</button>
          </form>
          <form aria-label="Resolver" onSubmit={(e) => { e.preventDefault(); void agir(() => resolverChamado(id, texto, disputa ? resultado : undefined)) }}
            className="flex flex-col gap-2">
            {disputa && (
              <select aria-label="Resultado da disputa" value={resultado} onChange={(e) => setResultado(e.target.value)} className={CAIXA}>
                <option value="">Resultado da disputa…</option>
                <option value="ACORDO">Acordo entre as partes</option>
                <option value="EM_DISPUTA">Sem acordo (registro em disputa)</option>
              </select>
            )}
            <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Resolver com a mensagem acima</button>
          </form>
        </>
      )}
    </section>
  )
}
