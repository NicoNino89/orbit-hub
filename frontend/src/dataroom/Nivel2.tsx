import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router'
import {
  baixarDocumento, baixarNda, confirmarNda, conteudoNivel2, type ConteudoNivel2, definirRegraDeConfidencialidade, type DocumentoDoDataRoom,
  documentosDoDataRoom, enviarAoDataRoom, type EventoDeAcesso, historicoNivel2, liberarNivel2, NOMES_DA_SITUACAO_DA_CONEXAO, type Nivel2ParaInvestidor,
  painelNivel2, type PainelNivel2, type RegraDeConfidencialidade, removerDoDataRoom, renovarNivel2, revogarNivel2, situacaoNivel2, substituirNoDataRoom,
} from '../api/catalogo'
import { opcoesDaLista } from '../api/organizacoes'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { ApresentacaoEmPaginas } from '../catalogo/ApresentacaoEmPaginas'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const NOMES_DO_EVENTO: Record<string, string> = {
  LIBERACAO: 'Liberação', RENOVACAO: 'Renovação', REVOGACAO: 'Revogação', ENCERRAMENTO: 'Encerramento automático',
  VISUALIZACAO: 'Visualização', DOWNLOAD: 'Download', NDA_CONFIRMADO: 'NDA confirmado',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-145, KAN-32
 * Título: Salvar arquivo
 * Objetivo: Entregar ao navegador o arquivo recebido como Blob.
 * Decisões: Link temporário de objeto, removido logo depois.
 */
function salvarArquivo(arquivo: Blob, nome: string) {
  const endereco = URL.createObjectURL(arquivo)
  const a = document.createElement('a')
  a.href = endereco
  a.download = nome
  a.click()
  URL.revokeObjectURL(endereco)
}

async function tentar(acao: () => Promise<unknown>): Promise<string | null> {
  try {
    await acao()
    return null
  } catch (e) {
    return e instanceof Error ? e.message : 'Não foi possível concluir.'
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-145
 * Título: Data Room da equipe
 * Objetivo: Enviar documentos por categoria, substituir (mantendo a versão anterior) e remover.
 * Decisões: Só quem edita a seção Data Room envia; o servidor confere formato e tamanho.
 */
function DataRoomDaEquipe({ startupId, aoMudar }: { startupId: string; aoMudar: () => void }) {
  const [docs, setDocs] = useState<DocumentoDoDataRoom[]>([])
  const [categorias, setCategorias] = useState<{ codigo: string; rotulo: string }[]>([])
  const [categoria, setCategoria] = useState('')
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => { documentosDoDataRoom(startupId).then(setDocs).catch(() => undefined) }, [startupId, versao])
  useEffect(() => { opcoesDaLista('CATEGORIA_DATAROOM').then(setCategorias).catch(() => undefined) }, [])
  const pronto = (f: string | null) => { setErro(f); setVersao((v) => v + 1); aoMudar() }

  return (
    <section aria-label="Data Room" className="flex flex-col gap-3">
      <h2 className="font-semibold">Data Room</h2>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <form aria-label="Enviar documento" className="flex flex-wrap items-end gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          if (!arquivo) return
          void tentar(() => enviarAoDataRoom(startupId, arquivo, categoria, nome, descricao)).then((f) => {
            pronto(f)
            if (!f) { setNome(''); setDescricao(''); setArquivo(null) }
          })
        }}>
        <label className="flex flex-col gap-1">
          Categoria
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)} className="rounded-md border border-line px-2 py-1">
            <option value="">Escolha…</option>
            {categorias.map((c) => <option key={c.codigo} value={c.codigo}>{c.rotulo}</option>)}
          </select>
        </label>
        <Campo rotulo="Nome do documento" value={nome} maxLength={160} onChange={(e) => setNome(e.target.value)} />
        <Campo rotulo="Descrição curta" value={descricao} maxLength={300} onChange={(e) => setDescricao(e.target.value)} />
        <label className="flex flex-col gap-1">
          Arquivo
          <input type="file" aria-label="Arquivo do documento" onChange={(e) => setArquivo(e.target.files?.[0] ?? null)} />
        </label>
        <button type="submit" className={CLASSE_BOTAO_PRIMARIO} disabled={!arquivo || !categoria || !nome.trim()}>Enviar</button>
      </form>
      {docs.length === 0 && <p className="text-muted">Nenhum documento ainda.</p>}
      <ul aria-label="Documentos" className="flex flex-col gap-1">
        {docs.map((d) => (
          <li key={d.id} className={`flex flex-wrap items-center gap-2 ${d.atual ? '' : 'text-muted'}`}>
            <span className="flex-1">
              <strong>{d.nome}</strong> · {d.categoriaRotulo} · v{d.versao}{d.atual ? ' (atual)' : ' (anterior)'} · {FORMATO_DATA_HORA.format(new Date(d.enviadoEm))}
              {d.descricao && <span className="block text-xs">{d.descricao}</span>}
            </span>
            {d.atual && (
              <>
                <label className={CLASSE_LINK}>
                  Substituir
                  <input type="file" className="sr-only" aria-label={`Nova versão de ${d.nome}`}
                    onChange={(e) => { const f = e.target.files?.[0]; e.target.value = ''; if (f) void tentar(() => substituirNoDataRoom(startupId, d.id, f)).then(pronto) }} />
                </label>
                <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => removerDoDataRoom(startupId, d.id)).then(pronto)}>Remover</button>
              </>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-30, KAN-31, KAN-33, KAN-63, KAN-145
 * Título: Confidencial (equipe)
 * Objetivo: Reunir na aba Confidencial a regra (termo da plataforma ou NDA), o Data Room, as conexões que podem receber o
 *           Nível 2 (com selo e o que falta), liberar com prazo, renovar, revogar, confirmar o NDA e o histórico.
 * Decisões: Só o fundador administrador muda a regra e libera; o histórico é só leitura.
 */
export function ConfidencialDaStartup({ startupId, admin }: { startupId: string; admin: boolean }) {
  const [painel, setPainel] = useState<PainelNivel2 | null>(null)
  const [historico, setHistorico] = useState<EventoDeAcesso[]>([])
  const [dias, setDias] = useState<Record<string, string>>({})
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const recarregar = useCallback((f: string | null) => { setErro(f); setVersao((v) => v + 1) }, [])
  useEffect(() => {
    let ativo = true
    painelNivel2(startupId).then((p) => { if (ativo) setPainel(p) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    historicoNivel2(startupId).then((h) => { if (ativo) setHistorico(h) }).catch(() => undefined)
    return () => { ativo = false }
  }, [startupId, versao])

  if (!painel) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const prazo = (id: string) => (dias[id] ? Number(dias[id]) : null)
  return (
    <section aria-label="Confidencial" className="flex flex-col gap-5 text-sm">
      <p className="rounded-lg border border-line bg-surface p-3 text-muted">
        Nenhum investidor vê as informações confidenciais sem a sua liberação, mesmo depois do Match. O acesso termina sozinho no
        prazo, quando a conexão termina, se você bloquear o investidor ou revogar.
      </p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <fieldset className="flex flex-col gap-1">
        <legend className="font-semibold">Regra para liberar</legend>
        {([['TERMO_PLATAFORMA', 'Termo de confidencialidade da plataforma (aceito pelo investidor no cadastro)'],
          ['NDA_BILATERAL', 'NDA bilateral (envie o NDA no Data Room, categoria NDA; as duas partes confirmam)']] as [RegraDeConfidencialidade, string][]).map(([valor, texto]) => (
          <label key={valor} className="flex items-center gap-2">
            <input type="radio" name="regra" checked={painel.regra === valor} disabled={!admin}
              onChange={() => void tentar(() => definirRegraDeConfidencialidade(startupId, valor)).then(recarregar)} />
            {texto}
          </label>
        ))}
        {painel.regra === 'NDA_BILATERAL' && !painel.ndaEnviado && <p className="text-warn">O NDA ainda não foi enviado.</p>}
      </fieldset>

      <DataRoomDaEquipe startupId={startupId} aoMudar={() => recarregar(null)} />

      <section aria-label="Acessos ao Nível 2" className="flex flex-col gap-2">
        <h2 className="font-semibold">Investidores com conexão aberta</h2>
        {painel.conexoes.length === 0 && <p className="text-muted">Nenhuma conexão em Match, em análise ou em negociação.</p>}
        <ul className="flex flex-col gap-2">
          {painel.conexoes.map((c) => (
            <li key={c.conexaoId} className="flex flex-col gap-1 rounded-lg border border-line p-3">
              <span className="flex flex-wrap justify-between gap-2">
                <strong>{c.investidor}</strong>
                <span className="text-xs text-muted">{NOMES_DA_SITUACAO_DA_CONEXAO[c.situacao]} · {c.verificado ? 'verificado' : 'não verificado'}</span>
              </span>
              {c.acesso ? (
                <span className="flex flex-wrap items-center gap-2">
                  Acesso até {FORMATO_DATA_HORA.format(new Date(c.acesso.validoAte))}
                  {admin && (
                    <>
                      <input type="number" min={7} max={365} aria-label={`Dias para ${c.investidor}`} placeholder={String(painel.validadePadraoDias)}
                        value={dias[c.conexaoId] ?? ''} onChange={(e) => setDias({ ...dias, [c.conexaoId]: e.target.value })}
                        className="w-20 rounded-md border border-line px-2 py-1" />
                      <button type="button" className={CLASSE_LINK}
                        onClick={() => void tentar(() => renovarNivel2(startupId, c.acesso?.id ?? '', prazo(c.conexaoId))).then(recarregar)}>Renovar</button>
                      <button type="button" className={CLASSE_LINK}
                        onClick={() => void tentar(() => revogarNivel2(startupId, c.acesso?.id ?? '')).then(recarregar)}>Revogar</button>
                    </>
                  )}
                </span>
              ) : (
                <span className="flex flex-wrap items-center gap-2">
                  {c.falta && <span className="text-warn">{c.falta}</span>}
                  {painel.regra === 'NDA_BILATERAL' && painel.ndaEnviado && !c.ndaStartup && (
                    <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => confirmarNda(c.conexaoId)).then(recarregar)}>
                      Confirmar NDA pela startup
                    </button>
                  )}
                  {admin && (
                    <>
                      <input type="number" min={7} max={365} aria-label={`Dias para ${c.investidor}`} placeholder={String(painel.validadePadraoDias)}
                        value={dias[c.conexaoId] ?? ''} onChange={(e) => setDias({ ...dias, [c.conexaoId]: e.target.value })}
                        className="w-20 rounded-md border border-line px-2 py-1" />
                      <button type="button" className={CLASSE_BOTAO_PRIMARIO} disabled={!!c.falta}
                        onClick={() => void tentar(() => liberarNivel2(startupId, c.conexaoId, prazo(c.conexaoId))).then(recarregar)}>
                        Liberar informações confidenciais
                      </button>
                    </>
                  )}
                </span>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section aria-label="Histórico de acessos" className="flex flex-col gap-1">
        <h2 className="font-semibold">Histórico de acessos</h2>
        {historico.length === 0 && <p className="text-muted">Nenhum acesso registrado.</p>}
        <ol className="text-xs text-muted">
          {historico.map((h) => (
            <li key={h.em + h.tipo}>
              {FORMATO_DATA_HORA.format(new Date(h.em))}: {NOMES_DO_EVENTO[h.tipo] ?? h.tipo} · {h.investidor}
              {h.documento && ` · ${h.documento}`}{h.por && ` · por ${h.por}`}
            </li>
          ))}
        </ol>
      </section>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 12:04
 * Cartão Jira: KAN-32, KAN-63, KAN-145
 * Título: Informações confidenciais (investidor)
 * Objetivo: Na página da startup no catálogo, mostrar a área confidencial separada: o NDA para confirmar, o aviso do
 *           termo e, com acesso liberado, as informações e os documentos para baixar.
 * Decisões: Só aparece com conexão aberta. Baixar gera um link temporário a cada clique.
 */
export function AreaConfidencial({ startupId }: { startupId: string }) {
  const [situacao, setSituacao] = useState<Nivel2ParaInvestidor | null>(null)
  const [conteudo, setConteudo] = useState<ConteudoNivel2 | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const ler = useCallback(() => { situacaoNivel2(startupId).then(setSituacao).catch(() => setSituacao(null)) }, [startupId])
  useEffect(() => { ler() }, [ler])

  if (!situacao || !situacao.conexaoId) return null
  return (
    <section aria-label="Informações confidenciais" className="flex flex-col gap-3 rounded-lg border-2 border-dashed border-primary p-4 text-sm">
      <h2 className="font-semibold">Informações confidenciais (Nível 2)</h2>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {!situacao.termoAceito && situacao.regra === 'TERMO_PLATAFORMA' && (
        <p className="text-warn">Aceite a versão atual do <Link to="/termos" className={CLASSE_LINK}>Termo de Confidencialidade</Link> para acessar.</p>
      )}
      {situacao.regra === 'NDA_BILATERAL' && situacao.ndaDisponivel && (
        <div className="flex flex-wrap items-center gap-3">
          <button type="button" className={CLASSE_LINK} onClick={() => void baixarNda(startupId).then((b) => salvarArquivo(b, 'nda.pdf')).catch(() => undefined)}>
            Baixar o NDA da startup
          </button>
          {situacao.ndaConfirmado ? <span className="text-ok">NDA confirmado por você</span> : (
            <button type="button" className={CLASSE_LINK} onClick={() => void tentar(() => confirmarNda(situacao.conexaoId ?? '')).then((f) => { setErro(f); ler() })}>
              Confirmo que li e aceito o NDA
            </button>
          )}
        </div>
      )}
      {!situacao.temAcesso && <p className="text-muted">A startup ainda não liberou o acesso para você.</p>}
      {situacao.temAcesso && !conteudo && (
        <button type="button" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}
          onClick={() => void conteudoNivel2(startupId).then(setConteudo).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Sem acesso.'))}>
          Ver informações confidenciais
        </button>
      )}
      {conteudo && (
        <>
          <p className="text-xs text-muted">Acesso até {FORMATO_DATA_HORA.format(new Date(conteudo.validoAte))}. Cada visualização e download é registrado.</p>
          <ApresentacaoEmPaginas apresentacao={conteudo.informacoes} />
          <ul aria-label="Documentos confidenciais" className="flex flex-col gap-1">
            {conteudo.documentos.map((d) => (
              <li key={d.id} className="flex flex-wrap items-center gap-2">
                <span className="flex-1"><strong>{d.nome}</strong> · {d.categoria} · v{d.versao}{d.descricao && ` · ${d.descricao}`}</span>
                <button type="button" className={CLASSE_LINK}
                  onClick={() => void baixarDocumento(startupId, d.id).then((b) => salvarArquivo(b, d.nome)).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Não foi possível baixar.'))}>
                  Baixar
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
