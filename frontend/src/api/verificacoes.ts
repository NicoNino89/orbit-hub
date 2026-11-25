import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Selo e situação
 * Objetivo: Tipar o selo do perfil e a situação do pedido de verificação.
 * Decisões: Mesmos nomes da API.
 */
export type Selo = 'VERIFICADO' | 'EM_ANALISE' | 'NAO_VERIFICADO'
export type SituacaoDoPedido = 'RASCUNHO' | 'EM_ANALISE' | 'PENDENTE_COMPLEMENTO' | 'VERIFICADO' | 'RECUSADO' | 'ENCERRADO' | 'VENCIDO'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Nomes das situações
 * Objetivo: Escrever cada situação do pedido para a tela.
 * Decisões: Frases curtas.
 */
export const NOMES_DA_SITUACAO: Record<SituacaoDoPedido, string> = {
  RASCUNHO: 'Em preparação: envie os documentos e mande para análise',
  EM_ANALISE: 'Em análise pela equipe de verificação',
  PENDENTE_COMPLEMENTO: 'A análise pediu documentos complementares',
  VERIFICADO: 'Verificado',
  RECUSADO: 'Recusado',
  ENCERRADO: 'Encerrado por falta de complemento',
  VENCIDO: 'Verificação vencida: envie documentos atualizados',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Situação da verificação
 * Objetivo: Tipar a resposta da verificação de quem atua.
 * Decisões: Pedido nulo quando ainda não houve.
 */
export interface SituacaoDaVerificacao {
  sujeito: 'PESSOA' | 'ORGANIZACAO'
  selo: Selo
  podeEnviar: boolean
  exigidos: { codigo: string; rotulo: string }[]
  pedido: { id: string; situacao: SituacaoDoPedido; enviadoEm: string | null; motivo: string | null; observacao: string | null; validoAte: string | null; prazoComplementoAte?: string | null } | null
  documentos: { id: string; tipo: string; nome: string; formato: string; tamanho: number; enviadoEm: string }[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Minha verificação
 * Objetivo: Buscar o selo, o pedido e os documentos de quem atua.
 * Decisões: A organização da atuação vai no cabeçalho, pelo cliente.
 */
export function minhaVerificacao() {
  return requisitar<SituacaoDaVerificacao>('/api/v1/verificacoes/minha')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Enviar documento
 * Objetivo: Subir um documento de um tipo exigido.
 * Decisões: Multipart, um por vez.
 */
export function enviarDocumento(tipo: string, arquivo: File) {
  const formulario = new FormData()
  formulario.append('tipo', tipo)
  formulario.append('arquivo', arquivo)
  return requisitar<unknown>('/api/v1/verificacoes/documentos', { metodo: 'POST', formulario })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Remover documento
 * Objetivo: Tirar um documento do pedido.
 * Decisões: Só em preparação ou complemento.
 */
export function removerDocumento(id: string) {
  return requisitar<void>(`/api/v1/verificacoes/documentos/${id}`, { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Mandar para análise
 * Objetivo: Enviar o pedido para a equipe de verificação.
 * Decisões: O servidor confere os documentos exigidos.
 */
export function mandarParaAnalise() {
  return requisitar<unknown>('/api/v1/verificacoes/envio', { metodo: 'POST' })
}
