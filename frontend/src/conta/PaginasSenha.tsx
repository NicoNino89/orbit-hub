import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErroApi } from '../api/cliente'
import { esqueciASenha, redefinirSenha } from '../api/contas'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Tela "esqueci minha senha"
 * Objetivo: Pedir o link de redefinição pelo e-mail.
 * Decisões: A confirmação na tela é a mesma exista ou não a conta, e aparece mesmo se a chamada falhar.
 */
export function PaginaEsqueciSenha() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [enviando, setEnviando] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Enviar pedido
   * Objetivo: Chamar a API e mostrar a confirmação.
   * Decisões: Erros são engolidos para não revelar nada.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    await esqueciASenha(email).catch(() => undefined)
    setEnviado(true)
  }

  return (
    <MolduraConta titulo="Esqueci minha senha">
      {enviado ? (
        <>
          <p role="status">
            Se houver uma conta ativa com <strong>{email}</strong>, enviamos um link para criar uma senha nova. O link
            vale por 30 minutos.
          </p>
          <Link to="/entrar" className={CLASSE_LINK}>Voltar para o login</Link>
        </>
      ) : (
        <form onSubmit={enviar} className="flex flex-col gap-4">
          <p className="text-muted">Informe o e-mail da sua conta. Vamos enviar um link para você criar uma senha nova.</p>
          <Campo rotulo="E-mail" type="email" autoComplete="email" required value={email}
            onChange={(e) => setEmail(e.target.value)} />
          <button type="submit" disabled={enviando} className={CLASSE_BOTAO_PRIMARIO}>Enviar link</button>
          <Link to="/entrar" className={`${CLASSE_LINK} text-sm`}>Voltar para o login</Link>
        </form>
      )}
    </MolduraConta>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Tela de senha nova
 * Objetivo: Criar a senha nova a partir do link do e-mail.
 * Decisões: Pede a senha duas vezes; a política é conferida no servidor. Link inválido leva a pedir outro.
 *           Depois da troca, todas as sessões abertas caem, e a tela avisa isso.
 */
export function PaginaRedefinirSenha() {
  const [parametros] = useSearchParams()
  const token = parametros.get('token') ?? ''
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<ErroApi | null>(null)
  const [diferentes, setDiferentes] = useState(false)
  const [concluido, setConcluido] = useState(false)
  const [enviando, setEnviando] = useState(false)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:30
   * Cartão Jira: KAN-14
   * Título: Enviar senha nova
   * Objetivo: Conferir a repetição e enviar a senha com o token.
   * Decisões: Senhas diferentes não chegam a ir para a API.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setDiferentes(senha !== confirmacao)
    if (senha !== confirmacao) return
    setEnviando(true)
    try {
      await redefinirSenha(token, senha)
      setConcluido(true)
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, 'Não foi possível trocar a senha.'))
    } finally {
      setEnviando(false)
    }
  }

  if (concluido) {
    return (
      <MolduraConta titulo="Senha alterada">
        <p role="status">Sua senha foi trocada. Por segurança, encerramos as sessões abertas em todos os aparelhos.</p>
        <Link to="/entrar" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Entrar</Link>
      </MolduraConta>
    )
  }
  if (!token || erro?.codigo === 'link_invalido') {
    return (
      <MolduraConta titulo="Link inválido ou expirado">
        <AlertaErro>Este link de redefinição não vale mais. Peça um novo.</AlertaErro>
        <Link to="/esqueci-senha" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Pedir novo link</Link>
      </MolduraConta>
    )
  }
  return (
    <MolduraConta titulo="Criar senha nova">
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <Campo rotulo="Senha nova" type="password" autoComplete="new-password" required minLength={10} maxLength={128}
          value={senha} onChange={(e) => setSenha(e.target.value)}
          erro={erro?.campos.senha ?? (erro?.codigo === 'senha_fraca' ? erro.message : undefined)}
          dica="Pelo menos 10 caracteres. Uma frase fácil de lembrar é mais segura que uma senha curta e complicada." />
        <Campo rotulo="Repita a senha nova" type="password" autoComplete="new-password" required value={confirmacao}
          onChange={(e) => setConfirmacao(e.target.value)}
          erro={diferentes ? 'As duas senhas precisam ser iguais.' : undefined} />
        {erro && erro.codigo !== 'senha_fraca' && Object.keys(erro.campos).length === 0 && (
          <AlertaErro>{erro.message}</AlertaErro>
        )}
        <button type="submit" disabled={enviando} className={CLASSE_BOTAO_PRIMARIO}>Salvar senha nova</button>
      </form>
    </MolduraConta>
  )
}
