import { useEffect, useId, useState, type FormEvent } from 'react'
import {
  consultarAuditoria, listarAcoesAuditadas, listarAtoresAuditados, type FiltrosAuditoria, type PaginaAuditoria as Pagina,
} from '../api/auditoria'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'
import { FORMATO_DATA_HORA, inicioDoDia } from './datas'

const CLASSE_CAMPO = 'rounded-md border border-line bg-surface px-3 py-2 outline-none focus:ring-2 focus:ring-primary'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:10
 * Cartão Jira: KAN-80
 * Título: Tela da trilha de auditoria
 * Objetivo: Consultar a trilha por pessoa, período e tipo de ação, com paginação.
 * Decisões: Só leitura: não há botão de editar ou apagar. As listas dos filtros vêm da própria trilha.
 *           Cada consulta também fica registrada na trilha, e a tela avisa isso. O estado de carregamento
 *           muda nos eventos (filtrar, trocar de página), e o efeito só sincroniza com a API.
 */
export function PaginaAuditoria() {
  const id = useId()
  const [atores, setAtores] = useState<{ id: string; nome: string | null }[]>([])
  const [acoes, setAcoes] = useState<string[]>([])
  const [rascunho, setRascunho] = useState({ ator: '', acao: '', de: '', ate: '' })
  const [filtros, setFiltros] = useState<FiltrosAuditoria>({ pagina: 0 })
  const [pagina, setPagina] = useState<Pagina | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    listarAtoresAuditados().then(setAtores).catch(() => setAtores([]))
    listarAcoesAuditadas().then(setAcoes).catch(() => setAcoes([]))
  }, [])

  useEffect(() => {
    let ativo = true
    consultarAuditoria(filtros)
      .then((resultado) => { if (ativo) setPagina(resultado) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível consultar a trilha.') })
      .finally(() => { if (ativo) setCarregando(false) })
    return () => { ativo = false }
  }, [filtros])

  function consultar(novos: FiltrosAuditoria) {
    setCarregando(true)
    setErro(null)
    setFiltros(novos)
  }

  function aplicar(evento: FormEvent) {
    evento.preventDefault()
    consultar({
      ator: rascunho.ator || undefined,
      acao: rascunho.acao || undefined,
      de: rascunho.de ? inicioDoDia(rascunho.de) : undefined,
      ate: rascunho.ate ? inicioDoDia(rascunho.ate, 1) : undefined,
      pagina: 0,
    })
  }

  const totalPaginas = pagina ? Math.max(1, Math.ceil(pagina.total / pagina.tamanho)) : 1

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Trilha de auditoria</h1>
        <p className="text-sm text-muted">
          Ações da equipe interna, do mais recente para o mais antigo. Os registros não podem ser alterados nem
          apagados, e esta consulta também fica registrada.
        </p>
      </header>

      <form onSubmit={aplicar} className="grid gap-3 rounded-lg border border-line bg-surface p-4 sm:grid-cols-2 lg:grid-cols-5">
        <div className="flex flex-col gap-1">
          <label htmlFor={`${id}-ator`} className="text-sm font-semibold">Pessoa</label>
          <select id={`${id}-ator`} className={CLASSE_CAMPO} value={rascunho.ator}
            onChange={(e) => setRascunho({ ...rascunho, ator: e.target.value })}>
            <option value="">Todas</option>
            {atores.map((a) => <option key={a.id} value={a.id}>{a.nome ?? a.id}</option>)}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor={`${id}-acao`} className="text-sm font-semibold">Tipo de ação</label>
          <select id={`${id}-acao`} className={CLASSE_CAMPO} value={rascunho.acao}
            onChange={(e) => setRascunho({ ...rascunho, acao: e.target.value })}>
            <option value="">Todas</option>
            {acoes.map((a) => <option key={a} value={a}>{a}</option>)}
          </select>
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor={`${id}-de`} className="text-sm font-semibold">De</label>
          <input id={`${id}-de`} type="date" className={CLASSE_CAMPO} value={rascunho.de}
            onChange={(e) => setRascunho({ ...rascunho, de: e.target.value })} />
        </div>
        <div className="flex flex-col gap-1">
          <label htmlFor={`${id}-ate`} className="text-sm font-semibold">Até</label>
          <input id={`${id}-ate`} type="date" className={CLASSE_CAMPO} value={rascunho.ate}
            onChange={(e) => setRascunho({ ...rascunho, ate: e.target.value })} />
        </div>
        <div className="flex items-end">
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Filtrar</button>
        </div>
      </form>

      {erro && <AlertaErro>{erro}</AlertaErro>}

      {pagina && !erro && (
        <>
          <p className="text-sm text-muted" aria-live="polite">
            {carregando ? 'Carregando...' : `${pagina.total} registro(s)`}
          </p>
          {pagina.itens.length === 0 ? (
            <p>Nenhum registro com esses filtros.</p>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-line">
              <table className="w-full text-left text-sm">
                <thead className="bg-surface-2">
                  <tr>
                    <th scope="col" className="px-3 py-2">Quando</th>
                    <th scope="col" className="px-3 py-2">Quem</th>
                    <th scope="col" className="px-3 py-2">Ação</th>
                    <th scope="col" className="px-3 py-2">Objeto</th>
                    <th scope="col" className="px-3 py-2">Resultado</th>
                    <th scope="col" className="px-3 py-2">Detalhe</th>
                  </tr>
                </thead>
                <tbody>
                  {pagina.itens.map((r) => (
                    <tr key={r.id} className="border-t border-line align-top">
                      <td className="whitespace-nowrap px-3 py-2">{FORMATO_DATA_HORA.format(new Date(r.ocorridoEm))}</td>
                      <td className="px-3 py-2">{r.atorNome ?? 'Sistema'}</td>
                      <td className="px-3 py-2 font-mono text-xs">{r.acao}</td>
                      <td className="px-3 py-2">{r.objetoTipo ? `${r.objetoTipo} ${r.objetoId ?? ''}` : (r.objetoId ?? '—')}</td>
                      <td className="px-3 py-2">
                        <span className={`rounded px-2 py-0.5 text-xs font-semibold ${r.resultado === 'SUCESSO' ? 'bg-ok-soft text-ok' : 'bg-crit-soft text-crit'}`}>
                          {r.resultado === 'SUCESSO' ? 'Sucesso' : `Falha${r.statusHttp ? ` (${r.statusHttp})` : ''}`}
                        </span>
                      </td>
                      <td className="px-3 py-2">
                        {r.detalhe}
                        {r.justificativa && <p className="mt-1 text-xs text-muted">Justificativa: {r.justificativa}</p>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <nav aria-label="Páginas da trilha" className="flex items-center gap-3 text-sm">
            <button type="button" className="rounded-md border border-line px-3 py-1 font-semibold disabled:opacity-50"
              disabled={pagina.pagina === 0} onClick={() => consultar({ ...filtros, pagina: pagina.pagina - 1 })}>
              Anterior
            </button>
            <span>Página {pagina.pagina + 1} de {totalPaginas}</span>
            <button type="button" className="rounded-md border border-line px-3 py-1 font-semibold disabled:opacity-50"
              disabled={pagina.pagina + 1 >= totalPaginas} onClick={() => consultar({ ...filtros, pagina: pagina.pagina + 1 })}>
              Próxima
            </button>
          </nav>
        </>
      )}
    </section>
  )
}
