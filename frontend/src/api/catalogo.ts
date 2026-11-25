import { requisitar } from './cliente'
import type { SecaoDaStartup, SituacaoDaStartup } from './startups'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-29, KAN-36, KAN-66, KAN-84, KAN-85
 * Título: Tipos do catálogo e da curadoria
 * Objetivo: Tipar a apresentação em três páginas, a visibilidade, as revisões da curadoria e as edições temáticas.
 * Decisões: Mesmos nomes da API.
 */
export type Selo = 'VERIFICADO' | 'EM_ANALISE' | 'NAO_VERIFICADO'

export interface ItemDaApresentacao { rotulo: string; valor: string }
export interface BlocoDaApresentacao { titulo: string; itens: ItemDaApresentacao[]; avisos: string[] }
export interface PaginaDaApresentacao { id: string; titulo: string; blocos: BlocoDaApresentacao[] }

export interface Apresentacao {
  modo: 'CATALOGO' | 'PREVIA' | 'COMPLETA'
  comoVerificado: boolean
  cabecalho: {
    id: string
    nome: string
    segmento: string
    estagio: string | null
    localizacao: string | null
    site: string | null
    redes: { rede: string; url: string }[]
    temLogotipo: boolean
    naoConstituida: boolean
    selo: Selo
  }
  paginas: PaginaDaApresentacao[]
}

export interface ItemDeVisibilidade {
  item: { codigo: string; rotulo: string; secao: SecaoDaStartup; padraoPublico: boolean }
  publico: boolean
}

export interface ConfiguracaoDeVisibilidade { itens: ItemDeVisibilidade[]; participaEdicoes: boolean }

export type SituacaoDaRevisao = 'EM_ANALISE' | 'AGUARDANDO_APROVACAO' | 'APROVADA' | 'DEVOLVIDA' | 'RECUSADA' | 'CANCELADA'
export type TipoDaRevisao = 'PUBLICACAO' | 'ALTERACAO'

export const NOMES_DA_SITUACAO_DA_REVISAO: Record<SituacaoDaRevisao, string> = {
  EM_ANALISE: 'Em análise', AGUARDANDO_APROVACAO: 'Recusa aguardando segunda aprovação', APROVADA: 'Aprovada',
  DEVOLVIDA: 'Devolvida para ajustes', RECUSADA: 'Recusada', CANCELADA: 'Cancelada',
}

export const NOMES_DO_TIPO_DA_REVISAO: Record<TipoDaRevisao, string> = {
  PUBLICACAO: 'Publicação', ALTERACAO: 'Alteração relevante',
}

export interface Apontamento { tipo: 'CHECKLIST' | 'SECAO'; codigo: string; rotulo: string; comentario: string | null }

export interface RevisaoParaEquipe {
  id: string
  tipo: TipoDaRevisao
  situacao: SituacaoDaRevisao
  enviadaEm: string
  decididaEm: string | null
  parecer: string | null
  prazo: string | null
  apontamentos: Apontamento[]
}

export interface ItemDaFilaDeCuradoria {
  id: string
  startupId: string
  startup: string
  tipo: TipoDaRevisao
  situacao: SituacaoDaRevisao
  enviadaEm: string
  prazo: string
  atrasado: boolean
  responsavel: string | null
  conflitoComigo: boolean
  semCuradorLivre: boolean
}

export interface RevisaoDeCuradoria {
  id: string
  tipo: TipoDaRevisao
  situacao: SituacaoDaRevisao
  enviadaEm: string
  decididaEm: string | null
  parecer: string | null
  aguardandoSegundaAprovacao: boolean
  itens: { tipo: 'CHECKLIST' | 'SECAO'; codigo: string; atendido: boolean | null; comentario: string | null }[]
}

export interface DetalheDaCuradoria {
  revisao: RevisaoDeCuradoria
  startupId: string
  startup: string
  cnpj: string | null
  situacaoDaStartup: SituacaoDaStartup
  seloDaStartup: Selo
  responsavel: string | null
  prazo: string
  checklist: { codigo: string; rotulo: string }[]
  fundadores: { nome: string | null; papel: string; selo: Selo }[]
  alteracoes: { campo: string; aprovado: string | null; novo: string | null }[]
  anteriores: RevisaoDeCuradoria[]
}

export interface DecisaoDaCuradoria {
  resultado: 'APROVADA' | 'DEVOLVIDA' | 'RECUSADA'
  checklist: { codigo: string; atendido: boolean; comentario: string | null }[]
  apontamentos: { secao: SecaoDaStartup; comentario: string }[]
  parecer: string
}

export type SituacaoDaEdicao = 'RASCUNHO' | 'AGENDADA' | 'VIGENTE' | 'ARQUIVADA'

export const NOMES_DA_SITUACAO_DA_EDICAO: Record<SituacaoDaEdicao, string> = {
  RASCUNHO: 'Rascunho', AGENDADA: 'Agendada', VIGENTE: 'No ar', ARQUIVADA: 'Arquivada',
}

export interface CartaoDeStartup {
  id: string
  nome: string
  segmento: string
  estagio: string | null
  localizacao: string | null
  temLogotipo: boolean
}

export interface Edicao {
  id: string
  titulo: string
  descricao: string
  inicio: string
  fim: string
  situacao: SituacaoDaEdicao
  temCapa: boolean
  quantidade: number
  curador: string | null
  atualizadaEm: string
}

export interface DetalheDaEdicao {
  edicao: Edicao
  criterios: string
  startups: { startup: CartaoDeStartup; visivel: boolean }[]
  historico: { acao: string; detalhe: string | null; por: string | null; em: string }[]
}

export interface DadosDaEdicao { titulo: string; descricao: string; criterios: string; inicio: string; fim: string | null }

export interface EdicaoNoCatalogo {
  id: string
  titulo: string
  descricao: string
  temCapa: boolean
  inicio: string
  fim: string
  startups: CartaoDeStartup[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-29, KAN-36
 * Título: Chamadas da apresentação e da visibilidade
 * Objetivo: Ler a apresentação (catálogo, pré-visualização e completa), o logotipo, e ler e trocar a visibilidade.
 * Decisões: Logotipo como arquivo (a rota exige login).
 */
export function apresentacaoNoCatalogo(id: string) {
  return requisitar<Apresentacao>(`/api/v1/catalogo/startups/${id}`)
}

export function previaDaApresentacao(id: string, verificado: boolean) {
  return requisitar<Apresentacao>(`/api/v1/startups/${id}/apresentacao?verificado=${verificado}`)
}

export function apresentacaoCompleta(id: string) {
  return requisitar<Apresentacao>(`/api/v1/admin/curadoria/startups/${id}/apresentacao`)
}

export function logotipoNoCatalogo(id: string) {
  return requisitar<Blob>(`/api/v1/catalogo/startups/${id}/logotipo`, { comoArquivo: true })
}

export function lerVisibilidade(id: string) {
  return requisitar<ConfiguracaoDeVisibilidade>(`/api/v1/startups/${id}/visibilidade`)
}

export function salvarVisibilidade(id: string, escolhas: Record<string, boolean>) {
  return requisitar<ConfiguracaoDeVisibilidade>(`/api/v1/startups/${id}/visibilidade`, { metodo: 'PUT', corpo: escolhas })
}

export function participarDeEdicoes(id: string, participa: boolean) {
  return requisitar<ConfiguracaoDeVisibilidade>(`/api/v1/startups/${id}/participacao-edicoes`, { metodo: 'PUT', corpo: { participa } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66, KAN-84
 * Título: Chamadas da curadoria
 * Objetivo: Ler as revisões da startup (equipe), a fila e o detalhe (curador), assumir, decidir e declarar impedimento.
 * Decisões: Backoffice em /api/v1/admin/curadoria.
 */
export function revisoesDaStartup(id: string) {
  return requisitar<RevisaoParaEquipe[]>(`/api/v1/startups/${id}/curadoria`)
}

export function filaDaCuradoria() {
  return requisitar<ItemDaFilaDeCuradoria[]>('/api/v1/admin/curadoria')
}

export function detalheDaCuradoria(id: string) {
  return requisitar<DetalheDaCuradoria>(`/api/v1/admin/curadoria/${id}`)
}

export function assumirRevisao(id: string) {
  return requisitar<RevisaoDeCuradoria>(`/api/v1/admin/curadoria/${id}/assumir`, { metodo: 'POST' })
}

export function decidirRevisao(id: string, decisao: DecisaoDaCuradoria) {
  return requisitar<RevisaoDeCuradoria>(`/api/v1/admin/curadoria/${id}/decisao`, { metodo: 'POST', corpo: decisao })
}

export function declararImpedimentoNaCuradoria(id: string, motivo: string) {
  return requisitar<RevisaoDeCuradoria>(`/api/v1/admin/curadoria/${id}/impedimento`, { metodo: 'POST', corpo: { motivo } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Chamadas das edições
 * Objetivo: Montar as edições (curadoria) e ler as vigentes (catálogo).
 * Decisões: Capa por multipart.
 */
export function listarEdicoes() {
  return requisitar<Edicao[]>('/api/v1/admin/edicoes')
}

export function detalheDaEdicao(id: string) {
  return requisitar<DetalheDaEdicao>(`/api/v1/admin/edicoes/${id}`)
}

export function startupsElegiveis() {
  return requisitar<CartaoDeStartup[]>('/api/v1/admin/edicoes/startups-elegiveis')
}

export function criarEdicao(dados: DadosDaEdicao) {
  return requisitar<Edicao>('/api/v1/admin/edicoes', { metodo: 'POST', corpo: dados })
}

export function editarEdicao(id: string, dados: DadosDaEdicao) {
  return requisitar<Edicao>(`/api/v1/admin/edicoes/${id}`, { metodo: 'PUT', corpo: dados })
}

export function comporEdicao(id: string, startups: string[]) {
  return requisitar<Edicao>(`/api/v1/admin/edicoes/${id}/startups`, { metodo: 'PUT', corpo: { startups } })
}

export function publicarEdicao(id: string) {
  return requisitar<Edicao>(`/api/v1/admin/edicoes/${id}/publicacao`, { metodo: 'POST' })
}

export function arquivarEdicao(id: string) {
  return requisitar<Edicao>(`/api/v1/admin/edicoes/${id}/arquivamento`, { metodo: 'POST' })
}

export function trocarCapaDaEdicao(id: string, arquivo: File) {
  const dados = new FormData()
  dados.append('arquivo', arquivo)
  return requisitar<Edicao>(`/api/v1/admin/edicoes/${id}/capa`, { metodo: 'PUT', formulario: dados })
}

export function edicoesVigentes() {
  return requisitar<EdicaoNoCatalogo[]>('/api/v1/catalogo/edicoes')
}

export function edicaoVigente(id: string) {
  return requisitar<EdicaoNoCatalogo>(`/api/v1/catalogo/edicoes/${id}`)
}
