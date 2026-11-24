import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { FORMATO_DATA_HORA } from '../admin/datas'
import {
  type Aviso, type Canal, EVENTO_AVISOS, lerPreferencias, listarAvisos, marcarLido, type PaginaDeAvisos,
  type Preferencias, salvarPreferencias,
} from '../api/notificacoes'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const NOMES_DOS_CANAIS: Record<Canal, string> = {
  AMBOS: 'Plataforma e e-mail',
  PLATAFORMA: 'Só na plataforma',
  EMAIL: 'Só por e-mail',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Central de notificações
 * Objetivo: Listar os avisos da pessoa, marcar como lidos e escolher como receber cada tipo.
 * Decisões: Uma tela com duas abas (avisos e preferências). Abrir o link de um aviso também o marca como lido.
 *           Avisos obrigatórios aparecem travados nas preferências, com a explicação.
 */
export function PaginaNotificacoes() {
  const [aba, setAba] = useState<'avisos' | 'preferencias'>('avisos')
  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-3">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Notificações</h1>
        <div role="tablist" aria-label="Notificações" className="flex gap-1 rounded-md bg-surface-2 p-1 text-sm self-start">
          {(['avisos', 'preferencias'] as const).map((a) => (
            <button key={a} type="button" role="tab" aria-selected={aba === a} onClick={() => setAba(a)}
              className={`rounded px-3 py-1 font-semibold ${aba === a ? 'bg-primary text-on-primary' : ''}`}>
              {a === 'avisos' ? 'Avisos' : 'Como receber'}
            </button>
          ))}
        </div>
      </header>
      {aba === 'avisos' ? <ListaDeAvisos /> : <FormularioDePreferencias />}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Lista de avisos
 * Objetivo: Mostrar os avisos paginados, com destaque para os não lidos.
 * Decisões: Depois de marcar, avisa o sino para recontar.
 */
function ListaDeAvisos() {
  const [pagina, setPagina] = useState(0)
  const [dados, setDados] = useState<PaginaDeAvisos | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    listarAvisos(pagina)
      .then((r) => { if (ativo) { setDados(r); setErro(null) } })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [pagina, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:34
   * Cartão Jira: KAN-146
   * Título: Marcar e atualizar
   * Objetivo: Marcar um aviso (ou todos) como lido e atualizar a lista e o sino.
   * Decisões: Erro aparece no alerta da tela.
   */
  async function marcar(id?: string) {
    try {
      await marcarLido(id)
      setVersao((v) => v + 1)
      window.dispatchEvent(new Event(EVENTO_AVISOS))
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível marcar.')
    }
  }

  if (erro) return <AlertaErro>{erro}</AlertaErro>
  if (!dados) return <p className="text-muted">Carregando…</p>
  const ultimaPagina = Math.max(Math.ceil(dados.total / 20) - 1, 0)
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between text-sm">
        <p className="text-muted">{dados.naoLidas > 0 ? `${dados.naoLidas} não lido(s)` : 'Tudo lido.'}</p>
        {dados.naoLidas > 0 && (
          <button type="button" onClick={() => void marcar()} className={CLASSE_LINK}>Marcar todos como lidos</button>
        )}
      </div>
      {dados.itens.length === 0 ? (
        <p className="rounded-lg border border-line bg-surface p-5 text-muted">Você ainda não tem avisos.</p>
      ) : (
        <ul className="flex flex-col gap-2" aria-label="Avisos">
          {dados.itens.map((a) => <ItemDeAviso key={a.id} aviso={a} aoMarcar={() => void marcar(a.id)} />)}
        </ul>
      )}
      {ultimaPagina > 0 && (
        <nav aria-label="Páginas" className="flex items-center gap-3 text-sm">
          <button type="button" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)} className={CLASSE_LINK}>Anteriores</button>
          <span className="text-muted">Página {pagina + 1} de {ultimaPagina + 1}</span>
          <button type="button" disabled={pagina >= ultimaPagina} onClick={() => setPagina(pagina + 1)} className={CLASSE_LINK}>Mais antigos</button>
        </nav>
      )}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Um aviso
 * Objetivo: Mostrar título, mensagem, data e ações de um aviso.
 * Decisões: Não lido tem borda de destaque e o botão "Marcar como lido".
 */
function ItemDeAviso({ aviso, aoMarcar }: { aviso: Aviso; aoMarcar: () => void }) {
  return (
    <li className={`flex flex-col gap-1 rounded-lg border bg-surface p-4 ${aviso.lida ? 'border-line' : 'border-primary'}`}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <p className="font-semibold">
          {!aviso.lida && <span className="sr-only">Não lido: </span>}
          {aviso.titulo}
        </p>
        <span className="text-xs text-muted">{FORMATO_DATA_HORA.format(new Date(aviso.criadaEm))}</span>
      </div>
      <p className="text-sm">{aviso.mensagem}</p>
      <div className="flex gap-4 text-sm">
        {aviso.link && <Link to={aviso.link} onClick={aviso.lida ? undefined : aoMarcar} className={CLASSE_LINK}>Abrir</Link>}
        {!aviso.lida && <button type="button" onClick={aoMarcar} className={CLASSE_LINK}>Marcar como lido</button>}
      </div>
    </li>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Preferências de aviso
 * Objetivo: Escolher o canal de cada tipo, agrupado, e o resumo diário dos e-mails.
 * Decisões: Obrigatórios ficam travados em "Plataforma e e-mail" com a explicação; o resumo não vale para eles.
 */
function FormularioDePreferencias() {
  const [prefs, setPrefs] = useState<Preferencias | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    let ativo = true
    lerPreferencias()
      .then((r) => { if (ativo) setPrefs(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:34
   * Cartão Jira: KAN-146
   * Título: Salvar
   * Objetivo: Gravar as escolhas e mostrar a confirmação.
   * Decisões: A resposta do servidor substitui o formulário.
   */
  async function salvar(e: React.FormEvent) {
    e.preventDefault()
    if (!prefs) return
    setSalvando(true)
    try {
      setPrefs(await salvarPreferencias(prefs))
      setErro(null)
      setAviso('Preferências salvas.')
    } catch (falha) {
      setAviso(null)
      setErro(falha instanceof Error ? falha.message : 'Não foi possível salvar.')
    } finally {
      setSalvando(false)
    }
  }

  if (!prefs) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const grupos = [...new Set(prefs.tipos.map((t) => t.grupo))]
  const mudar = (tipo: string, canal: Canal) =>
    setPrefs({ ...prefs, tipos: prefs.tipos.map((t) => (t.tipo === tipo ? { ...t, canal } : t)) })

  return (
    <form onSubmit={(e) => void salvar(e)} className="flex flex-col gap-5">
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {grupos.map((g) => (
        <fieldset key={g} className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-4">
          <legend className="px-1 font-semibold">{g}</legend>
          {prefs.tipos.filter((t) => t.grupo === g).map((t) => (
            <label key={t.tipo} className="flex flex-wrap items-center justify-between gap-2 text-sm">
              <span>
                {t.descricao}
                {t.obrigatoria && <span className="block text-xs text-muted">Aviso obrigatório: chega sempre pelos dois canais.</span>}
              </span>
              <select value={t.canal} disabled={t.obrigatoria} onChange={(e) => mudar(t.tipo, e.target.value as Canal)}
                className="rounded-md border border-line bg-surface px-2 py-1">
                {(Object.keys(NOMES_DOS_CANAIS) as Canal[]).map((c) => <option key={c} value={c}>{NOMES_DOS_CANAIS[c]}</option>)}
              </select>
            </label>
          ))}
        </fieldset>
      ))}
      <label className="flex items-start gap-2 text-sm">
        <input type="checkbox" checked={prefs.resumoDiario} onChange={(e) => setPrefs({ ...prefs, resumoDiario: e.target.checked })}
          className="mt-1" />
        <span>
          Receber os e-mails num resumo diário, às 7h
          <span className="block text-xs text-muted">Os avisos obrigatórios continuam chegando na hora.</span>
        </span>
      </label>
      <button type="submit" disabled={salvando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
        {salvando ? 'Salvando…' : 'Salvar preferências'}
      </button>
    </form>
  )
}
