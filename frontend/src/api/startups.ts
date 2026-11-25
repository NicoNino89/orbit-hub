import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Tipos da startup
 * Objetivo: Tipar o papel na equipe, a situação, a rede social e o fundador.
 * Decisões: Mesmos nomes da API.
 */
export type PapelNaStartup = 'FUNDADOR_ADMINISTRADOR' | 'FUNDADOR' | 'COLABORADOR'
export type SituacaoDaStartup = 'RASCUNHO' | 'EM_REVISAO' | 'PUBLICADA' | 'DEVOLVIDA' | 'DESPUBLICADA'
export type RedeSocial = 'LINKEDIN' | 'INSTAGRAM' | 'X' | 'FACEBOOK' | 'YOUTUBE' | 'OUTRA'

export interface Fundador {
  nome: string
  cargo: string | null
  participacao: number | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Nomes para a tela
 * Objetivo: Escrever papéis, situações e redes em português.
 * Decisões: Usados nas listas e no editor.
 */
export const NOMES_DO_PAPEL_NA_STARTUP: Record<PapelNaStartup, string> = {
  FUNDADOR_ADMINISTRADOR: 'Fundador administrador',
  FUNDADOR: 'Fundador',
  COLABORADOR: 'Colaborador',
}

export const NOMES_DA_SITUACAO_DA_STARTUP: Record<SituacaoDaStartup, string> = {
  RASCUNHO: 'Rascunho',
  EM_REVISAO: 'Em revisão',
  PUBLICADA: 'Publicada',
  DEVOLVIDA: 'Devolvida para ajustes',
  DESPUBLICADA: 'Despublicada',
}

export const NOMES_DAS_REDES: Record<RedeSocial, string> = {
  LINKEDIN: 'LinkedIn', INSTAGRAM: 'Instagram', X: 'X (Twitter)', FACEBOOK: 'Facebook', YOUTUBE: 'YouTube', OUTRA: 'Outra',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Dados cadastrais
 * Objetivo: Tipar o formulário de criação e edição.
 * Decisões: CNPJ opcional.
 */
export interface CadastroDaStartup {
  nome: string
  nomeFantasia: string | null
  razaoSocial: string | null
  cnpj: string | null
  segmento: string
  localizacao: string
  site: string | null
  redes: Partial<Record<RedeSocial, string>>
  fundadores: Fundador[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Cadastro vazio
 * Objetivo: Valor inicial do formulário de criação.
 * Decisões: Só o nome, o segmento e a localização são obrigatórios.
 */
export const CADASTRO_VAZIO: CadastroDaStartup = {
  nome: '', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: '', localizacao: '', site: null, redes: {}, fundadores: [],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Startup
 * Objetivo: Tipar a startup como a API devolve.
 * Decisões: "constituida" falso = empresa ainda não constituída.
 */
export interface Startup extends CadastroDaStartup {
  id: string
  situacao: SituacaoDaStartup
  constituida: boolean
  temLogotipo: boolean
  atualizadaEm: string
  meuPapel: PapelNaStartup
  /** Seções que o colaborador edita (KAN-65); vazio para fundadores. */
  minhasSecoes: SecaoDaStartup[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Resumo da startup
 * Objetivo: Tipar uma startup na lista "minhas startups".
 * Decisões: Sem fundadores.
 */
export interface ResumoDaStartup {
  id: string
  nome: string
  situacao: SituacaoDaStartup
  constituida: boolean
  meuPapel: PapelNaStartup
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Minhas startups
 * Objetivo: Buscar as startups de quem está logado.
 * Decisões: Sem cache.
 */
export function minhasStartups() {
  return requisitar<ResumoDaStartup[]>('/api/v1/startups/minhas')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Criar startup
 * Objetivo: Cadastrar uma startup.
 * Decisões: Quem cria vira fundador administrador.
 */
export function criarStartup(dados: CadastroDaStartup) {
  return requisitar<Startup>('/api/v1/startups', { metodo: 'POST', corpo: dados })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Ler startup
 * Objetivo: Buscar os dados cadastrais de uma startup.
 * Decisões: 404 para quem não é da equipe.
 */
export function lerStartup(id: string) {
  return requisitar<Startup>(`/api/v1/startups/${id}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Salvar cadastro
 * Objetivo: Gravar os dados cadastrais.
 * Decisões: Regras no servidor.
 */
export function salvarCadastro(id: string, dados: CadastroDaStartup) {
  return requisitar<Startup>(`/api/v1/startups/${id}/cadastro`, { metodo: 'PUT', corpo: dados })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Trocar logotipo
 * Objetivo: Enviar o arquivo do logotipo.
 * Decisões: Multipart.
 */
export function trocarLogotipo(id: string, arquivo: File) {
  const formulario = new FormData()
  formulario.append('arquivo', arquivo)
  return requisitar<Startup>(`/api/v1/startups/${id}/logotipo`, { metodo: 'PUT', formulario })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Ler logotipo
 * Objetivo: Baixar o logotipo com a sessão.
 * Decisões: Devolve o arquivo para a tela criar um endereço local.
 */
export function lerLogotipo(id: string) {
  return requisitar<Blob>(`/api/v1/startups/${id}/logotipo`, { comoArquivo: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Seções do cadastro
 * Objetivo: Tipar e nomear as seções que o colaborador pode editar.
 * Decisões: Mesma ordem das etapas do editor.
 */
export type SecaoDaStartup =
  | 'CADASTRO' | 'PROBLEMA_E_SOLUCAO' | 'PRODUTO' | 'MERCADO' | 'MODELO_DE_NEGOCIO' | 'TRACAO' | 'EQUIPE_E_ESTRUTURA'
  | 'PLANEJAMENTO' | 'NECESSIDADE_DE_INVESTIMENTO' | 'MATURIDADE'

export const NOMES_DAS_SECOES: Record<SecaoDaStartup, string> = {
  CADASTRO: 'Dados cadastrais',
  PROBLEMA_E_SOLUCAO: 'Problema e solução',
  PRODUTO: 'Produto ou serviço',
  MERCADO: 'Mercado',
  MODELO_DE_NEGOCIO: 'Modelo de negócio',
  TRACAO: 'Tração e resultados',
  EQUIPE_E_ESTRUTURA: 'Equipe e estrutura',
  PLANEJAMENTO: 'Planejamento',
  NECESSIDADE_DE_INVESTIMENTO: 'Necessidade de investimento',
  MATURIDADE: 'Estágio do negócio',
}

export const EXPLICACAO_DO_PAPEL_NA_STARTUP: Record<PapelNaStartup, string> = {
  FUNDADOR_ADMINISTRADOR: 'Gerencia a equipe, publica a startup, abre e encerra rodadas e libera o Nível 2',
  FUNDADOR: 'Edita todo o cadastro e acompanha manifestações e rodadas, sem liberar o Nível 2 nem encerrar rodadas',
  COLABORADOR: 'Edita só as seções do cadastro que você escolher',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Pode editar a seção
 * Objetivo: Dizer, na tela, se quem está logado edita uma seção.
 * Decisões: Fundadores editam tudo; o servidor confere de novo.
 */
export function podeEditar(startup: Pick<Startup, 'meuPapel' | 'minhasSecoes'>, secao: SecaoDaStartup) {
  return startup.meuPapel !== 'COLABORADOR' || startup.minhasSecoes.includes(secao)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Tipos da equipe
 * Objetivo: Tipar o membro, o convite pendente e o resumo do convite.
 * Decisões: Mesmos nomes da API.
 */
export interface MembroDaStartup {
  usuarioId: string
  nome: string
  papel: PapelNaStartup
  secoes: SecaoDaStartup[]
  entrouEm: string
}

export interface ConviteDaStartup {
  id: string
  email: string
  papel: PapelNaStartup
  secoes: SecaoDaStartup[]
  criadoEm: string
  expiraEm: string
}

export interface ResumoConviteDaStartup {
  startup: string
  papel: PapelNaStartup
  secoes: SecaoDaStartup[]
  email: string
  expiraEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Chamadas da equipe
 * Objetivo: Listar, convidar, trocar papel, remover ou sair, e consultar e aceitar o convite.
 * Decisões: O token do convite vai no corpo.
 */
export function equipeDaStartup(id: string) {
  return requisitar<MembroDaStartup[]>(`/api/v1/startups/${id}/equipe`)
}

export function convitesDaStartup(id: string) {
  return requisitar<ConviteDaStartup[]>(`/api/v1/startups/${id}/convites`)
}

export function convidarParaStartup(id: string, email: string, papel: PapelNaStartup, secoes: SecaoDaStartup[]) {
  return requisitar<ConviteDaStartup>(`/api/v1/startups/${id}/convites`, { metodo: 'POST', corpo: { email, papel, secoes } })
}

export function cancelarConviteDaStartup(id: string, conviteId: string) {
  return requisitar<void>(`/api/v1/startups/${id}/convites/${conviteId}`, { metodo: 'DELETE' })
}

export function trocarPapelNaStartup(id: string, usuarioId: string, papel: PapelNaStartup, secoes: SecaoDaStartup[]) {
  return requisitar<void>(`/api/v1/startups/${id}/equipe/${usuarioId}`, { metodo: 'PUT', corpo: { papel, secoes } })
}

export function sairDaStartup(id: string, usuarioId: string) {
  return requisitar<void>(`/api/v1/startups/${id}/equipe/${usuarioId}`, { metodo: 'DELETE' })
}

export function consultarConviteDaStartup(token: string) {
  return requisitar<ResumoConviteDaStartup>('/api/v1/startups/convites/consulta', { metodo: 'POST', corpo: { token } })
}

export function aceitarConviteDaStartup(token: string) {
  return requisitar<{ id: string; nome: string }>('/api/v1/startups/convites/aceite', { metodo: 'POST', corpo: { token } })
}
