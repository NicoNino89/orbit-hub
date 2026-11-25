import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-88, KAN-89, KAN-103
 * Título: Central de atendimento
 * Objetivo: Tipar e chamar o catálogo de serviços, os artigos, os chamados (com e sem conta), as disputas e, para a
 *           equipe, a fila, as respostas, o escalonamento, a resolução, as respostas padrão e os indicadores.
 * Decisões: Mesmos nomes da API.
 */
export type NivelDeAtendimento = 'N1' | 'N2' | 'ESPECIALISTA'

export const NOMES_DA_SITUACAO_DO_CHAMADO: Record<string, string> = {
  ABERTO: 'Aberto', EM_ATENDIMENTO: 'Em atendimento', AGUARDANDO_USUARIO: 'Aguardando você', RESOLVIDO: 'Resolvido', FECHADO: 'Fechado',
  AGUARDANDO_CONFIRMACAO: 'Aguardando confirmação do e-mail',
}

export interface ServicoDaPlataforma { codigo: string; nome: string; publico: string; prestador: string; canal: string; prazo: string }

export interface Artigo { id: string; titulo: string; conteudo: string; publicado: boolean; atualizadoEm: string }

export interface ResumoDoChamado {
  id: string; categoria: string; servico: string; situacao: string; nivel: NivelDeAtendimento; criadoEm: string; atualizadoEm: string; souOutraParte: boolean
}

export interface Chamado {
  id: string; categoria: string; categoriaCodigo: string; servico: string; descricao: string; situacao: string; nivel: NivelDeAtendimento
  prioridade: string; prazo: string; contato: string; emailContato: string | null; responsavel: string | null; outraParte: string | null
  souOutraParte: boolean; referenciaTipo: string | null; referenciaId: string | null; resultadoDisputa: string | null; nota: number | null
  comentarioAvaliacao: string | null; escalonamento: string | null; criadoEm: string
  mensagens: { autor: string | null; equipe: boolean; interna: boolean; texto: string; em: string; minha: boolean }[]
}

export interface ItemDaFilaDeAtendimento {
  id: string; categoria: string; servico: string; situacao: string; nivel: NivelDeAtendimento; prioridade: string; prazo: string; vencido: boolean
  responsavel: string | null; contato: string; criadoEm: string
}

export interface RespostaPadrao { id: string; titulo: string; texto: string }

export interface IndicadorDoAtendimento { servico: string; chamados: number; noPrazo: number; satisfacao: number | null; avaliacoes: number }

export function servicosDaPlataforma() {
  return requisitar<ServicoDaPlataforma[]>('/api/v1/ajuda/servicos')
}

export function artigosDaAjuda() {
  return requisitar<Artigo[]>('/api/v1/ajuda/artigos')
}

export function abrirChamadoPublico(dados: { nome: string; email: string; categoria: string; descricao: string }) {
  return requisitar<void>('/api/v1/ajuda/chamados-publicos', { metodo: 'POST', corpo: dados })
}

export function confirmarChamadoPublico(token: string) {
  return requisitar<{ protocolo: string }>('/api/v1/ajuda/chamados-publicos/confirmacao', { metodo: 'POST', corpo: { token } })
}

export function abrirChamado(dados: { categoria: string; servico?: string; descricao: string; anexos?: string; outraParteId?: string
  referenciaTipo?: string; referenciaId?: string }) {
  return requisitar<Chamado>('/api/v1/chamados', { metodo: 'POST', corpo: dados })
}

export function meusChamados() {
  return requisitar<ResumoDoChamado[]>('/api/v1/chamados')
}

export function meuChamado(id: string) {
  return requisitar<Chamado>(`/api/v1/chamados/${id}`)
}

export function responderChamado(id: string, texto: string) {
  return requisitar<Chamado>(`/api/v1/chamados/${id}/mensagens`, { metodo: 'POST', corpo: { texto } })
}

export function avaliarChamado(id: string, nota: number, comentario: string) {
  return requisitar<Chamado>(`/api/v1/chamados/${id}/avaliacao`, { metodo: 'POST', corpo: { nota, comentario } })
}

export function filaDeAtendimento() {
  return requisitar<ItemDaFilaDeAtendimento[]>('/api/v1/admin/atendimento/chamados')
}

export function chamadoDaEquipe(id: string) {
  return requisitar<Chamado>(`/api/v1/admin/atendimento/chamados/${id}`)
}

export function assumirChamado(id: string) {
  return requisitar<Chamado>(`/api/v1/admin/atendimento/chamados/${id}/assumir`, { metodo: 'POST' })
}

export function responderComoEquipe(id: string, texto: string, interna: boolean, aguardarUsuario: boolean) {
  return requisitar<Chamado>(`/api/v1/admin/atendimento/chamados/${id}/mensagens`, { metodo: 'POST', corpo: { texto, interna, aguardarUsuario } })
}

export function escalarChamado(id: string, nivel: NivelDeAtendimento, motivo: string) {
  return requisitar<Chamado>(`/api/v1/admin/atendimento/chamados/${id}/escalonamento`, { metodo: 'POST', corpo: { nivel, motivo } })
}

export function resolverChamado(id: string, texto: string, resultadoDisputa?: string) {
  return requisitar<Chamado>(`/api/v1/admin/atendimento/chamados/${id}/resolucao`, { metodo: 'POST', corpo: { texto, resultadoDisputa } })
}

export function respostasPadrao() {
  return requisitar<RespostaPadrao[]>('/api/v1/admin/atendimento/respostas')
}

export function criarRespostaPadrao(titulo: string, texto: string) {
  return requisitar<RespostaPadrao>('/api/v1/admin/atendimento/respostas', { metodo: 'POST', corpo: { titulo, texto } })
}

export function todosOsArtigos() {
  return requisitar<Artigo[]>('/api/v1/admin/atendimento/artigos')
}

export function salvarArtigo(dados: { id?: string; titulo: string; conteudo: string; publicado: boolean }) {
  return requisitar<Artigo>(dados.id ? `/api/v1/admin/atendimento/artigos/${dados.id}` : '/api/v1/admin/atendimento/artigos',
    { metodo: dados.id ? 'PUT' : 'POST', corpo: { titulo: dados.titulo, conteudo: dados.conteudo, publicado: dados.publicado } })
}

export function indicadoresDoAtendimento() {
  return requisitar<IndicadorDoAtendimento[]>('/api/v1/admin/atendimento/indicadores')
}
