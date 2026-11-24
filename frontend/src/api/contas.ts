import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:50
 * Cartão Jira: KAN-75
 * Título: Papéis da plataforma
 * Objetivo: Tipar os papéis públicos (investidor, empreendedor) e os da equipe interna.
 * Decisões: Os mesmos nomes do enum Papel do back-end.
 */
export type PapelPublico = 'INVESTIDOR' | 'EMPREENDEDOR'
export type PapelInterno =
  | 'ADMIN' | 'ANALISTA_VERIFICACAO' | 'CURADOR' | 'MODERADOR' | 'SUPORTE_N1' | 'SUPORTE_N2'
  | 'ENCARREGADO_DADOS' | 'JURIDICO'
export type Papel = PapelPublico | PapelInterno

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:50
 * Cartão Jira: KAN-75
 * Título: Nome de cada papel
 * Objetivo: Exibir os papéis em português, num lugar só.
 * Decisões: Nomes da seção 1.2 de processos-e-operacao.md.
 */
export const NOMES_DOS_PAPEIS: Record<Papel, string> = {
  INVESTIDOR: 'Investidor',
  EMPREENDEDOR: 'Empreendedor',
  ADMIN: 'Administrador',
  ANALISTA_VERIFICACAO: 'Analista de verificação',
  CURADOR: 'Curador',
  MODERADOR: 'Moderador',
  SUPORTE_N1: 'Suporte nível 1',
  SUPORTE_N2: 'Suporte nível 2',
  ENCARREGADO_DADOS: 'Encarregado de dados',
  JURIDICO: 'Jurídico',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:50
 * Cartão Jira: KAN-75
 * Título: Papel público
 * Objetivo: Dizer se o papel é de investidor ou empreendedor.
 * Decisões: Os demais são da equipe interna.
 */
export function ehPapelPublico(papel: Papel): papel is PapelPublico {
  return papel === 'INVESTIDOR' || papel === 'EMPREENDEDOR'
}

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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Pedir redefinição de senha
 * Objetivo: Pedir o link de redefinição para o e-mail informado.
 * Decisões: A API responde igual para qualquer e-mail.
 */
export function esqueciASenha(email: string) {
  return requisitar<void>('/api/v1/contas/senha/esqueci', { metodo: 'POST', corpo: { email }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Redefinir a senha
 * Objetivo: Enviar o token do link e a senha nova.
 * Decisões: Erros chegam como ErroApi (link inválido ou senha fraca).
 */
export function redefinirSenha(token: string, senha: string) {
  return requisitar<void>('/api/v1/contas/senha/redefinir', { metodo: 'POST', corpo: { token, senha }, anonima: true })
}
