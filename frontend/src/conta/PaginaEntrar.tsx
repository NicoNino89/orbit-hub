import { useState, type FormEvent } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router'
import type { DesafioApi } from '../api/autenticacao'
import { ErroApi } from '../api/cliente'
import { reenviarConfirmacao } from '../api/contas'
import { useSessao } from '../sessao/contexto'
import { AlertaErro, Campo, MolduraConta } from '../ui/Campo'
import { CLASSE_BOTAO_PRIMARIO, CLASSE_LINK } from '../ui/estilos'
import { EtapaSegundoFator } from './EtapaSegundoFator'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Destino seguro depois do login
 * Objetivo: Ler o parâmetro "voltar" e aceitar só caminhos internos.
 * Decisões: Recusa endereços externos ("//site", "https:") para o link de login não virar redirecionamento
 *           aberto para phishing; sem destino válido, vai para o painel.
 */
function destinoSeguro(voltar: string | null) {
  return voltar && voltar.startsWith('/') && !voltar.startsWith('//') ? voltar : '/painel'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Tela de login
 * Objetivo: Entrar com e-mail e senha e voltar à página de origem.
 * Decisões: Mostra as mensagens da API (genéricas para credenciais e bloqueio). Para conta não confirmada,
 *           oferece o reenvio do link. Quem já está logado vai direto ao destino. Conta da equipe segue para a
 *           etapa do segundo fator (KAN-75).
 */
export function PaginaEntrar() {
  const { estado, entrar } = useSessao()
  const [parametros] = useSearchParams()
  const destino = destinoSeguro(parametros.get('voltar'))
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<ErroApi | null>(null)
  const [enviando, setEnviando] = useState(false)
  const [reenviado, setReenviado] = useState(false)
  const [desafio, setDesafio] = useState<DesafioApi | null>(null)

  if (estado.status === 'autenticado') {
    return <Navigate to={destino} replace />
  }

  if (desafio) {
    return <EtapaSegundoFator desafio={desafio} aoVoltar={() => { setDesafio(null); setSenha('') }} />
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 10:39
   * Cartão Jira: KAN-75
   * Título: Enviar login
   * Objetivo: Chamar a API e tratar o erro.
   * Decisões: Em caso de sucesso a sessão muda e o Navigate acima leva ao destino. Se a API pedir o segundo
   *           fator, guarda o desafio e mostra a etapa do código.
   */
  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      const pendente = await entrar(email, senha)
      if (pendente) {
        setDesafio(pendente)
        setEnviando(false)
      }
    } catch (e) {
      setErro(e instanceof ErroApi ? e : new ErroApi(0, 'Não foi possível entrar. Tente novamente.'))
      setEnviando(false)
    }
  }

  return (
    <MolduraConta titulo="Entrar">
      {estado.status === 'anonimo' && estado.expirou && (
        <p role="status" className="rounded-md bg-info-soft px-3 py-2 text-sm text-info">
          Sua sessão expirou por inatividade. Entre de novo para continuar de onde parou.
        </p>
      )}
      <form onSubmit={enviar} className="flex flex-col gap-4">
        <Campo rotulo="E-mail" type="email" autoComplete="username" required value={email}
          onChange={(e) => setEmail(e.target.value)} />
        <Campo rotulo="Senha" type="password" autoComplete="current-password" required value={senha}
          onChange={(e) => setSenha(e.target.value)} />
        {erro && (
          <AlertaErro>
            {erro.message}
            {erro.codigo === 'conta_pendente' && (
              reenviado
                ? <span className="mt-1 block">Se a conta estiver pendente, enviamos um novo link.</span>
                : <button type="button" className={`${CLASSE_LINK} mt-1 block`}
                    onClick={() => reenviarConfirmacao(email).finally(() => setReenviado(true))}>
                    Reenviar o link de confirmação
                  </button>
            )}
          </AlertaErro>
        )}
        <button type="submit" disabled={enviando} className={CLASSE_BOTAO_PRIMARIO}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
      <div className="flex flex-col gap-1 text-sm text-muted">
        <Link to="/esqueci-senha" className={CLASSE_LINK}>Esqueci minha senha</Link>
        <span>Ainda não tem conta? <Link to="/cadastro" className={CLASSE_LINK}>Criar conta</Link></span>
      </div>
    </MolduraConta>
  )
}
