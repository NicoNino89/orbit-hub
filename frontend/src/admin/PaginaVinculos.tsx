import { useEffect, useState } from 'react'
import { declararVinculo, meusVinculos, removerVinculo, TIPOS_DE_VINCULO, type Vinculo, vinculosDaEquipe } from '../api/vinculos'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const TIPOS = Object.keys(TIPOS_DE_VINCULO) as Vinculo['tipo'][]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Meus vínculos
 * Objetivo: Deixar a pessoa da equipe declarar e remover as organizações, startups e pessoas com que tem interesse.
 * Decisões: Explica para que serve: casos com esses vínculos não são atribuídos a ela. Remover é para vínculo que
 *           deixou de existir.
 */
export function PaginaVinculos() {
  const [lista, setLista] = useState<Vinculo[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [tipo, setTipo] = useState<Vinculo['tipo']>('ORGANIZACAO')
  const [alvo, setAlvo] = useState('')
  const [descricao, setDescricao] = useState('')

  useEffect(() => {
    let ativo = true
    meusVinculos()
      .then((r) => { if (ativo) setLista(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:43
   * Cartão Jira: KAN-87
   * Título: Executar e recarregar
   * Objetivo: Rodar uma ação e recarregar a lista.
   * Decisões: Erro no alerta.
   */
  async function agir(acao: () => Promise<unknown>, limpar = false) {
    try {
      await acao()
      setErro(null)
      if (limpar) { setAlvo(''); setDescricao('') }
      setVersao((v) => v + 1)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Meus vínculos</h1>
        <p className="text-sm text-muted">
          Declare as organizações de que você é membro, as startups em que investe ou trabalha e as pessoas próximas que usam a
          plataforma. Casos que envolvem esses vínculos não são atribuídos a você. A lista é revisada na revisão periódica de
          acessos.
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <form aria-label="Declarar vínculo" className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4"
        onSubmit={(e) => { e.preventDefault(); void agir(() => declararVinculo(tipo, alvo, descricao), true) }}>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-semibold">Tipo</span>
          <select value={tipo} onChange={(e) => setTipo(e.target.value as Vinculo['tipo'])} className="rounded-md border border-line bg-surface px-3 py-2">
            {TIPOS.map((t) => <option key={t} value={t}>{TIPOS_DE_VINCULO[t].nome}</option>)}
          </select>
        </label>
        <Campo rotulo={TIPOS_DE_VINCULO[tipo].alvo} value={alvo} onChange={(e) => setAlvo(e.target.value)} required />
        <Campo rotulo="Qual é o vínculo" value={descricao} onChange={(e) => setDescricao(e.target.value)} required maxLength={200}
          placeholder="Por exemplo: sou sócia; investi na rodada seed; é minha irmã" />
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Declarar vínculo</button>
      </form>
      {lista && lista.length === 0 && <p className="text-sm text-muted">Nenhum vínculo declarado.</p>}
      {lista && lista.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Vínculos declarados">
          {lista.map((v) => (
            <li key={v.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-3 text-sm">
              <span>
                {TIPOS_DE_VINCULO[v.tipo].nome}: {v.alvo}
                <span className="block text-xs text-muted">{v.descricao} · desde {FORMATO_DATA_HORA.format(new Date(v.criadoEm))}</span>
              </span>
              <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => removerVinculo(v.id))}>Remover</button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Vínculos da equipe na revisão
 * Objetivo: Mostrar ao administrador os vínculos declarados por todos, para conferir na revisão periódica de acessos.
 * Decisões: Agrupados por membro; falha na carga não impede a revisão.
 */
export function VinculosDaEquipe() {
  const [lista, setLista] = useState<Vinculo[]>([])
  useEffect(() => {
    let ativo = true
    vinculosDaEquipe().then((r) => { if (ativo) setLista(r) }).catch(() => { if (ativo) setLista([]) })
    return () => { ativo = false }
  }, [])
  return (
    <section aria-label="Vínculos declarados pela equipe" className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-4 text-sm">
      <h2 className="font-semibold">Vínculos declarados pela equipe</h2>
      <p className="text-xs text-muted">Confira na revisão de acessos se as declarações continuam completas.</p>
      {lista.length === 0 ? <p className="text-muted">Ninguém declarou vínculos.</p> : (
        <ul className="flex flex-col gap-1">
          {lista.map((v) => (
            <li key={v.id}><span className="font-semibold">{v.membro}</span>: {TIPOS_DE_VINCULO[v.tipo].nome} {v.alvo} ({v.descricao})</li>
          ))}
        </ul>
      )}
    </section>
  )
}
