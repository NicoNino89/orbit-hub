import { useEffect, useState } from 'react'
import { Link, Outlet } from 'react-router'
import { requisitar } from '../api/cliente'
import type { Papel } from '../api/contas'
import { useSessao } from '../sessao/contexto'
import { AlertaErro } from '../ui/Campo'
import { CLASSE_LINK } from '../ui/estilos'

const NOMES: Record<Papel, string> = { INVESTIDOR: 'investidores', EMPREENDEDOR: 'empreendedores', ADMIN: 'administradores' }

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
        <p>Esta área é só para {NOMES[papel]}. Você pode ativar esse papel na sua conta.</p>
        <Link to="/conta" className={CLASSE_LINK}>Ir para Minha conta</Link>
      </section>
    )
  }
  return <Outlet />
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Carregar dados de uma área
 * Objetivo: Buscar os dados da área do papel e expor carregando, erro ou dados.
 * Decisões: Um 403 da API aparece como mensagem (por exemplo, papel desativado em outro aparelho).
 */
function useArea<T>(caminho: string) {
  const [dados, setDados] = useState<T | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  useEffect(() => {
    requisitar<T>(caminho).then(setDados).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Falha ao carregar.'))
  }, [caminho])
  return { dados, erro }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Área do investidor
 * Objetivo: Mostrar a situação da conta como investidor.
 * Decisões: A tese de investimento chega na S3 (KAN-18).
 */
export function AreaInvestidor() {
  const { dados, erro } = useArea<{ teseCadastrada: boolean; proximoPasso: string }>('/api/v1/investidores/eu')
  return (
    <section className="flex flex-col gap-3">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Área do investidor</h1>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {dados && (
        <p className="text-muted">
          {dados.teseCadastrada ? 'Sua tese está cadastrada.' : 'Você ainda não cadastrou sua tese de investimento. Disponível em breve.'}
        </p>
      )}
    </section>
  )
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
