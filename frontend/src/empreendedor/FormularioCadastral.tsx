import { useEffect, useState } from 'react'
import { opcoesDaLista } from '../api/organizacoes'
import { type CadastroDaStartup, NOMES_DAS_REDES, type RedeSocial } from '../api/startups'
import { Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

type Opcao = { codigo: string; rotulo: string; grupo?: string | null }
const REDES = Object.keys(NOMES_DAS_REDES) as RedeSocial[]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Formulário dos dados cadastrais
 * Objetivo: Coletar nome, nome fantasia, razão social, CNPJ, segmento, localização, site, redes e fundadores.
 * Decisões: CNPJ opcional, com a explicação de que sem ele não se abre rodada. A participação dos fundadores é
 *           confidencial (Nível 2) e a tela diz isso. Segmento e localização vêm das listas.
 */
export function FormularioCadastral({ inicial, rotuloDoBotao, bloqueado = false, aoSalvar }: {
  inicial: CadastroDaStartup
  rotuloDoBotao: string
  bloqueado?: boolean
  aoSalvar: (dados: CadastroDaStartup) => Promise<void>
}) {
  const [dados, setDados] = useState<CadastroDaStartup>(inicial)
  const [segmentos, setSegmentos] = useState<Opcao[]>([])
  const [localizacoes, setLocalizacoes] = useState<Opcao[]>([])
  const [salvando, setSalvando] = useState(false)

  useEffect(() => {
    opcoesDaLista('SEGMENTO').then(setSegmentos).catch(() => setSegmentos([]))
    opcoesDaLista('LOCALIZACAO').then(setLocalizacoes).catch(() => setLocalizacoes([]))
  }, [])

  const texto = (campo: 'nome' | 'nomeFantasia' | 'razaoSocial' | 'cnpj' | 'site') => (e: React.ChangeEvent<HTMLInputElement>) =>
    setDados({ ...dados, [campo]: e.target.value })
  const mudarFundador = (i: number, parte: Partial<CadastroDaStartup['fundadores'][number]>) =>
    setDados({ ...dados, fundadores: dados.fundadores.map((f, j) => (j === i ? { ...f, ...parte } : f)) })

  return (
    <form aria-label="Dados cadastrais" className="flex flex-col gap-4"
      onSubmit={(e) => { e.preventDefault(); setSalvando(true); void aoSalvar(dados).finally(() => setSalvando(false)) }}>
      <fieldset disabled={bloqueado} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
        <Campo rotulo="Nome da startup" value={dados.nome} onChange={texto('nome')} required maxLength={120} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Campo rotulo="Nome fantasia" value={dados.nomeFantasia ?? ''} onChange={texto('nomeFantasia')} maxLength={120} />
          <Campo rotulo="Razão social" value={dados.razaoSocial ?? ''} onChange={texto('razaoSocial')} maxLength={200} />
        </div>
        <Campo rotulo="CNPJ" value={dados.cnpj ?? ''} onChange={texto('cnpj')} inputMode="numeric" placeholder="00.000.000/0000-00"
          dica="Opcional enquanto a empresa não estiver constituída. Sem CNPJ, a startup aparece como “empresa ainda não constituída” e não pode abrir rodada." />
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-semibold">Segmento</span>
            <select value={dados.segmento} required onChange={(e) => setDados({ ...dados, segmento: e.target.value })}
              className="rounded-md border border-line bg-surface px-3 py-2">
              <option value="">Escolha</option>
              {segmentos.map((o) => <option key={o.codigo} value={o.codigo}>{o.rotulo}</option>)}
            </select>
          </label>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-semibold">Localização</span>
            <select value={dados.localizacao} required onChange={(e) => setDados({ ...dados, localizacao: e.target.value })}
              className="rounded-md border border-line bg-surface px-3 py-2">
              <option value="">Escolha</option>
              {localizacoes.map((o) => <option key={o.codigo} value={o.codigo}>{o.rotulo}</option>)}
            </select>
          </label>
        </div>
        <Campo rotulo="Site" type="url" value={dados.site ?? ''} onChange={texto('site')} placeholder="https://" />
        <fieldset className="flex flex-col gap-2 text-sm">
          <legend className="font-semibold">Redes sociais</legend>
          <div className="grid gap-2 sm:grid-cols-2">
            {REDES.map((r) => (
              <Campo key={r} rotulo={NOMES_DAS_REDES[r]} type="url" placeholder="https://" value={dados.redes[r] ?? ''}
                onChange={(e) => setDados({ ...dados, redes: { ...dados.redes, [r]: e.target.value } })} />
            ))}
          </div>
        </fieldset>
        <fieldset className="flex flex-col gap-2 text-sm">
          <legend className="font-semibold">Fundadores e composição societária</legend>
          <p className="text-xs text-muted">A participação de cada fundador é confidencial (Nível 2): só a equipe da startup vê.</p>
          {dados.fundadores.map((f, i) => (
            <div key={i} className="grid items-end gap-2 sm:grid-cols-[2fr_1fr_1fr_auto]" role="group" aria-label={`Fundador ${i + 1}`}>
              <Campo rotulo="Nome" value={f.nome} required maxLength={120} onChange={(e) => mudarFundador(i, { nome: e.target.value })} />
              <Campo rotulo="Cargo" value={f.cargo ?? ''} maxLength={80} onChange={(e) => mudarFundador(i, { cargo: e.target.value })} />
              <Campo rotulo="Participação (%)" type="number" min={0} max={100} step="0.01" value={f.participacao ?? ''}
                onChange={(e) => mudarFundador(i, { participacao: e.target.value === '' ? null : Number(e.target.value) })} />
              <button type="button" className={`${CLASSE_LINK} pb-2`}
                onClick={() => setDados({ ...dados, fundadores: dados.fundadores.filter((_, j) => j !== i) })}>Remover</button>
            </div>
          ))}
          <button type="button" className={`${CLASSE_LINK} self-start`}
            onClick={() => setDados({ ...dados, fundadores: [...dados.fundadores, { nome: '', cargo: null, participacao: null }] })}>
            Adicionar fundador
          </button>
        </fieldset>
      </fieldset>
      {!bloqueado && (
        <button type="submit" disabled={salvando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
          {salvando ? 'Salvando…' : rotuloDoBotao}
        </button>
      )}
    </form>
  )
}
