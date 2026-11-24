import { useState, type FormEvent } from 'react'
import { configurarSegundoFator, type ConfiguracaoSegundoFator, type DesafioApi } from '../api/autenticacao'
import { ErroApi } from '../api/cliente'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Etapa do segundo fator
 * Objetivo: Depois da senha de uma conta da equipe, pedir o código do aplicativo autenticador e, no primeiro
 *           acesso, conduzir a configuração do aplicativo.
 * Decisões: Na configuração, mostra o segredo em blocos (para digitar no celular) e o link otpauth:// (abre o
 *           aplicativo direto quando a tela está no próprio celular). O código aceita só dígitos e usa
 *           autocomplete "one-time-code", que alguns celulares preenchem sozinhos. Desafio vencido ou bloqueio
 *           mandam de volta para a senha.
 */
export function EtapaSegundoFator({ desafio, aoVoltar }: { desafio: DesafioApi; aoVoltar: () => void }) {
  const { confirmarSegundoFator } = useSessao()
  const [configuracao, setConfiguracao] = useState<ConfiguracaoSegundoFator | null>(null)
  const [codigo, setCodigo] = useState('')
  const [erro, setErro] = useState<ErroApi | null>(null)
  const [enviando, setEnviando] = useState(false)
  const precisaConfigurar = desafio.segundoFator === 'CONFIGURAR'

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Gerar a chave do aplicativo
   * Objetivo: Pedir à API o segredo para cadastrar no aplicativo autenticador.
   * Decisões: Só no primeiro acesso.
   */
  async function gerarChave() {
    setErro(null)
    try {
      setConfiguracao(await configurarSegundoFator(desafio.desafio))
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, 'Não foi possível gerar a chave. Tente novamente.'))
    }
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Enviar o código
   * Objetivo: Confirmar o código e abrir a sessão.
   * Decisões: A tela de login redireciona sozinha quando a sessão abre.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await confirmarSegundoFator(desafio.desafio, codigo)
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, 'Não foi possível conferir o código. Tente novamente.'))
      setEnviando(false)
    }
  }

  const terminal = erro?.codigo === 'desafio_invalido' || erro?.codigo === 'login_bloqueado'

  return (
    <MolduraConta titulo="Verificação em duas etapas">
      {precisaConfigurar && !configuracao && (
        <div className="flex flex-col gap-3 text-sm">
          <p>
            Contas da equipe usam um segundo fator. Instale um aplicativo autenticador no celular (Google
            Authenticator, Microsoft Authenticator ou Authy) e gere a chave da sua conta.
          </p>
          <button type="button" className={CLASSE_BOTAO_PRIMARIO} onClick={() => void gerarChave()}>
            Gerar a chave
          </button>
        </div>
      )}
      {configuracao && (
        <div className="flex flex-col gap-2 rounded-md border border-line bg-surface p-3 text-sm">
          <p>No aplicativo, adicione uma conta com a chave abaixo (tipo: baseada em tempo).</p>
          <p className="break-all rounded bg-surface-2 px-2 py-1 text-center font-mono text-base tracking-wider"
            aria-label="Chave do aplicativo autenticador">
            {configuracao.segredo}
          </p>
          <a href={configuracao.endereco} className={CLASSE_LINK}>Abrir no aplicativo (no celular)</a>
        </div>
      )}
      {(!precisaConfigurar || configuracao) && (
        <form onSubmit={enviar} className="flex flex-col gap-4">
          <Campo rotulo="Código de 6 dígitos" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9 ]{6,7}"
            maxLength={7} required value={codigo} onChange={(e) => setCodigo(e.target.value.replace(/[^0-9 ]/g, ''))}
            dica="O código muda a cada 30 segundos." />
          <button type="submit" disabled={enviando || terminal} className={CLASSE_BOTAO_PRIMARIO}>
            {enviando ? 'Conferindo…' : 'Confirmar'}
          </button>
        </form>
      )}
      {erro && <AlertaErro>{erro.message}</AlertaErro>}
      <button type="button" className={`${CLASSE_LINK} self-start text-sm`} onClick={aoVoltar}>
        Voltar e entrar com outra conta
      </button>
    </MolduraConta>
  )
}
