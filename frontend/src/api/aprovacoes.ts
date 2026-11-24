import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Proposta de ação grave
 * Objetivo: Tipar uma proposta como a API devolve.
 * Decisões: "possoDecidir" e "minha" vêm calculados pelo servidor para a pessoa logada.
 */
export interface Proposta {
  id: string
  tipo: string
  tipoDescricao: string
  objetoTipo: string
  objetoId: string
  resumo: string
  dados: Record<string, string>
  justificativa: string
  propostoPor: string
  propostoEm: string
  situacao: 'PENDENTE' | 'APROVADA' | 'REJEITADA' | 'CANCELADA'
  vencida: boolean
  decididoPor: string | null
  decididoEm: string | null
  justificativaDecisao: string | null
  possoDecidir: boolean
  minha: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Fila de aprovações
 * Objetivo: Tipar a fila e a lista de ações graves.
 * Decisões: "disponivel" diz se a ação já existe na plataforma.
 */
export interface FilaDeAprovacoes {
  propostas: Proposta[]
  acoes: { acao: string; descricao: string; disponivel: boolean }[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Buscar a fila
 * Objetivo: Ler as propostas pendentes e decididas.
 * Decisões: Sem cache.
 */
export function buscarAprovacoes() {
  return requisitar<FilaDeAprovacoes>('/api/v1/admin/aprovacoes')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Decidir proposta
 * Objetivo: Aprovar ou rejeitar uma proposta com justificativa.
 * Decisões: A aprovação executa a ação no servidor.
 */
export function decidirProposta(id: string, decisao: 'aprovacao' | 'rejeicao', justificativa: string) {
  return requisitar<Proposta>(`/api/v1/admin/aprovacoes/${id}/${decisao}`, { metodo: 'POST', corpo: { justificativa } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Cancelar proposta
 * Objetivo: Desistir da própria proposta pendente.
 * Decisões: Só quem propôs.
 */
export function cancelarProposta(id: string) {
  return requisitar<Proposta>(`/api/v1/admin/aprovacoes/${id}/cancelamento`, { metodo: 'POST' })
}
