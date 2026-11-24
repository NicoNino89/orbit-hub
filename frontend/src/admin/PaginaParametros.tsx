import { useEffect, useState, type FormEvent } from 'react'
import { alterarParametro, historicoDoParametro, listarParametros, type AlteracaoDeParametro, type Parametro } from '../api/parametros'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { FORMATO_DATA_HORA } from './datas'

const GRUPOS: Record<string, string> = {
  cadastro: 'Cadastro', conta: 'Conta', seguranca: 'Segurança', equipe: 'Equipe e governança', verificacao: 'Verificação',
  curadoria: 'Curadoria', catalogo: 'Catálogo', conexao: 'Conexões e Nível 2', reuniao: 'Reuniões', rodada: 'Rodadas',
  startup: 'Qualidade da base', moderacao: 'Moderação e recursos', atendimento: 'Atendimento', privacidade: 'Privacidade e retenção',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Valor com unidade
 * Objetivo: Escrever o valor de um parâmetro com a unidade, para leitura.
 * Decisões: Listas viram "7 e 21 dias".
 */
function valorLegivel(p: Pick<Parametro, 'tipo' | 'valor' | 'unidade'>, valor = p.valor) {
  if (p.tipo === 'OPCAO') return valor
  const texto = p.tipo === 'LISTA_INTEIROS' ? valor.split(',').join(', ').replace(/, ([^,]*)$/, ' e $1') : valor
  return p.unidade === '%' ? `${texto}%` : `${texto} ${p.unidade}`
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Tela de parâmetros
 * Objetivo: Mostrar o catálogo por grupo e deixar o administrador alterar com justificativa e ver o histórico.
 * Decisões: Sensível aparece marcado e, ao alterar, avisa que foi para segunda aprovação (o valor só muda depois).
 *           A tela mostra os limites aceitos, mas quem confere é o servidor.
 */
export function PaginaParametros() {
  const [parametros, setParametros] = useState<Parametro[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    listarParametros()
      .then((lista) => { if (ativo) setParametros(lista) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [versao])

  const grupos = new Map<string, Parametro[]>()
  parametros?.forEach((p) => grupos.set(p.grupo, [...(grupos.get(p.grupo) ?? []), p]))

  return (
    <section className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Parâmetros</h1>
        <p className="text-sm text-muted">
          Prazos e limites das regras da plataforma. Toda alteração exige justificativa e fica no histórico. Parâmetros
          sensíveis só mudam depois da segunda aprovação. Prazos que já começaram mantêm o valor da época.
        </p>
      </header>
      {aviso && <p role="status" className="rounded-md bg-ok-soft px-3 py-2 text-sm text-ok">{aviso}</p>}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {[...grupos.entries()].map(([grupo, lista]) => (
        <section key={grupo} aria-labelledby={`grupo-${grupo}`} className="flex flex-col gap-2">
          <h2 id={`grupo-${grupo}`} className="font-semibold">{GRUPOS[grupo] ?? grupo}</h2>
          <ul className="flex flex-col gap-2" aria-label={GRUPOS[grupo] ?? grupo}>
            {lista.map((p) => (
              <LinhaParametro key={p.chave} parametro={p}
                aoConcluir={(texto) => { setErro(null); setAviso(texto); setVersao((v) => v + 1) }} aoFalhar={setErro} />
            ))}
          </ul>
        </section>
      ))}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Parâmetro na lista
 * Objetivo: Mostrar um parâmetro e, sob demanda, o formulário de alteração e o histórico.
 * Decisões: Opção vira lista de escolha; inteiro e lista, campo de texto com os limites na dica.
 */
function LinhaParametro({ parametro, aoConcluir, aoFalhar }: {
  parametro: Parametro; aoConcluir: (t: string) => void; aoFalhar: (m: string) => void
}) {
  const [editando, setEditando] = useState(false)
  const [valor, setValor] = useState(parametro.valor)
  const [justificativa, setJustificativa] = useState('')
  const [historico, setHistorico] = useState<AlteracaoDeParametro[] | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    try {
      const r = await alterarParametro(parametro.chave, valor, justificativa)
      setEditando(false)
      setJustificativa('')
      aoConcluir(r.aplicado
        ? `${parametro.chave} alterado para ${valorLegivel(parametro, r.parametro.valor)}.`
        : `${parametro.chave} é sensível: a alteração foi para a segunda aprovação e só vale depois que outro administrador aprovar.`)
    } catch (e) {
      aoFalhar(e instanceof Error ? e.message : 'Não foi possível alterar.')
    }
  }

  async function verHistorico() {
    try {
      setHistorico(await historicoDoParametro(parametro.chave))
    } catch (e) {
      aoFalhar(e instanceof Error ? e.message : 'Não foi possível carregar o histórico.')
    }
  }

  const limites = parametro.minimo !== null ? `Aceita de ${parametro.minimo} a ${parametro.maximo} ${parametro.unidade}.` : undefined

  return (
    <li className="flex flex-col gap-2 rounded-md border border-line bg-surface p-3">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="font-mono text-xs text-muted">{parametro.chave}</p>
          <p className="text-sm">{parametro.descricao}</p>
        </div>
        <div className="flex flex-col items-end gap-1 text-sm">
          <strong>{valorLegivel(parametro)}</strong>
          {parametro.valor !== parametro.valorPadrao && <span className="text-xs text-muted">Padrão: {valorLegivel(parametro, parametro.valorPadrao)}</span>}
          {parametro.sensivel && <span className="rounded bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn">Sensível</span>}
        </div>
      </div>
      {!editando && (
        <div className="flex gap-3 text-sm">
          <button type="button" className={CLASSE_LINK} onClick={() => { setValor(parametro.valor); setEditando(true) }}>Alterar</button>
          <button type="button" className={CLASSE_LINK} onClick={() => void verHistorico()}>Histórico</button>
        </div>
      )}
      {editando && (
        <form onSubmit={enviar} className="flex flex-col gap-2" aria-label={`Alterar ${parametro.chave}`}>
          {parametro.tipo === 'OPCAO' ? (
            <label className="flex flex-col gap-1 text-sm font-semibold">
              Novo valor
              <select value={valor} onChange={(e) => setValor(e.target.value)} className="rounded-md border border-line bg-surface px-3 py-2 font-normal">
                {parametro.opcoes.map((o) => <option key={o} value={o}>{o}</option>)}
              </select>
            </label>
          ) : (
            <Campo rotulo="Novo valor" required value={valor} onChange={(e) => setValor(e.target.value)}
              dica={parametro.tipo === 'LISTA_INTEIROS' ? `${limites ?? ''} Separe os valores por vírgula.` : limites} />
          )}
          <Campo rotulo="Justificativa" required maxLength={1000} value={justificativa} onChange={(e) => setJustificativa(e.target.value)} />
          <div className="flex gap-3">
            <button type="submit" className={CLASSE_BOTAO_PRIMARIO} disabled={justificativa.trim() === ''}>
              {parametro.sensivel ? 'Enviar para segunda aprovação' : 'Salvar'}
            </button>
            <button type="button" className={CLASSE_LINK} onClick={() => setEditando(false)}>Cancelar</button>
          </div>
        </form>
      )}
      {historico && (
        <div className="text-sm">
          {historico.length === 0 ? <p className="text-muted">Nunca alterado: vale o padrão do catálogo.</p> : (
            <ul className="flex flex-col gap-1" aria-label={`Histórico de ${parametro.chave}`}>
              {historico.map((h) => (
                <li key={h.alteradoEm} className="text-muted">
                  {FORMATO_DATA_HORA.format(new Date(h.alteradoEm))}: {h.valorAnterior} → {h.valorNovo}, por {h.autor}
                  {h.aprovadoPor && `, aprovado por ${h.aprovadoPor}`}. Motivo: {h.justificativa}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </li>
  )
}
