import { useEffect, useState } from 'react'
import { Link, Outlet } from 'react-router'
import { requisitar } from '../api/cliente'
import type { Papel } from '../api/contas'
import { PAPEIS_NA_ORGANIZACAO, type PapelNaOrganizacao } from '../api/organizacoes'
import { lerPerfil, type DadosDoPerfil } from '../api/perfilDeInvestimento'
import { minhaVerificacao, type Selo } from '../api/verificacoes'
import { SeloDeVerificacao } from '../investidor/PaginaVerificacao'
import { ResumoDoPerfil } from '../investidor/ResumoDoPerfil'
import { NOMES_DA_SITUACAO_DA_STARTUP, NOMES_DO_PAPEL_NA_STARTUP, type ResumoDaStartup } from '../api/startups'
import { useAtuacao } from '../sessao/atuacao'
import { useSessao } from '../sessao/contexto'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'

const NOMES: Partial<Record<Papel, string>> = { INVESTIDOR: 'investidores', EMPREENDEDOR: 'empreendedores' }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Rota de um papel
 * Objetivo: Mostrar a área só a quem tem o papel e orientar quem não tem a ativá-lo.
 * Decisões: Esconder na tela é só conforto; a API confere o papel em toda chamada e responde 403.
 */
export function RotaDePapel({ papel }: { papel: Papel }) {
  const { estado, escolherPapel, papelAtivo } = useSessao()
  const tem = estado.status === 'autenticado' && estado.usuario.papeis.includes(papel)

  useEffect(() => {
    if (tem && papelAtivo !== papel) escolherPapel(papel)
  }, [tem, papel, papelAtivo, escolherPapel])

  if (!tem) {
    return (
      <section className="flex flex-col gap-3">
        <h1 className="text-2xl font-semibold">Área exclusiva</h1>
        <p>Esta área é só para {NOMES[papel] ?? 'a equipe'}. Você pode ativar esse papel na sua conta.</p>
        <Link to="/conta" className={CLASSE_LINK}>Ir para Minha conta</Link>
      </section>
    )
  }
  return <Outlet />
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Carregar dados de uma área
 * Objetivo: Buscar os dados da área do papel e expor carregando, erro ou dados.
 * Decisões: Um 403 da API aparece como mensagem (por exemplo, papel desativado em outro aparelho). A versão
 *           recarrega quando a atuação muda (KAN-60).
 */
function useArea<T>(caminho: string, versao = 0) {
  const [dados, setDados] = useState<T | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    requisitar<T>(caminho).then((r) => { setDados(r); setErro(null) })
      .catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Falha ao carregar.'))
  }, [caminho, versao])
  return { dados, erro }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Área do investidor
 * Objetivo: Mostrar a situação da conta como investidor.
 * Decisões: A tese de investimento chega na S3 (KAN-18). Mostra em nome de quem a pessoa atua e leva às
 *           organizações (KAN-60) e ao perfil de investimento (KAN-61). Sem tese, convida para o onboarding (KAN-18);
 *           com tese, mostra o resumo do perfil (KAN-20). Mostra o selo de verificação e leva à verificação (KAN-62).
 */
export function AreaInvestidor() {
  const { estado } = useSessao()
  const { versao } = useAtuacao(estado.status === 'autenticado' ? estado.usuario.id : null)
  const { dados, erro } = useArea<SituacaoInvestidor>('/api/v1/investidores/eu', versao)
  const [perfil, setPerfil] = useState<DadosDoPerfil | null>(null)
  const [selo, setSelo] = useState<Selo | null>(null)
  useEffect(() => {
    let ativo = true
    minhaVerificacao().then((s) => { if (ativo) setSelo(s.selo) }).catch(() => { if (ativo) setSelo(null) })
    return () => { ativo = false }
  }, [versao])
  const concluido = dados?.onboarding === 'CONCLUIDO'
  useEffect(() => {
    if (!concluido) return
    let ativo = true
    lerPerfil().then((s) => { if (ativo) setPerfil(s.perfil) }).catch(() => { if (ativo) setPerfil(null) })
    return () => { ativo = false }
  }, [concluido, versao])
  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Área do investidor</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {dados && (
        <>
          <p className="text-sm">
            {dados.atuandoPor
              ? <>Atuando em nome de <strong>{dados.atuandoPor.nome}</strong> ({PAPEIS_NA_ORGANIZACAO[dados.atuandoPor.papel].nome.toLowerCase()}).</>
              : 'Atuando como pessoa física.'}
          </p>
          {dados.onboarding === 'CONCLUIDO' ? (
            <>
              <p className="text-muted">
                A tese de investimento está cadastrada. <Link to="/investidor/perfil" className={CLASSE_LINK}>Editar o perfil</Link>
                {' · '}<Link to="/catalogo" className={CLASSE_LINK}>Abrir o catálogo</Link>
                {' · '}<Link to="/conexoes" className={CLASSE_LINK}>Minhas conexões</Link>
              </p>
              {perfil && <ResumoDoPerfil perfil={perfil} />}
            </>
          ) : (
            <p className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
              {dados.onboarding === 'PULADO' ? 'Você deixou a tese para depois.' : 'Você ainda não definiu a tese de investimento.'}{' '}
              <Link to="/investidor/onboarding" className={CLASSE_LINK}>Definir a tese</Link>
            </p>
          )}
        </>
      )}
      <p className="flex items-center gap-2 text-sm">
        {selo && <SeloDeVerificacao selo={selo} />}
        <Link to="/investidor/verificacao" className={CLASSE_LINK}>{selo === 'VERIFICADO' ? 'Ver a verificação' : 'Verificar identidade'}</Link>
      </p>
      <Link to="/investidor/organizacoes" className={CLASSE_LINK}>Organizações (fundos, redes de anjos e outras entidades)</Link>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Situação do investidor
 * Objetivo: Tipar a resposta da área do investidor.
 * Decisões: atuandoPor nulo = pessoa física; onboarding diz se a tese está concluída, pulada ou pendente (KAN-18).
 */
interface SituacaoInvestidor {
  teseCadastrada: boolean
  proximoPasso: string
  atuandoPor: { organizacao: string; nome: string; papel: PapelNaOrganizacao } | null
  onboarding: 'PENDENTE' | 'CONCLUIDO' | 'PULADO'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Área do empreendedor
 * Objetivo: Mostrar as startups de que a pessoa faz parte e levar ao cadastro de uma nova.
 * Decisões: Cada startup mostra a situação, o papel e o selo de empresa ainda não constituída.
 */
export function AreaEmpreendedor() {
  const { dados, erro } = useArea<ResumoDaStartup[]>('/api/v1/startups/minhas')
  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Área do empreendedor</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {dados && dados.length === 0 && <p className="text-muted">Você ainda não cadastrou nenhuma startup.</p>}
      {dados && dados.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Minhas startups">
          {dados.map((s) => (
            <li key={s.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-4 text-sm">
              <Link to={`/empreendedor/startups/${s.id}`} className={CLASSE_LINK}>{s.nome}</Link>
              <span className="flex flex-wrap items-center gap-2 text-xs">
                {!s.constituida && <span className="rounded bg-warn-soft px-2 py-0.5 font-semibold text-warn">Empresa ainda não constituída</span>}
                <span className="rounded bg-surface-2 px-2 py-0.5 font-semibold">{NOMES_DA_SITUACAO_DA_STARTUP[s.situacao]}</span>
                <span className="text-muted">{NOMES_DO_PAPEL_NA_STARTUP[s.meuPapel]}</span>
              </span>
            </li>
          ))}
        </ul>
      )}
      <Link to="/empreendedor/startups/nova" className={CLASSE_LINK}>Cadastrar startup</Link>
    </section>
  )
}
