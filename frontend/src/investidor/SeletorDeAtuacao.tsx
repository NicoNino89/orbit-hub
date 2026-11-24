import { useEffect, useState } from 'react'
import { minhasOrganizacoes, type Organizacao } from '../api/organizacoes'
import { escolherAtuacao, EVENTO_ORGANIZACOES, lerAtuacao, useAtuacao } from '../sessao/atuacao'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Seletor "atuando por"
 * Objetivo: Deixar o investidor escolher se age por si ou em nome de uma das suas organizações.
 * Decisões: Só aparece para quem tem organização. Recarrega a lista quando uma tela avisa que ela mudou (criou, entrou,
 *           saiu); se a organização escolhida sumiu da lista (a pessoa saiu ou foi removida), volta para "por mim".
 */
export function SeletorDeAtuacao({ usuario }: { usuario: string }) {
  const [organizacoes, setOrganizacoes] = useState<Organizacao[]>([])
  const [versao, setVersao] = useState(0)
  const { atuacao } = useAtuacao(usuario)

  useEffect(() => {
    const recarregar = () => setVersao((v) => v + 1)
    window.addEventListener(EVENTO_ORGANIZACOES, recarregar)
    return () => window.removeEventListener(EVENTO_ORGANIZACOES, recarregar)
  }, [])

  useEffect(() => {
    let ativo = true
    minhasOrganizacoes()
      .then((lista) => {
        if (!ativo) return
        setOrganizacoes(lista)
        const escolhida = lerAtuacao(usuario)?.organizacao
        if (escolhida && !lista.some((o) => o.id === escolhida)) escolherAtuacao(usuario, null)
      })
      .catch(() => { if (ativo) setOrganizacoes([]) })
    return () => { ativo = false }
  }, [usuario, versao])

  if (organizacoes.length === 0) return null
  return (
    <label className="flex items-center gap-2">
      <span className="text-muted">Atuando por</span>
      <select value={atuacao?.organizacao ?? ''} className="rounded-md border border-line bg-surface px-2 py-1"
        onChange={(e) => {
          const escolhida = organizacoes.find((o) => o.id === e.target.value)
          escolherAtuacao(usuario, escolhida ? { id: escolhida.id, nome: escolhida.nome } : null)
        }}>
        <option value="">Mim (pessoa física)</option>
        {organizacoes.map((o) => <option key={o.id} value={o.id}>{o.nome}</option>)}
      </select>
    </label>
  )
}
