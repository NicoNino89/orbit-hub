import { useEffect, useState } from 'react'
import { declararIndicador, escreverMes, escreverValor, type Indicador, indicadoresDaStartup } from '../api/indicadores'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Painel de indicadores
 * Objetivo: Mostrar os indicadores de tração com o valor atual, o mês de referência, a marca de desatualizado e o
 *           histórico, e declarar um novo valor.
 * Decisões: O aviso "declarado pelo empreendedor, não auditado" fica sempre à vista. O mês é obrigatório (campo de mês
 *           do navegador). Quem não edita a tração só vê.
 */
export function PainelDeIndicadores({ startupId, podeEditar }: { startupId: string; podeEditar: boolean }) {
  const [indicadores, setIndicadores] = useState<Indicador[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [aberto, setAberto] = useState<string | null>(null)
  const [novo, setNovo] = useState({ indicador: '', valor: '', referencia: '' })

  useEffect(() => {
    let ativo = true
    indicadoresDaStartup(startupId).then((i) => { if (ativo) setIndicadores(i) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [startupId])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:11
   * Cartão Jira: KAN-24
   * Título: Declarar
   * Objetivo: Enviar o novo valor e trocar o indicador na lista.
   * Decisões: Vírgula decimal aceita; o servidor confere o resto.
   */
  async function declarar() {
    try {
      const atualizado = await declararIndicador(startupId, novo.indicador, Number(novo.valor.replace(',', '.')), novo.referencia)
      setIndicadores((lista) => (lista ?? []).map((i) => (i.indicador === atualizado.indicador ? atualizado : i)))
      setNovo({ indicador: '', valor: '', referencia: '' })
      setErro(null)
      setAviso(`${atualizado.rotulo}: valor registrado.`)
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível registrar.')
    }
  }

  if (!indicadores) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando indicadores…</p>

  return (
    <section aria-label="Indicadores de tração" className="flex flex-col gap-3">
      <h2 className="font-semibold">Indicadores de tração</h2>
      <p className="rounded-md bg-surface-2 px-3 py-2 text-sm">
        Declarado pelo empreendedor, não auditado. A plataforma não verifica estes números.
      </p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      <ul className="flex flex-col gap-2 text-sm">
        {indicadores.map((i) => (
          <li key={i.indicador} className="flex flex-col gap-1 rounded-lg border border-line bg-surface p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="font-semibold">{i.rotulo}</span>
              {i.atual ? (
                <span className="flex items-center gap-2">
                  {escreverValor(i.formato, i.atual.valor)}
                  <span className="text-xs text-muted">ref. {escreverMes(i.atual.referencia)}</span>
                  {i.desatualizado && <span className="rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Desatualizado</span>}
                </span>
              ) : <span className="text-muted">Não informado</span>}
            </div>
            {i.historico.length > 1 && (
              <button type="button" className={`${CLASSE_LINK} self-start text-xs`} onClick={() => setAberto(aberto === i.indicador ? null : i.indicador)}>
                {aberto === i.indicador ? 'Esconder histórico' : `Ver histórico (${i.historico.length})`}
              </button>
            )}
            {aberto === i.indicador && (
              <ol aria-label={`Histórico de ${i.rotulo}`} className="flex flex-col gap-0.5 text-xs text-muted">
                {i.historico.map((h) => (
                  <li key={h.registradoEm}>{escreverMes(h.referencia)}: {escreverValor(i.formato, h.valor)}</li>
                ))}
              </ol>
            )}
          </li>
        ))}
      </ul>
      {podeEditar && (
        <form aria-label="Declarar indicador" className="flex flex-wrap items-end gap-3 rounded-lg border border-line bg-surface p-4"
          onSubmit={(e) => { e.preventDefault(); void declarar() }}>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-semibold">Indicador</span>
            <select value={novo.indicador} required onChange={(e) => setNovo({ ...novo, indicador: e.target.value })}
              className="rounded-md border border-line bg-surface px-3 py-2">
              <option value="">Escolha</option>
              {indicadores.map((i) => <option key={i.indicador} value={i.indicador}>{i.rotulo}</option>)}
            </select>
          </label>
          <Campo rotulo="Valor" value={novo.valor} required inputMode="decimal" onChange={(e) => setNovo({ ...novo, valor: e.target.value })} />
          <Campo rotulo="Mês de referência" type="month" value={novo.referencia} required
            onChange={(e) => setNovo({ ...novo, referencia: e.target.value })} />
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Registrar valor</button>
          <p className="w-full text-xs text-muted">Os valores anteriores ficam no histórico. Mais de 6 meses sem atualizar, o indicador aparece como desatualizado.</p>
        </form>
      )}
    </section>
  )
}
