import { useEffect, useState } from 'react'
import { buscarAprovacoes, cancelarProposta, decidirProposta, type FilaDeAprovacoes, type Proposta } from '../api/aprovacoes'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const SITUACOES: Record<Proposta['situacao'], string> = {
  PENDENTE: 'Pendente', APROVADA: 'Aprovada', REJEITADA: 'Rejeitada', CANCELADA: 'Cancelada',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Tela de ações com segunda aprovação
 * Objetivo: Mostrar à equipe a fila de ações graves e deixar a segunda pessoa aprovar ou rejeitar.
 * Decisões: Pendentes primeiro (mais antigas no topo), com destaque para as vencidas; depois o histórico. Os
 *           botões aparecem só para quem pode decidir, e a própria proposta mostra "aguardando outra pessoa".
 */
export function PaginaAprovacoes() {
  const [fila, setFila] = useState<FilaDeAprovacoes | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    buscarAprovacoes()
      .then((f) => { if (ativo) setFila(f) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:52
   * Cartão Jira: KAN-81
   * Título: Concluir ação
   * Objetivo: Mostrar o resultado e recarregar a fila.
   * Decisões: Limpa o erro anterior.
   */
  function concluir(texto: string) {
    setErro(null)
    setAviso(texto)
    setVersao((v) => v + 1)
  }

  const pendentes = fila?.propostas.filter((p) => p.situacao === 'PENDENTE') ?? []
  const decididas = fila?.propostas.filter((p) => p.situacao !== 'PENDENTE') ?? []

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Segunda aprovação</h1>
        <p className="text-sm text-muted">
          Ações graves só acontecem quando outra pessoa aprova. Quem propõe não decide a própria proposta, e nada
          muda enquanto a proposta está pendente.
        </p>
      </header>
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}

      {fila && (
        <>
          <section aria-labelledby="pendentes" className="flex flex-col gap-3">
            <h2 id="pendentes" className="font-semibold">Pendentes ({pendentes.length})</h2>
            {pendentes.length === 0 && <p className="text-sm text-muted">Nenhuma proposta esperando decisão.</p>}
            <ul className="flex flex-col gap-3" aria-label="Propostas pendentes">
              {pendentes.map((p) => <CartaoProposta key={p.id} proposta={p} aoConcluir={concluir} aoFalhar={setErro} />)}
            </ul>
          </section>
          <section aria-labelledby="historico" className="flex flex-col gap-3">
            <h2 id="historico" className="font-semibold">Decididas recentemente</h2>
            {decididas.length === 0 && <p className="text-sm text-muted">Nada decidido ainda.</p>}
            <ul className="flex flex-col gap-2" aria-label="Propostas decididas">
              {decididas.map((p) => (
                <li key={p.id} className="rounded-md border border-line bg-surface p-3 text-sm">
                  <p><strong>{SITUACOES[p.situacao]}</strong>: {p.resumo}</p>
                  <p className="text-muted">
                    {p.tipoDescricao}. Proposta por {p.propostoPor}; decidida por {p.decididoPor ?? '—'}
                    {p.decididoEm && ` em ${FORMATO_DATA_HORA.format(new Date(p.decididoEm))}`}.
                  </p>
                  {p.justificativaDecisao && <p className="text-muted">Motivo: {p.justificativaDecisao}</p>}
                </li>
              ))}
            </ul>
          </section>
          <section aria-labelledby="acoes" className="flex flex-col gap-2 text-sm">
            <h2 id="acoes" className="font-semibold">Ações que exigem segunda aprovação</h2>
            <ul className="list-disc pl-5">
              {fila.acoes.map((a) => (
                <li key={a.acao}>{a.descricao}{!a.disponivel && <span className="text-muted"> (chega com a funcionalidade dela)</span>}</li>
              ))}
            </ul>
          </section>
        </>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Proposta pendente
 * Objetivo: Mostrar uma proposta pendente e as ações de quem pode decidir (ou cancelar, se for a própria).
 * Decisões: A justificativa da decisão é obrigatória e serve para aprovar ou rejeitar.
 */
function CartaoProposta({ proposta, aoConcluir, aoFalhar }: {
  proposta: Proposta; aoConcluir: (t: string) => void; aoFalhar: (m: string) => void
}) {
  const [justificativa, setJustificativa] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function executar(acao: () => Promise<unknown>, texto: string) {
    setEnviando(true)
    try {
      await acao()
      aoConcluir(texto)
    } catch (e) {
      aoFalhar(e instanceof Error ? e.message : 'Não foi possível concluir.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <li className={`flex flex-col gap-3 rounded-lg border p-4 ${proposta.vencida ? 'border-warn bg-warn-soft' : 'border-line bg-surface'}`}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="font-semibold">{proposta.resumo}</p>
          <p className="text-sm text-muted">{proposta.tipoDescricao}</p>
        </div>
        {proposta.vencida && <span className="rounded bg-warn px-2 py-0.5 text-xs font-semibold text-on-primary">Prazo vencido</span>}
      </div>
      <p className="text-sm">
        Proposta por <strong>{proposta.propostoPor}</strong> em {FORMATO_DATA_HORA.format(new Date(proposta.propostoEm))}.
      </p>
      <p className="text-sm">Justificativa: {proposta.justificativa}</p>
      {proposta.minha && (
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="text-muted">Aguardando outra pessoa analisar.</span>
          <button type="button" className={CLASSE_LINK} disabled={enviando}
            onClick={() => void executar(() => cancelarProposta(proposta.id), 'Proposta cancelada.')}>
            Cancelar proposta
          </button>
        </div>
      )}
      {proposta.possoDecidir && (
        <div className="flex flex-col gap-2">
          <Campo rotulo="Justificativa da decisão" required maxLength={1000} value={justificativa}
            onChange={(e) => setJustificativa(e.target.value)} />
          <div className="flex gap-3">
            <button type="button" className={CLASSE_BOTAO_PRIMARIO} disabled={enviando || justificativa.trim() === ''}
              onClick={() => void executar(() => decidirProposta(proposta.id, 'aprovacao', justificativa),
                'Proposta aprovada: a ação foi executada.')}>
              Aprovar e executar
            </button>
            <button type="button" className="rounded-md border border-line px-4 py-2 font-semibold disabled:opacity-60"
              disabled={enviando || justificativa.trim() === ''}
              onClick={() => void executar(() => decidirProposta(proposta.id, 'rejeicao', justificativa), 'Proposta rejeitada.')}>
              Rejeitar
            </button>
          </div>
        </div>
      )}
      {!proposta.minha && !proposta.possoDecidir && (
        <p className="text-sm text-muted">Seu papel não decide este tipo de ação.</p>
      )}
    </li>
  )
}
