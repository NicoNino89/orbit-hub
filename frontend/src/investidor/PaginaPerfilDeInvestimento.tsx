import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { opcoesDaLista } from '../api/organizacoes'
import {
  type DadosDoPerfil, type Lideranca, lerPerfil, type Maturidade, NOMES_DA_LIDERANCA, NOMES_DA_MATURIDADE, NOMES_DA_PARTICIPACAO,
  type ParticipacaoNaGestao, pularOnboarding, salvarPerfil, type SituacaoDoPerfil,
} from '../api/perfilDeInvestimento'
import { useSessao } from '../sessao/contexto'
import { useAtuacao } from '../sessao/atuacao'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

type Opcao = { codigo: string; rotulo: string; grupo?: string | null }

const REGIOES: Record<string, string> = {
  NORTE: 'Norte', NORDESTE: 'Nordeste', CENTRO_OESTE: 'Centro-Oeste', SUDESTE: 'Sudeste', SUL: 'Sul', EXTERIOR: 'Exterior',
}

const VAZIO: DadosDoPerfil = {
  tipo: '', tipoOutro: null, ticketMinimo: 0, ticketMaximo: 0, lideranca: 'AMBOS', followOn: false,
  participacaoNaGestao: 'NAO_PARTICIPA', instrumentos: [], segmentos: [], estagios: [], maturidades: [], localizacoes: [],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Perfil de investimento
 * Objetivo: Informar o tipo de investidor, o ticket, a liderança, o follow-on, a participação na gestão e os
 *           instrumentos aceitos, da pessoa ou da organização em nome da qual ela atua.
 * Decisões: Recarrega quando a atuação muda. Na organização, o tipo é o dela (só exibido) e quem não é administrador vê
 *           o formulário bloqueado, com a explicação. Na pessoa física, o tipo é investidor-anjo ou outro. A tese
 *           (segmentos, estágios da rodada, maturidade do negócio e localização, KAN-18) faz parte do mesmo formulário.
 *           No modo onboarding, a pessoa pode pular por agora; salvar ou pular leva à área do investidor.
 */
export function PaginaPerfilDeInvestimento({ onboarding = false }: { onboarding?: boolean }) {
  const navegar = useNavigate()
  const { estado } = useSessao()
  const { atuacao, versao } = useAtuacao(estado.status === 'autenticado' ? estado.usuario.id : null)
  const [situacao, setSituacao] = useState<SituacaoDoPerfil | null>(null)
  const [dados, setDados] = useState<DadosDoPerfil>(VAZIO)
  const [tipos, setTipos] = useState<Opcao[]>([])
  const [instrumentos, setInstrumentos] = useState<Opcao[]>([])
  const [segmentos, setSegmentos] = useState<Opcao[]>([])
  const [estagios, setEstagios] = useState<Opcao[]>([])
  const [localizacoes, setLocalizacoes] = useState<Opcao[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    opcoesDaLista('TIPO_INVESTIDOR').then(setTipos).catch(() => setTipos([]))
    opcoesDaLista('INSTRUMENTO').then(setInstrumentos).catch(() => setInstrumentos([]))
    opcoesDaLista('SEGMENTO').then(setSegmentos).catch(() => setSegmentos([]))
    opcoesDaLista('ESTAGIO_RODADA').then(setEstagios).catch(() => setEstagios([]))
    opcoesDaLista('LOCALIZACAO').then(setLocalizacoes).catch(() => setLocalizacoes([]))
  }, [])

  useEffect(() => {
    let ativo = true
    lerPerfil()
      .then((s) => {
        if (!ativo) return
        setSituacao(s)
        setDados(s.perfil ? { ...VAZIO, ...s.perfil } : { ...VAZIO, tipo: s.tipoFixo ?? '' })
        setErro(null)
        setAviso(null)
      })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:25
   * Cartão Jira: KAN-18
   * Título: Salvar
   * Objetivo: Enviar o perfil e mostrar a confirmação.
   * Decisões: A resposta do servidor substitui o formulário. No onboarding, segue para a área do investidor.
   */
  async function salvar(e: React.FormEvent) {
    e.preventDefault()
    setSalvando(true)
    try {
      const s = await salvarPerfil(dados)
      setSituacao(s)
      if (s.perfil) setDados({ ...VAZIO, ...s.perfil })
      setErro(null)
      setAviso('Perfil salvo.')
      if (onboarding) void navegar('/investidor')
    } catch (falha) {
      setAviso(null)
      setErro(falha instanceof Error ? falha.message : 'Não foi possível salvar.')
    } finally {
      setSalvando(false)
    }
  }

  if (!situacao) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const bloqueado = !situacao.podeEditar
  const rotuloDoTipo = (codigo: string) => tipos.find((t) => t.codigo === codigo)?.rotulo ?? codigo
  const alternar = (codigo: string) => setDados({
    ...dados,
    instrumentos: dados.instrumentos.includes(codigo) ? dados.instrumentos.filter((i) => i !== codigo) : [...dados.instrumentos, codigo],
  })
  const regioes = [...new Set(localizacoes.map((l) => l.grupo ?? 'EXTERIOR'))]
  const estadosDa = (regiao: string) => localizacoes.filter((l) => (l.grupo ?? 'EXTERIOR') === regiao).map((l) => l.codigo)
  const marcarRegiao = (regiao: string) => {
    const daRegiao = estadosDa(regiao)
    const todos = daRegiao.every((c) => dados.localizacoes.includes(c))
    setDados({
      ...dados,
      localizacoes: todos ? dados.localizacoes.filter((c) => !daRegiao.includes(c)) : [...new Set([...dados.localizacoes, ...daRegiao])],
    })
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:25
   * Cartão Jira: KAN-18
   * Título: Pular por agora
   * Objetivo: Registrar que a pessoa vai completar a tese depois e seguir para a área do investidor.
   * Decisões: Erro aparece no alerta.
   */
  async function pular() {
    try {
      await pularOnboarding()
      void navegar('/investidor')
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : 'Não foi possível pular.')
    }
  }

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">{onboarding ? 'Sua tese de investimento' : 'Perfil de investimento'}</h1>
        {onboarding && (
          <p className="text-sm">
            Conte como você investe para receber um catálogo de startups alinhado aos seus interesses. Leva poucos minutos.
          </p>
        )}
        <p className="text-sm text-muted">
          {situacao.daOrganizacao ? `Perfil de ${atuacao?.nome ?? 'organização'}.` : 'Seu perfil como pessoa física.'}{' '}
          Usado para mostrar startups compatíveis com a forma como você investe.
        </p>
        {situacao.atualizadoEm && (
          <p className="text-xs text-muted">
            Última alteração por {situacao.atualizadoPor ?? 'alguém da equipe'} em {FORMATO_DATA_HORA.format(new Date(situacao.atualizadoEm))}.
          </p>
        )}
      </header>
      {bloqueado && (
        <p role="note" className="rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
          Só o administrador da organização altera este perfil.
        </p>
      )}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}

      <form onSubmit={(e) => void salvar(e)} aria-label="Perfil de investimento" className="flex flex-col gap-4">
        <fieldset disabled={bloqueado} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
          {situacao.daOrganizacao ? (
            <p className="text-sm"><span className="font-semibold">Tipo de investidor:</span> {rotuloDoTipo(situacao.tipoFixo ?? '')}</p>
          ) : (
            <>
              <label className="flex flex-col gap-1 text-sm">
                <span className="font-semibold">Tipo de investidor</span>
                <select value={dados.tipo} required onChange={(e) => setDados({ ...dados, tipo: e.target.value })}
                  className="rounded-md border border-line bg-surface px-3 py-2">
                  <option value="">Escolha</option>
                  {tipos.filter((t) => t.codigo === 'ANJO' || t.codigo === 'OUTROS')
                    .map((t) => <option key={t.codigo} value={t.codigo}>{t.rotulo}</option>)}
                </select>
              </label>
              <p className="-mt-3 text-xs text-muted">Fundos, redes de anjos e outras entidades atuam como organização.</p>
              {dados.tipo === 'OUTROS' && (
                <Campo rotulo="Descreva o seu tipo" value={dados.tipoOutro ?? ''} maxLength={120} required
                  onChange={(e) => setDados({ ...dados, tipoOutro: e.target.value })} />
              )}
            </>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <Campo rotulo="Ticket mínimo (R$)" type="number" min={1} step="0.01" required
              value={dados.ticketMinimo || ''} onChange={(e) => setDados({ ...dados, ticketMinimo: Number(e.target.value) })} />
            <Campo rotulo="Ticket máximo (R$)" type="number" min={1} step="0.01" required
              value={dados.ticketMaximo || ''} onChange={(e) => setDados({ ...dados, ticketMaximo: Number(e.target.value) })} />
          </div>

          <fieldset className="flex flex-col gap-1 text-sm">
            <legend className="font-semibold">Liderança nas rodadas</legend>
            {(Object.keys(NOMES_DA_LIDERANCA) as Lideranca[]).map((l) => (
              <label key={l} className="flex items-center gap-2">
                <input type="radio" name="lideranca" checked={dados.lideranca === l} onChange={() => setDados({ ...dados, lideranca: l })} />
                {NOMES_DA_LIDERANCA[l]}
              </label>
            ))}
          </fieldset>

          <fieldset className="flex flex-col gap-1 text-sm">
            <legend className="font-semibold">Participação na gestão</legend>
            {(Object.keys(NOMES_DA_PARTICIPACAO) as ParticipacaoNaGestao[]).map((p) => (
              <label key={p} className="flex items-center gap-2">
                <input type="radio" name="participacao" checked={dados.participacaoNaGestao === p}
                  onChange={() => setDados({ ...dados, participacaoNaGestao: p })} />
                {NOMES_DA_PARTICIPACAO[p]}
              </label>
            ))}
          </fieldset>

          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={dados.followOn} onChange={(e) => setDados({ ...dados, followOn: e.target.checked })} />
            Reservo capital para acompanhar as próximas rodadas da mesma empresa (follow-on)
          </label>

          <fieldset className="flex flex-col gap-1 text-sm">
            <legend className="font-semibold">Instrumentos aceitos</legend>
            {instrumentos.map((i) => (
              <label key={i.codigo} className="flex items-center gap-2">
                <input type="checkbox" checked={dados.instrumentos.includes(i.codigo)} onChange={() => alternar(i.codigo)} />
                {i.rotulo}
              </label>
            ))}
          </fieldset>
        </fieldset>

        <fieldset disabled={bloqueado} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
          <legend className="px-1 font-semibold">Tese de investimento</legend>
          <Multipla titulo="Segmentos de interesse" opcoes={segmentos} escolhidos={dados.segmentos}
            aoMudar={(v) => setDados({ ...dados, segmentos: v })} />
          <Multipla titulo="Estágio da rodada" dica="Em que momento da captação você costuma entrar." opcoes={estagios}
            escolhidos={dados.estagios} aoMudar={(v) => setDados({ ...dados, estagios: v })} />
          <Multipla titulo="Maturidade do negócio (opcional)" dica="Em que ponto do negócio a startup está. Independe do estágio da rodada."
            opcoes={(Object.keys(NOMES_DA_MATURIDADE) as Maturidade[]).map((m) => ({ codigo: m, rotulo: NOMES_DA_MATURIDADE[m] }))}
            escolhidos={dados.maturidades} aoMudar={(v) => setDados({ ...dados, maturidades: v as Maturidade[] })} />
          <fieldset className="flex flex-col gap-2 text-sm">
            <legend className="font-semibold">Localização de interesse</legend>
            {regioes.map((r) => (
              <div key={r} className="flex flex-col gap-1">
                <button type="button" onClick={() => marcarRegiao(r)} className={`${CLASSE_LINK} self-start text-xs`}>
                  {estadosDa(r).every((c) => dados.localizacoes.includes(c)) ? 'Desmarcar' : 'Marcar'} {REGIOES[r] ?? r}
                </button>
                <div className="flex flex-wrap gap-x-4 gap-y-1">
                  {localizacoes.filter((l) => (l.grupo ?? 'EXTERIOR') === r).map((l) => (
                    <label key={l.codigo} className="flex items-center gap-1">
                      <input type="checkbox" checked={dados.localizacoes.includes(l.codigo)}
                        onChange={() => setDados({ ...dados, localizacoes: dados.localizacoes.includes(l.codigo)
                          ? dados.localizacoes.filter((c) => c !== l.codigo) : [...dados.localizacoes, l.codigo] })} />
                      {l.rotulo}
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </fieldset>
        </fieldset>
        {!bloqueado && (
          <div className="flex flex-wrap items-center gap-4">
            <button type="submit" disabled={salvando} className={CLASSE_BOTAO_PRIMARIO}>
              {salvando ? 'Salvando…' : onboarding ? 'Salvar e continuar' : 'Salvar perfil'}
            </button>
            {onboarding && <button type="button" onClick={() => void pular()} className={CLASSE_LINK}>Pular por agora</button>}
          </div>
        )}
      </form>
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Escolha múltipla
 * Objetivo: Mostrar um grupo de caixas de seleção com título e dica.
 * Decisões: Devolve a lista nova de códigos a cada clique.
 */
function Multipla({ titulo, dica, opcoes, escolhidos, aoMudar }: {
  titulo: string
  dica?: string
  opcoes: Opcao[]
  escolhidos: string[]
  aoMudar: (novos: string[]) => void
}) {
  return (
    <fieldset className="flex flex-col gap-1 text-sm">
      <legend className="font-semibold">{titulo}</legend>
      {dica && <p className="text-xs text-muted">{dica}</p>}
      <div className="flex flex-wrap gap-x-4 gap-y-1">
        {opcoes.map((o) => (
          <label key={o.codigo} className="flex items-center gap-1">
            <input type="checkbox" checked={escolhidos.includes(o.codigo)}
              onChange={() => aoMudar(escolhidos.includes(o.codigo) ? escolhidos.filter((c) => c !== o.codigo) : [...escolhidos, o.codigo])} />
            {o.rotulo}
          </label>
        ))}
      </div>
    </fieldset>
  )
}
