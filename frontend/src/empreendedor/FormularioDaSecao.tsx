import { useId, useState } from 'react'
import type { CampoDoCatalogo, ValoresDasSecoes } from '../api/secoes'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Formulário de uma seção
 * Objetivo: Montar, a partir do catálogo, os campos de uma seção do cadastro, com o contador de caracteres visível.
 * Decisões: Texto vira área de texto com limite; sim/não vira escolha única. Bloqueado para quem não pode editar a
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

  return (
    <form aria-label={titulo} className="flex flex-col gap-4"
      onSubmit={(e) => { e.preventDefault(); setSalvando(true); void aoSalvar(valores).finally(() => setSalvando(false)) }}>
      <fieldset disabled={bloqueado} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
        {campos.map((c) => c.tipo === 'SIM_NAO'
          ? <SimOuNao key={c.campo} campo={c} valor={valores[c.campo]} aoMudar={(v) => mudar(c.campo, v)} />
          : <Texto key={c.campo} campo={c} valor={valores[c.campo]} aoMudar={(v) => mudar(c.campo, v)} />)}
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
