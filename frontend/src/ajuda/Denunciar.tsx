import { useEffect, useState, type FormEvent } from 'react'
import { abrirChamado } from '../api/atendimento'
import { denunciar, type ObjetoDenunciado } from '../api/moderacao'
import { opcoesDaLista } from '../api/organizacoes'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-99, KAN-102
 * Título: Denunciar
 * Objetivo: Abrir o formulário de denúncia de um perfil, organização, startup, conexão ou reunião, com categoria,
 *           descrição e evidências, e confirmar o recebimento com o protocolo.
 * Decisões: O denunciado não fica sabendo quem denunciou (o texto diz isso).
 */
export function Denunciar({ objetoTipo, objetoId, rotulo = 'Denunciar' }: { objetoTipo: ObjetoDenunciado; objetoId: string; rotulo?: string }) {
  const [aberto, setAberto] = useState(false)
  const [categorias, setCategorias] = useState<{ codigo: string; rotulo: string }[]>([])
  const [categoria, setCategoria] = useState('')
  const [descricao, setDescricao] = useState('')
  const [evidencias, setEvidencias] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [protocolo, setProtocolo] = useState<string | null>(null)
  useEffect(() => {
    if (aberto) opcoesDaLista('CATEGORIA_DENUNCIA').then(setCategorias).catch(() => setCategorias([]))
  }, [aberto])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-99
   * Título: Enviar denúncia
   * Objetivo: Enviar e mostrar o protocolo.
   * Decisões: Mostra o erro da API (por exemplo, denúncia repetida).
   */
  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      const r = await denunciar({ objetoTipo, objetoId, categoria, descricao, evidencias: evidencias || undefined })
      setProtocolo(r.protocolo)
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Não foi possível enviar.')
    }
  }

  if (protocolo) return <p role="status" className="text-sm">Denúncia recebida (protocolo {protocolo.slice(0, 8)}). Avisaremos quando for analisada.</p>
  if (!aberto) return <button type="button" onClick={() => setAberto(true)} className="text-sm text-muted underline">{rotulo}</button>
  return (
    <form aria-label="Denunciar" onSubmit={(e) => void enviar(e)} className="flex flex-col gap-2 rounded-md border border-line p-3">
      <p className="text-xs text-muted">Quem é denunciado não fica sabendo quem denunciou.</p>
      <label className="flex flex-col gap-1 text-sm font-semibold">Categoria
        <select value={categoria} onChange={(e) => setCategoria(e.target.value)} className={CAIXA}>
          <option value="">Escolha…</option>
          {categorias.map((c) => <option key={c.codigo} value={c.codigo}>{c.rotulo}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-sm font-semibold">O que aconteceu
        <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} rows={3} className={CAIXA} />
      </label>
      <Campo rotulo="Evidências (links, opcional)" value={evidencias} onChange={(e) => setEvidencias(e.target.value)} />
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <div className="flex gap-3">
        <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar denúncia</button>
        <button type="button" onClick={() => setAberto(false)} className={CLASSE_LINK}>Cancelar</button>
      </div>
    </form>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-103
 * Título: Abrir disputa
 * Objetivo: Abrir a disputa sobre um registro (por exemplo, a participação contestada na rodada) com a outra parte.
 * Decisões: A plataforma media e registra, mas não arbitra (o texto diz isso). A outra parte vem do registro.
 */
export function AbrirDisputa({ referenciaTipo, referenciaId }: { referenciaTipo: string; referenciaId: string }) {
  const [aberto, setAberto] = useState(false)
  const [descricao, setDescricao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [feito, setFeito] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-103
   * Título: Enviar disputa
   * Objetivo: Abrir o chamado de disputa.
   * Decisões: Categoria DISPUTA.
   */
  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await abrirChamado({ categoria: 'DISPUTA', descricao, referenciaTipo, referenciaId })
      setFeito(true)
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Não foi possível abrir.')
    }
  }

  if (feito) return <p role="status" className="text-sm">Disputa aberta. Acompanhe em Ajuda e atendimento.</p>
  if (!aberto) return <button type="button" onClick={() => setAberto(true)} className={CLASSE_LINK}>Abrir disputa</button>
  return (
    <form aria-label="Abrir disputa" onSubmit={(e) => void enviar(e)} className="flex flex-col gap-2">
      <p className="text-xs text-muted">O atendimento ouve as duas partes e registra o resultado. Investimentos são combinados fora da plataforma.</p>
      <label className="flex flex-col gap-1 text-sm font-semibold">Explique o desacordo
        <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} rows={3} className={CAIXA} />
      </label>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar</button>
    </form>
  )
}
