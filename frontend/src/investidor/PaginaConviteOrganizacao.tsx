import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { ErroApi } from '../api/cliente'
import { aceitarConvite, consultarConvite, PAPEIS_NA_ORGANIZACAO, type ResumoConvite } from '../api/organizacoes'
import { EVENTO_ORGANIZACOES } from '../sessao/atuacao'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Aceite do convite para organização
 * Objetivo: Mostrar de qual organização é o convite e o papel, e aceitar.
 * Decisões: Fica na área logada (quem não está logado entra e volta para cá). Sem o papel de investidor, explica e
 *           leva à conta para ativar.
 */
export function PaginaConviteOrganizacao() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const navegar = useNavigate()
  const [convite, setConvite] = useState<ResumoConvite | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [semPapel, setSemPapel] = useState(false)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let ativo = true
    consultarConvite(token)
      .then((r) => { if (ativo) setConvite(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Convite inválido.') })
    return () => { ativo = false }
  }, [token])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Aceitar
   * Objetivo: Entrar na organização e abrir a página dela.
   * Decisões: Avisa o seletor de atuação que a lista mudou.
   */
  async function aceitar() {
    setEnviando(true)
    try {
      const org = await aceitarConvite(token)
      window.dispatchEvent(new Event(EVENTO_ORGANIZACOES))
      void navegar(`/investidor/organizacoes/${org.id}`)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível aceitar.')
      setSemPapel(e instanceof ErroApi && e.codigo === 'sem_papel_investidor')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section className="mx-auto flex max-w-lg flex-col gap-4">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Convite para organização</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {semPapel && <Link to="/conta" className={CLASSE_LINK}>Ativar o papel de investidor em Minha conta</Link>}
      {convite && (
        <div className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-5">
          <p>
            Você foi convidado(a) para <strong>{convite.organizacao}</strong> como{' '}
            <strong>{PAPEIS_NA_ORGANIZACAO[convite.papel].nome.toLowerCase()}</strong>.
          </p>
          <p className="text-sm text-muted">{PAPEIS_NA_ORGANIZACAO[convite.papel].explicacao}. Convite para {convite.email}.</p>
          <button type="button" onClick={() => void aceitar()} disabled={enviando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
            {enviando ? 'Entrando…' : 'Aceitar e entrar'}
          </button>
        </div>
      )}
    </section>
  )
}
