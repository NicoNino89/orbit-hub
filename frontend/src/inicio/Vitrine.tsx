import { useEffect, useState, type FormEvent } from 'react'
import { definirChamada, lerVitrine, salvarBanner, todosOsBanners, trocarCapa, trocarImagemDoBanner, type Banner, type DadosDoBanner } from '../api/inicio'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

const CAIXA = 'rounded-lg border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-170
 * Título: Vitrine da startup
 * Objetivo: Deixar os fundadores definir a chamada (frase curta) e a imagem de capa do cartão no catálogo.
 * Decisões: Fundadores editam; colaborador só vê.
 */
export function SecaoVitrine({ startupId, podeEditar }: { startupId: string; podeEditar: boolean }) {
  const [chamada, setChamada] = useState('')
  const [temCapa, setTemCapa] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  useEffect(() => {
    lerVitrine(startupId).then((v) => { setChamada(v.chamada ?? ''); setTemCapa(v.temCapa) }).catch(() => undefined)
  }, [startupId])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-170
   * Título: Agir
   * Objetivo: Executar a ação e mostrar o resultado.
   * Decisões: Mostra o erro da API.
   */
  async function agir(acao: () => Promise<unknown>, texto: string) {
    setErro(null)
    setAviso(null)
    try {
      await acao()
      setAviso(texto)
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Não foi possível salvar.')
    }
  }

  return (
    <section aria-label="Vitrine" className="flex flex-col gap-3 rounded-xl border border-line bg-surface p-5">
      <h2 className="text-lg font-semibold">Vitrine no catálogo</h2>
      <p className="text-sm text-muted">A chamada e a capa aparecem no cartão da startup para os investidores. Valores nunca aparecem no cartão.</p>
      <form aria-label="Chamada" onSubmit={(e: FormEvent) => { e.preventDefault(); void agir(() => definirChamada(startupId, chamada), 'Chamada salva.') }}
        className="flex flex-col gap-2">
        <Campo rotulo="Chamada (até 120 caracteres)" value={chamada} maxLength={120} disabled={!podeEditar} onChange={(e) => setChamada(e.target.value)} />
        {podeEditar && <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Salvar chamada</button>}
      </form>
      <p className="text-sm">{temCapa ? 'A startup tem capa.' : 'Sem capa: o cartão mostra uma ilustração do segmento.'}</p>
      {podeEditar && (
        <label className="flex flex-col gap-1 text-sm font-semibold">Imagem de capa (PNG, JPG ou WebP, até 2 MB)
          <input type="file" accept="image/png,image/jpeg,image/webp" onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) void agir(async () => { const v = await trocarCapa(startupId, f); setTemCapa(v.temCapa) }, 'Capa enviada.')
          }} />
        </label>
      )}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="text-sm">{aviso}</p>}
    </section>
  )
}

const NOVO: DadosDoBanner = { tipo: 'BANNER', titulo: '', destaque: '', texto: '', botao: '', link: '', publico: 'TODOS', inicio: new Date().toISOString().slice(0, 10),
  fim: null, ordem: 1, textoAlt: '', ativo: true }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-171
 * Título: Banners no backoffice
 * Objetivo: Listar, criar e alterar os banners e as chamadas institucionais e enviar a imagem.
 * Decisões: Só administrador; a imagem exige o texto alternativo.
 */
export function PaginaBanners() {
  const [lista, setLista] = useState<Banner[]>([])
  const [d, setD] = useState<DadosDoBanner>(NOVO)
  const [id, setId] = useState<string | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    todosOsBanners().then(setLista).catch((e: unknown) => setErro(e instanceof Error ? e.message : 'Não foi possível carregar.'))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-171
   * Título: Salvar
   * Objetivo: Criar ou alterar e recarregar.
   * Decisões: Volta ao formulário vazio.
   */
  async function salvar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await salvarBanner(id, d)
      setD(NOVO)
      setId(null)
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(x instanceof Error ? x.message : 'Não foi possível salvar.')
    }
  }

  const campo = (k: keyof DadosDoBanner, rotulo: string) => (
    <Campo rotulo={rotulo} value={String(d[k] ?? '')} onChange={(e) => setD({ ...d, [k]: e.target.value })} />
  )
  return (
    <section className="flex flex-col gap-6">
      <h1 className="font-display text-3xl font-bold tracking-tight">Banners e chamadas</h1>
      <p className="text-sm text-muted">Comunicação institucional da página inicial. Anúncios pagos seguem o módulo de publicidade.</p>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <ul className="flex flex-col gap-2">
        {lista.map((b) => (
          <li key={b.id} className={`${CAIXA} flex flex-wrap items-center gap-3`}>
            <span className="font-semibold">{b.titulo} {b.destaque}</span>
            <span className="text-sm text-muted">{b.tipo === 'BANNER' ? 'Banner' : 'Chamada'} · {b.publico.toLowerCase()} · ordem {b.ordem}{b.ativo ? '' : ' · inativo'}</span>
            <button type="button" className={CLASSE_LINK} onClick={() => { setId(b.id); setD({ ...b, destaque: b.destaque ?? '', texto: b.texto ?? '', botao: b.botao ?? '',
              link: b.link ?? '', textoAlt: b.textoAlt ?? '' }) }}>Editar</button>
            <label className="text-sm">Imagem
              <input type="file" accept="image/png,image/jpeg,image/webp" aria-label={`Imagem de ${b.titulo}`} className="ml-2" onChange={(e) => {
                const f = e.target.files?.[0]
                if (f) trocarImagemDoBanner(b.id, f).then(() => setVersao((v) => v + 1)).catch((x: unknown) => setErro(x instanceof Error ? x.message : 'Falhou.'))
              }} />
            </label>
          </li>
        ))}
      </ul>
      <form aria-label="Banner" onSubmit={(e) => void salvar(e)} className="grid gap-3 md:grid-cols-2">
        <label className="flex flex-col gap-1 text-sm font-semibold">Tipo
          <select value={d.tipo} onChange={(e) => setD({ ...d, tipo: e.target.value as Banner['tipo'] })} className={CAIXA}>
            <option value="BANNER">Banner rotativo</option><option value="CHAMADA">Chamada lateral</option>
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold">Público
          <select value={d.publico} onChange={(e) => setD({ ...d, publico: e.target.value as Banner['publico'] })} className={CAIXA}>
            <option value="TODOS">Todos</option><option value="INVESTIDOR">Investidores</option><option value="EMPREENDEDOR">Empreendedores</option>
          </select>
        </label>
        {campo('titulo', 'Título')}{campo('destaque', 'Destaque (em âmbar)')}{campo('texto', 'Texto')}{campo('botao', 'Botão')}
        {campo('link', 'Link (/caminho ou https)')}{campo('textoAlt', 'Texto alternativo da imagem')}
        <Campo rotulo="Início" type="date" value={d.inicio} onChange={(e) => setD({ ...d, inicio: e.target.value })} />
        <Campo rotulo="Fim (opcional)" type="date" value={d.fim ?? ''} onChange={(e) => setD({ ...d, fim: e.target.value || null })} />
        <Campo rotulo="Ordem" type="number" value={String(d.ordem)} onChange={(e) => setD({ ...d, ordem: Number(e.target.value) })} />
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={d.ativo} onChange={(e) => setD({ ...d, ativo: e.target.checked })} />Ativo</label>
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>{id ? 'Salvar alterações' : 'Incluir'}</button>
      </form>
    </section>
  )
}
