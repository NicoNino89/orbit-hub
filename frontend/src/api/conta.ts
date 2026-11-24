import { requisitar } from './cliente'
import type { Papel } from './contas'

export interface Conta {
  id: string
  nome: string
  email: string
  telefone: string | null
  temFoto: boolean
  papeis: Papel[]
}

export interface EventoConta {
  evento: 'PAPEL_ATIVADO' | 'PAPEL_DESATIVADO' | 'EMAIL_ALTERADO' | 'SENHA_ALTERADA'
  detalhe: string | null
  ocorridoEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Ler a conta
 * Objetivo: Buscar os dados da conta logada.
 * Decisões: Usa o token da sessão.
 */
export function buscarConta() {
  return requisitar<Conta>('/api/v1/conta')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Salvar perfil
 * Objetivo: Atualizar nome e telefone.
 * Decisões: Telefone vazio apaga o contato.
 */
export function salvarPerfil(nome: string, telefone: string) {
  return requisitar<Conta>('/api/v1/conta', { metodo: 'PUT', corpo: { nome, telefone } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Enviar foto
 * Objetivo: Enviar a imagem escolhida como foto do perfil.
 * Decisões: Formulário multipart com o campo "arquivo".
 */
export function enviarFoto(arquivo: File) {
  const formulario = new FormData()
  formulario.append('arquivo', arquivo)
  return requisitar<Conta>('/api/v1/conta/foto', { metodo: 'PUT', formulario })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Buscar foto
 * Objetivo: Baixar a foto do perfil como arquivo, com o token da sessão.
 * Decisões: A tela transforma o Blob em endereço local (URL.createObjectURL).
 */
export function buscarFoto() {
  return requisitar<Blob>('/api/v1/conta/foto', { comoArquivo: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Pedir troca de e-mail
 * Objetivo: Enviar o novo e-mail e a senha atual.
 * Decisões: A troca só acontece depois da confirmação pelo novo endereço.
 */
export function pedirTrocaDeEmail(novoEmail: string, senhaAtual: string) {
  return requisitar<void>('/api/v1/conta/email', { metodo: 'POST', corpo: { novoEmail, senhaAtual } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Confirmar novo e-mail
 * Objetivo: Enviar o token do link recebido no novo endereço.
 * Decisões: Rota pública.
 */
export function confirmarNovoEmail(token: string) {
  return requisitar<void>('/api/v1/contas/email/confirmacao', { metodo: 'POST', corpo: { token }, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Trocar senha
 * Objetivo: Enviar a senha atual e a nova.
 * Decisões: Os outros aparelhos são desconectados pela API.
 */
export function trocarSenha(senhaAtual: string, novaSenha: string) {
  return requisitar<void>('/api/v1/conta/senha', { metodo: 'PUT', corpo: { senhaAtual, novaSenha } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Ativar papel
 * Objetivo: Acrescentar um papel à conta, com os aceites necessários.
 * Decisões: Devolve a rota de onboarding do papel.
 */
export function ativarPapel(papel: Papel, aceites: string[]) {
  return requisitar<{ proximoPasso: string }>(`/api/v1/conta/papeis/${papel}`, { metodo: 'POST', corpo: { aceites } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Desativar papel
 * Objetivo: Retirar um papel da conta.
 * Decisões: A API recusa com mensagem quando há pendências ou é o último papel.
 */
export function desativarPapel(papel: Papel) {
  return requisitar<void>(`/api/v1/conta/papeis/${papel}`, { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Histórico da conta
 * Objetivo: Buscar as mudanças de papéis, e-mail e senha.
 * Decisões: Do mais recente ao mais antigo.
 */
export function buscarHistorico() {
  return requisitar<EventoConta[]>('/api/v1/conta/historico')
}
