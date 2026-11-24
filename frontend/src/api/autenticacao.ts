import { ErroApi, requisitar } from './cliente'
import type { Papel } from './contas'

export interface UsuarioResumo {
  id: string
  nome: string
  email: string
  papeis: Papel[]
}

export interface SessaoApi {
  tokenAcesso: string
  expiraEm: string
  usuario: UsuarioResumo
  termosPendentes: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Entrar
 * Objetivo: Enviar e-mail e senha e receber a sessão (o refresh token chega no cookie HttpOnly).
 * Decisões: Rota anônima: nenhum token antigo é enviado.
 */
export function entrar(email: string, senha: string) {
  return requisitar<SessaoApi>('/api/v1/auth/entrar', { metodo: 'POST', corpo: { email, senha }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Sair
 * Objetivo: Encerrar a sessão no servidor e apagar o cookie.
 * Decisões: Erros são ignorados: do ponto de vista da tela, sair sempre funciona.
 */
export async function sair() {
  await requisitar<void>('/api/v1/auth/sair', { metodo: 'POST', anonima: true }).catch(() => undefined)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Renovar a sessão
 * Objetivo: Trocar o refresh token do cookie por uma sessão nova.
 * Decisões: 409 (outra aba acabou de renovar) é repetido uma vez depois de um instante, porque o navegador
 *           já recebeu o cookie novo da outra resposta.
 */
export async function renovar(): Promise<SessaoApi> {
  try {
    return await requisitar<SessaoApi>('/api/v1/auth/renovar', { metodo: 'POST', anonima: true })
  } catch (erro) {
    if (erro instanceof ErroApi && erro.status === 409) {
      await new Promise((r) => setTimeout(r, 300))
      return requisitar<SessaoApi>('/api/v1/auth/renovar', { metodo: 'POST', anonima: true })
    }
    throw erro
  }
}
