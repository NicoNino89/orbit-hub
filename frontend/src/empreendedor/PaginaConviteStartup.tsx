import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ErroApi } from '../api/cliente'
import {
  aceitarConviteDaStartup, consultarConviteDaStartup, EXPLICACAO_DO_PAPEL_NA_STARTUP, NOMES_DAS_SECOES,
  NOMES_DO_PAPEL_NA_STARTUP, type ResumoConviteDaStartup,
} from '../api/startups'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Aceite do convite para a startup
 * Objetivo: Mostrar de qual startup é o convite, o papel e as seções, e aceitar.
 * Decisões: Fica na área logada. Sem o papel de empreendedor, explica e leva à conta para ativar.
 */
export function PaginaConviteStartup() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const navegar = useNavigate()
  const [convite, setConvite] = useState<ResumoConviteDaStartup | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [semPapel, setSemPapel] = useState(false)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let ativo = true
    consultarConviteDaStartup(token)
      .then((r) => { if (ativo) setConvite(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Convite inválido.') })
    return () => { ativo = false }
  }, [token])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 22:48
   * Cartão Jira: KAN-65
   * Título: Aceitar
   * Objetivo: Entrar na equipe e abrir a startup.
   * Decisões: Erro do servidor vai para o alerta.
   */
  async function aceitar() {
    setEnviando(true)
    try {
      const s = await aceitarConviteDaStartup(token)
      void navegar(`/empreendedor/startups/${s.id}`)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível aceitar.')
      setSemPapel(e instanceof ErroApi && e.codigo === 'sem_papel_empreendedor')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="mx-auto flex max-w-lg flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Convite para startup</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {semPapel && <Link to="/conta" className={CLASSE_LINK}>Ativar o papel de empreendedor em Minha conta</Link>}
      {convite && (
        <div className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-5">
          <p>
            Você foi convidado(a) para a equipe da <strong>{convite.startup}</strong> como{' '}
            <strong>{NOMES_DO_PAPEL_NA_STARTUP[convite.papel].toLowerCase()}</strong>.
          </p>
          <p className="text-sm text-muted">{EXPLICACAO_DO_PAPEL_NA_STARTUP[convite.papel]}. Convite para {convite.email}.</p>
          {convite.secoes.length > 0 && (
            <p className="text-sm">Seções que você poderá editar: {convite.secoes.map((s) => NOMES_DAS_SECOES[s]).join(', ')}.</p>
          )}
          <button type="button" onClick={() => void aceitar()} disabled={enviando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
            {enviando ? 'Entrando…' : 'Aceitar e entrar'}
          </button>
        </div>
      )}
    </section>
  )
}
