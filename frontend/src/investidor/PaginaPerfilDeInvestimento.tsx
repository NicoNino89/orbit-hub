import { useEffect, useState } from 'react'
import { FORMATO_DATA_HORA } from '../admin/datas'
import { opcoesDaLista } from '../api/organizacoes'
import {
  type DadosDoPerfil, type Lideranca, lerPerfil, NOMES_DA_LIDERANCA, NOMES_DA_PARTICIPACAO, type ParticipacaoNaGestao,
  salvarPerfil, type SituacaoDoPerfil,
} from '../api/perfilDeInvestimento'
import { useSessao } from '../sessao/contexto'
import { useAtuacao } from '../sessao/atuacao'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

type Opcao = { codigo: string; rotulo: string }

const VAZIO: DadosDoPerfil = {
  tipo: '', tipoOutro: null, ticketMinimo: 0, ticketMaximo: 0, lideranca: 'AMBOS', followOn: false,
  participacaoNaGestao: 'NAO_PARTICIPA', instrumentos: [],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Perfil de investimento
 * Objetivo: Informar o tipo de investidor, o ticket, a liderança, o follow-on, a participação na gestão e os
 *           instrumentos aceitos, da pessoa ou da organização em nome da qual ela atua.
 * Decisões: Recarrega quando a atuação muda. Na organização, o tipo é o dela (só exibido) e quem não é administrador vê
 *           o formulário bloqueado, com a explicação. Na pessoa física, o tipo é investidor-anjo ou outro.
 */
export function PaginaPerfilDeInvestimento() {
  const { estado } = useSessao()
  const { atuacao, versao } = useAtuacao(estado.status === 'autenticado' ? estado.usuario.id : null)
  const [situacao, setSituacao] = useState<SituacaoDoPerfil | null>(null)
  const [dados, setDados] = useState<DadosDoPerfil>(VAZIO)
  const [tipos, setTipos] = useState<Opcao[]>([])
  const [instrumentos, setInstrumentos] = useState<Opcao[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    opcoesDaLista('TIPO_INVESTIDOR').then(setTipos).catch(() => setTipos([]))
    opcoesDaLista('INSTRUMENTO').then(setInstrumentos).catch(() => setInstrumentos([]))
  }, [])

  useEffect(() => {
    let ativo = true
    lerPerfil()
      .then((s) => {
        if (!ativo) return
        setSituacao(s)
        setDados(s.perfil ?? { ...VAZIO, tipo: s.tipoFixo ?? '' })
        setErro(null)
        setAviso(null)
      })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:16
   * Cartão Jira: KAN-61
   * Título: Salvar
   * Objetivo: Enviar o perfil e mostrar a confirmação.
   * Decisões: A resposta do servidor substitui o formulário.
   */
  async function salvar(e: React.FormEvent) {
    e.preventDefault()
    setSalvando(true)
    try {
      const s = await salvarPerfil(dados)
      setSituacao(s)
      if (s.perfil) setDados(s.perfil)
      setErro(null)
      setAviso('Perfil salvo.')
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

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Perfil de investimento</h1>
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
        {!bloqueado && (
          <button type="submit" disabled={salvando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
            {salvando ? 'Salvando…' : 'Salvar perfil'}
          </button>
        )}
      </form>
    </section>
  )
}
