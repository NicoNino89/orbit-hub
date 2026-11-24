import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { ErroApi } from '../api/cliente'
import { aceitarConvite } from '../api/equipe'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO } from '../ui/estilos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:05
 * Cartão Jira: KAN-75
 * Título: Aceite do convite da equipe
 * Objetivo: Deixar o membro convidado escolher a própria senha e avisar do segundo fator no primeiro acesso.
 * Decisões: Mesma política e mesmos campos da redefinição de senha (KAN-14). Link inválido ou usado mostra a
 *           orientação de pedir um novo ao administrador.
 */
export function PaginaConviteEquipe() {
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
   * Hora: 12:05
   * Cartão Jira: KAN-75
   * Título: Enviar a senha
   * Objetivo: Conferir a repetição e aceitar o convite.
   * Decisões: A política de senha é conferida no servidor.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setDiferentes(senha !== confirmacao)
    if (senha !== confirmacao) return
    setEnviando(true)
    try {
      await aceitarConvite(token, senha)
      setConcluido(true)
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, 'Não foi possível aceitar o convite.'))
    } finally {
      setEnviando(false)
    }
  }

  if (concluido) {
    return (
      <MolduraConta titulo="Bem-vindo à equipe">
        <p role="status">
          Senha criada. Ao entrar pela primeira vez, você vai configurar um aplicativo autenticador no celular
          (Google Authenticator, Microsoft Authenticator ou Authy).
        </p>
        <Link to="/entrar" className={`${CLASSE_BOTAO_PRIMARIO} self-start`}>Entrar</Link>
      </MolduraConta>
    )
  }
  if (!token || erro?.codigo === 'link_invalido') {
    return (
      <MolduraConta titulo="Convite inválido ou expirado">
        <AlertaErro>Este convite não vale mais. Peça um novo ao administrador da plataforma.</AlertaErro>
      </MolduraConta>
    )
  }
  return (
    <MolduraConta titulo="Aceitar convite da equipe">
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <Campo rotulo="Senha" type="password" autoComplete="new-password" required minLength={10} maxLength={128}
          value={senha} onChange={(e) => setSenha(e.target.value)}
          erro={erro?.campos.senha ?? (erro?.codigo === 'senha_fraca' ? erro.message : undefined)}
          dica="Pelo menos 10 caracteres. Uma frase fácil de lembrar é mais segura que uma senha curta e complicada." />
        <Campo rotulo="Repita a senha" type="password" autoComplete="new-password" required value={confirmacao}
          onChange={(e) => setConfirmacao(e.target.value)}
          erro={diferentes ? 'As duas senhas precisam ser iguais.' : undefined} />
        {erro && erro.codigo !== 'senha_fraca' && Object.keys(erro.campos).length === 0 && (
          <AlertaErro>{erro.message}</AlertaErro>
        )}
        <button type="submit" disabled={enviando} className={CLASSE_BOTAO_PRIMARIO}>Criar senha</button>
      </form>
    </MolduraConta>
  )
}
