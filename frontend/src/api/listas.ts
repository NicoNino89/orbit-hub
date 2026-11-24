import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Resumo de uma lista
 * Objetivo: Tipar uma lista com título e contagens.
 * Decisões: "comGrupo" pede o campo de grupo na inclusão.
 */
export interface ResumoDeLista {
  lista: string
  titulo: string
  comGrupo: boolean
  ativos: number
  total: number
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Item de lista
 * Objetivo: Tipar um item como o painel recebe.
 * Decisões: O código identifica o item dentro da lista.
 */
export interface ItemDeLista {
  codigo: string
  rotulo: string
  grupo: string | null
  ordem: number
  ativo: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Alteração de item
 * Objetivo: Tipar uma alteração do histórico do item.
 * Decisões: Ação em texto.
 */
export interface AlteracaoDeItem {
  acao: 'INCLUIDO' | 'RENOMEADO' | 'DESATIVADO' | 'REATIVADO'
  rotuloAnterior: string | null
  rotuloNovo: string | null
  autor: string
  ocorridoEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Resumo das listas
 * Objetivo: Buscar as listas gerenciáveis.
 * Decisões: Sem cache.
 */
export function resumoDasListas() {
  return requisitar<ResumoDeLista[]>('/api/v1/admin/listas')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Itens de uma lista
 * Objetivo: Buscar todos os itens, inclusive desativados.
 * Decisões: Para o painel.
 */
export function itensDaLista(lista: string) {
  return requisitar<ItemDeLista[]>(`/api/v1/admin/listas/${lista}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Incluir item
 * Objetivo: Acrescentar um item à lista.
 * Decisões: Código gerado no servidor a partir do rótulo.
 */
export function incluirItem(lista: string, rotulo: string, grupo?: string) {
  return requisitar<ItemDeLista>(`/api/v1/admin/listas/${lista}`, { metodo: 'POST', corpo: { rotulo, grupo: grupo || undefined } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Renomear item
 * Objetivo: Trocar o rótulo de um item.
 * Decisões: O código não muda.
 */
export function renomearItem(lista: string, codigo: string, rotulo: string) {
  return requisitar<ItemDeLista>(`/api/v1/admin/listas/${lista}/${codigo}`, { metodo: 'PUT', corpo: { rotulo } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Desativar ou reativar
 * Objetivo: Tirar o item dos formulários ou devolvê-lo.
 * Decisões: Não existe exclusão.
 */
export function definirItemAtivo(lista: string, codigo: string, ativo: boolean) {
  return requisitar<ItemDeLista>(`/api/v1/admin/listas/${lista}/${codigo}/${ativo ? 'reativacao' : 'desativacao'}`, { metodo: 'POST' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Histórico de um item
 * Objetivo: Buscar as alterações de um item.
 * Decisões: Da mais recente para a mais antiga.
 */
export function historicoDoItem(lista: string, codigo: string) {
  return requisitar<AlteracaoDeItem[]>(`/api/v1/admin/listas/${lista}/${codigo}/historico`)
}
