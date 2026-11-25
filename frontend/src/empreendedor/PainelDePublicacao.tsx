import { useEffect, useState } from 'react'
import { FORMATO_DATA_HORA } from '../admin/datas'
import {
  cancelarEnvio, despublicar, enviarParaPublicacao, historicoDeSituacoes, type MudancaDeSituacao, type ProgressoDaStartup,
} from '../api/publicacao'
import type { RevisaoParaEquipe } from '../api/catalogo'
import { NOMES_DA_SITUACAO_DA_STARTUP, type SituacaoDaStartup } from '../api/startups'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { RevisoesDaCuradoria } from './RevisoesDaCuradoria'

const EXPLICACAO: Record<SituacaoDaStartup, string> = {
  RASCUNHO: 'Só a equipe vê. Preencha os obrigatórios e envie para a revisão da plataforma.',
  EM_REVISAO: 'A curadoria da plataforma está revisando. A startup aparece no catálogo depois de aprovada.',
  PUBLICADA: 'A startup está no catálogo. Alterações relevantes passam por nova revisão.',
  DEVOLVIDA: 'A curadoria pediu ajustes. Faça as correções e envie de novo.',
  DESPUBLICADA: 'A startup saiu do catálogo. Conexões e rodadas em andamento continuam visíveis para as partes. Envie de novo para voltar.',
  RECUSADA: 'A curadoria recusou a startup por indício de fraude, com segunda aprovação. Fale com o atendimento se discordar.',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Painel de publicação
 * Objetivo: Mostrar a situação da startup com a explicação, o que falta para enviar e o histórico, e oferecer ao
 *           fundador administrador enviar, cancelar o envio e despublicar.
 * Decisões: Despublicar pede confirmação, com motivo opcional. Os demais membros só acompanham. Mostra as revisões da
 *           curadoria e, na startup publicada com alteração devolvida, permite reenviar a alteração (KAN-66, 25/11/2026).
 */
export function PainelDePublicacao({ startupId, admin, progresso, revisoes = [], aoMudar }: {
  startupId: string
  admin: boolean
  progresso: ProgressoDaStartup
  revisoes?: RevisaoParaEquipe[]
  aoMudar: () => void
}) {
  const [historico, setHistorico] = useState<MudancaDeSituacao[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [confirmar, setConfirmar] = useState(false)
  const [motivo, setMotivo] = useState('')
  const situacao = progresso.situacao

  useEffect(() => {
    let ativo = true
    historicoDeSituacoes(startupId).then((h) => { if (ativo) setHistorico(h) }).catch(() => undefined)
    return () => { ativo = false }
  }, [startupId, situacao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-27
   * Título: Agir
   * Objetivo: Rodar uma ação de publicação e avisar a página para recarregar.
   * Decisões: Erro do servidor vai para o alerta.
   */
  async function agir(acao: () => Promise<unknown>) {
    try {
      await acao()
      setErro(null)
      setConfirmar(false)
      aoMudar()
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }

  return (
    <section aria-label="Publicação" className="flex flex-col gap-4">
      <div className="flex flex-col gap-1 rounded-lg border border-line bg-surface p-4 text-sm">
        <p><span className="font-semibold">Situação:</span> {NOMES_DA_SITUACAO_DA_STARTUP[situacao]}</p>
        <p className="text-muted">{EXPLICACAO[situacao]}</p>
      </div>
      {erro && <AlertaErro>{erro}</AlertaErro>}

      {progresso.pendencias.length > 0 && (
        <div className="flex flex-col gap-1 text-sm">
          <p className="font-semibold">Falta para enviar:</p>
          <ul className="list-disc pl-5" aria-label="Pendências">
            {progresso.pendencias.map((p) => <li key={p}>{p}</li>)}
          </ul>
        </div>
      )}

      {admin ? (
        <div className="flex flex-wrap items-center gap-3 text-sm">
          {(situacao === 'RASCUNHO' || situacao === 'DEVOLVIDA' || situacao === 'DESPUBLICADA') && (
            <button type="button" disabled={!progresso.podeEnviar} className={CLASSE_BOTAO_PRIMARIO}
              onClick={() => void agir(() => enviarParaPublicacao(startupId))}>
              Enviar para publicação
            </button>
          )}
          {situacao === 'EM_REVISAO' && (
            <button type="button" className={CLASSE_LINK} onClick={() => void agir(() => cancelarEnvio(startupId))}>Cancelar envio</button>
          )}
          {situacao === 'PUBLICADA' && revisoes[0]?.tipo === 'ALTERACAO' && revisoes[0].situacao === 'DEVOLVIDA' && (
            <button type="button" disabled={!progresso.podeEnviar} className={CLASSE_BOTAO_PRIMARIO}
              onClick={() => void agir(() => enviarParaPublicacao(startupId))}>
              Reenviar alteração para a curadoria
            </button>
          )}
          {situacao === 'PUBLICADA' && !confirmar && (
            <button type="button" className={CLASSE_LINK} onClick={() => setConfirmar(true)}>Despublicar</button>
          )}
          {confirmar && (
            <div role="group" aria-label="Confirmar despublicação" className="flex w-full flex-col gap-2 rounded-lg border border-line bg-surface p-3">
              <p>A startup sai do catálogo. Conexões e rodadas em andamento continuam visíveis para as partes.</p>
              <Campo rotulo="Motivo (opcional)" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={300} />
              <div className="flex gap-3">
                <button type="button" className="rounded-md bg-crit px-3 py-1 font-semibold text-white"
                  onClick={() => void agir(() => despublicar(startupId, motivo))}>Despublicar agora</button>
                <button type="button" className={CLASSE_LINK} onClick={() => setConfirmar(false)}>Cancelar</button>
              </div>
            </div>
          )}
        </div>
      ) : <p className="text-sm text-muted">Só o fundador administrador envia para publicação ou despublica.</p>}

      <RevisoesDaCuradoria revisoes={revisoes} />

      {historico.length > 0 && (
        <section aria-label="Histórico de situações" className="flex flex-col gap-1 text-sm">
          <h2 className="font-semibold">Histórico</h2>
          <ol className="flex flex-col gap-1 text-muted">
            {historico.map((h) => (
              <li key={h.em + h.para}>
                {FORMATO_DATA_HORA.format(new Date(h.em))}: {NOMES_DA_SITUACAO_DA_STARTUP[h.de]} → {NOMES_DA_SITUACAO_DA_STARTUP[h.para]}
                {h.pelaPlataforma && ' (pela plataforma)'}{h.motivo && ` · ${h.motivo}`}
              </li>
            ))}
          </ol>
        </section>
      )}
    </section>
  )
}
