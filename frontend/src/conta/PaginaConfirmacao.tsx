import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { confirmarEmail, reenviarConfirmacao } from '../api/contas'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

type Estado = 'confirmando' | 'confirmado' | 'falhou'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Tela de confirmação do e-mail
 * Objetivo: Confirmar o e-mail com o token do link e, se falhar, permitir pedir um link novo.
 * Decisões: A confirmação roda uma única vez (o StrictMode do React monta duas vezes em desenvolvimento e o
 *           token é de uso único). O token não fica visível na tela.
 */
export function PaginaConfirmacao() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const [estado, setEstado] = useState<Estado>(token ? 'confirmando' : 'falhou')
  const jaEnviou = useRef(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Confirmar ao abrir
   * Objetivo: Enviar o token assim que a tela abre.
   * Decisões: useRef impede o segundo envio do StrictMode.
   */
  useEffect(() => {
    if (!token || jaEnviou.current) return
    jaEnviou.current = true
    confirmarEmail(token).then(() => setEstado('confirmado')).catch(() => setEstado('falhou'))
  }, [token])

  if (estado === 'confirmando') {
    return <MolduraConta titulo="Confirmando…"><p role="status" className="text-muted">Só um instante.</p></MolduraConta>
  }
  if (estado === 'confirmado') {
    return (
      <MolduraConta titulo="E-mail confirmado">
        <p role="status">Sua conta está ativa. Entre para começar.</p>
        <Link to="/entrar" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Entrar</Link>
      </MolduraConta>
    )
  }
  return <LinkInvalido />
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Link inválido com reenvio
 * Objetivo: Explicar que o link não vale mais e oferecer um novo.
 * Decisões: A mensagem após o reenvio é a mesma exista ou não a conta.
 */
function LinkInvalido() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:00
   * Cartão Jira: KAN-12
   * Título: Pedir link novo
   * Objetivo: Enviar o pedido de reenvio.
   * Decisões: Mostra a confirmação mesmo se a chamada falhar, para não revelar nada.
   */
  async function reenviar(evento: FormEvent) {
    evento.preventDefault()
    await reenviarConfirmacao(email).catch(() => {})
    setEnviado(true)
  }

  return (
    <MolduraConta titulo="Link inválido ou expirado">
      <AlertaErro>Este link de confirmação não vale mais. Peça um novo abaixo.</AlertaErro>
      {enviado ? (
        <p role="status">Se houver uma conta pendente com esse e-mail, enviamos um novo link.</p>
      ) : (
        <form onSubmit={reenviar} className="flex flex-col gap-4">
          <Campo rotulo="E-mail da conta" type="email" autoComplete="email" required value={email}
            onChange={(e) => setEmail(e.target.value)} />
          <button type="submit" className={CLASSE_BOTAO_PRIMARIO}>Enviar novo link</button>
        </form>
      )}
    </MolduraConta>
  )
}
