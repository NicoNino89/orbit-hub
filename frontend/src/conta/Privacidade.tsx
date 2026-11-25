import { useEffect, useState, type FormEvent } from 'react'
import {
  baixarMeusDados, excluirMinhaConta, meusPedidosDePrivacidade, NOMES_DA_SITUACAO_DO_PEDIDO, NOMES_DO_PEDIDO, pedirAoEncarregado, pendenciasDaExclusao,
  type PedidoDoTitular, type TipoDePedido,
} from '../api/privacidade'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

const CAIXA = 'rounded-md border border-line bg-surface px-3 py-2'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90
 * Título: Mensagem de erro
 * Objetivo: Tirar o texto de um erro desconhecido.
 * Decisões: Texto padrão quando não há mensagem.
 */
function mensagemDe(e: unknown) {
  return e instanceof Error ? e.message : 'Não foi possível concluir.'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90, KAN-17
 * Título: Privacidade na conta
 * Objetivo: Deixar o titular baixar a cópia dos dados, fazer pedidos ao encarregado e excluir a conta.
 * Decisões: Cada ação pede a senha (confirmação de identidade). A exclusão mostra antes o que a impede.
 */
export function SecaoPrivacidade() {
  const { sair } = useSessao()
  const [pedidos, setPedidos] = useState<PedidoDoTitular[]>([])
  const [pendencias, setPendencias] = useState<string[]>([])
  const [senha, setSenha] = useState('')
  const [tipo, setTipo] = useState<TipoDePedido>('CORRECAO')
  const [detalhes, setDetalhes] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    meusPedidosDePrivacidade().then(setPedidos).catch(() => setPedidos([]))
    pendenciasDaExclusao().then((r) => setPendencias(r.pendencias)).catch(() => setPendencias([]))
  }, [versao])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-90
   * Título: Agir
   * Objetivo: Executar a ação, mostrar o aviso e recarregar.
   * Decisões: Limpa a senha depois.
   */
  async function agir(acao: () => Promise<unknown>, texto: string) {
    setErro(null)
    setAviso(null)
    try {
      await acao()
      setAviso(texto)
      setSenha('')
      setVersao((v) => v + 1)
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-90
   * Título: Baixar dados
   * Objetivo: Baixar o arquivo com a cópia dos dados.
   * Decisões: JSON salvo pelo navegador.
   */
  async function baixar() {
    const b = await baixarMeusDados(senha)
    const u = URL.createObjectURL(b)
    const a = document.createElement('a')
    a.href = u
    a.download = 'meus-dados-orbit-hub.json'
    a.click()
    URL.revokeObjectURL(u)
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-17
   * Título: Excluir conta
   * Objetivo: Excluir a conta e sair.
   * Decisões: Sai da sessão logo depois.
   */
  async function excluir(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await excluirMinhaConta(senha, confirmacao)
      await sair()
    } catch (x) {
      setErro(mensagemDe(x))
    }
  }

  return (
    <section aria-label="Privacidade" className="flex flex-col gap-4 rounded-lg border border-line bg-surface p-5">
      <h2 className="text-lg font-semibold">Privacidade e meus dados</h2>
      <p className="text-sm text-muted">Para cada ação, confirme a sua identidade com a senha. O encarregado de dados responde em até 15 dias.</p>
      <Campo rotulo="Senha da conta" type="password" value={senha} onChange={(e) => setSenha(e.target.value)} autoComplete="current-password" />
      {erro && <AlertaErro>{erro}</AlertaErro>}
      {aviso && <p role="status" className="text-sm">{aviso}</p>}
      <button type="button" onClick={() => void agir(baixar, 'Cópia dos dados baixada.')} className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>
        Baixar meus dados
      </button>
      <form aria-label="Pedido ao encarregado" onSubmit={(e) => { e.preventDefault(); void agir(() => pedirAoEncarregado(tipo, detalhes, senha), 'Pedido enviado.') }}
        className="flex flex-col gap-2">
        <label className="flex flex-col gap-1 text-sm font-semibold">Pedido
          <select value={tipo} onChange={(e) => setTipo(e.target.value as TipoDePedido)} className={CAIXA}>
            {(Object.keys(NOMES_DO_PEDIDO) as TipoDePedido[]).map((t) => <option key={t} value={t}>{NOMES_DO_PEDIDO[t]}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm font-semibold">Detalhes
          <textarea value={detalhes} onChange={(e) => setDetalhes(e.target.value)} rows={2} className={CAIXA} />
        </label>
        <button type="submit" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Enviar pedido</button>
      </form>
      {pedidos.length > 0 && (
        <ul aria-label="Meus pedidos de privacidade" className="flex flex-col gap-1 text-sm">
          {pedidos.map((p) => (
            <li key={p.id}>{NOMES_DO_PEDIDO[p.tipo]} · {NOMES_DA_SITUACAO_DO_PEDIDO[p.situacao] ?? p.situacao}{p.resposta ? ` · ${p.resposta}` : ''}</li>
          ))}
        </ul>
      )}
      <form aria-label="Excluir conta" onSubmit={(e) => void excluir(e)} className="flex flex-col gap-2 rounded-md border border-crit p-3">
        <h3 className="font-semibold text-crit">Excluir a conta</h3>
        {pendencias.length > 0 ? (
          <p className="text-sm">Antes, passe a administração de {pendencias.join(', ')} para outra pessoa.</p>
        ) : (
          <>
            <p className="text-sm">Os seus dados pessoais serão removidos ou anonimizados. Registros que a lei obriga a guardar ficam restritos pelo prazo legal.</p>
            <Campo rotulo="Digite EXCLUIR para confirmar" value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)} />
            <button type="submit" className="self-start rounded-md bg-crit px-4 py-2 font-semibold text-white">Excluir minha conta</button>
          </>
        )}
      </form>
    </section>
  )
}
