import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { ErroApi } from '../api/cliente'
import {
  ENDERECOS,
  listarVersoes,
  listarVigentes,
  tipoDoEndereco,
  type DocumentoLegal,
} from '../api/documentos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Data de vigência por extenso
 * Objetivo: Mostrar a data de vigência no formato brasileiro.
 * Decisões: Só a data (sem hora), no fuso de Brasília.
 */
function formatarData(iso: string) {
  return new Date(iso).toLocaleDateString('pt-BR', { timeZone: 'America/Sao_Paulo' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Página de termos, políticas e avisos
 * Objetivo: Exibir a versão vigente de cada documento e permitir consultar as versões anteriores.
 * Decisões: Pública (o visitante lê antes de se cadastrar). O endereço /termos/:endereco escolhe o
 *           documento; a versão anterior é escolhida na lista lateral e substitui o texto exibido.
 */
export function PaginaTermos() {
  const { endereco } = useParams()
  const [vigentes, setVigentes] = useState<DocumentoLegal[] | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Carregar documentos vigentes
   * Objetivo: Buscar a lista de documentos ao abrir a página.
   * Decisões: Cancela a chamada ao sair da página.
   */
  useEffect(() => {
    const controle = new AbortController()
    listarVigentes(controle.signal)
      .then(setVigentes)
      .catch((e: unknown) => {
        if (!controle.signal.aborted) setErro(e instanceof ErroApi ? e.message : 'Não foi possível carregar os documentos.')
      })
    return () => controle.abort()
  }, [])

  const tipo = tipoDoEndereco(endereco)
  const atual = vigentes?.find((d) => d.tipo === tipo) ?? vigentes?.[0]

  return (
    <main className="mx-auto flex min-h-svh max-w-5xl flex-col gap-6 px-4 py-10">
      <header className="flex items-center justify-between gap-3">
        <Link to="/" className="text-xl font-bold tracking-tight">Orbit Hub</Link>
      </header>
      <h1 className="font-display text-3xl font-semibold tracking-tight">Termos, políticas e avisos</h1>

      {erro && <p role="alert" className="rounded-md bg-crit-soft px-3 py-2 text-crit">{erro}</p>}
      {!erro && !vigentes && <p role="status" className="text-muted">Carregando…</p>}

      {vigentes && atual && (
        <div className="grid gap-6 md:grid-cols-[16rem_1fr]">
          <nav aria-label="Documentos">
            <ul className="flex flex-col gap-1">
              {vigentes.map((d) => (
                <li key={d.tipo}>
                  <Link
                    to={`/termos/${ENDERECOS[d.tipo]}`}
                    aria-current={d.tipo === atual.tipo ? 'page' : undefined}
                    className={`block rounded-md px-3 py-2 text-sm ${
                      d.tipo === atual.tipo ? 'bg-primary-soft font-semibold text-primary' : 'hover:bg-surface-2'
                    }`}
                  >
                    {d.titulo}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <Documento key={atual.tipo} vigente={atual} />
        </div>
      )}
    </main>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Documento com histórico de versões
 * Objetivo: Mostrar o texto da versão escolhida e a lista de versões publicadas.
 * Decisões: Começa na vigente; o histórico é buscado ao abrir o documento. Uma versão anterior aparece
 *           com um aviso de que não é a vigente.
 */
function Documento({ vigente }: { vigente: DocumentoLegal }) {
  const [versoes, setVersoes] = useState<DocumentoLegal[]>([vigente])
  const [exibida, setExibida] = useState<DocumentoLegal>(vigente)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Carregar o histórico
   * Objetivo: Buscar as versões do documento exibido.
   * Decisões: Falha no histórico não esconde o texto vigente; a lista fica só com a vigente.
   */
  useEffect(() => {
    const controle = new AbortController()
    listarVersoes(vigente.tipo, controle.signal).then(setVersoes).catch(() => {})
    return () => controle.abort()
  }, [vigente.tipo])

  const ehAnterior = exibida.id !== vigente.id

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_12rem]">
      <article className="rounded-lg border border-line bg-surface p-6">
        <h2 className="text-2xl font-semibold">{exibida.titulo}</h2>
        <p className="mt-1 text-sm text-muted">
          Versão {exibida.versao} · em vigor desde {formatarData(exibida.vigenteDesde)}
        </p>
        {ehAnterior && (
          <p role="note" className="mt-3 rounded-md bg-warn-soft px-3 py-2 text-sm text-warn">
            Esta é uma versão anterior. A versão em vigor é a {vigente.versao}.
          </p>
        )}
        <div className="mt-5 whitespace-pre-line leading-relaxed">{exibida.conteudo}</div>
      </article>
      <aside aria-label="Versões publicadas">
        <h3 className="mb-2 text-sm font-semibold">Versões</h3>
        <ul className="flex flex-col gap-1 text-sm">
          {versoes.map((v) => (
            <li key={v.id}>
              <button
                type="button"
                onClick={() => setExibida(v)}
                aria-pressed={v.id === exibida.id}
                className={`w-full rounded-md px-3 py-1.5 text-left ${
                  v.id === exibida.id ? 'bg-surface-2 font-semibold' : 'hover:bg-surface-2'
                }`}
              >
                Versão {v.versao}
                <span className="block text-xs text-muted">{formatarData(v.vigenteDesde)}</span>
              </button>
            </li>
          ))}
        </ul>
      </aside>
    </div>
  )
}
