import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { confirmarNovoEmail } from '../api/conta'
import { AlertaErro, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Tela de confirmação do novo e-mail
 * Objetivo: Aplicar a troca de e-mail com o token do link enviado ao novo endereço.
 * Decisões: Envia o token uma única vez (StrictMode) e funciona sem login, porque o link pode ser aberto em
 *           outro aparelho.
 */
export function PaginaNovoEmail() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const [estado, setEstado] = useState<'confirmando' | 'confirmado' | 'falhou'>(token ? 'confirmando' : 'falhou')
  const [mensagem, setMensagem] = useState('Este link não vale mais. Peça a troca de novo na sua conta.')
  const jaEnviou = useRef(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:40
   * Cartão Jira: KAN-16
   * Título: Confirmar ao abrir
   * Objetivo: Enviar o token assim que a tela abre.
   * Decisões: Mostra a mensagem da API quando o e-mail já foi tomado por outra conta.
   */
  useEffect(() => {
    if (!token || jaEnviou.current) return
    jaEnviou.current = true
    confirmarNovoEmail(token)
      .then(() => setEstado('confirmado'))
      .catch((e: unknown) => {
        if (e instanceof Error && !e.message.includes('inválido')) setMensagem(e.message)
        setEstado('falhou')
      })
  }, [token])

  if (estado === 'confirmando') {
    return <MolduraConta titulo="Confirmando…"><p role="status" className="text-muted">Só um instante.</p></MolduraConta>
  }
  if (estado === 'confirmado') {
    return (
      <MolduraConta titulo="E-mail alterado">
        <p role="status">Pronto. A partir de agora, use o novo e-mail para entrar.</p>
        <Link to="/painel" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Ir para o painel</Link>
      </MolduraConta>
    )
  }
  return (
    <MolduraConta titulo="Link inválido ou expirado">
      <AlertaErro>{mensagem}</AlertaErro>
    </MolduraConta>
  )
}
