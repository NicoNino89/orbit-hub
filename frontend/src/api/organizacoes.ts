import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Papel na organização
 * Objetivo: Tipar o que a pessoa pode fazer na organização.
 * Decisões: Mesmos nomes da API.
 */
export type PapelNaOrganizacao = 'ADMINISTRADOR' | 'MEMBRO' | 'OBSERVADOR'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Nomes dos papéis
 * Objetivo: Escrever cada papel em português, com o que ele permite.
 * Decisões: Usado nos seletores e nas listas.
 */
export const PAPEIS_NA_ORGANIZACAO: Record<PapelNaOrganizacao, { nome: string; explicacao: string }> = {
  ADMINISTRADOR: { nome: 'Administrador', explicacao: 'Gerencia membros e dados e atua em nome da organização' },
  MEMBRO: { nome: 'Membro', explicacao: 'Atua em nome da organização (interesse, Match, Nível 2)' },
  OBSERVADOR: { nome: 'Observador', explicacao: 'Apenas visualiza' },
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Organização
 * Objetivo: Tipar a organização com o papel de quem pergunta.
 * Decisões: CNPJ já formatado pela API.
 */
export interface Organizacao {
  id: string
  nome: string
  tipo: string
  cnpj: string
  site: string | null
  descricao: string | null
  meuPapel: PapelNaOrganizacao
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Membro
 * Objetivo: Tipar um membro ativo.
 * Decisões: Sem e-mail.
 */
export interface MembroDaOrganizacao {
  usuarioId: string
  nome: string
  papel: PapelNaOrganizacao
  entrouEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convite pendente
 * Objetivo: Tipar um convite ainda válido.
 * Decisões: Sem token.
 */
export interface ConvitePendente {
  id: string
  email: string
  papel: PapelNaOrganizacao
  criadoEm: string
  expiraEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Dados da organização
 * Objetivo: Tipar o formulário de cadastro e edição.
 * Decisões: O CNPJ só vale no cadastro.
 */
export interface DadosOrganizacao {
  nome: string
  tipo: string
  cnpj?: string
  site?: string
  descricao?: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Resumo do convite
 * Objetivo: Tipar o que a tela mostra antes do aceite.
 * Decisões: Inclui o e-mail de destino.
 */
export interface ResumoConvite {
  organizacao: string
  tipo: string
  papel: PapelNaOrganizacao
  email: string
  expiraEm: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Minhas organizações
 * Objetivo: Buscar as organizações de que a pessoa participa.
 * Decisões: Sem cache.
 */
export function minhasOrganizacoes() {
  return requisitar<Organizacao[]>('/api/v1/organizacoes/minhas')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Criar organização
 * Objetivo: Cadastrar uma organização.
 * Decisões: Quem cria vira administrador.
 */
export function criarOrganizacao(dados: DadosOrganizacao) {
  return requisitar<Organizacao>('/api/v1/organizacoes', { metodo: 'POST', corpo: dados })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Detalhe
 * Objetivo: Buscar a organização e os membros.
 * Decisões: 404 para quem não é membro.
 */
export function detalheDaOrganizacao(id: string) {
  return requisitar<{ organizacao: Organizacao; membros: MembroDaOrganizacao[] }>(`/api/v1/organizacoes/${id}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Editar organização
 * Objetivo: Salvar nome, tipo, site e descrição.
 * Decisões: Só administrador.
 */
export function editarOrganizacao(id: string, dados: DadosOrganizacao) {
  return requisitar<Organizacao>(`/api/v1/organizacoes/${id}`, { metodo: 'PUT', corpo: dados })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convidar
 * Objetivo: Enviar um convite por e-mail.
 * Decisões: Só administrador.
 */
export function convidar(id: string, email: string, papel: PapelNaOrganizacao) {
  return requisitar<ConvitePendente>(`/api/v1/organizacoes/${id}/convites`, { metodo: 'POST', corpo: { email, papel } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convites pendentes
 * Objetivo: Buscar os convites ainda válidos.
 * Decisões: Só administrador.
 */
export function convitesPendentes(id: string) {
  return requisitar<ConvitePendente[]>(`/api/v1/organizacoes/${id}/convites`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Cancelar convite
 * Objetivo: Invalidar um convite pendente.
 * Decisões: Só administrador.
 */
export function cancelarConvite(id: string, conviteId: string) {
  return requisitar<void>(`/api/v1/organizacoes/${id}/convites/${conviteId}`, { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Trocar papel
 * Objetivo: Mudar o papel de um membro.
 * Decisões: Só administrador.
 */
export function trocarPapel(id: string, usuarioId: string, papel: PapelNaOrganizacao) {
  return requisitar<void>(`/api/v1/organizacoes/${id}/membros/${usuarioId}`, { metodo: 'PUT', corpo: { papel } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Remover ou sair
 * Objetivo: Encerrar o vínculo de um membro (ou o próprio, para sair).
 * Decisões: O servidor decide se é saída ou remoção.
 */
export function encerrarVinculo(id: string, usuarioId: string) {
  return requisitar<void>(`/api/v1/organizacoes/${id}/membros/${usuarioId}`, { metodo: 'DELETE' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Consultar convite
 * Objetivo: Ver de qual organização é o convite.
 * Decisões: Token no corpo.
 */
export function consultarConvite(token: string) {
  return requisitar<ResumoConvite>('/api/v1/organizacoes/convites/consulta', { metodo: 'POST', corpo: { token } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Aceitar convite
 * Objetivo: Entrar na organização do convite.
 * Decisões: Devolve o identificador e o nome.
 */
export function aceitarConvite(token: string) {
  return requisitar<{ id: string; nome: string }>('/api/v1/organizacoes/convites/aceite', { metodo: 'POST', corpo: { token } })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Opções de uma lista
 * Objetivo: Buscar as opções ativas de uma lista pública (por exemplo, tipos de investidor).
 * Decisões: Rota pública de listas (KAN-79).
 */
export function opcoesDaLista(lista: string) {
  return requisitar<{ codigo: string; rotulo: string; grupo: string | null }[]>(`/api/v1/listas/${lista}`)
}
