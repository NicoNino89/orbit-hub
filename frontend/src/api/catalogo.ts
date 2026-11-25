import { requisitar } from './cliente'
import type { SecaoDaStartup, SituacaoDaStartup } from './startups'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
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
 * Hora: 11:03
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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38, KAN-39, KAN-41, KAN-42, KAN-43, KAN-44
 * Título: Navegação e listas pessoais do catálogo
 * Objetivo: Tipar e chamar a navegação com filtros, os favoritos, o histórico e os descartes.
 * Decisões: Filtros viram parâmetros repetidos na consulta; vazios ficam de fora.
 */
export type ListaDoCatalogo = 'PERSONALIZADA' | 'NOVAS'

export interface FiltrosDoCatalogo {
  segmento: string[]
  maturidade: string[]
  localizacao: string[]
  comRodada: '' | 'true' | 'false'
  estagio: string[]
  instrumento: string[]
  captacaoMinima: string
  captacaoMaxima: string
}

export const SEM_FILTROS: FiltrosDoCatalogo = {
  segmento: [], maturidade: [], localizacao: [], comRodada: '', estagio: [], instrumento: [], captacaoMinima: '', captacaoMaxima: '',
}

export interface ItemDoCatalogo {
  startup: CartaoDeStartup
  criterios: string[]
  rodadaAberta: boolean
  favorito: boolean
  publicadaEm: string | null
}

export interface ResultadoDoCatalogo { lista: ListaDoCatalogo; verificado: boolean; comPerfil: boolean; itens: ItemDoCatalogo[] }

export type TipoDeMarcacao = 'FAVORITOS' | 'HISTORICO' | 'DESCARTADAS'

export interface Marcada { startup: CartaoDeStartup; disponivel: boolean; em: string }

export function consultaDosFiltros(lista: ListaDoCatalogo, f: FiltrosDoCatalogo) {
  const p = new URLSearchParams({ lista })
  for (const chave of ['segmento', 'maturidade', 'localizacao', 'estagio', 'instrumento'] as const) {
    f[chave].forEach((v) => p.append(chave, v))
  }
  if (f.comRodada) p.set('comRodada', f.comRodada)
  if (f.captacaoMinima) p.set('captacaoMinima', f.captacaoMinima)
  if (f.captacaoMaxima) p.set('captacaoMaxima', f.captacaoMaxima)
  return p.toString()
}

export function navegarNoCatalogo(lista: ListaDoCatalogo, filtros: FiltrosDoCatalogo) {
  return requisitar<ResultadoDoCatalogo>(`/api/v1/catalogo/startups?${consultaDosFiltros(lista, filtros)}`)
}

export function marcacoesDaStartup(id: string) {
  return requisitar<{ favorito: boolean; descartada: boolean }>(`/api/v1/catalogo/startups/${id}/marcacoes`)
}

export function favoritar(id: string, salvar: boolean) {
  return requisitar<void>(`/api/v1/catalogo/favoritos/${id}`, { metodo: salvar ? 'PUT' : 'DELETE' })
}

export function descartar(id: string, descartada: boolean) {
  return requisitar<void>(`/api/v1/catalogo/descartadas/${id}`, { metodo: descartada ? 'PUT' : 'DELETE' })
}

export function minhasMarcacoes(tipo: TipoDeMarcacao) {
  return requisitar<Marcada[]>(`/api/v1/catalogo/minhas/${tipo}`)
}

export function limparHistorico() {
  return requisitar<void>('/api/v1/catalogo/minhas/HISTORICO', { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46, KAN-47, KAN-48, KAN-49, KAN-67
 * Título: Conexões e bloqueios
 * Objetivo: Tipar e chamar o interesse, o Match, o ciclo das conexões, os painéis e os bloqueios da startup.
 * Decisões: Mesmos nomes da API.
 */
export type SituacaoDaConexao = 'PENDENTE' | 'RECUSADA' | 'EXPIRADA' | 'RETIRADA' | 'MATCH' | 'EM_ANALISE' | 'EM_NEGOCIACAO' | 'INVESTIU' | 'ENCERRADA'
export type PedidoDaConexao = 'NENHUM' | 'INFORMACOES' | 'APRESENTACAO'

export const NOMES_DA_SITUACAO_DA_CONEXAO: Record<SituacaoDaConexao, string> = {
  PENDENTE: 'Pendente', RECUSADA: 'Recusada', EXPIRADA: 'Expirada', RETIRADA: 'Retirada', MATCH: 'Match', EM_ANALISE: 'Em análise',
  EM_NEGOCIACAO: 'Em negociação', INVESTIU: 'Investiu', ENCERRADA: 'Encerrada',
}

export const NOMES_DO_PEDIDO: Record<PedidoDaConexao, string> = {
  NENHUM: 'Só demonstrar interesse', INFORMACOES: 'Pedir informações adicionais', APRESENTACAO: 'Pedir uma apresentação',
}

export interface ConexaoDoPainel {
  id: string
  startupId: string
  startup: string
  situacao: SituacaoDaConexao
  valorPreliminar: number | null
  condicoes: string | null
  pedido: PedidoDaConexao
  mensagem: string | null
  criadaEm: string
  atualizadaEm: string
  aguardaVoce: boolean
  motivo: string | null
  investidor: { pessoa: string | null; organizacao: string | null; tipo: string | null; ticketMinimo: number | null;
    ticketMaximo: number | null; lideranca: string | null; selo: Selo } | null
  contatos: { nome: string | null; email: string | null }[]
  feitaPor: string | null
}

export interface ConexaoParaInvestidor { conexao: ConexaoDoPainel | null; podeManifestar: boolean; impedimento: string | null; precisaVerificar: boolean }

export interface MudancaDaConexao { de: SituacaoDaConexao | null; para: SituacaoDaConexao; por: string | null; lado: string; em: string }

export interface PainelDeBloqueios {
  ativos: { tipo: 'PESSOA' | 'ORGANIZACAO'; alvoId: string; rotulo: string; em: string }[]
  historico: { acao: string; tipo: string; rotulo: string; por: string | null; em: string }[]
  apenasVerificados: boolean
}

export function conexaoComAStartup(startupId: string) {
  return requisitar<ConexaoParaInvestidor>(`/api/v1/catalogo/startups/${startupId}/conexao`)
}

export function demonstrarInteresse(startupId: string, dados: { valor: number | null; condicoes: string; pedido: PedidoDaConexao; mensagem: string }) {
  return requisitar<{ id: string; situacao: SituacaoDaConexao }>(`/api/v1/catalogo/startups/${startupId}/interesse`, { metodo: 'POST', corpo: dados })
}

export function minhasConexoes() {
  return requisitar<ConexaoDoPainel[]>('/api/v1/conexoes')
}

export function conexoesDaStartup(startupId: string, situacao: SituacaoDaConexao | '') {
  return requisitar<ConexaoDoPainel[]>(`/api/v1/startups/${startupId}/conexoes${situacao ? `?situacao=${situacao}` : ''}`)
}

export function agirNaConexao(id: string, acao: 'retirada' | 'aceite' | 'recusa' | 'avanco' | 'encerramento', corpo?: object) {
  return requisitar<{ id: string; situacao: SituacaoDaConexao }>(`/api/v1/conexoes/${id}/${acao}`, { metodo: 'POST', corpo })
}

export function historicoDaConexao(id: string) {
  return requisitar<MudancaDaConexao[]>(`/api/v1/conexoes/${id}/historico`)
}

export function bloqueiosDaStartup(startupId: string) {
  return requisitar<PainelDeBloqueios>(`/api/v1/startups/${startupId}/bloqueios`)
}

export function bloquear(startupId: string, tipo: 'PESSOA' | 'ORGANIZACAO', identificador: string) {
  return requisitar<unknown>(`/api/v1/startups/${startupId}/bloqueios`, { metodo: 'POST', corpo: { tipo, identificador } })
}

export function desbloquear(startupId: string, tipo: string, alvoId: string) {
  return requisitar<void>(`/api/v1/startups/${startupId}/bloqueios/${tipo}/${alvoId}`, { metodo: 'DELETE' })
}

export function definirApenasVerificados(startupId: string, apenasVerificados: boolean) {
  return requisitar<{ apenasVerificados: boolean }>(`/api/v1/startups/${startupId}/apenas-verificados`, { metodo: 'PUT', corpo: { apenasVerificados } })
}
