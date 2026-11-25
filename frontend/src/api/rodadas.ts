import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Tipos da rodada
 * Objetivo: Tipar a rodada, os termos de captação e o catálogo dos termos por instrumento.
 * Decisões: Mesmos nomes da API; valores em reais.
 */
export type SituacaoDaRodada = 'RASCUNHO' | 'ABERTA' | 'EM_NEGOCIACAO' | 'AGUARDANDO_RESULTADO' | 'ENCERRADA'
export type ResultadoDaRodada = 'CONCLUIDA' | 'CANCELADA' | 'SEM_RESULTADO'

export const NOMES_DA_SITUACAO_DA_RODADA: Record<SituacaoDaRodada, string> = {
  RASCUNHO: 'Rascunho', ABERTA: 'Aberta', EM_NEGOCIACAO: 'Em negociação', AGUARDANDO_RESULTADO: 'Aguardando resultado', ENCERRADA: 'Encerrada',
}

export interface TermosDaRodada {
  estagio: string | null
  instrumento: string | null
  valorAlvo: number | null
  valorMinimo: number | null
  valorMaximo: number | null
  prazo: string | null
  especificos: Record<string, string>
}

export interface Rodada extends TermosDaRodada {
  id: string
  situacao: SituacaoDaRodada
  resultado: ResultadoDaRodada | null
  semLider: boolean
  abertaEm: string | null
  encerradaEm: string | null
  atualizadaEm: string
}

export interface TermoDoCatalogo {
  termo: string
  instrumento: string | null
  rotulo: string
  tipo: 'VALOR' | 'PERCENTUAL' | 'MESES' | 'TEXTO'
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Chamadas da rodada
 * Objetivo: Ler o catálogo dos termos e as rodadas, criar, editar, abrir, negociar e encerrar.
 * Decisões: Regras no servidor.
 */
export function termosDasRodadas() {
  return requisitar<TermoDoCatalogo[]>('/api/v1/rodadas/termos')
}

export function rodadasDaStartup(startupId: string) {
  return requisitar<Rodada[]>(`/api/v1/startups/${startupId}/rodadas`)
}

export function criarRodada(startupId: string, termos: TermosDaRodada) {
  return requisitar<Rodada>(`/api/v1/startups/${startupId}/rodadas`, { metodo: 'POST', corpo: termos })
}

export function editarRodada(rodadaId: string, termos: TermosDaRodada) {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}`, { metodo: 'PUT', corpo: termos })
}

export function acaoNaRodada(rodadaId: string, acao: 'abertura' | 'negociacao') {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}/${acao}`, { metodo: 'POST' })
}

export function encerrarRodada(rodadaId: string, resultado: ResultadoDaRodada, motivo: string) {
  return requisitar<Rodada>(`/api/v1/rodadas/${rodadaId}/encerramento`, { metodo: 'POST', corpo: { resultado, motivo } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Termos do instrumento
 * Objetivo: Escolher, no catálogo, os termos do instrumento escolhido.
 * Decisões: Sem termos próprios, usa o termo genérico (instrumento nulo).
 */
export function termosDoInstrumento(catalogo: TermoDoCatalogo[], instrumento: string | null) {
  if (!instrumento) return []
  const proprios = catalogo.filter((t) => t.instrumento === instrumento)
  return proprios.length > 0 ? proprios : catalogo.filter((t) => t.instrumento === null)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109
 * Título: Condução da rodada
 * Objetivo: Tipar e chamar o painel, a entrada, os convites, a liderança, a política, a prorrogação, o pedido de
 *           encerramento, o cancelamento, a conclusão e a resposta ao investimento.
 * Decisões: Mesmos nomes da API.
 */
export type Politica = 'LIDER_PRIMEIRO' | 'PROPORCIONAL' | 'CRITERIO_EMPREENDEDOR'

export const NOMES_DA_POLITICA: Record<Politica, string> = {
  LIDER_PRIMEIRO: 'Líder primeiro, depois por ordem de entrada', PROPORCIONAL: 'Proporcional às intenções', CRITERIO_EMPREENDEDOR: 'Critério do empreendedor',
}

export interface ParticipanteDaRodada {
  id: string; investidor: string; situacao: 'ATIVO' | 'AGUARDANDO_APROVACAO' | 'SAIU'; intencao: number | null; condicoes: string | null; entrouEm: string
}

export interface InvestimentoDaRodada { id: string; participanteId: string; investidor: string; valor: number | null; situacao: string; criadoEm: string }

export interface PainelDaRodada {
  id: string; situacao: SituacaoDaRodada; resultado: ResultadoDaRodada | null; politica: Politica; lider: string | null; liderConvidado: string | null
  participantes: ParticipanteDaRodada[]; somaIntencoes: number; valorAlvo: number | null; valorMinimo: number | null; valorMaximo: number | null
  excessoDeDemanda: boolean; prazo: string | null; prorrogacoes: number; encerramentoPedido: boolean; valorCaptado: number | null
  instrumentoUtilizado: string | null; dataFechamento: string | null; investimentos: InvestimentoDaRodada[]
}

export interface RodadaParaInvestidor {
  id: string; situacao: SituacaoDaRodada; politica: Politica; lider: string | null; convidadoParaLiderar: boolean; souLider: boolean
  podeEntrar: boolean; minhaParticipacao: ParticipanteDaRodada | null; participantes: { id: string; investidor: string; situacao: string }[]
  somaIntencoes: number | null; meusInvestimentos: InvestimentoDaRodada[]
}

export function painelDaRodada(id: string) {
  return requisitar<PainelDaRodada>(`/api/v1/rodadas/${id}/painel`)
}

export function rodadaParaInvestidor(startupId: string) {
  return requisitar<RodadaParaInvestidor>(`/api/v1/catalogo/startups/${startupId}/rodada`)
}

export function entrarNaRodada(id: string, intencao: number | null, condicoes: string) {
  return requisitar<ParticipanteDaRodada>(`/api/v1/rodadas/${id}/participantes`, { metodo: 'POST', corpo: { intencao, condicoes } })
}

export function atualizarIntencao(id: string, intencao: number | null, condicoes: string) {
  return requisitar<ParticipanteDaRodada>(`/api/v1/rodadas/${id}/participantes/minha`, { metodo: 'PUT', corpo: { intencao, condicoes } })
}

export function convidarCoInvestidor(id: string, email: string) {
  return requisitar<ParticipanteDaRodada>(`/api/v1/rodadas/${id}/convites`, { metodo: 'POST', corpo: { email } })
}

export function decidirConvidado(id: string, participanteId: string, aprovar: boolean) {
  return requisitar<ParticipanteDaRodada>(`/api/v1/rodadas/${id}/participantes/${participanteId}/decisao`, { metodo: 'POST', corpo: { aprovar } })
}

export function convidarLider(id: string, participanteId: string) {
  return requisitar<void>(`/api/v1/rodadas/${id}/participantes/${participanteId}/lideranca`, { metodo: 'POST' })
}

export function responderLideranca(id: string, aprovar: boolean) {
  return requisitar<void>(`/api/v1/rodadas/${id}/lideranca/resposta`, { metodo: 'POST', corpo: { aprovar } })
}

export function definirPolitica(id: string, politica: Politica) {
  return requisitar<{ politica: Politica }>(`/api/v1/rodadas/${id}/politica`, { metodo: 'PUT', corpo: { politica } })
}

export function prorrogarRodada(id: string, prazo: string) {
  return requisitar<unknown>(`/api/v1/rodadas/${id}/prorrogacao`, { metodo: 'POST', corpo: { prazo } })
}

export function pedirEncerramento(id: string) {
  return requisitar<void>(`/api/v1/rodadas/${id}/pedido-de-encerramento`, { metodo: 'POST' })
}

export function cancelarRodada(id: string, motivo: string) {
  return requisitar<unknown>(`/api/v1/rodadas/${id}/cancelamento`, { metodo: 'POST', corpo: { motivo } })
}

export function concluirRodada(id: string, dados: { valorCaptado: number; instrumento: string; dataFechamento: string;
  investidores: { participanteId: string; valor: number | null }[] }) {
  return requisitar<unknown>(`/api/v1/rodadas/${id}/conclusao`, { metodo: 'POST', corpo: dados })
}

export function responderInvestimento(investimentoId: string, aprovar: boolean) {
  return requisitar<InvestimentoDaRodada>(`/api/v1/rodadas/investimentos/${investimentoId}/resposta`, { metodo: 'POST', corpo: { aprovar } })
}
