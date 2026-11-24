import { Link, Outlet } from 'react-router'
import { ehPapelPublico, NOMES_DOS_PAPEIS } from '../api/contas'
import { useSessao } from '../sessao/contexto'


/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:00
 * Cartão Jira: KAN-80
 * Título: Moldura da área logada
 * Objetivo: Cabeçalho comum com a marca, o papel em que a pessoa está atuando, o nome e o botão de sair.
 * Decisões: Com os dois papéis, um seletor deixa claro e troca o papel ativo (critério do KAN-12); com um só,
 *           o papel aparece como etiqueta. Sair encerra a sessão no servidor. Contas da equipe veem o link
 *           do backoffice (KAN-80).
 */
export function AreaLogada() {
  const { estado, papelAtivo, escolherPapel, sair } = useSessao()
  if (estado.status !== 'autenticado') return null
  const papeis = estado.usuario.papeis.filter(ehPapelPublico)
  const daEquipe = estado.usuario.papeis.some((p) => !ehPapelPublico(p))

  return (
    <div className="min-h-svh">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-3 px-4 py-3">
          <Link to="/painel" className="flex items-center gap-2 font-bold tracking-tight">
            <img src="/favicon.svg" alt="" width={28} height={28} />
            Orbit Hub
          </Link>
          <div className="flex flex-wrap items-center gap-3 text-sm">
            {papeis.length > 1 ? (
              <div role="group" aria-label="Atuando como" className="flex items-center gap-1 rounded-md bg-surface-2 p-1">
                <span className="px-2 text-muted">Atuando como</span>
                {papeis.map((p) => (
                  <button key={p} type="button" aria-pressed={p === papelAtivo} onClick={() => escolherPapel(p)}
                    className={`rounded px-3 py-1 font-semibold ${p === papelAtivo ? 'bg-primary text-on-primary' : ''}`}>
                    {NOMES_DOS_PAPEIS[p]}
                  </button>
                ))}
              </div>
            ) : (
              papelAtivo && <span className="rounded bg-primary-soft px-2 py-1 font-semibold text-primary">{NOMES_DOS_PAPEIS[papelAtivo]}</span>
            )}
            {daEquipe && <Link to="/admin" className="font-semibold text-primary underline-offset-2 hover:underline">Backoffice</Link>}
            <Link to="/conta" className="text-muted underline-offset-2 hover:underline">{estado.usuario.nome}</Link>
            <button type="button" onClick={() => void sair()} className="rounded-md border border-line px-3 py-1 font-semibold">
              Sair
            </button>
          </div>
        </div>
      </header>
      <div className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </div>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:00
 * Cartão Jira: KAN-80
 * Título: Painel inicial
 * Objetivo: Dar as boas-vindas e indicar o próximo passo do papel ativo.
 * Decisões: Os onboardings (tese do investidor, KAN-18; cadastro da startup, KAN-21) chegam na S3 e na S4;
 *           até lá o cartão explica o que vem. Conta só da equipe é levada ao backoffice (KAN-80).
 */
export function Painel() {
  const { estado, papelAtivo } = useSessao()
  if (estado.status !== 'autenticado') return null
  const primeiroNome = estado.usuario.nome.split(' ')[0]

  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-semibold tracking-tight">Olá, {primeiroNome}</h1>
      <div className="rounded-lg border border-line bg-surface p-5">
        <h2 className="mb-1 font-semibold">Próximo passo</h2>
        {papelAtivo === 'INVESTIDOR' && (
          <p className="text-muted">
            Defina sua tese de investimento para receber startups compatíveis. Disponível em breve.{" "}
            <Link to="/investidor" className="font-semibold text-primary">Abrir a área do investidor</Link>
          </p>
        )}
        {papelAtivo === 'EMPREENDEDOR' && (
          <p className="text-muted">
            Cadastre sua startup para apresentá-la a investidores. Disponível em breve.{" "}
            <Link to="/empreendedor" className="font-semibold text-primary">Abrir a área do empreendedor</Link>
          </p>
        )}
        {papelAtivo === null && (
          <p className="text-muted">
            Sua conta não tem papel público.{' '}
            {estado.usuario.papeis.some((p) => !ehPapelPublico(p)) && (
              <Link to="/admin" className="font-semibold text-primary">Abrir o backoffice</Link>
            )}
          </p>
        )}
      </div>
    </section>
  )
}
