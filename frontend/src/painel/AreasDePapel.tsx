import { useEffect, useState } from 'react'
import { Link, Outlet } from 'react-router'
import { requisitar } from '../api/cliente'
import type { Papel } from '../api/contas'
import { PAPEIS_NA_ORGANIZACAO, type PapelNaOrganizacao } from '../api/organizacoes'
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
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Área do investidor
 * Objetivo: Mostrar a situação da conta como investidor.
 * Decisões: A tese de investimento chega na S3 (KAN-18). Mostra em nome de quem a pessoa atua e leva às
 *           organizações (KAN-60) e ao perfil de investimento (KAN-61).
 */
export function AreaInvestidor() {
  const { estado } = useSessao()
  const { versao } = useAtuacao(estado.status === 'autenticado' ? estado.usuario.id : null)
  const { dados, erro } = useArea<SituacaoInvestidor>('/api/v1/investidores/eu', versao)
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
          <p className="text-muted">
            {dados.teseCadastrada ? 'O perfil de investimento está cadastrado.' : 'Você ainda não cadastrou o perfil de investimento.'}{' '}
            <Link to="/investidor/perfil" className={CLASSE_LINK}>{dados.teseCadastrada ? 'Ver ou editar o perfil' : 'Cadastrar agora'}</Link>
          </p>
        </>
      )}
      <Link to="/investidor/organizacoes" className={CLASSE_LINK}>Organizações (fundos, redes de anjos e outras entidades)</Link>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Situação do investidor
 * Objetivo: Tipar a resposta da área do investidor.
 * Decisões: atuandoPor nulo = pessoa física.
 */
interface SituacaoInvestidor {
  teseCadastrada: boolean
  proximoPasso: string
  atuandoPor: { organizacao: string; nome: string; papel: PapelNaOrganizacao } | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Área do empreendedor
 * Objetivo: Mostrar as startups da conta.
 * Decisões: O cadastro da startup chega na S4 (KAN-21).
 */
export function AreaEmpreendedor() {
  const { dados, erro } = useArea<{ startups: string[]; proximoPasso: string }>('/api/v1/empreendimentos/meus')
  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Área do empreendedor</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {dados && (
        <p className="text-muted">
          {dados.startups.length === 0
            ? 'Você ainda não cadastrou nenhuma startup. Disponível em breve.'
            : `${dados.startups.length} startup(s) cadastrada(s).`}
        </p>
      )}
    </section>
  )
}
