import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91
 * Título: Privacidade e operação
 * Objetivo: Tipar e chamar os pedidos do titular, a cópia dos dados, a exclusão da conta, os incidentes, o painel de
 *           operação, os times e a titularidade.
 * Decisões: Mesmos nomes da API.
 */
export type TipoDePedido = 'CONFIRMACAO' | 'ACESSO' | 'CORRECAO' | 'PORTABILIDADE' | 'ANONIMIZACAO' | 'ELIMINACAO' | 'COMPARTILHAMENTOS'
  | 'REVOGACAO_CONSENTIMENTO'

export const NOMES_DO_PEDIDO: Record<TipoDePedido, string> = {
  CONFIRMACAO: 'Confirmar se tratamos meus dados', ACESSO: 'Acesso aos meus dados', CORRECAO: 'Corrigir dados', PORTABILIDADE: 'Portabilidade',
  ANONIMIZACAO: 'Anonimizar ou bloquear dados', ELIMINACAO: 'Eliminar dados', COMPARTILHAMENTOS: 'Com quem meus dados são compartilhados',
  REVOGACAO_CONSENTIMENTO: 'Revogar consentimento',
}

export const NOMES_DA_SITUACAO_DO_PEDIDO: Record<string, string> = {
  ABERTO: 'Aberto', EM_ANALISE: 'Em análise', AGUARDANDO_APROVACAO: 'Aguardando segunda aprovação', CONCLUIDO: 'Concluído', RECUSADO: 'Recusado',
}

export interface PedidoDoTitular {
  id: string; tipo: TipoDePedido; titular: string | null; detalhes: string | null; situacao: string; prazo: string; vencido: boolean
  responsavel: string | null; resposta: string | null; respondidoEm: string | null; criadoEm: string
}

export interface Incidente {
  id: string; titulo: string; descricao: string; dadosAfetados: string | null; titularesAfetados: string | null; detectadoEm: string
  contencao: string | null; risco: string | null; decisoes: string | null; prazoComunicacao: string; comunicacaoAtrasada: boolean
  comunicadoAnpdEm: string | null; comunicadoTitularesEm: string | null; licoes: string | null; acoesPreventivas: string | null; situacao: string
  historico: { autor: string | null; acao: string; detalhe: string | null; em: string }[]
}

export interface SituacaoDaFila {
  fila: string; nome: string; abertos: number; idadeMediaHoras: number; pertoDoPrazo: number; vencidos: number; porResponsavel: Record<string, number>
}

export interface PainelDeOperacao {
  filas: SituacaoDaFila[]
  indicadores: { casosNoMes: number; horasMediasParaResolver: number | null; percentualNoPrazo: number | null; percentualDeRecursosAceitos: number | null
    satisfacao: number | null }
  ecossistema: { novosCadastros: number; startupsPublicadas: number; matches: number; reunioesRealizadas: number; rodadasAbertas: number
    investimentosConcretizados: number }
  geradoEm: string
}

export interface Time {
  time: string; nome: string; liderId: string | null; lider: string | null; distribuicao: string
  membros: { id: string; nome: string | null; ausenteHoje: boolean }[]
}

export interface Ausencia { id: string; membroId: string; inicio: string; fim: string; motivo: string | null }

export function pedirAoEncarregado(tipo: TipoDePedido, detalhes: string, senha: string) {
  return requisitar<PedidoDoTitular>('/api/v1/privacidade/pedidos', { metodo: 'POST', corpo: { tipo, detalhes, senha } })
}

export function meusPedidosDePrivacidade() {
  return requisitar<PedidoDoTitular[]>('/api/v1/privacidade/pedidos')
}

export function baixarMeusDados(senha: string) {
  return requisitar<Blob>('/api/v1/privacidade/meus-dados', { metodo: 'POST', corpo: { senha }, comoArquivo: true })
}

export function pendenciasDaExclusao() {
  return requisitar<{ pendencias: string[] }>('/api/v1/conta/exclusao')
}

export function excluirMinhaConta(senha: string, confirmacao: string) {
  return requisitar<void>('/api/v1/conta/exclusao', { metodo: 'POST', corpo: { senha, confirmacao } })
}

export function filaDePrivacidade() {
  return requisitar<PedidoDoTitular[]>('/api/v1/admin/privacidade/pedidos')
}

export function responderPedido(id: string, resposta: string, recusar: boolean) {
  return requisitar<PedidoDoTitular>(`/api/v1/admin/privacidade/pedidos/${id}/resposta`, { metodo: 'POST', corpo: { resposta, recusar } })
}

export function proporExclusaoDoTitular(id: string, justificativa: string) {
  return requisitar<PedidoDoTitular>(`/api/v1/admin/privacidade/pedidos/${id}/exclusao`, { metodo: 'POST', corpo: { justificativa } })
}

export function registrarIncidente(dados: { titulo: string; descricao: string }) {
  return requisitar<Incidente>('/api/v1/admin/incidentes', { metodo: 'POST', corpo: dados })
}

export function incidentes() {
  return requisitar<Incidente[]>('/api/v1/admin/incidentes')
}

export function avaliarIncidente(id: string, dados: { contencao?: string; risco?: string; decisoes?: string; titularesAfetados?: string }) {
  return requisitar<Incidente>(`/api/v1/admin/incidentes/${id}/avaliacao`, { metodo: 'PUT', corpo: dados })
}

export function comunicarIncidente(id: string, destino: 'ANPD' | 'TITULARES') {
  return requisitar<Incidente>(`/api/v1/admin/incidentes/${id}/comunicacao`, { metodo: 'POST', corpo: { destino } })
}

export function encerrarIncidente(id: string, licoes: string, acoesPreventivas: string) {
  return requisitar<Incidente>(`/api/v1/admin/incidentes/${id}/encerramento`, { metodo: 'POST', corpo: { licoes, acoesPreventivas } })
}

export function painelDeOperacao() {
  return requisitar<PainelDeOperacao>('/api/v1/admin/operacao/painel')
}

export function timesInternos() {
  return requisitar<Time[]>('/api/v1/admin/times')
}

export function definirLiderDoTime(time: string, membroId: string) {
  return requisitar<void>(`/api/v1/admin/times/${time}/lider`, { metodo: 'PUT', corpo: { membroId } })
}

export function minhasAusencias() {
  return requisitar<Ausencia[]>('/api/v1/admin/times/ausencias')
}

export function registrarAusencia(inicio: string, fim: string, motivo: string) {
  return requisitar<Ausencia>('/api/v1/admin/times/ausencias', { metodo: 'POST', corpo: { inicio, fim, motivo } })
}

export function removerAusencia(id: string) {
  return requisitar<void>(`/api/v1/admin/times/ausencias/${id}`, { metodo: 'DELETE' })
}

export function devolverCasosDoMembro(id: string) {
  return requisitar<{ casos: number }>(`/api/v1/admin/times/membros/${id}/devolucao`, { metodo: 'POST' })
}

export function convidarOrganizacao(dados: { nome: string; tipo: string; cnpj: string; emailResponsavel: string }) {
  return requisitar<{ organizacaoId: string }>('/api/v1/admin/atendimento/organizacoes-assistidas', { metodo: 'POST', corpo: dados })
}

export function proporTransferencia(dados: { alvo: 'STARTUP' | 'ORGANIZACAO'; alvoId: string; emailNovoTitular: string; comprovacao: string }) {
  return requisitar<{ propostaId: string }>('/api/v1/admin/atendimento/transferencias', { metodo: 'POST', corpo: dados })
}
