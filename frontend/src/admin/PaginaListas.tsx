import { useEffect, useState, type FormEvent } from 'react'
import {
  definirItemAtivo, historicoDoItem, incluirItem, itensDaLista, renomearItem, resumoDasListas,
  type AlteracaoDeItem, type ItemDeLista, type ResumoDeLista,
} from '../api/listas'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const ACOES: Record<AlteracaoDeItem['acao'], string> = {
  INCLUIDO: 'Incluído', RENOMEADO: 'Renomeado', DESATIVADO: 'Desativado', REATIVADO: 'Reativado',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Tela das listas gerenciáveis
 * Objetivo: Deixar o administrador escolher uma lista e incluir, renomear, desativar ou reativar itens, com histórico.
 * Decisões: Não há botão de excluir: itens que não devem mais aparecer são desativados e continuam valendo nos
 *           registros antigos. O código de cada item aparece para referência, mas nunca muda.
 */
export function PaginaListas() {
  const [listas, setListas] = useState<ResumoDeLista[]>([])
  const [escolhida, setEscolhida] = useState('SEGMENTO')
  const [itens, setItens] = useState<ItemDeLista[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    Promise.all([resumoDasListas(), itensDaLista(escolhida)])
      .then(([r, i]) => { if (ativo) { setListas(r); setItens(i) } })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [escolhida, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:06
   * Cartão Jira: KAN-79
   * Título: Executar ação
   * Objetivo: Rodar uma ação sobre a lista, avisar o resultado e recarregar.
   * Decisões: Erros da API aparecem no alerta.
   */
  async function executar(acao: () => Promise<unknown>, texto: string) {
    try {
      await acao()
      setErro(null)
      setAviso(texto)
      setVersao((v) => v + 1)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }

  const atual = listas.find((l) => l.lista === escolhida)

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Listas</h1>
        <p className="text-sm text-muted">
          Opções usadas nos formulários e nas regras. Itens não são apagados: desative o que não deve mais aparecer,
          e os registros antigos continuam mostrando o item.
        </p>
      </header>
      <label className="flex max-w-md flex-col gap-1 text-sm font-semibold">
        Lista
        <select value={escolhida} onChange={(e) => { setAviso(null); setEscolhida(e.target.value) }}
          className="rounded-md border border-line bg-surface px-3 py-2 font-normal">
          {listas.map((l) => <option key={l.lista} value={l.lista}>{l.titulo} ({l.ativos} ativos)</option>)}
        </select>
      </label>
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {atual && (
        <FormularioInclusao comGrupo={atual.comGrupo}
          aoIncluir={(rotulo, grupo) => executar(() => incluirItem(escolhida, rotulo, grupo), `"${rotulo}" incluído.`)} />
      )}
      {itens && (
        <ul className="flex flex-col gap-2" aria-label="Itens da lista">
          {itens.map((item) => (
            <LinhaItem key={item.codigo} lista={escolhida} item={item} executar={executar} aoFalhar={setErro} />
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Formulário de inclusão
 * Objetivo: Incluir um item pelo rótulo (e grupo, nas listas que usam).
 * Decisões: O código é gerado no servidor.
 */
function FormularioInclusao({ comGrupo, aoIncluir }: {
  comGrupo: boolean; aoIncluir: (rotulo: string, grupo: string) => Promise<void>
}) {
  const [rotulo, setRotulo] = useState('')
  const [grupo, setGrupo] = useState('')

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    await aoIncluir(rotulo, grupo)
    setRotulo('')
  }

  return (
    <form onSubmit={enviar} aria-label="Incluir item" className="flex flex-wrap items-end gap-3 rounded-lg border border-line bg-surface p-4">
      <div className="min-w-60 flex-1">
        <Campo rotulo="Novo item" required maxLength={160} value={rotulo} onChange={(e) => setRotulo(e.target.value)} />
      </div>
      {comGrupo && (
        <div className="min-w-48">
          <Campo rotulo="Grupo" required maxLength={60} value={grupo} onChange={(e) => setGrupo(e.target.value)} />
        </div>
      )}
      <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Incluir</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Item na lista
 * Objetivo: Mostrar um item com as ações de renomear, desativar ou reativar e o histórico.
 * Decisões: Item desativado aparece esmaecido e com a etiqueta "Desativado".
 */
function LinhaItem({ lista, item, executar, aoFalhar }: {
  lista: string; item: ItemDeLista; executar: (a: () => Promise<unknown>, t: string) => Promise<void>
  aoFalhar: (m: string) => void
}) {
  const [renomeando, setRenomeando] = useState(false)
  const [rotulo, setRotulo] = useState(item.rotulo)
  const [historico, setHistorico] = useState<AlteracaoDeItem[] | null>(null)

  return (
    <li className={`flex flex-col gap-2 rounded-md border border-line bg-surface p-3 ${item.ativo ? '' : 'opacity-70'}`}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="font-semibold">{item.rotulo}</p>
          <p className="font-mono text-xs text-muted">{item.codigo}{item.grupo && ` · ${item.grupo}`}</p>
        </div>
        {!item.ativo && <span className="rounded bg-surface-2 px-2 py-0.5 text-xs font-semibold text-muted">Desativado</span>}
      </div>
      {renomeando ? (
        <form className="flex flex-wrap items-end gap-3" onSubmit={(e) => {
          e.preventDefault()
          void executar(() => renomearItem(lista, item.codigo, rotulo), `Item renomeado para "${rotulo}".`)
        }}>
          <div className="min-w-60 flex-1">
            <Campo rotulo="Novo rótulo" required maxLength={160} value={rotulo} onChange={(e) => setRotulo(e.target.value)} />
          </div>
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Salvar</button>
          <button type="button" className={CLASSE_LINK} onClick={() => setRenomeando(false)}>Cancelar</button>
        </form>
      ) : (
        <div className="flex flex-wrap gap-3 text-sm">
          <button type="button" className={CLASSE_LINK} onClick={() => setRenomeando(true)}>Renomear</button>
          <button type="button" className={CLASSE_LINK}
            onClick={() => void executar(() => definirItemAtivo(lista, item.codigo, !item.ativo),
              item.ativo ? `"${item.rotulo}" desativado.` : `"${item.rotulo}" reativado.`)}>
            {item.ativo ? 'Desativar' : 'Reativar'}
          </button>
          <button type="button" className={CLASSE_LINK}
            onClick={() => historicoDoItem(lista, item.codigo).then(setHistorico)
              .catch((e: unknown) => aoFalhar(e instanceof Error ? e.message : 'Não foi possível carregar o histórico.'))}>
            Histórico
          </button>
        </div>
      )}
      {historico && (
        historico.length === 0
          ? <p className="text-sm text-muted">Item inicial da plataforma, sem alterações.</p>
          : (
            <ul className="flex flex-col gap-1 text-sm text-muted" aria-label={`Histórico de ${item.codigo}`}>
              {historico.map((h) => (
                <li key={`${h.acao}-${h.ocorridoEm}`}>
                  {FORMATO_DATA_HORA.format(new Date(h.ocorridoEm))}: {ACOES[h.acao]}
                  {h.acao === 'RENOMEADO' && ` (${h.rotuloAnterior} → ${h.rotuloNovo})`}, por {h.autor}
                </li>
              ))}
            </ul>
          )
      )}
    </li>
  )
}
