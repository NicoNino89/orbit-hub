import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { FORMATO_DATA_HORA } from '../admin/datas'
import {
  cancelarConvite, convidar, convitesPendentes, type ConvitePendente, detalheDaOrganizacao, editarOrganizacao,
  encerrarVinculo, type MembroDaOrganizacao, opcoesDaLista, type Organizacao, PAPEIS_NA_ORGANIZACAO,
  type PapelNaOrganizacao, trocarPapel,
} from '../api/organizacoes'
import { escolherAtuacao, EVENTO_ORGANIZACOES, lerAtuacao } from '../sessao/atuacao'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const PAPEIS = Object.keys(PAPEIS_NA_ORGANIZACAO) as PapelNaOrganizacao[]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Página da organização
 * Objetivo: Mostrar os dados e os membros; ao administrador, editar, convidar, mudar papéis e remover; a todos, sair.
 * Decisões: As ações aparecem conforme o papel de quem vê, mas quem decide é o servidor. Sair pede confirmação na
 *           própria tela (sem janela do navegador) e, se a pessoa atuava pela organização, volta a atuar por si.
 */
export function PaginaOrganizacao() {
  const { id = '' } = useParams()
  const { estado } = useSessao()
  const navegar = useNavigate()
  const eu = estado.status === 'autenticado' ? estado.usuario.id : ''
  const [dados, setDados] = useState<{ organizacao: Organizacao; membros: MembroDaOrganizacao[] } | null>(null)
  const [convites, setConvites] = useState<ConvitePendente[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  const [confirmarSaida, setConfirmarSaida] = useState(false)

  useEffect(() => {
    let ativo = true
    detalheDaOrganizacao(id)
      .then((r) => {
        if (!ativo) return
        setDados(r)
        if (r.organizacao.meuPapel === 'ADMINISTRADOR') {
          convitesPendentes(id).then((c) => { if (ativo) setConvites(c) }).catch(() => undefined)
        }
      })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [id, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Executar e recarregar
   * Objetivo: Rodar uma ação, mostrar o aviso e recarregar a página.
   * Decisões: Erro do servidor vai para o alerta.
   */
  const agir = useCallback(async (acao: () => Promise<unknown>, sucesso: string) => {
    try {
      await acao()
      setErro(null)
      setAviso(sucesso)
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
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Sair da organização
   * Objetivo: Encerrar o próprio vínculo e voltar à lista.
   * Decisões: Se a atuação estava nesta organização, volta para "por mim".
   */
  async function sair() {
    try {
      await encerrarVinculo(id, eu)
      if (lerAtuacao(eu)?.organizacao === id) escolherAtuacao(eu, null)
      window.dispatchEvent(new Event(EVENTO_ORGANIZACOES))
      void navegar('/investidor/organizacoes')
    } catch (e) {
      setConfirmarSaida(false)
      setErro(e instanceof Error ? e.message : 'Não foi possível sair.')
    }
  }

  if (!dados) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const { organizacao, membros } = dados
  const admin = organizacao.meuPapel === 'ADMINISTRADOR'

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">{organizacao.nome}</h1>
        <p className="text-sm text-muted">
          CNPJ {organizacao.cnpj} · Você é {PAPEIS_NA_ORGANIZACAO[organizacao.meuPapel].nome.toLowerCase()}
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      {admin
        ? <EdicaoDaOrganizacao organizacao={organizacao}
            aoSalvar={(d) => agir(() => editarOrganizacao(id, d), 'Dados salvos.')} />
        : (
          <div className="rounded-lg border border-line bg-surface p-4 text-sm">
            {organizacao.descricao && <p>{organizacao.descricao}</p>}
            {organizacao.site && <a href={organizacao.site} className={CLASSE_LINK} rel="noreferrer noopener" target="_blank">{organizacao.site}</a>}
          </div>
        )}

      <section className="flex flex-col gap-2" aria-label="Membros">
        <h2 className="font-semibold">Membros</h2>
        <ul className="flex flex-col gap-2">
          {membros.map((m) => (
            <li key={m.usuarioId} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-3 text-sm">
              <span>
                {m.nome}{m.usuarioId === eu && ' (você)'}
                <span className="block text-xs text-muted">Desde {FORMATO_DATA_HORA.format(new Date(m.entrouEm))}</span>
              </span>
              {admin ? (
                <span className="flex items-center gap-3">
                  <select aria-label={`Papel de ${m.nome}`} value={m.papel} className="rounded-md border border-line bg-surface px-2 py-1"
                    onChange={(e) => void agir(() => trocarPapel(id, m.usuarioId, e.target.value as PapelNaOrganizacao), 'Papel alterado.')}>
                    {PAPEIS.map((p) => <option key={p} value={p}>{PAPEIS_NA_ORGANIZACAO[p].nome}</option>)}
                  </select>
                  {m.usuarioId !== eu && (
                    <button type="button" className={CLASSE_LINK}
                      onClick={() => void agir(() => encerrarVinculo(id, m.usuarioId), `${m.nome} foi removido(a).`)}>
                      Remover
                    </button>
                  )}
                </span>
              ) : <span className="text-xs font-semibold">{PAPEIS_NA_ORGANIZACAO[m.papel].nome}</span>}
            </li>
          ))}
        </ul>
      </section>

      {admin && (
        <Convites convites={convites}
          aoConvidar={(email, papel) => agir(() => convidar(id, email, papel), `Convite enviado para ${email}.`)}
          aoCancelar={(c) => void agir(() => cancelarConvite(id, c), 'Convite cancelado.')} />
      )}

      <div className="flex flex-col gap-2 border-t border-line pt-4 text-sm">
        {confirmarSaida ? (
          <div className="flex flex-wrap items-center gap-3" role="group" aria-label="Confirmar saída">
            <span>Você perde na hora o acesso aos dados da organização. Confirmar?</span>
            <button type="button" onClick={() => void sair()} className="rounded-md bg-crit px-3 py-1 font-semibold text-white">Sair da organização</button>
            <button type="button" onClick={() => setConfirmarSaida(false)} className={CLASSE_LINK}>Cancelar</button>
          </div>
        ) : (
          <button type="button" onClick={() => setConfirmarSaida(true)} className={`${CLASSE_LINK} self-start`}>Sair desta organização</button>
        )}
      </div>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Edição dos dados
 * Objetivo: Deixar o administrador trocar nome, tipo, site e descrição.
 * Decisões: CNPJ só exibido; tipos da lista pública, sem investidor-anjo.
 */
function EdicaoDaOrganizacao({ organizacao, aoSalvar }: {
  organizacao: Organizacao
  aoSalvar: (d: { nome: string; tipo: string; site: string; descricao: string }) => Promise<boolean>
}) {
  const [tipos, setTipos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [d, setD] = useState({ nome: organizacao.nome, tipo: organizacao.tipo, site: organizacao.site ?? '', descricao: organizacao.descricao ?? '' })

  useEffect(() => {
    opcoesDaLista('TIPO_INVESTIDOR').then((r) => setTipos(r.filter((t) => t.codigo !== 'ANJO'))).catch(() => setTipos([]))
  }, [])

  return (
    <form aria-label="Dados da organização" className="flex flex-col gap-3 rounded-lg border border-line bg-surface p-4"
      onSubmit={(e) => { e.preventDefault(); void aoSalvar(d) }}>
      <Campo rotulo="Nome" value={d.nome} onChange={(e) => setD({ ...d, nome: e.target.value })} required maxLength={160} />
      <label className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">Tipo</span>
        <select value={d.tipo} onChange={(e) => setD({ ...d, tipo: e.target.value })} className="rounded-md border border-line bg-surface px-3 py-2">
          {tipos.map((t) => <option key={t.codigo} value={t.codigo}>{t.rotulo}</option>)}
        </select>
      </label>
      <Campo rotulo="Site" value={d.site} onChange={(e) => setD({ ...d, site: e.target.value })} type="url" />
      <label className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">Descrição</span>
        <textarea value={d.descricao} onChange={(e) => setD({ ...d, descricao: e.target.value })} rows={3} maxLength={2000}
          className="rounded-md border border-line bg-surface px-3 py-2" />
      </label>
      <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Salvar dados</button>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convites
 * Objetivo: Convidar por e-mail com um papel e listar ou cancelar os convites pendentes.
 * Decisões: Limpa o e-mail depois do envio.
 */
function Convites({ convites, aoConvidar, aoCancelar }: {
  convites: ConvitePendente[]
  aoConvidar: (email: string, papel: PapelNaOrganizacao) => Promise<boolean>
  aoCancelar: (id: string) => void
}) {
  const [email, setEmail] = useState('')
  const [papel, setPapel] = useState<PapelNaOrganizacao>('MEMBRO')
  return (
    <section className="flex flex-col gap-3" aria-label="Convites">
      <h2 className="font-semibold">Convidar</h2>
      <form className="flex flex-wrap items-end gap-3 rounded-lg border border-line bg-surface p-4"
        onSubmit={(e) => { e.preventDefault(); void aoConvidar(email, papel).then((ok) => { if (ok) setEmail('') }) }}>
        <div className="min-w-64 flex-1">
          <Campo rotulo="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-semibold">Papel</span>
          <select value={papel} onChange={(e) => setPapel(e.target.value as PapelNaOrganizacao)} className="rounded-md border border-line bg-surface px-3 py-2">
            {PAPEIS.map((p) => <option key={p} value={p}>{PAPEIS_NA_ORGANIZACAO[p].nome}</option>)}
          </select>
        </label>
        <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar convite</button>
        <p className="w-full text-xs text-muted">{PAPEIS_NA_ORGANIZACAO[papel].explicacao}.</p>
      </form>
      {convites.length > 0 && (
        <ul className="flex flex-col gap-2 text-sm" aria-label="Convites pendentes">
          {convites.map((c) => (
            <li key={c.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-line bg-surface p-3">
              <span>
                {c.email} · {PAPEIS_NA_ORGANIZACAO[c.papel].nome}
                <span className="block text-xs text-muted">Vale até {FORMATO_DATA_HORA.format(new Date(c.expiraEm))}</span>
              </span>
              <button type="button" onClick={() => aoCancelar(c.id)} className={CLASSE_LINK}>Cancelar convite</button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
