import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  abrirDocumento, assumirPedido, decidirPedido, type DetalheDoPedido, detalheDoPedido, filaDeVerificacao,
  type ItemDaFila,
} from '../api/filaDeVerificacao'
import { NOMES_DA_SITUACAO } from '../api/verificacoes'
import { declararImpedimentoNaVerificacao } from '../api/vinculos'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const NOMES_DO_SUJEITO: Record<ItemDaFila['sujeitoTipo'], string> = {
  PESSOA: 'Pessoa física', ORGANIZACAO: 'Organização', STARTUP: 'Startup', FUNDADOR: 'Fundador',
}

const DECISOES = [
  { valor: 'VERIFICADO', rotulo: 'Verificado', grupos: ['VERIFICADO'] },
  { valor: 'PENDENTE_COMPLEMENTO', rotulo: 'Pendente de complemento', grupos: ['COMPLEMENTO'] },
  { valor: 'RECUSADO', rotulo: 'Recusado', grupos: ['RECUSA', 'FRAUDE'] },
] as const

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Fila de verificação
 * Objetivo: Mostrar ao time de verificação os pedidos em análise por ordem de chegada, com o prazo e o responsável.
 * Decisões: Atrasado em destaque; o pedido abre na página de análise. Marca o conflito de quem vê e o pedido sem
 *           analista livre, que fica com o administrador (KAN-87).
 */
export function PaginaFilaDeVerificacao() {
  const [fila, setFila] = useState<ItemDaFila[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    let ativo = true
    filaDeVerificacao()
      .then((r) => { if (ativo) setFila(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Fila de verificação</h1>
        <p className="text-sm text-muted">Pedidos em análise, do mais antigo ao mais novo. Assuma um pedido para decidir.</p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {fila && fila.length === 0 && <p className="rounded-lg border border-line bg-surface p-5 text-muted">A fila está vazia.</p>}
      {fila && fila.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Pedidos em análise">
          {fila.map((i) => (
            <li key={i.id} className={`flex flex-wrap items-center justify-between gap-2 rounded-lg border bg-surface p-4 text-sm ${i.atrasado ? 'border-crit' : 'border-line'}`}>
              <span>
                <Link to={`/admin/verificacao/${i.id}`} className={CLASSE_LINK}>{i.sujeito}</Link>
                <span className="block text-xs text-muted">
                  {NOMES_DO_SUJEITO[i.sujeitoTipo]} · enviado em {FORMATO_DATA_HORA.format(new Date(i.enviadoEm))}
                  {i.responsavel ? ` · com ${i.responsavel}` : ' · sem responsável'}
                </span>
              </span>
              <span className="flex flex-wrap items-center gap-2">
                {i.conflitoComigo && <span className="rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Você tem conflito</span>}
                {i.semAnalistaLivre && <span className="rounded bg-surface-2 px-2 py-0.5 text-xs font-semibold">Sem analista livre: administrador</span>}
              </span>
              <span className={`text-xs font-semibold ${i.atrasado ? 'text-crit' : 'text-muted'}`}>
                {i.atrasado ? 'Atrasado' : 'Prazo'}: {FORMATO_DATA_HORA.format(new Date(i.prazo))}
              </span>
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
 * Título: Análise de um pedido
 * Objetivo: Assumir o pedido, abrir os documentos e decidir com motivo da lista e observação.
 * Decisões: Os motivos se filtram pela decisão escolhida; motivo de fraude avisa que o caso será encaminhado. O
 *           histórico do sujeito ajuda a ver tentativas anteriores. A pessoa pode se declarar impedida, com motivo
 *           (KAN-87).
 */
export function PaginaPedidoDeVerificacao() {
  const { id = '' } = useParams()
  const [detalhe, setDetalhe] = useState<DetalheDoPedido | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [decisao, setDecisao] = useState<(typeof DECISOES)[number]['valor']>('VERIFICADO')
  const [motivo, setMotivo] = useState('')
  const [observacao, setObservacao] = useState('')
  const [impedimento, setImpedimento] = useState('')

  useEffect(() => {
    let ativo = true
    detalheDoPedido(id)
      .then((r) => { if (ativo) setDetalhe(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:33
   * Cartão Jira: KAN-83
   * Título: Executar e recarregar
   * Objetivo: Rodar uma ação do analista, mostrar o aviso e recarregar o pedido.
   * Decisões: Erro do servidor no alerta.
   */
  const agir = useCallback(async (acao: () => Promise<unknown>, sucesso: string) => {
    try {
      await acao()
      setErro(null)
      setAviso(sucesso)
      setVersao((v) => v + 1)
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    }
  }, [])

  if (!detalhe) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const { pedido } = detalhe
  const grupos: readonly string[] = DECISOES.find((d) => d.valor === decisao)?.grupos ?? []
  const motivos = detalhe.motivos.filter((m) => grupos.includes(m.grupo))
  const fraude = detalhe.motivos.find((m) => m.codigo === motivo)?.grupo === 'FRAUDE'
  const emAnalise = pedido.situacao === 'EM_ANALISE'

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <Link to="/admin/verificacao" className={`${CLASSE_LINK} text-sm`}>← Fila de verificação</Link>
        <h1 className="font-display text-3xl font-semibold tracking-tight">{detalhe.sujeito}</h1>
        <p className="text-sm text-muted">
          {NOMES_DO_SUJEITO[detalhe.sujeitoTipo]} · {NOMES_DA_SITUACAO[pedido.situacao]}
          {detalhe.responsavel ? ` · responsável: ${detalhe.responsavel}` : ''}
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {pedido.encaminhadoModeracao && (
        <p className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">Encaminhado à moderação por indício de fraude.</p>
      )}

      <section aria-label="Documentos" className="flex flex-col gap-2">
        <h2 className="font-semibold">Documentos</h2>
        {pedido.documentosApagados && <p className="text-sm text-muted">Os documentos foram apagados ao fim do prazo de guarda.</p>}
        <ul className="flex flex-col gap-2">
          {detalhe.documentos.map((d) => (
            <li key={d.id} className="flex items-center justify-between gap-2 rounded-lg border border-line bg-surface p-3 text-sm">
              <span>{d.tipo}<span className="block text-xs text-muted">{d.nome}</span></span>
              <button type="button" className={CLASSE_LINK}
                onClick={() => void abrirDocumento(pedido.id, d.id).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Não foi possível abrir.'))}>
                Abrir
              </button>
            </li>
          ))}
        </ul>
        <p className="text-xs text-muted">Cada abertura de documento fica registrada na trilha de auditoria.</p>
      </section>

      {emAnalise && (
        <>
          <button type="button" className={`${CLASSE_LINK} self-start`} onClick={() => void agir(() => assumirPedido(pedido.id), 'Pedido assumido.')}>
            Assumir este pedido
          </button>
          <form aria-label="Decisão" className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4"
            onSubmit={(e) => { e.preventDefault(); void agir(() => decidirPedido(pedido.id, decisao, motivo, observacao), 'Decisão registrada.') }}>
            <fieldset className="flex flex-wrap gap-4 text-sm">
              <legend className="mb-1 font-semibold">Decisão</legend>
              {DECISOES.map((d) => (
                <label key={d.valor} className="flex items-center gap-1">
                  <input type="radio" name="decisao" checked={decisao === d.valor} onChange={() => { setDecisao(d.valor); setMotivo('') }} />
                  {d.rotulo}
                </label>
              ))}
            </fieldset>
            <label className="flex flex-col gap-1 text-sm">
              <span className="font-semibold">Motivo</span>
              <select value={motivo} required onChange={(e) => setMotivo(e.target.value)} className="rounded-md border border-line bg-surface px-3 py-2">
                <option value="">Escolha</option>
                {motivos.map((m) => <option key={m.codigo} value={m.codigo}>{m.rotulo}{m.grupo === 'FRAUDE' ? ' (fraude)' : ''}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span className="font-semibold">{decisao === 'PENDENTE_COMPLEMENTO' ? 'O que falta enviar (o usuário vê)' : 'Observação para o usuário (opcional)'}</span>
              <textarea value={observacao} onChange={(e) => setObservacao(e.target.value)} rows={3} maxLength={1000}
                required={decisao === 'PENDENTE_COMPLEMENTO'} className="rounded-md border border-line bg-surface px-3 py-2" />
            </label>
            {fraude && <p className="text-sm text-warn">O pedido será recusado e encaminhado à administração para a moderação.</p>}
            <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Registrar decisão</button>
          </form>
        </>
      )}

      {emAnalise && (
        <form aria-label="Impedimento" className="flex flex-wrap items-end gap-3 text-sm"
          onSubmit={(e) => { e.preventDefault(); void agir(() => declararImpedimentoNaVerificacao(pedido.id, impedimento), 'Impedimento registrado.') }}>
          <div className="min-w-64 flex-1">
            <label className="flex flex-col gap-1">
              <span className="font-semibold">Não posso analisar este caso porque…</span>
              <input value={impedimento} onChange={(e) => setImpedimento(e.target.value)} required maxLength={500}
                className="rounded-md border border-line bg-surface px-3 py-2" />
            </label>
          </div>
          <button type="submit" className={CLASSE_LINK}>Declarar impedimento</button>
        </form>
      )}

      {detalhe.historico.length > 0 && (
        <section aria-label="Histórico" className="flex flex-col gap-1 text-sm">
          <h2 className="font-semibold">Pedidos anteriores</h2>
          {detalhe.historico.map((h) => (
            <p key={h.id} className="text-muted">
              {FORMATO_DATA_HORA.format(new Date(h.criadoEm))}: {NOMES_DA_SITUACAO[h.situacao]}{h.motivo ? ` (${h.motivo})` : ''}
            </p>
          ))}
        </section>
      )}
    </section>
  )
}
