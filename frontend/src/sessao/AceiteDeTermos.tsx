import { useEffect, useState } from 'react'
import { ErroApi } from '../api/cliente'
import { aceitar, ENDERECOS, listarPendentes, type DocumentoLegal } from '../api/documentos'
import { AlertaErro, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { useSessao } from './contexto'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-92
 * Título: Aceite de nova versão dos termos
 * Objetivo: Pedir, no próximo acesso, o aceite das versões novas dos Termos de Uso e da Política de
 *           Privacidade, antes de liberar a plataforma.
 * Decisões: Lista só o que bloqueia o acesso; a confidencialidade é cobrada antes do Nível 2 (S8). Cada
 *           documento tem a própria caixa, com link para o texto completo. Depois de aceitar, a sessão é
 *           renovada para sair do bloqueio. Sair continua disponível.
 */
export function AceiteDeTermos() {
  const { renovar, sair } = useSessao()
  const [pendentes, setPendentes] = useState<DocumentoLegal[] | null>(null)
  const [marcados, setMarcados] = useState<string[]>([])
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-92
   * Título: Carregar pendências
   * Objetivo: Buscar as versões que a conta precisa aceitar.
   * Decisões: Filtra as que bloqueiam o acesso.
   */
  useEffect(() => {
    listarPendentes()
      .then((lista) => setPendentes(lista.filter((d) => d.aceite === 'TODOS_NO_ACESSO')))
      .catch(() => setErro('Não foi possível carregar os termos. Recarregue a página.'))
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-92
   * Título: Confirmar aceite
   * Objetivo: Registrar os aceites e liberar a plataforma.
   * Decisões: Botão só habilita com todas as caixas marcadas.
   */
  async function confirmar() {
    setEnviando(true)
    setErro(null)
    try {
      await aceitar(marcados)
      await renovar()
    } catch (e) {
      setErro(e instanceof ErroApi ? e.message : 'Não foi possível registrar o aceite.')
      setEnviando(false)
    }
  }

  const todosMarcados = pendentes !== null && pendentes.every((d) => marcados.includes(d.id))

  return (
    <MolduraConta titulo="Atualizamos nossos termos">
      <p>Para continuar usando o Orbit Hub, leia e aceite a versão atual dos documentos abaixo.</p>
      {pendentes === null && !erro && <p role="status" className="text-muted">Carregando…</p>}
      {pendentes && (
        <ul className="flex flex-col gap-3">
          {pendentes.map((d) => (
            <li key={d.id}>
              <label className="flex gap-2">
                <input type="checkbox" checked={marcados.includes(d.id)}
                  onChange={(e) => setMarcados((m) => (e.target.checked ? [...m, d.id] : m.filter((id) => id !== d.id)))} />
                <span>
                  Li e aceito a versão {d.versao} de{' '}
                  <a href={`/termos/${ENDERECOS[d.tipo]}`} target="_blank" rel="noreferrer" className={CLASSE_LINK}>{d.titulo}</a>
                </span>
              </label>
            </li>
          ))}
        </ul>
      )}
      {erro && <AlertaErro>{erro}</AlertaErro>}
      <div className="flex items-center gap-4">
        <button type="button" disabled={!todosMarcados || enviando} onClick={confirmar} className={CLASSE_BOTAO_PRIMARIO}>
          {enviando ? 'Registrando…' : 'Aceitar e continuar'}
        </button>
        <button type="button" onClick={() => void sair()} className={CLASSE_LINK}>Sair</button>
      </div>
    </MolduraConta>
  )
}
