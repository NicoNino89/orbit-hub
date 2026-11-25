import { useEffect, useId, useState } from 'react'
import { opcoesDaLista } from '../api/organizacoes'
import type { CampoDoCatalogo, ValoresDasSecoes } from '../api/secoes'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Campo conforme o tipo
 * Objetivo: Escolher o componente do campo pelo tipo do catálogo.
 * Decisões: Texto é o padrão.
 */
function Campo(props: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  if (props.campo.tipo === 'SIM_NAO') return <SimOuNao {...props} />
  if (props.campo.tipo === 'LISTA') return <OpcaoDaLista {...props} />
  if (props.campo.tipo === 'PERCENTUAL' || props.campo.tipo === 'INTEIRO') return <Numero {...props} />
  if (props.campo.tipo === 'ESCALA') return <Escala {...props} />
  return <Texto {...props} />
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Formulário de uma seção
 * Objetivo: Montar, a partir do catálogo, os campos de uma seção do cadastro, com o contador de caracteres visível.
 * Decisões: Texto vira área de texto com limite; sim/não vira escolha única; lista vira seleção (KAN-23); percentual e
 *           inteiro viram campo numérico, com a soma das destinações à vista (KAN-25); a escala vira uma lista de opções com
 *           a explicação de cada nível (KAN-26). Os obrigatórios para publicar ganham a marca (KAN-27). Bloqueado para quem não pode editar a
 *           seção. Nada é obrigatório para salvar o rascunho.
 */
export function FormularioDaSecao({ titulo, campos, inicial, bloqueado, aoSalvar }: {
  titulo: string
  campos: CampoDoCatalogo[]
  inicial: ValoresDasSecoes
  bloqueado: boolean
  aoSalvar: (valores: ValoresDasSecoes) => Promise<void>
}) {
  const [valores, setValores] = useState<ValoresDasSecoes>(() =>
    Object.fromEntries(campos.map((c) => [c.campo, inicial[c.campo] ?? ''])))
  const [salvando, setSalvando] = useState(false)
  const mudar = (campo: string, valor: string) => setValores({ ...valores, [campo]: valor })
  const percentuais = campos.filter((c) => c.tipo === 'PERCENTUAL')
  const soma = percentuais.reduce((total, c) => total + (Number((valores[c.campo] ?? '').replace(',', '.')) || 0), 0)

  return (
    <form aria-label={titulo} className="flex flex-col gap-4"
      onSubmit={(e) => { e.preventDefault(); setSalvando(true); void aoSalvar(valores).finally(() => setSalvando(false)) }}>
      <fieldset disabled={bloqueado} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
        {campos.map((c) => (
          <div key={c.campo} className="flex flex-col gap-1">
            {c.obrigatorio && <span className="self-start rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Obrigatório para publicar</span>}
            <Campo campo={c} valor={valores[c.campo]} aoMudar={(v: string) => mudar(c.campo, v)} />
          </div>
        ))}
        {percentuais.length > 0 && (
          <p role="status" className={`text-sm ${soma > 100 ? 'font-semibold text-crit' : 'text-muted'}`}>
            Soma das destinações: {soma.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}% de 100%
          </p>
        )}
      </fieldset>
      {bloqueado
        ? <p className="text-sm text-muted">Você não tem autorização para editar esta seção. Peça a um fundador administrador.</p>
        : <button type="submit" disabled={salvando} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
            {salvando ? 'Salvando…' : `Salvar ${titulo.toLowerCase()}`}
          </button>}
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Campo de texto
 * Objetivo: Mostrar a área de texto com a ajuda e o contador "usados de limite".
 * Decisões: O contador fica ligado ao campo por aria-describedby e avisa quando passa de 90%.
 */
function Texto({ campo, valor, aoMudar }: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  const id = useId()
  const limite = campo.limite ?? 1000
  const perto = valor.length > limite * 0.9
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{campo.rotulo}</label>
      <textarea id={id} value={valor} maxLength={limite} rows={limite > 1000 ? 6 : 3} aria-describedby={`${id}-ajuda ${id}-contador`}
        onChange={(e) => aoMudar(e.target.value)} className="rounded-md border border-line bg-surface px-3 py-2" />
      <div className="flex justify-between gap-3 text-xs text-muted">
        <span id={`${id}-ajuda`}>{campo.ajuda}</span>
        <span id={`${id}-contador`} className={perto ? 'font-semibold text-warn' : undefined}>{valor.length}/{limite}</span>
      </div>
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Sim ou não
 * Objetivo: Mostrar a pergunta com as opções sim, não e sem resposta.
 * Decisões: "Sem resposta" apaga o valor.
 */
function SimOuNao({ campo, valor, aoMudar }: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  return (
    <fieldset className="flex flex-col gap-1 text-sm">
      <legend className="font-semibold">{campo.rotulo}</legend>
      <div className="flex gap-4">
        {([['SIM', 'Sim'], ['NAO', 'Não'], ['', 'Sem resposta']] as const).map(([codigo, nome]) => (
          <label key={nome} className="flex items-center gap-2">
            <input type="radio" name={campo.campo} checked={valor === codigo} onChange={() => aoMudar(codigo)} />
            {nome}
          </label>
        ))}
      </div>
    </fieldset>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:02
 * Cartão Jira: KAN-23
 * Título: Opção da lista
 * Objetivo: Mostrar a escolha entre as opções ativas da lista gerenciável do campo.
 * Decisões: "Escolha" deixa o campo vazio; a ajuda explica onde a classificação é usada.
 */
function OpcaoDaLista({ campo, valor, aoMudar }: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  const id = useId()
  const [opcoes, setOpcoes] = useState<{ codigo: string; rotulo: string }[]>([])
  useEffect(() => {
    if (campo.lista) opcoesDaLista(campo.lista).then(setOpcoes).catch(() => setOpcoes([]))
  }, [campo.lista])
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{campo.rotulo}</label>
      <select id={id} value={valor} onChange={(e) => aoMudar(e.target.value)} aria-describedby={`${id}-ajuda`}
        className="self-start rounded-md border border-line bg-surface px-3 py-2">
        <option value="">Escolha</option>
        {opcoes.map((o) => <option key={o.codigo} value={o.codigo}>{o.rotulo}</option>)}
      </select>
      {campo.ajuda && <p id={`${id}-ajuda`} className="text-xs text-muted">{campo.ajuda}</p>}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:26
 * Cartão Jira: KAN-25
 * Título: Campo numérico
 * Objetivo: Mostrar o campo de percentual ou de número inteiro.
 * Decisões: Texto com teclado numérico, para aceitar vírgula; o servidor confere a faixa.
 */
function Numero({ campo, valor, aoMudar }: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  const id = useId()
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{campo.rotulo}</label>
      <input id={id} value={valor} inputMode={campo.tipo === 'INTEIRO' ? 'numeric' : 'decimal'} onChange={(e) => aoMudar(e.target.value)}
        aria-describedby={campo.ajuda ? `${id}-ajuda` : undefined} className="max-w-40 rounded-md border border-line bg-surface px-3 py-2" />
      {campo.ajuda && <p id={`${id}-ajuda`} className="text-xs text-muted">{campo.ajuda}</p>}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:30
 * Cartão Jira: KAN-26
 * Título: Escala
 * Objetivo: Mostrar os níveis da escala, cada um com a breve explicação, para a escolha de um só.
 * Decisões: A ajuda do campo lembra que o estágio é obrigatório para publicar.
 */
function Escala({ campo, valor, aoMudar }: { campo: CampoDoCatalogo; valor: string; aoMudar: (v: string) => void }) {
  return (
    <fieldset className="flex flex-col gap-2 text-sm">
      <legend className="font-semibold">{campo.rotulo}</legend>
      {campo.ajuda && <p className="text-xs text-muted">{campo.ajuda}</p>}
      {(campo.opcoes ?? []).map((o) => (
        <label key={o.codigo} className={`flex items-start gap-2 rounded-md border p-2 ${valor === o.codigo ? 'border-primary' : 'border-line'}`}>
          <input type="radio" name={campo.campo} checked={valor === o.codigo} onChange={() => aoMudar(o.codigo)} className="mt-1" />
          <span><span className="font-semibold">{o.rotulo}</span><span className="block text-xs text-muted">{o.explicacao}</span></span>
        </label>
      ))}
    </fieldset>
  )
}
