import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
 * Título: Agenda
 * Objetivo: Tipar e chamar a proposta, as respostas, a agenda, o convite .ics, a remarcação, o cancelamento, o registro, a
 *           anotação e a reunião coletiva.
 * Decisões: Horários em ISO (UTC); a tela mostra no fuso do navegador.
 */
export type TipoDeReuniao = 'APRESENTACAO' | 'DUE_DILIGENCE' | 'NEGOCIACAO' | 'RODADA'
export type FormatoDeReuniao = 'VIDEO' | 'PRESENCIAL'
export type ResultadoDaReuniao = 'REALIZADA' | 'AUSENCIA_OUTRA_PARTE' | 'REMARCADA' | 'CANCELADA'
export type ProximoPasso = 'ANALISE' | 'NEGOCIACAO' | 'NOVA_REUNIAO' | 'ENCERRAR'

export const NOMES_DO_TIPO: Record<TipoDeReuniao, string> = {
  APRESENTACAO: 'Apresentação', DUE_DILIGENCE: 'Due diligence', NEGOCIACAO: 'Negociação', RODADA: 'Reunião da rodada',
}

export const NOMES_DA_SITUACAO_DA_REUNIAO: Record<string, string> = {
  PROPOSTA: 'Proposta', CONFIRMADA: 'Confirmada', RECUSADA: 'Recusada', EXPIRADA: 'Expirada', CANCELADA: 'Cancelada', REMARCADA: 'Remarcada',
}

export const NOMES_DO_RESULTADO: Record<ResultadoDaReuniao, string> = {
  REALIZADA: 'Realizada', AUSENCIA_OUTRA_PARTE: 'A outra parte não compareceu', REMARCADA: 'Remarcada', CANCELADA: 'Cancelada',
}

export const NOMES_DO_PROXIMO_PASSO: Record<ProximoPasso, string> = {
  ANALISE: 'Seguir para análise', NEGOCIACAO: 'Seguir para negociação', NOVA_REUNIAO: 'Nova reunião', ENCERRAR: 'Encerrar a conexão',
}

export interface Reuniao {
  id: string; conexaoId: string | null; rodadaId: string | null; startupId: string; startup: string; tipo: TipoDeReuniao; formato: FormatoDeReuniao
  local: string; pauta: string; duracaoMinutos: number; coletiva: boolean; situacao: string; ladoDaVez: string | null; contrapropostas: number
  inicio: string | null; motivo: string | null; tardia: boolean; resumo: string | null; opcoes: { ordem: number; inicio: string }[]
  participantes: { usuarioId: string; nome: string | null; lado: string; resposta: string }[]
  registros: { lado: string; resultado: ResultadoDaReuniao; proximoPasso: ProximoPasso | null; em: string }[]
  minhaAnotacao: string | null; aguardaVoce: boolean
}

export interface DadosDaProposta {
  tipo: TipoDeReuniao; formato: FormatoDeReuniao; local: string; pauta: string; duracaoMinutos: number; opcoes: string[]; membros: string[]
}

export function proporReuniao(conexaoId: string, dados: DadosDaProposta) {
  return requisitar<Reuniao>(`/api/v1/conexoes/${conexaoId}/reunioes`, { metodo: 'POST', corpo: dados })
}

export function minhasReunioes() {
  return requisitar<Reuniao[]>('/api/v1/reunioes')
}

export function aceitarHorario(id: string, ordem: number) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/aceite`, { metodo: 'POST', corpo: { ordem } })
}

export function contrapropor(id: string, opcoes: string[]) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/contraproposta`, { metodo: 'POST', corpo: { opcoes } })
}

export function recusarReuniao(id: string, motivo: string) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/recusa`, { metodo: 'POST', corpo: { motivo } })
}

export function cancelarReuniao(id: string, motivo: string) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/cancelamento`, { metodo: 'POST', corpo: { motivo } })
}

export function remarcarReuniao(id: string, opcoes: string[]) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/remarcacao`, { metodo: 'POST', corpo: { opcoes } })
}

export function registrarReuniao(id: string, dados: { resultado: ResultadoDaReuniao; proximoPasso: ProximoPasso | null; aplicarNaConexao: boolean; resumo: string | null }) {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/registro`, { metodo: 'POST', corpo: dados })
}

export function anotarReuniao(id: string, texto: string, compartilhar: boolean) {
  return requisitar<void>(`/api/v1/reunioes/${id}/anotacao`, { metodo: 'PUT', corpo: { texto, compartilhar } })
}

export function responderPresenca(id: string, resposta: 'CONFIRMADO' | 'RECUSADO' | 'TALVEZ') {
  return requisitar<Reuniao>(`/api/v1/reunioes/${id}/presenca`, { metodo: 'POST', corpo: { resposta } })
}

export function conviteDaReuniao(id: string) {
  return requisitar<Blob>(`/api/v1/reunioes/${id}/convite.ics`, { comoArquivo: true })
}

export function convocarReuniaoDaRodada(startupId: string, dados: { inicio: string; formato: FormatoDeReuniao; local: string; pauta: string;
  duracaoMinutos: number; participantes: string[] }) {
  return requisitar<Reuniao>(`/api/v1/startups/${startupId}/rodada/reunioes`, { metodo: 'POST', corpo: dados })
}
