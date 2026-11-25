import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Vínculo declarado
 * Objetivo: Tipar um vínculo de membro da equipe.
 * Decisões: Alvo é o CNPJ formatado ou o e-mail; "membro" só na lista do administrador.
 */
export interface Vinculo {
  id: string
  membroId: string
  membro: string | null
  tipo: 'ORGANIZACAO' | 'STARTUP' | 'PESSOA'
  alvo: string
  descricao: string
  criadoEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Nomes dos tipos
 * Objetivo: Escrever cada tipo de vínculo e o que informar.
 * Decisões: Organização e startup pelo CNPJ; pessoa pelo e-mail.
 */
export const TIPOS_DE_VINCULO: Record<Vinculo['tipo'], { nome: string; alvo: string }> = {
  ORGANIZACAO: { nome: 'Organização investidora', alvo: 'CNPJ' },
  STARTUP: { nome: 'Startup', alvo: 'CNPJ' },
  PESSOA: { nome: 'Pessoa', alvo: 'E-mail' },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Meus vínculos
 * Objetivo: Buscar os vínculos de quem está logado.
 * Decisões: Sem cache.
 */
export function meusVinculos() {
  return requisitar<Vinculo[]>('/api/v1/admin/vinculos/meus')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Vínculos da equipe
 * Objetivo: Buscar os vínculos de todos os membros (administrador).
 * Decisões: Para a revisão de acessos.
 */
export function vinculosDaEquipe() {
  return requisitar<Vinculo[]>('/api/v1/admin/vinculos')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Declarar vínculo
 * Objetivo: Registrar um vínculo.
 * Decisões: Validação no servidor.
 */
export function declararVinculo(tipo: Vinculo['tipo'], alvo: string, descricao: string) {
  return requisitar<Vinculo>('/api/v1/admin/vinculos', { metodo: 'POST', corpo: { tipo, alvo, descricao } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Remover vínculo
 * Objetivo: Tirar um vínculo próprio.
 * Decisões: Só o próprio.
 */
export function removerVinculo(id: string) {
  return requisitar<void>(`/api/v1/admin/vinculos/${id}`, { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Declarar impedimento na verificação
 * Objetivo: Registrar que quem está logado não pode atuar num pedido de verificação.
 * Decisões: Motivo obrigatório.
 */
export function declararImpedimentoNaVerificacao(pedido: string, motivo: string) {
  return requisitar<unknown>(`/api/v1/admin/verificacoes/${pedido}/impedimento`, { metodo: 'POST', corpo: { motivo } })
}
