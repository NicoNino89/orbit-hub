import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Canal de aviso
 * Objetivo: Tipar por onde o aviso chega.
 * Decisões: Mesmos nomes da API.
 */
export type Canal = 'PLATAFORMA' | 'EMAIL' | 'AMBOS'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Aviso
 * Objetivo: Tipar um aviso do sino.
 * Decisões: Link interno ou nulo.
 */
export interface Aviso {
  id: string
  tipo: string
  titulo: string
  mensagem: string
  link: string | null
  criadaEm: string
  lida: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Página de avisos
 * Objetivo: Tipar a lista paginada com o total de não lidos.
 * Decisões: Página de 20 no servidor.
 */
export interface PaginaDeAvisos {
  itens: Aviso[]
  pagina: number
  total: number
  naoLidas: number
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Preferência de um tipo
 * Objetivo: Tipar um tipo de aviso com o canal escolhido.
 * Decisões: Obrigatório não pode ser mudado.
 */
export interface PreferenciaDeTipo {
  tipo: string
  grupo: string
  descricao: string
  obrigatoria: boolean
  canal: Canal
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Preferências
 * Objetivo: Tipar os canais por tipo e o resumo diário.
 * Decisões: Na ordem do catálogo.
 */
export interface Preferencias {
  tipos: PreferenciaDeTipo[]
  resumoDiario: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Evento de atualização do sino
 * Objetivo: Nome do evento que a página dispara para o sino recontar.
 * Decisões: Evento do navegador, sem estado global.
 */
export const EVENTO_AVISOS = 'orbit:avisos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Contador do sino
 * Objetivo: Buscar quantos avisos não lidos a pessoa tem.
 * Decisões: Consulta leve.
 */
export function contarNaoLidas() {
  return requisitar<{ naoLidas: number }>('/api/v1/notificacoes/contagem')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Avisos
 * Objetivo: Buscar uma página de avisos.
 * Decisões: Mais novos primeiro.
 */
export function listarAvisos(pagina = 0) {
  return requisitar<PaginaDeAvisos>(`/api/v1/notificacoes?pagina=${pagina}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Marcar como lido
 * Objetivo: Marcar um aviso, ou todos, como lidos.
 * Decisões: Sem id, marca todos.
 */
export function marcarLido(id?: string) {
  return requisitar<void>(id ? `/api/v1/notificacoes/${id}/leitura` : '/api/v1/notificacoes/leitura', { metodo: 'POST' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Ler preferências
 * Objetivo: Buscar os canais por tipo e o resumo diário.
 * Decisões: Sem cache.
 */
export function lerPreferencias() {
  return requisitar<Preferencias>('/api/v1/notificacoes/preferencias')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Salvar preferências
 * Objetivo: Gravar os canais dos tipos não obrigatórios e o resumo diário.
 * Decisões: Os obrigatórios não são enviados.
 */
export function salvarPreferencias(p: Preferencias) {
  const canais = Object.fromEntries(p.tipos.filter((t) => !t.obrigatoria).map((t) => [t.tipo, t.canal]))
  return requisitar<Preferencias>('/api/v1/notificacoes/preferencias', { metodo: 'PUT', corpo: { canais, resumoDiario: p.resumoDiario } })
}
