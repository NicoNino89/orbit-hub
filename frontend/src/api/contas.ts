import { requisitar } from './cliente'

export type Papel = 'INVESTIDOR' | 'EMPREENDEDOR' | 'ADMIN'

export interface NovoCadastro {
  nome: string
  email: string
  senha: string
  papeis: Papel[]
  aceites: string[]
}

export interface ContaCriada {
  id: string
  status: string
  papeis: Papel[]
  confirmacaoPendente: boolean
  proximosPassos: string[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Cadastrar conta
 * Objetivo: Enviar o cadastro e devolver a conta criada.
 * Decisões: Rota pública; erros chegam como ErroApi com campos ou código.
 */
export function cadastrar(dados: NovoCadastro) {
  return requisitar<ContaCriada>('/api/v1/contas', { metodo: 'POST', corpo: dados, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Confirmar e-mail
 * Objetivo: Enviar o token do link de confirmação.
 * Decisões: 204 em caso de sucesso.
 */
export function confirmarEmail(token: string) {
  return requisitar<void>('/api/v1/contas/confirmacao', { metodo: 'POST', corpo: { token }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Reenviar confirmação
 * Objetivo: Pedir um novo link de confirmação.
 * Decisões: A API responde igual para qualquer e-mail.
 */
export function reenviarConfirmacao(email: string) {
  return requisitar<void>('/api/v1/contas/confirmacao/reenvio', { metodo: 'POST', corpo: { email }, anonima: true })
}
