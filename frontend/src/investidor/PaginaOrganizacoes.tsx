import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ErroApi } from '../api/cliente'
import {
  criarOrganizacao, minhasOrganizacoes, opcoesDaLista, type Organizacao, PAPEIS_NA_ORGANIZACAO,
} from '../api/organizacoes'
import { EVENTO_ORGANIZACOES } from '../sessao/atuacao'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Minhas organizações
 * Objetivo: Listar as organizações de que o investidor participa e cadastrar uma nova.
 * Decisões: O formulário fica recolhido até a pessoa pedir; criado, abre a organização.
 */
export function PaginaOrganizacoes() {
  const [lista, setLista] = useState<Organizacao[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [criando, setCriando] = useState(false)

  useEffect(() => {
    let ativo = true
    minhasOrganizacoes()
      .then((r) => { if (ativo) setLista(r) })
      .catch((e: unknown) => { if (ativo) setErro(e instanceof Error ? e.message : 'Não foi possível carregar.') })
    return () => { ativo = false }
  }, [])

  return (
    <section className="flex flex-col gap-5">
      <header className="flex flex-col gap-1">
        <h1 className="font-display text-3xl font-semibold tracking-tight">Organizações</h1>
        <p className="text-sm text-muted">
          Fundos, redes de anjos, CVCs, family offices e aceleradoras. Os membros atuam na plataforma em nome da
          organização; escolha no cabeçalho em nome de quem você está agindo.
        </p>
      </header>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {lista && lista.length === 0 && !criando && (
        <p className="rounded-lg border border-line bg-surface p-5 text-muted">
          Você ainda não participa de nenhuma organização. Crie uma, ou peça a um administrador para convidar você.
        </p>
      )}
      {lista && lista.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Minhas organizações">
          {lista.map((o) => (
            <li key={o.id} className="flex items-center justify-between gap-3 rounded-lg border border-line bg-surface p-4">
              <div>
                <Link to={`/investidor/organizacoes/${o.id}`} className={CLASSE_LINK}>{o.nome}</Link>
                <p className="text-xs text-muted">CNPJ {o.cnpj}</p>
              </div>
              <span className="rounded bg-primary-soft px-2 py-0.5 text-xs font-semibold text-primary">
                {PAPEIS_NA_ORGANIZACAO[o.meuPapel].nome}
              </span>
            </li>
          ))}
        </ul>
      )}
      {criando
        ? <FormularioDeOrganizacao aoCancelar={() => setCriando(false)} />
        : <button type="button" onClick={() => setCriando(true)} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Criar organização</button>}
    </section>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Cadastro da organização
 * Objetivo: Coletar nome, tipo, CNPJ, site e descrição e criar a organização.
 * Decisões: Tipos da lista pública, sem "investidor-anjo" (pessoa física). Erros do servidor aparecem no campo.
 */
function FormularioDeOrganizacao({ aoCancelar }: { aoCancelar: () => void }) {
  const navegar = useNavigate()
  const [tipos, setTipos] = useState<{ codigo: string; rotulo: string }[]>([])
  const [dados, setDados] = useState({ nome: '', tipo: '', cnpj: '', site: '', descricao: '' })
  const [erro, setErro] = useState<string | null>(null)
  const [campos, setCampos] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    opcoesDaLista('TIPO_INVESTIDOR').then((r) => setTipos(r.filter((t) => t.codigo !== 'ANJO'))).catch(() => setTipos([]))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 14:05
   * Cartão Jira: KAN-60
   * Título: Enviar o cadastro
   * Objetivo: Criar a organização e abrir a página dela.
   * Decisões: Avisa o seletor de atuação que a lista mudou.
   */
  async function enviar(e: React.FormEvent) {
    e.preventDefault()
    setEnviando(true)
    try {
      const criada = await criarOrganizacao(dados)
      window.dispatchEvent(new Event(EVENTO_ORGANIZACOES))
      void navegar(`/investidor/organizacoes/${criada.id}`)
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : 'Não foi possível criar.')
      setCampos(falha instanceof ErroApi ? falha.campos : {})
    } finally {
      setEnviando(false)
    }
  }

  const mudar = (campo: keyof typeof dados) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setDados({ ...dados, [campo]: e.target.value })

  return (
    <form onSubmit={(e) => void enviar(e)} className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5"
      aria-label="Nova organização">
      <h2 className="font-semibold">Nova organização</h2>
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <Campo rotulo="Nome" value={dados.nome} onChange={mudar('nome')} erro={campos.nome} required maxLength={160} />
      <label className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">Tipo</span>
        <select value={dados.tipo} onChange={mudar('tipo')} required className="rounded-md border border-line bg-surface px-3 py-2">
          <option value="">Escolha</option>
          {tipos.map((t) => <option key={t.codigo} value={t.codigo}>{t.rotulo}</option>)}
        </select>
      </label>
      <Campo rotulo="CNPJ" value={dados.cnpj} onChange={mudar('cnpj')} erro={campos.cnpj} required inputMode="numeric"
        placeholder="00.000.000/0000-00" dica="Não pode ser alterado depois." />
      <Campo rotulo="Site" value={dados.site} onChange={mudar('site')} erro={campos.site} type="url" placeholder="https://" />
      <label className="flex flex-col gap-1 text-sm">
        <span className="font-semibold">Descrição</span>
        <textarea value={dados.descricao} onChange={mudar('descricao')} maxLength={2000} rows={3}
          className="rounded-md border border-line bg-surface px-3 py-2" />
      </label>
      <div className="flex gap-3">
        <button type="submit" disabled={enviando} className={CLASSE_BOTAO_PRIMARIO}>{enviando ? 'Criando…' : 'Criar organização'}</button>
        <button type="button" onClick={aoCancelar} className={CLASSE_LINK}>Cancelar</button>
      </div>
    </form>
  )
}
