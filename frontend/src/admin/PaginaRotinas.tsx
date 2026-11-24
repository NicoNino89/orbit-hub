import { useEffect, useState } from 'react'
import { requisitar } from '../api/cliente'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Execução de rotina
 * Objetivo: Tipar uma execução como a API devolve.
 * Decisões: Resultado EM_ANDAMENTO, SUCESSO ou FALHA.
 */
interface Execucao {
  disparo: 'AUTOMATICO' | 'MANUAL'
  iniciadaEm: string
  terminadaEm: string | null
  itens: number
  erros: number
  resultado: 'EM_ANDAMENTO' | 'SUCESSO' | 'FALHA'
  mensagem: string | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Rotina
 * Objetivo: Tipar uma rotina com status e última execução.
 * Decisões: Próxima execução calculada no servidor.
 */
interface Rotina {
  codigo: string
  descricao: string
  cron: string
  modulo: string
  parametros: string[]
  pausada: boolean
  proximaExecucao: string
  ultimaExecucao: Execucao | null
}

const RESULTADOS: Record<Execucao['resultado'], { texto: string; classe: string }> = {
  SUCESSO: { texto: 'Sucesso', classe: 'bg-ok-soft text-ok' },
  FALHA: { texto: 'Falha', classe: 'bg-crit-soft text-crit' },
  EM_ANDAMENTO: { texto: 'Em andamento', classe: 'bg-info-soft text-info' },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Tela das rotinas automáticas
 * Objetivo: Mostrar a última execução e o status de cada rotina e deixar pausar, retomar ou executar agora.
 * Decisões: Pausada aparece marcada e não roda no horário; executar agora funciona mesmo pausada. Toda ação fica na
 *           trilha de auditoria.
 */
export function PaginaRotinas() {
  const [rotinas, setRotinas] = useState<Rotina[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    requisitar<Rotina[]>('/api/v1/admin/rotinas')
      .then((r) => { if (ativo) setRotinas(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:17
   * Cartão Jira: KAN-104
   * Título: Ação sobre uma rotina
   * Objetivo: Pausar, retomar ou executar e recarregar a lista.
   * Decisões: A execução manual devolve itens e erros, mostrados no aviso.
   */
  async function agir(codigo: string, acao: 'pausa' | 'retomada' | 'execucao') {
    try {
      const r = await requisitar<{ itens: number; erros: number } | undefined>(`/api/v1/admin/rotinas/${codigo}/${acao}`, { metodo: 'POST' })
      setErro(null)
      setAviso(acao === 'execucao' && r ? `${codigo} executada: ${r.itens} item(ns), ${r.erros} erro(s).`
        : acao === 'pausa' ? `${codigo} pausada.` : `${codigo} retomada.`)
      setVersao((v) => v + 1)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Rotinas automáticas</h1>
        <p className="text-sm text-muted">
          Tarefas que a plataforma faz sozinha, no horário de Brasília. Falhas e atrasos geram alerta para o time.
        </p>
      </header>
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {rotinas && (
        <ul className="flex flex-col gap-3" aria-label="Rotinas">
          {rotinas.map((r) => {
            const ultima = r.ultimaExecucao
            return (
              <li key={r.codigo} className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold">{r.codigo}: {r.descricao}</p>
                    <p className="text-xs text-muted">
                      Módulo {r.modulo} · cron <span className="font-mono">{r.cron}</span>
                      {r.parametros.length > 0 && ` · parâmetros: ${r.parametros.join(', ')}`}
                    </p>
                  </div>
                  {r.pausada
                    ? <span className="rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Pausada</span>
                    : <span className="text-xs text-muted">Próxima: {FORMATO_DATA_HORA.format(new Date(r.proximaExecucao))}</span>}
                </div>
                <p className="text-sm">
                  {ultima ? (
                    <>
                      Última: {FORMATO_DATA_HORA.format(new Date(ultima.iniciadaEm))} ({ultima.disparo === 'MANUAL' ? 'manual' : 'automática'}){' '}
                      <span className={`rounded px-2 py-0.5 text-xs font-semibold ${RESULTADOS[ultima.resultado].classe}`}>
                        {RESULTADOS[ultima.resultado].texto}
                      </span>{' '}
                      {ultima.itens} item(ns){ultima.erros > 0 && `, ${ultima.erros} erro(s)`}
                      {ultima.mensagem && <span className="text-muted"> · {ultima.mensagem}</span>}
                    </>
                  ) : <span className="text-muted">Ainda não rodou.</span>}
                </p>
                <div className="flex gap-3 text-sm">
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(r.codigo, 'execucao')}>Executar agora</button>
                  <button type="button" className={CLASSE_LINK} onClick={() => void agir(r.codigo, r.pausada ? 'retomada' : 'pausa')}>
                    {r.pausada ? 'Retomar' : 'Pausar'}
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
