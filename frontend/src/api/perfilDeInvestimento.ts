import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Maturidade do negócio
 * Objetivo: Tipar a escala de 7 níveis do negócio.
 * Decisões: Escala fixa, a mesma da startup (KAN-26).
 */
export type Maturidade = 'IDEACAO' | 'VALIDACAO' | 'MVP' | 'OPERACAO_INICIAL' | 'TRACAO' | 'CRESCIMENTO' | 'EXPANSAO'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Nomes da maturidade
 * Objetivo: Escrever cada nível da escala, na ordem.
 * Decisões: A ordem do objeto é a da escala.
 */
export const NOMES_DA_MATURIDADE: Record<Maturidade, string> = {
  IDEACAO: 'Ideação',
  VALIDACAO: 'Validação',
  MVP: 'MVP',
  OPERACAO_INICIAL: 'Operação inicial',
  TRACAO: 'Tração',
  CRESCIMENTO: 'Crescimento',
  EXPANSAO: 'Expansão',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Liderança e participação
 * Objetivo: Tipar as opções fixas do perfil.
 * Decisões: Mesmos nomes da API.
 */
export type Lideranca = 'LIDERA' | 'SO_COINVESTE' | 'AMBOS'
export type ParticipacaoNaGestao = 'EXIGE_CONSELHO' | 'ACEITA_OBSERVADOR' | 'NAO_PARTICIPA'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Nomes da liderança
 * Objetivo: Escrever cada opção de liderança como o investidor diria.
 * Decisões: Primeira pessoa.
 */
export const NOMES_DA_LIDERANCA: Record<Lideranca, string> = {
  LIDERA: 'Lidero rodadas',
  SO_COINVESTE: 'Apenas co-invisto',
  AMBOS: 'Lidero ou co-invisto',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Nomes da participação
 * Objetivo: Escrever cada opção de participação na gestão.
 * Decisões: Primeira pessoa.
 */
export const NOMES_DA_PARTICIPACAO: Record<ParticipacaoNaGestao, string> = {
  EXIGE_CONSELHO: 'Exijo assento no conselho',
  ACEITA_OBSERVADOR: 'Aceito ser observador do conselho',
  NAO_PARTICIPA: 'Não participo da gestão',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Dados do perfil
 * Objetivo: Tipar o perfil de investimento, com a tese (KAN-18).
 * Decisões: Ticket em reais; segmentos, estágios e localizações são códigos das listas.
 */
export interface DadosDoPerfil {
  tipo: string
  tipoOutro: string | null
  ticketMinimo: number
  ticketMaximo: number
  lideranca: Lideranca
  followOn: boolean
  participacaoNaGestao: ParticipacaoNaGestao
  instrumentos: string[]
  segmentos: string[]
  estagios: string[]
  maturidades: Maturidade[]
  localizacoes: string[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Situação do perfil
 * Objetivo: Tipar a resposta: dono, se pode editar, tipo fixo da organização, perfil e última alteração.
 * Decisões: Perfil nulo quando ainda não cadastrado.
 */
export interface SituacaoDoPerfil {
  daOrganizacao: boolean
  podeEditar: boolean
  tipoFixo: string | null
  perfil: DadosDoPerfil | null
  atualizadoPor: string | null
  atualizadoEm: string | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Ler o perfil
 * Objetivo: Buscar o perfil de quem atua.
 * Decisões: A organização da atuação vai no cabeçalho, pelo cliente.
 */
export function lerPerfil() {
  return requisitar<SituacaoDoPerfil>('/api/v1/investidores/perfil')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Salvar o perfil
 * Objetivo: Gravar o perfil de quem atua.
 * Decisões: Na organização, o tipo enviado é ignorado pelo servidor.
 */
export function salvarPerfil(dados: DadosDoPerfil) {
  return requisitar<SituacaoDoPerfil>('/api/v1/investidores/perfil', { metodo: 'PUT', corpo: dados })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Pular o onboarding
 * Objetivo: Registrar que a pessoa vai completar a tese depois.
 * Decisões: Vale para a pessoa, em qualquer atuação.
 */
export function pularOnboarding() {
  return requisitar<void>('/api/v1/investidores/onboarding/pulo', { metodo: 'POST' })
}
