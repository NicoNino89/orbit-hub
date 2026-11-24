import { requisitar } from './cliente'
import type { PapelInterno } from './contas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Membro da equipe
 * Objetivo: Tipar um membro como a API devolve.
 * Decisões: Situação em linguagem de negócio; nunca traz o segredo do segundo fator.
 */
export interface Membro {
  id: string
  nome: string
  email: string
  papeis: PapelInterno[]
  situacao: 'CONVIDADO' | 'ATIVO' | 'DESLIGADO'
  segundoFatorAtivo: boolean
  criadoEm: string
  desligadoEm: string | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Situação da revisão de acessos
 * Objetivo: Tipar a última revisão, a próxima e se está vencida.
 * Decisões: Datas ISO.
 */
export interface SituacaoRevisao {
  ultimaEm: string | null
  ultimoRevisor: string | null
  proximaEm: string
  vencida: boolean
  intervaloMeses: number
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Listar a equipe
 * Objetivo: Buscar todos os membros.
 * Decisões: Inclui convidados e desligados.
 */
export function listarEquipe() {
  return requisitar<Membro[]>('/api/v1/admin/equipe')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Convidar membro
 * Objetivo: Criar o membro e enviar o convite.
 * Decisões: O membro escolhe a senha pelo link.
 */
export function convidarMembro(nome: string, email: string, papeis: PapelInterno[]) {
  return requisitar<Membro>('/api/v1/admin/equipe', { metodo: 'POST', corpo: { nome, email, papeis } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Reenviar convite
 * Objetivo: Mandar o convite de novo.
 * Decisões: Só para convidados.
 */
export function reenviarConvite(id: string) {
  return requisitar<void>(`/api/v1/admin/equipe/${id}/convite`, { metodo: 'POST' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Trocar papéis
 * Objetivo: Definir os papéis de um membro.
 * Decisões: Conjunto inteiro.
 */
export function alterarPapeis(id: string, papeis: PapelInterno[]) {
  return requisitar<Membro>(`/api/v1/admin/equipe/${id}/papeis`, { metodo: 'PUT', corpo: { papeis } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Desligar membro
 * Objetivo: Revogar o acesso de um membro.
 * Decisões: Devolve quantos casos voltaram à fila.
 */
export function desligarMembro(id: string, justificativa: string) {
  return requisitar<{ casosDevolvidos: number }>(`/api/v1/admin/equipe/${id}/desligamento`,
    { metodo: 'POST', corpo: { justificativa } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Redefinir segundo fator
 * Objetivo: Apagar o segundo fator de um membro.
 * Decisões: O membro configura de novo no próximo login.
 */
export function redefinirSegundoFator(id: string) {
  return requisitar<void>(`/api/v1/admin/equipe/${id}/segundo-fator/redefinicao`, { metodo: 'POST' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Situação da revisão
 * Objetivo: Buscar a situação da revisão de acessos.
 * Decisões: Sem cache.
 */
export function buscarRevisao() {
  return requisitar<SituacaoRevisao>('/api/v1/admin/equipe/revisao')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Confirmar revisão
 * Objetivo: Registrar a revisão de acessos.
 * Decisões: Observação opcional.
 */
export function confirmarRevisao(observacao: string) {
  return requisitar<unknown>('/api/v1/admin/equipe/revisao', { metodo: 'POST', corpo: { observacao } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Aceitar convite
 * Objetivo: Definir a senha a partir do link do convite.
 * Decisões: Requisição anônima: o membro ainda não tem sessão.
 */
export function aceitarConvite(token: string, senha: string) {
  return requisitar<void>('/api/v1/equipe/convite', { metodo: 'POST', corpo: { token, senha }, anonima: true })
}
