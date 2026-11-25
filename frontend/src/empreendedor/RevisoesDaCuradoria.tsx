import { FORMATO_DATA_HORA } from '../admin/datas'
import { NOMES_DA_SITUACAO_DA_REVISAO, NOMES_DO_TIPO_DA_REVISAO, type RevisaoParaEquipe } from '../api/catalogo'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66
 * Título: Revisões da curadoria para a equipe
 * Objetivo: Mostrar à equipe o andamento e o resultado de cada revisão, com o parecer e os comentários por item e por
 *           seção.
 * Decisões: A revisão mais recente vem aberta; as anteriores, num resumo.
 */
export function RevisoesDaCuradoria({ revisoes }: { revisoes: RevisaoParaEquipe[] }) {
  if (revisoes.length === 0) return null
  const [atual, ...anteriores] = revisoes
  return (
    <section aria-label="Curadoria" className="flex flex-col gap-2 text-sm">
      <h2 className="font-semibold">Curadoria</h2>
      <div className={`flex flex-col gap-1 rounded-lg border p-3 ${atual.situacao === 'DEVOLVIDA' ? 'border-warn' : 'border-line'}`}>
        <p>
          <span className="font-semibold">{NOMES_DO_TIPO_DA_REVISAO[atual.tipo]}:</span> {NOMES_DA_SITUACAO_DA_REVISAO[atual.situacao]}
          <span className="text-muted"> · enviada em {FORMATO_DATA_HORA.format(new Date(atual.enviadaEm))}</span>
        </p>
        {atual.prazo && <p className="text-muted">Prazo previsto da análise: {FORMATO_DATA_HORA.format(new Date(atual.prazo))}</p>}
        {atual.parecer && <p className="whitespace-pre-line">{atual.parecer}</p>}
        {atual.apontamentos.length > 0 && (
          <ul aria-label="Comentários da curadoria" className="list-disc pl-5">
            {atual.apontamentos.map((a) => (
              <li key={a.tipo + a.codigo}><span className="font-semibold">{a.rotulo}:</span> {a.comentario ?? 'não atendido'}</li>
            ))}
          </ul>
        )}
      </div>
      {anteriores.length > 0 && (
        <ol className="flex flex-col gap-1 text-muted">
          {anteriores.map((r) => (
            <li key={r.id}>
              {FORMATO_DATA_HORA.format(new Date(r.enviadaEm))}: {NOMES_DO_TIPO_DA_REVISAO[r.tipo]} · {NOMES_DA_SITUACAO_DA_REVISAO[r.situacao]}
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
