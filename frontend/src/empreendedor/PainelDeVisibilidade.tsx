import { useEffect, useState } from 'react'
import {
  type Apresentacao, type ConfiguracaoDeVisibilidade, lerVisibilidade, participarDeEdicoes, previaDaApresentacao, salvarVisibilidade,
} from '../api/catalogo'
import { lerLogotipo, NOMES_DAS_SECOES, type SecaoDaStartup } from '../api/startups'
import { ApresentacaoEmPaginas } from '../catalogo/ApresentacaoEmPaginas'
import { AlertaErro } from '../ui/Campo'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-29, KAN-85
 * Título: Visibilidade e pré-visualização
 * Objetivo: Deixar a equipe escolher o que é público (Nível 1) ou confidencial (Nível 2), ver na hora como a startup
 *           aparece para o investidor (verificado ou não) e escolher participar das edições temáticas.
 * Decisões: Cada troca é salva na hora e recarrega a pré-visualização. Só o fundador administrador troca; os demais
 *           veem as escolhas e a pré-visualização. Nome, segmento, logotipo e estágio são sempre públicos, e a participação
 *           societária, sempre confidencial (avisos fixos).
 */
export function PainelDeVisibilidade({ startupId, admin }: { startupId: string; admin: boolean }) {
  const [config, setConfig] = useState<ConfiguracaoDeVisibilidade | null>(null)
  const [previa, setPrevia] = useState<Apresentacao | null>(null)
  const [verificado, setVerificado] = useState(true)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)

  useEffect(() => {
    let ativo = true
    lerVisibilidade(startupId).then((c) => { if (ativo) setConfig(c) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [startupId])

  useEffect(() => {
    let ativo = true
    previaDaApresentacao(startupId, verificado).then((p) => { if (ativo) setPrevia(p) }).catch(() => undefined)
    return () => { ativo = false }
  }, [startupId, verificado, versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 10:40
   * Cartão Jira: KAN-28
   * Título: Aplicar troca
   * Objetivo: Salvar a troca e atualizar a pré-visualização.
   * Decisões: Erro do servidor vai para o alerta.
   */
  async function aplicar(acao: () => Promise<ConfiguracaoDeVisibilidade>) {
    try {
      setConfig(await acao())
      setErro(null)
      setVersao((v) => v + 1)
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível salvar.')
    }
  }

  if (!config) return erro ? <AlertaErro>{erro}</AlertaErro> : <p className="text-muted">Carregando…</p>
  const secoes = [...new Set(config.itens.map((i) => i.item.secao))] as SecaoDaStartup[]

  return (
    <section aria-label="Visibilidade" className="flex flex-col gap-5">
      <div className="rounded-lg border border-line bg-surface p-4 text-sm">
        <p className="font-semibold">O que aparece no catálogo</p>
        <p className="text-muted">
          Marque o que é público (Nível 1). O resto fica confidencial (Nível 2) e só é liberado depois do Match. Nome, segmento,
          logotipo e estágio são sempre públicos; a participação de cada fundador é sempre confidencial. Os dados da rodada só
          aparecem para investidores verificados. O catálogo só é visto por usuários cadastrados.
        </p>
      </div>
      {erro && <AlertaErro>{erro}</AlertaErro>}

      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={config.participaEdicoes} disabled={!admin}
          onChange={(e) => void aplicar(() => participarDeEdicoes(startupId, e.target.checked))} />
        Participar das edições temáticas do catálogo
      </label>

      <div className="grid gap-4 md:grid-cols-2">
        {secoes.map((secao) => (
          <fieldset key={secao} className="flex flex-col gap-1 rounded-lg border border-line p-3 text-sm">
            <legend className="px-1 font-semibold">{NOMES_DAS_SECOES[secao]}</legend>
            {config.itens.filter((i) => i.item.secao === secao).map((i) => (
              <label key={i.item.codigo} className="flex items-start gap-2">
                <input type="checkbox" checked={i.publico} disabled={!admin} aria-label={`Público: ${i.item.rotulo}`}
                  onChange={(e) => void aplicar(() => salvarVisibilidade(startupId, { [i.item.codigo]: e.target.checked }))} />
                <span>{i.item.rotulo}{!i.item.padraoPublico && <span className="text-xs text-muted"> · confidencial por padrão</span>}</span>
              </label>
            ))}
          </fieldset>
        ))}
      </div>
      {!admin && <p className="text-sm text-muted">Só o fundador administrador muda o que é público.</p>}

      <section aria-label="Pré-visualização" className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="font-semibold">Pré-visualização</h2>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={verificado} onChange={(e) => setVerificado(e.target.checked)} />
            Ver como investidor verificado
          </label>
        </div>
        {previa && <ApresentacaoEmPaginas key={`${versao}-${verificado}`} apresentacao={previa} carregarLogotipo={() => lerLogotipo(startupId)} />}
      </section>
    </section>
  )
}
