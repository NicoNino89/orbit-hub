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
  /** Fim da sessão se não houver uso até lá (inatividade, KAN-15). */
  sessaoExpiraEm: string
  usuario: UsuarioResumo
  termosPendentes: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Desafio do segundo fator
 * Objetivo: Tipar a resposta do login quando a conta é da equipe e falta o código do aplicativo.
 * Decisões: "segundoFator" diz se é a primeira configuração ou só o código.
 */
export interface DesafioApi {
  segundoFator: 'CONFIGURAR' | 'CODIGO'
  desafio: string
  expiraEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Dados para o aplicativo autenticador
 * Objetivo: Tipar o segredo e o endereço otpauth:// devolvidos na configuração.
 * Decisões: O segredo vem em blocos de 4 letras.
 */
export interface ConfiguracaoSegundoFator {
  segredo: string
  endereco: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Entrar
 * Objetivo: Enviar e-mail e senha e receber a sessão ou, nas contas da equipe, o desafio do segundo fator.
 * Decisões: Requisição anônima; o cookie de refresh só vem com a sessão.
 */
export function entrar(email: string, senha: string) {
  return requisitar<SessaoApi | DesafioApi>('/api/v1/auth/entrar', { metodo: 'POST', corpo: { email, senha }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: É desafio
 * Objetivo: Distinguir a resposta de desafio da sessão aberta.
 * Decisões: Pela presença do campo "segundoFator".
 */
export function ehDesafio(resposta: SessaoApi | DesafioApi): resposta is DesafioApi {
  return 'segundoFator' in resposta
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Configurar o aplicativo
 * Objetivo: Pedir o segredo para cadastrar no aplicativo autenticador.
 * Decisões: Só com o desafio do login.
 */
export function configurarSegundoFator(desafio: string) {
  return requisitar<ConfiguracaoSegundoFator>('/api/v1/auth/segundo-fator/configuracao',
    { metodo: 'POST', corpo: { desafio }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Confirmar o código
 * Objetivo: Enviar o código do aplicativo e receber a sessão.
 * Decisões: Mesmo formato de sessão do login comum.
 */
export function confirmarSegundoFator(desafio: string, codigo: string) {
  return requisitar<SessaoApi>('/api/v1/auth/segundo-fator', { metodo: 'POST', corpo: { desafio, codigo }, anonima: true })
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
