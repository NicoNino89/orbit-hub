import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { FORMATO_DATA_HORA } from '../admin/datas'
import {
  cancelarConviteDaStartup, convidarParaStartup, type ConviteDaStartup, convitesDaStartup, equipeDaStartup,
  EXPLICACAO_DO_PAPEL_NA_STARTUP, type MembroDaStartup, NOMES_DAS_SECOES, NOMES_DO_PAPEL_NA_STARTUP, type PapelNaStartup,
  sairDaStartup, type SecaoDaStartup, trocarPapelNaStartup,
} from '../api/startups'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const PAPEIS = Object.keys(NOMES_DO_PAPEL_NA_STARTUP) as PapelNaStartup[]
const SECOES = Object.keys(NOMES_DAS_SECOES) as SecaoDaStartup[]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Equipe da startup
 * Objetivo: Mostrar os membros e, ao fundador administrador, convidar, trocar papel e seções e remover; a qualquer
 *           membro, sair da equipe.
 * Decisões: Mesmo desenho da página da organização (KAN-60). Sair pede confirmação, porque o acesso acaba na hora.
 */
export function EquipeDaStartup({ startupId, admin }: { startupId: string; admin: boolean }) {
  const { estado } = useSessao()
  const navegar = useNavigate()
  const eu = estado.status === 'autenticado' ? estado.usuario.id : ''
  const [membros, setMembros] = useState<MembroDaStartup[] | null>(null)
  const [convites, setConvites] = useState<ConviteDaStartup[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [editando, setEditando] = useState<string | null>(null)
  const [confirmarSaida, setConfirmarSaida] = useState(false)

  useEffect(() => {
    let ativo = true
    equipeDaStartup(startupId).then((m) => { if (ativo) setMembros(m) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    if (admin) convitesDaStartup(startupId).then((c) => { if (ativo) setConvites(c) }).catch(() => undefined)
    return () => { ativo = false }
  }, [startupId, admin, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Executar e recarregar
   * Objetivo: Rodar uma ação, mostrar o aviso e recarregar a equipe.
   * Decisões: Erro do servidor vai para o alerta.
   */
  const agir = useCallback(async (acao: () => Promise<unknown>, sucesso: string) => {
    try {
      await acao()
      setErro(null)
      setAviso(sucesso)
      setEditando(null)
      setVersao((v) => v + 1)
      return true
    } catch (e) {
      setAviso(null)
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir.')
      return false
    }
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Sair da equipe
   * Objetivo: Encerrar o próprio vínculo e voltar à área do empreendedor.
   * Decisões: O último administrador recebe a explicação do servidor.
   */
  async function sair() {
    try {
      await sairDaStartup(startupId, eu)
      void navegar('/empreendedor')
    } catch (e) {
      setConfirmarSaida(false)
      setErro(e instanceof Error ? e.message : 'Não foi possível sair.')
    }
  }

  if (!membros) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>

  return (
    <section className="flex flex-col gap-5" aria-label="Equipe">
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      <ul className="flex flex-col gap-2">
        {membros.map((m) => (
          <li key={m.usuarioId} className="flex flex-col gap-2 rounded-lg border border-line bg-surface p-3 text-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span>
                {m.nome}{m.usuarioId === eu && ' (você)'}
                <span className="block text-xs text-muted">
                  {NOMES_DO_PAPEL_NA_STARTUP[m.papel]} · desde {FORMATO_DATA_HORA.format(new Date(m.entrouEm))}
                  {m.papel === 'COLABORADOR' && ` · edita: ${m.secoes.map((s) => NOMES_DAS_SECOES[s]).join(', ')}`}
                </span>
              </span>
              {admin && (
                <span className="flex items-center gap-3">
                  <button type="button" className={CLASSE_LINK} onClick={() => setEditando(editando === m.usuarioId ? null : m.usuarioId)}>
                    Mudar papel
                  </button>
                  {m.usuarioId !== eu && (
                    <button type="button" className={CLASSE_LINK}
                      onClick={() => void agir(() => sairDaStartup(startupId, m.usuarioId), `${m.nome} foi removido(a) da equipe.`)}>
                      Remover
                    </button>
                  )}
                </span>
              )}
            </div>
            {editando === m.usuarioId && (
              <EscolhaDePapel inicialPapel={m.papel} inicialSecoes={m.secoes} rotulo={`Salvar papel de ${m.nome}`}
                aoEscolher={(papel, secoes) => agir(() => trocarPapelNaStartup(startupId, m.usuarioId, papel, secoes), 'Papel alterado.')} />
            )}
          </li>
        ))}
      </ul>

      {admin && (
        <section className="flex flex-col gap-3" aria-label="Convites">
          <h2 className="font-semibold">Convidar para a equipe</h2>
          <p className="text-sm text-muted">
            O convite vai por e-mail. Quem recebe precisa entrar (ou criar a conta) com esse e-mail e ter o papel de
            empreendedor.
          </p>
          <FormularioDeConvite aoConvidar={(email, papel, secoes) =>
            agir(() => convidarParaStartup(startupId, email, papel, secoes), `Convite enviado para ${email}.`)} />
          {convites.length > 0 && (
            <ul className="flex flex-col gap-2 text-sm" aria-label="Convites pendentes">
              {convites.map((c) => (
                <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-3">
                  <span>
                    {c.email} · {NOMES_DO_PAPEL_NA_STARTUP[c.papel]}
                    <span className="block text-xs text-muted">Vale até {FORMATO_DATA_HORA.format(new Date(c.expiraEm))}</span>
                  </span>
                  <button type="button" className={CLASSE_LINK}
                    onClick={() => void agir(() => cancelarConviteDaStartup(startupId, c.id), 'Convite cancelado.')}>
                    Cancelar convite
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <div className="flex flex-col gap-2 border-t border-line pt-4 text-sm">
        {confirmarSaida ? (
          <div className="flex flex-wrap items-center gap-3" role="group" aria-label="Confirmar saída">
            <span>Você perde na hora o acesso à startup. Confirmar?</span>
            <button type="button" onClick={() => void sair()} className="rounded-md bg-crit px-3 py-1 font-semibold text-white">Sair da equipe</button>
            <button type="button" onClick={() => setConfirmarSaida(false)} className={CLASSE_LINK}>Cancelar</button>
          </div>
        ) : (
          <button type="button" onClick={() => setConfirmarSaida(true)} className={`${CLASSE_LINK} self-start`}>Sair desta equipe</button>
        )}
      </div>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Formulário de convite
 * Objetivo: Pedir o e-mail, o papel e, para colaborador, as seções.
 * Decisões: Limpa o e-mail depois de enviar.
 */
function FormularioDeConvite({ aoConvidar }: {
  aoConvidar: (email: string, papel: PapelNaStartup, secoes: SecaoDaStartup[]) => Promise<boolean>
}) {
  const [email, setEmail] = useState('')
  return (
    <div className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4">
      <Campo rotulo="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      <EscolhaDePapel inicialPapel="FUNDADOR" inicialSecoes={[]} rotulo="Enviar convite"
        aoEscolher={(papel, secoes) => aoConvidar(email, papel, secoes).then((ok) => { if (ok) setEmail(''); return ok })} />
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Escolha de papel
 * Objetivo: Escolher o papel e, para colaborador, as seções que ele edita.
 * Decisões: As seções só aparecem para colaborador; o servidor exige pelo menos uma.
 */
function EscolhaDePapel({ inicialPapel, inicialSecoes, rotulo, aoEscolher }: {
  inicialPapel: PapelNaStartup
  inicialSecoes: SecaoDaStartup[]
  rotulo: string
  aoEscolher: (papel: PapelNaStartup, secoes: SecaoDaStartup[]) => Promise<boolean>
}) {
  const [papel, setPapel] = useState<PapelNaStartup>(inicialPapel)
  const [secoes, setSecoes] = useState<SecaoDaStartup[]>(inicialSecoes)
  return (
    <form className="flex flex-col gap-3 text-sm" aria-label={rotulo}
      onSubmit={(e) => { e.preventDefault(); void aoEscolher(papel, papel === 'COLABORADOR' ? secoes : []) }}>
      <label className="flex flex-col gap-1">
        <span className="font-semibold">Papel</span>
        <select value={papel} onChange={(e) => setPapel(e.target.value as PapelNaStartup)}
          className="self-start rounded-md border border-line bg-surface px-3 py-2">
          {PAPEIS.map((p) => <option key={p} value={p}>{NOMES_DO_PAPEL_NA_STARTUP[p]}</option>)}
        </select>
      </label>
      <p className="text-xs text-muted">{EXPLICACAO_DO_PAPEL_NA_STARTUP[papel]}.</p>
      {papel === 'COLABORADOR' && (
        <fieldset className="flex flex-col gap-1">
          <legend className="font-semibold">Seções que pode editar</legend>
          <div className="grid gap-1 sm:grid-cols-2">
            {SECOES.map((s) => (
              <label key={s} className="flex items-center gap-2">
                <input type="checkbox" checked={secoes.includes(s)}
                  onChange={(e) => setSecoes(e.target.checked ? [...secoes, s] : secoes.filter((x) => x !== s))} />
                {NOMES_DAS_SECOES[s]}
              </label>
            ))}
          </div>
        </fieldset>
      )}
      <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>{rotulo.startsWith('Salvar') ? 'Salvar papel' : rotulo}</button>
    </form>
  )
}
