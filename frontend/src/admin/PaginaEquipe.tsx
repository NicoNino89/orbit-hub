import { useEffect, useId, useState, type FormEvent } from 'react'
import { ErroApi } from '../api/cliente'
import { NOMES_DOS_PAPEIS, type PapelInterno } from '../api/contas'
import {
  alterarPapeis, buscarRevisao, confirmarRevisao, convidarMembro, desligarMembro, listarEquipe, redefinirSegundoFator,
  reenviarConvite, type Membro, type SituacaoRevisao,
} from '../api/equipe'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'
import { PAPEIS_INTERNOS } from './papeisInternos'

const SITUACOES: Record<Membro['situacao'], { texto: string; classe: string }> = {
  CONVIDADO: { texto: 'Convidado', classe: 'bg-info-soft text-info' },
  ATIVO: { texto: 'Ativo', classe: 'bg-ok-soft text-ok' },
  DESLIGADO: { texto: 'Desligado', classe: 'bg-surface-2 text-muted' },
}

const FORMATO_DATA = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeZone: 'America/Sao_Paulo' })

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Mensagem de erro
 * Objetivo: Extrair a mensagem para a pessoa a partir de um erro qualquer.
 * Decisões: A API já devolve mensagens de negócio.
 */
function mensagem(e: unknown) {
  return e instanceof ErroApi || e instanceof Error ? e.message : 'Não foi possível concluir.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Tela da equipe interna
 * Objetivo: Deixar o administrador convidar, trocar papéis, desligar e redefinir o segundo fator da equipe, e
 *           fazer a revisão periódica de acessos.
 * Decisões: Tudo numa tela, com a revisão no topo quando vencida. Desligar pede o motivo e uma segunda
 *           confirmação na própria tela (sem caixa de diálogo do navegador). Não existe campo de senha: o membro
 *           escolhe a dele pelo convite.
 */
export function PaginaEquipe() {
  const [membros, setMembros] = useState<Membro[] | null>(null)
  const [revisao, setRevisao] = useState<SituacaoRevisao | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    Promise.all([listarEquipe(), buscarRevisao()])
      .then(([lista, situacao]) => {
        if (!ativo) return
        setMembros(lista)
        setRevisao(situacao)
      })
      .catch((e: unknown) => { if (ativo) setErro(mensagem(e)) })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 12:10
   * Cartão Jira: KAN-75
   * Título: Recarregar com aviso
   * Objetivo: Mostrar o resultado de uma ação e recarregar a lista.
   * Decisões: Limpa o erro anterior.
   */
  function concluir(texto: string) {
    setErro(null)
    setAviso(texto)
    setVersao((v) => v + 1)
  }

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Equipe interna</h1>
        <p className="text-sm text-muted">
          Contas da equipe são separadas das contas de investidor e empreendedor e entram com senha e aplicativo
          autenticador. Toda ação aqui fica na trilha de auditoria.
        </p>
      </header>
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {revisao && <CartaoRevisao situacao={revisao} aoConfirmar={() => concluir('Revisão de acessos registrada.')} aoFalhar={setErro} />}
      <FormularioConvite aoConvidar={(m) => concluir(`Convite enviado para ${m.email}.`)} aoFalhar={setErro} />
      {membros && (
        <ul className="flex flex-col gap-3" aria-label="Membros da equipe">
          {membros.map((m) => (
            <LinhaMembro key={m.id} membro={m} aoMudar={concluir} aoFalhar={setErro} />
          ))}
        </ul>
      )}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Cartão da revisão de acessos
 * Objetivo: Mostrar quando foi a última revisão e quando vence a próxima, e registrar uma nova.
 * Decisões: Vencida aparece em destaque de alerta; antes de confirmar, o administrador ajusta papéis e desliga
 *           quem não deve continuar.
 */
function CartaoRevisao({ situacao, aoConfirmar, aoFalhar }: {
  situacao: SituacaoRevisao; aoConfirmar: () => void; aoFalhar: (m: string) => void
}) {
  const id = useId()
  const [observacao, setObservacao] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function confirmar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    try {
      await confirmarRevisao(observacao)
      setObservacao('')
      aoConfirmar()
    } catch (e) {
      aoFalhar(mensagem(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={confirmar} aria-labelledby={`${id}-titulo`}
      className={`flex flex-col gap-3 rounded-lg border p-4 ${situacao.vencida ? 'border-warn bg-warn-soft' : 'border-line bg-surface'}`}>
      <h2 id={`${id}-titulo`} className="font-semibold">Revisão de acessos (a cada {situacao.intervaloMeses} meses)</h2>
      <p className="text-sm">
        {situacao.ultimaEm
          ? `Última: ${FORMATO_DATA.format(new Date(situacao.ultimaEm))}, por ${situacao.ultimoRevisor ?? '—'}. `
          : 'Nenhuma revisão registrada ainda. '}
        {situacao.vencida
          ? 'A revisão está vencida: confira quem continua com cada papel e confirme.'
          : `Próxima até ${FORMATO_DATA.format(new Date(situacao.proximaEm))}.`}
      </p>
      <label htmlFor={`${id}-obs`} className="text-sm font-semibold">Observação (opcional)</label>
      <textarea id={`${id}-obs`} rows={2} maxLength={1000} value={observacao} onChange={(e) => setObservacao(e.target.value)}
        className="rounded-md border border-line bg-surface px-3 py-2" />
      <button type="submit" disabled={enviando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
        Confirmar revisão dos acessos atuais
      </button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Escolha de papéis internos
 * Objetivo: Marcar os papéis da equipe de um membro.
 * Decisões: Caixas de seleção, porque uma pessoa pode acumular papéis.
 */
function EscolhaDePapeis({ valor, aoMudar, legenda }: {
  valor: PapelInterno[]; aoMudar: (p: PapelInterno[]) => void; legenda: string
}) {
  return (
    <fieldset className="flex flex-col gap-1">
      <legend className="text-sm font-semibold">{legenda}</legend>
      <div className="grid gap-1 sm:grid-cols-2">
        {PAPEIS_INTERNOS.map((papel) => (
          <label key={papel} className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={valor.includes(papel)}
              onChange={(e) => aoMudar(e.target.checked ? [...valor, papel] : valor.filter((p) => p !== papel))} />
            {NOMES_DOS_PAPEIS[papel]}
          </label>
        ))}
      </div>
    </fieldset>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Formulário de convite
 * Objetivo: Convidar um membro novo com nome, e-mail e papéis.
 * Decisões: Pede um e-mail próprio da equipe; o servidor recusa e-mail já usado na plataforma.
 */
function FormularioConvite({ aoConvidar, aoFalhar }: { aoConvidar: (m: Membro) => void; aoFalhar: (m: string) => void }) {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [papeis, setPapeis] = useState<PapelInterno[]>([])
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (papeis.length === 0) {
      aoFalhar('Escolha pelo menos um papel.')
      return
    }
    setEnviando(true)
    try {
      const membro = await convidarMembro(nome, email, papeis)
      setNome('')
      setEmail('')
      setPapeis([])
      aoConvidar(membro)
    } catch (e) {
      aoFalhar(mensagem(e))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar} aria-label="Convidar membro" className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4">
      <h2 className="font-semibold">Convidar membro</h2>
      <div className="grid gap-3 sm:grid-cols-2">
        <Campo rotulo="Nome" required maxLength={120} value={nome} onChange={(e) => setNome(e.target.value)} />
        <Campo rotulo="E-mail da equipe" type="email" required maxLength={254} value={email}
          onChange={(e) => setEmail(e.target.value)} dica="Use um e-mail só para a equipe (por exemplo, o corporativo)." />
      </div>
      <EscolhaDePapeis legenda="Papéis" valor={papeis} aoMudar={setPapeis} />
      <button type="submit" disabled={enviando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Enviar convite</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:10
 * Cartão Jira: KAN-75
 * Título: Membro na lista
 * Objetivo: Mostrar um membro e as ações possíveis conforme a situação dele.
 * Decisões: O administrador não vê "desligar" nem "redefinir segundo fator" na própria conta (o servidor também
 *           recusa). Desligado não tem ações.
 */
function LinhaMembro({ membro, aoMudar, aoFalhar }: {
  membro: Membro; aoMudar: (texto: string) => void; aoFalhar: (m: string) => void
}) {
  const { estado } = useSessao()
  const proprio = estado.status === 'autenticado' && estado.usuario.id === membro.id
  const [editando, setEditando] = useState(false)
  const [papeis, setPapeis] = useState<PapelInterno[]>(membro.papeis)
  const [desligando, setDesligando] = useState(false)
  const [motivo, setMotivo] = useState('')
  const situacao = SITUACOES[membro.situacao]

  async function executar(acao: () => Promise<unknown>, texto: string) {
    try {
      await acao()
      setEditando(false)
      setDesligando(false)
      aoMudar(texto)
    } catch (e) {
      aoFalhar(mensagem(e))
    }
  }

  return (
    <li className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="font-semibold">{membro.nome}{proprio && <span className="text-muted"> (você)</span>}</p>
          <p className="text-sm text-muted">{membro.email}</p>
          <p className="text-sm">{membro.papeis.map((p) => NOMES_DOS_PAPEIS[p]).join(', ')}</p>
        </div>
        <div className="flex flex-col items-end gap-1 text-xs">
          <span className={`rounded px-2 py-0.5 font-semibold ${situacao.classe}`}>{situacao.texto}</span>
          {membro.situacao === 'ATIVO' && (
            <span className={membro.segundoFatorAtivo ? 'text-ok' : 'text-warn'}>
              {membro.segundoFatorAtivo ? 'Segundo fator ativo' : 'Segundo fator pendente'}
            </span>
          )}
          {membro.desligadoEm && <span className="text-muted">Desligado em {FORMATO_DATA_HORA.format(new Date(membro.desligadoEm))}</span>}
        </div>
      </div>

      {membro.situacao !== 'DESLIGADO' && !editando && !desligando && (
        <div className="flex flex-wrap gap-3 text-sm">
          <button type="button" className={CLASSE_LINK} onClick={() => setEditando(true)}>Trocar papéis</button>
          {membro.situacao === 'CONVIDADO' && (
            <button type="button" className={CLASSE_LINK}
              onClick={() => void executar(() => reenviarConvite(membro.id), `Convite reenviado para ${membro.email}.`)}>
              Reenviar convite
            </button>
          )}
          {!proprio && membro.situacao === 'ATIVO' && membro.segundoFatorAtivo && (
            <button type="button" className={CLASSE_LINK}
              onClick={() => void executar(() => redefinirSegundoFator(membro.id),
                `Segundo fator de ${membro.nome} redefinido. Ele configura de novo no próximo login.`)}>
              Redefinir segundo fator
            </button>
          )}
          {!proprio && (
            <button type="button" className="font-semibold text-crit underline-offset-2 hover:underline"
              onClick={() => setDesligando(true)}>
              Desligar
            </button>
          )}
        </div>
      )}

      {editando && (
        <div className="flex flex-col gap-3">
          <EscolhaDePapeis legenda={`Papéis de ${membro.nome}`} valor={papeis} aoMudar={setPapeis} />
          <p className="text-xs text-muted">Ao salvar, as sessões abertas do membro são encerradas para os papéis novos valerem já.</p>
          <div className="flex gap-3">
            <button type="button" className={CLASSE_BOTAO_PRIMARIO} disabled={papeis.length === 0}
              onClick={() => void executar(() => alterarPapeis(membro.id, papeis), `Papéis de ${membro.nome} atualizados.`)}>
              Salvar papéis
            </button>
            <button type="button" className={CLASSE_LINK} onClick={() => { setEditando(false); setPapeis(membro.papeis) }}>Cancelar</button>
          </div>
        </div>
      )}

      {desligando && (
        <div className="flex flex-col gap-3 rounded-md border border-crit bg-crit-soft p-3">
          <p className="text-sm text-crit">
            Desligar revoga o acesso de {membro.nome} agora e devolve os casos abertos à fila. Não dá para desfazer:
            para voltar, é preciso um convite novo.
          </p>
          <Campo rotulo="Motivo do desligamento" required maxLength={1000} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
          <div className="flex gap-3">
            <button type="button" disabled={motivo.trim() === ''}
              className="rounded-md bg-crit px-4 py-2 font-semibold text-on-primary disabled:opacity-60"
              onClick={() => void desligarMembro(membro.id, motivo)
                .then((r) => {
                  setDesligando(false)
                  aoMudar(`${membro.nome} foi desligado. ${r.casosDevolvidos} caso(s) voltaram para a fila.`)
                })
                .catch((e: unknown) => aoFalhar(mensagem(e)))}>
              Confirmar desligamento
            </button>
            <button type="button" className={CLASSE_LINK} onClick={() => { setDesligando(false); setMotivo('') }}>Cancelar</button>
          </div>
        </div>
      )}
    </li>
  )
}
