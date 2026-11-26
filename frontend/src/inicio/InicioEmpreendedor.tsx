import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { minhasReunioes, type Reuniao } from '../api/agenda'
import { conexoesDaStartup, type ConexaoDoPainel } from '../api/catalogo'
import { bannersVigentes, type Banner } from '../api/inicio'
import { progressoDaStartup, type ProgressoDaStartup } from '../api/publicacao'
import { minhasStartups, NOMES_DA_SITUACAO_DA_STARTUP, type ResumoDaStartup } from '../api/startups'
import { useSessao } from '../sessao/contexto'
import { BannerRotativo } from './Inicio'

const CARTAO = 'rounded-2xl border border-line bg-surface p-5 shadow-sm'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:58
 * Cartão Jira: KAN-169
 * Título: Página inicial do empreendedor
 * Objetivo: Mostrar a startup com a situação e o progresso, as pendências (interesses a responder, reuniões a
 *           confirmar), as próximas reuniões e os banners para empreendedores.
 * Decisões: Considera a primeira startup da lista; as demais ficam em "Minha Startup". Sem startup, chama o cadastro.
 */
export function PaginaInicialDoEmpreendedor() {
  const { estado } = useSessao()
  const nome = estado.status === 'autenticado' ? estado.usuario.nome.split(' ')[0] : ''
  const [banners, setBanners] = useState<Banner[]>([])
  const [startups, setStartups] = useState<ResumoDaStartup[] | null>(null)
  const [progresso, setProgresso] = useState<ProgressoDaStartup | null>(null)
  const [conexoes, setConexoes] = useState<ConexaoDoPainel[]>([])
  const [reunioes, setReunioes] = useState<Reuniao[]>([])
  useEffect(() => {
    bannersVigentes('EMPREENDEDOR').then(setBanners).catch(() => setBanners([]))
    minhasStartups().then(setStartups).catch(() => setStartups([]))
    minhasReunioes().then(setReunioes).catch(() => setReunioes([]))
  }, [])
  const principal = startups?.[0]
  useEffect(() => {
    if (!principal) return
    progressoDaStartup(principal.id).then(setProgresso).catch(() => setProgresso(null))
    conexoesDaStartup(principal.id, '').then(setConexoes).catch(() => setConexoes([]))
  }, [principal])
  const pendentes = conexoes.filter((c) => c.situacao === 'PENDENTE').length
  const aConfirmar = reunioes.filter((r) => r.aguardaVoce).length
  const proximas = reunioes.filter((r) => r.situacao === 'CONFIRMADA' && r.inicio && new Date(r.inicio) > new Date()).slice(0, 3)
  return (
    <div className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-bold tracking-tight">Olá, {nome}</h1>
      <BannerRotativo banners={banners.filter((b) => b.tipo === 'BANNER')} />
      {startups?.length === 0 && (
        <section className={`${CARTAO} bg-primary-soft`}>
          <h2 className="text-xl font-bold">Cadastre sua startup para apresentá-la a investidores.</h2>
          <Link to="/empreendedor/startups/nova" className="mt-3 inline-block rounded-lg bg-primary px-4 py-2 font-semibold text-on-primary">Cadastrar startup →</Link>
        </section>
      )}
      {principal && (
        <div className="grid gap-4 lg:grid-cols-3">
          <section aria-label="Minha startup" className={`${CARTAO} flex flex-col gap-2 lg:col-span-2`}>
            <h2 className="text-xl font-bold">{principal.nome}</h2>
            {progresso && <p>Situação: <strong>{NOMES_DA_SITUACAO_DA_STARTUP[progresso.situacao]}</strong></p>}
            {progresso && (
              <div aria-label={`Cadastro ${progresso.percentual}% completo`} className="h-2 w-full rounded-full bg-surface-2">
                <div className="h-2 rounded-full bg-accent" style={{ width: `${progresso.percentual}%` }} />
              </div>
            )}
            <Link to={`/empreendedor/startups/${principal.id}`} className="font-semibold">Abrir a startup →</Link>
          </section>
          <section aria-label="Pendências" className={`${CARTAO} flex flex-col gap-2`}>
            <h2 className="font-semibold">Pendências</h2>
            <p>{pendentes} interesse(s) aguardando resposta</p>
            <p>{aConfirmar} reunião(ões) aguardando você</p>
            <Link to="/agenda" className="font-semibold">Ver agenda →</Link>
          </section>
        </div>
      )}
      <section aria-label="Próximas reuniões" className="flex flex-col gap-2">
        <h2 className="text-xl font-bold">Próximas reuniões</h2>
        {proximas.length === 0 && <p className="text-muted">Nenhuma reunião confirmada.</p>}
        {proximas.map((r) => (
          <p key={r.id} className={CARTAO}>{r.pauta} · {r.inicio && new Date(r.inicio).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</p>
        ))}
      </section>
    </div>
  )
}
