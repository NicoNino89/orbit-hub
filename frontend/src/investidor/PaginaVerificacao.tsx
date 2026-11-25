import { useCallback, useEffect, useState } from 'react'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { RecorrerDaDecisao } from '../ajuda/Ajuda'
import {
  enviarDocumento, mandarParaAnalise, minhaVerificacao, NOMES_DA_SITUACAO, removerDocumento, type Selo,
  type SituacaoDaVerificacao,
} from '../api/verificacoes'
import { useAtuacao } from '../sessao/atuacao'
import { useSessao } from '../sessao/contexto'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const ESTILO_DO_SELO: Record<Selo, { texto: string; classe: string }> = {
  VERIFICADO: { texto: 'Verificado', classe: 'bg-ok-soft text-ok' },
  EM_ANALISE: { texto: 'Em análise', classe: 'bg-warn-soft text-warn' },
  NAO_VERIFICADO: { texto: 'Não verificado', classe: 'bg-surface-2 text-muted' },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Selo de verificação
 * Objetivo: Mostrar o selo do perfil: verificado, em análise ou não verificado.
 * Decisões: Etiqueta com texto, não só cor, para quem não distingue cores.
 */
export function SeloDeVerificacao({ selo }: { selo: Selo }) {
  const estilo = ESTILO_DO_SELO[selo]
  return <span className={`rounded px-2 py-0.5 text-xs font-semibold ${estilo.classe}`}>{estilo.texto}</span>
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Verificação de identidade
 * Objetivo: Enviar os documentos exigidos, acompanhar o pedido e ver o selo, da pessoa ou da organização da atuação.
 * Decisões: Cada documento sobe sozinho, ao escolher o arquivo; "Enviar para análise" só aparece com todos os exigidos.
 *           Quem não pode enviar (membro de organização) vê a situação, sem os botões. Os documentos são dados pessoais:
 *           a tela explica o uso e a guarda. No complemento, mostra o prazo de resposta (KAN-83).
 */
export function PaginaVerificacao() {
  const { estado } = useSessao()
  const { atuacao, versao: versaoAtuacao } = useAtuacao(estado.status === 'autenticado' ? estado.usuario.id : null)
  const [situacao, setSituacao] = useState<SituacaoDaVerificacao | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [ocupado, setOcupado] = useState(false)

  useEffect(() => {
    let ativo = true
    minhaVerificacao()
      .then((s) => { if (ativo) setSituacao(s) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao, versaoAtuacao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:19
   * Cartão Jira: KAN-62
   * Título: Executar e recarregar
   * Objetivo: Rodar uma ação, mostrar o aviso e recarregar a situação.
   * Decisões: Erro do servidor no alerta.
   */
  const agir = useCallback(async (acao: () => Promise<unknown>, sucesso: string) => {
    setOcupado(true)
    try {
      await acao()
      setErro(null)
      setAviso(sucesso)
      setVersao((v) => v + 1)
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
    } finally {
      setOcupado(false)
    }
  }, [])

  if (!situacao) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const pedido = situacao.pedido
  const aceita = !pedido || pedido.situacao === 'RASCUNHO' || pedido.situacao === 'PENDENTE_COMPLEMENTO'
    || pedido.situacao === 'RECUSADO' || pedido.situacao === 'ENCERRADO' || pedido.situacao === 'VENCIDO'
  const emAberto = pedido && (pedido.situacao === 'RASCUNHO' || pedido.situacao === 'PENDENTE_COMPLEMENTO')
  const docs = emAberto ? situacao.documentos : []
  const completo = situacao.exigidos.every((e) => docs.some((d) => d.tipo === e.codigo))

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="flex items-center gap-3 font-display text-3xl font-semibold tracking-tight">
          Verificação <SeloDeVerificacao selo={situacao.selo} />
        </h1>
        <p className="text-sm text-muted">
          {situacao.sujeito === 'ORGANIZACAO' ? `Verificação de ${atuacao?.nome ?? 'organização'} (CNPJ e documentos da entidade).`
            : 'Verificação da sua identidade como pessoa física.'}{' '}
          Empreendedores podem aceitar manifestações só de investidores verificados, e valores de captação só aparecem
          para quem é verificado.
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      {pedido && (
        <div className="rounded-lg border border-line bg-surface p-4 text-sm">
          <p className="font-semibold">{NOMES_DA_SITUACAO[pedido.situacao]}</p>
          {pedido.observacao && <p className="mt-1">{pedido.observacao}</p>}
          {pedido.situacao === 'RECUSADO' && <RecorrerDaDecisao origemTipo="VERIFICACAO" origemId={pedido.id} />}
          {pedido.situacao === 'PENDENTE_COMPLEMENTO' && pedido.prazoComplementoAte && (
            <p className="text-warn">Envie o complemento até {FORMATO_DATA_HORA.format(new Date(pedido.prazoComplementoAte))}, ou o pedido é encerrado.</p>
          )}
          {pedido.validoAte && pedido.situacao === 'VERIFICADO' && (
            <p className="text-muted">Válida até {FORMATO_DATA_HORA.format(new Date(pedido.validoAte))}.</p>
          )}
        </div>
      )}

      {situacao.podeEnviar && aceita ? (
        <section aria-label="Documentos" className="flex flex-col gap-3">
          <h2 className="font-semibold">Documentos exigidos</h2>
          <ul className="flex flex-col gap-2">
            {situacao.exigidos.map((e) => {
              const doc = docs.find((d) => d.tipo === e.codigo)
              return (
                <li key={e.codigo} className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-line bg-surface p-3 text-sm">
                  <span>
                    {e.rotulo}
                    <span className="block text-xs text-muted">{doc ? `Enviado: ${doc.nome}` : 'Ainda não enviado'}</span>
                  </span>
                  <span className="flex items-center gap-3">
                    <label className={CLASSE_LINK}>
                      {doc ? 'Trocar arquivo' : 'Escolher arquivo'}
                      <input type="file" accept="application/pdf,image/jpeg,image/png" className="sr-only" disabled={ocupado}
                        aria-label={`Arquivo de ${e.rotulo}`}
                        onChange={(ev) => {
                          const arquivo = ev.target.files?.[0]
                          ev.target.value = ''
                          if (arquivo) void agir(() => enviarDocumento(e.codigo, arquivo), `${e.rotulo}: enviado.`)
                        }} />
                    </label>
                    {doc && (
                      <button type="button" className={CLASSE_LINK} disabled={ocupado}
                        onClick={() => void agir(() => removerDocumento(doc.id), `${e.rotulo}: removido.`)}>
                        Remover
                      </button>
                    )}
                  </span>
                </li>
              )
            })}
          </ul>
          <p className="text-xs text-muted">
            PDF, JPG ou PNG. Os documentos são dados pessoais: só você e a equipe de verificação acessam, e eles são
            apagados depois do prazo de guarda.
          </p>
          <button type="button" disabled={!completo || ocupado} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}
            onClick={() => void agir(() => mandarParaAnalise(), 'Pedido enviado para análise.')}>
            Enviar para análise
          </button>
        </section>
      ) : !situacao.podeEnviar && (
        <p role="note" className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
          Só o administrador da organização envia a verificação dela.
        </p>
      )}
    </section>
  )
}
