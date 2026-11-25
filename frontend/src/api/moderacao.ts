import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86, KAN-99, KAN-100, KAN-101, KAN-102
 * Título: Moderação e recursos
 * Objetivo: Tipar e chamar a denúncia, as sanções, os recursos, a fila e o caso de moderação, as medidas preventivas,
 *           o levantamento de acessos, o parecer e a decisão.
 * Decisões: Mesmos nomes da API.
 */
export type ObjetoDenunciado = 'PERFIL' | 'ORGANIZACAO' | 'STARTUP' | 'CONEXAO' | 'REUNIAO'
export type DecisaoDaModeracao = 'SEM_ACAO' | 'REMOCAO_CONTEUDO' | 'ADVERTENCIA' | 'SUSPENSAO' | 'BANIMENTO' | 'ESCALA'
export type MedidaPreventiva = 'SUSPENDER_CONTA' | 'DESPUBLICAR' | 'SUSPENDER_NIVEL2'
export type OrigemDoRecurso = 'VERIFICACAO' | 'CURADORIA' | 'SANCAO'

export const NOMES_DA_DECISAO: Record<DecisaoDaModeracao, string> = {
  SEM_ACAO: 'Sem ação', REMOCAO_CONTEUDO: 'Remoção ou correção de conteúdo', ADVERTENCIA: 'Advertência', SUSPENSAO: 'Suspensão',
  BANIMENTO: 'Banimento (segunda aprovação)', ESCALA: 'Próximo degrau da escala',
}

export const NOMES_DA_SANCAO: Record<string, string> = {
  ADVERTENCIA: 'Advertência', SUSPENSAO: 'Suspensão', BANIMENTO: 'Banimento', REMOCAO_CONTEUDO: 'Remoção de conteúdo',
  PREVENTIVA_SUSPENDER_CONTA: 'Conta suspensa (preventiva)', PREVENTIVA_DESPUBLICAR: 'Startup despublicada (preventiva)',
  PREVENTIVA_SUSPENDER_NIVEL2: 'Nível 2 suspenso (preventiva)',
}

export const NOMES_DA_PRIORIDADE: Record<string, string> = { CRITICA: 'Crítica', ALTA: 'Alta', NORMAL: 'Normal' }

export const NOMES_DA_ORIGEM: Record<OrigemDoRecurso, string> = {
  VERIFICACAO: 'Recusa da verificação', CURADORIA: 'Recusa da curadoria', SANCAO: 'Sanção da moderação',
}

export interface Sancao {
  id: string; alvoTipo: 'USUARIO' | 'STARTUP'; alvoId: string; tipo: string; dias: number | null; inicio: string; fim: string | null
  motivo: string; situacao: string
}

export interface MinhaSancao { sancao: Sancao; motivo: string; recorrerAte: string; podeRecorrer: boolean }

export interface MinhaDenuncia { protocolo: string; objetoTipo: ObjetoDenunciado; categoria: string; criadaEm: string; analisada: boolean }

export interface Recurso {
  id: string; origemTipo: OrigemDoRecurso; origemId: string; autor: string | null; argumentos: string; anexos: string | null; situacao: string
  prazo: string; criadoEm: string; julgadoPor: string | null; julgadoEm: string | null; motivo: string | null
}

export interface ItemDaFilaDeModeracao {
  id: string; objetoTipo: ObjetoDenunciado; objeto: string; prioridade: string; categoria: string; situacao: string; prazo: string
  vencido: boolean; responsavel: string | null; denuncias: number; criadoEm: string
}

export interface CasoDeModeracao {
  id: string; objetoTipo: ObjetoDenunciado; objetoId: string; objeto: string; prioridade: string; categoria: string; situacao: string
  prazo: string; responsavel: string | null; parecerJuridico: string | null; decisao: string | null; motivo: string | null
  observacao: string | null; alvoPadrao: 'USUARIO' | 'STARTUP' | null
  denuncias: { id: string; denunciante: string | null; categoria: string; descricao: string; evidencias: string | null; criadaEm: string
    semAcaoAntes: number }[]
  historico: Sancao[]; anteriores: { id: string; categoria: string; decisao: string; decididoEm: string }[]; medidas: Sancao[]
}

export interface AcessoConfidencial {
  pessoaId: string; pessoa: string | null; liberadoEm: string; validoAte: string; encerradoEm: string | null; motivo: string | null; links: number
}

export function denunciar(dados: { objetoTipo: ObjetoDenunciado; objetoId: string; categoria: string; descricao: string; evidencias?: string }) {
  return requisitar<{ protocolo: string; recebidaEm: string }>('/api/v1/denuncias', { metodo: 'POST', corpo: dados })
}

export function minhasDenuncias() {
  return requisitar<MinhaDenuncia[]>('/api/v1/denuncias')
}

export function minhasSancoes() {
  return requisitar<MinhaSancao[]>('/api/v1/sancoes')
}

export function recorrer(dados: { origemTipo: OrigemDoRecurso; origemId: string; argumentos: string; anexos?: string }) {
  return requisitar<Recurso>('/api/v1/recursos', { metodo: 'POST', corpo: dados })
}

export function meusRecursos() {
  return requisitar<Recurso[]>('/api/v1/recursos')
}

export function filaDeModeracao() {
  return requisitar<ItemDaFilaDeModeracao[]>('/api/v1/admin/moderacao/casos')
}

export function casoDeModeracao(id: string) {
  return requisitar<CasoDeModeracao>(`/api/v1/admin/moderacao/casos/${id}`)
}

export function assumirCaso(id: string) {
  return requisitar<CasoDeModeracao>(`/api/v1/admin/moderacao/casos/${id}/assumir`, { metodo: 'POST' })
}

export function aplicarPreventiva(id: string, tipo: MedidaPreventiva, motivo: string, alvoId?: string) {
  return requisitar<CasoDeModeracao>(`/api/v1/admin/moderacao/casos/${id}/medida-preventiva`, { metodo: 'POST', corpo: { tipo, motivo, alvoId: alvoId || undefined } })
}

export function acessosAoNivel2(id: string, justificativa: string) {
  return requisitar<AcessoConfidencial[]>(`/api/v1/admin/moderacao/casos/${id}/acessos-nivel2?justificativa=${encodeURIComponent(justificativa)}`)
}

export function registrarParecer(id: string, texto: string) {
  return requisitar<CasoDeModeracao>(`/api/v1/admin/moderacao/casos/${id}/parecer`, { metodo: 'PUT', corpo: { texto } })
}

export function decidirCaso(id: string, dados: { decisao: DecisaoDaModeracao; motivo: string; observacao?: string; alvoId?: string; dias?: number }) {
  return requisitar<CasoDeModeracao>(`/api/v1/admin/moderacao/casos/${id}/decisao`, { metodo: 'POST', corpo: dados })
}

export function filaDeRecursos() {
  return requisitar<Recurso[]>('/api/v1/admin/recursos')
}

export function julgarRecurso(id: string, deferir: boolean, motivo: string) {
  return requisitar<Recurso>(`/api/v1/admin/recursos/${id}/julgamento`, { metodo: 'POST', corpo: { deferir, motivo } })
}
