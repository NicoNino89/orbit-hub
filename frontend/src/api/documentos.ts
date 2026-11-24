import { requisitar } from './cliente'

export type TipoDocumento =
  | 'TERMOS_DE_USO'
  | 'POLITICA_DE_PRIVACIDADE'
  | 'TERMO_DE_CONFIDENCIALIDADE'
  | 'POLITICA_DE_MODERACAO'
  | 'POLITICA_DE_PUBLICIDADE'
  | 'AVISO_NAO_COMPROMISSO'
  | 'AVISO_DADOS_DECLARADOS'

export type RegraAceite = 'TODOS_NO_ACESSO' | 'INVESTIDOR_ANTES_DO_NIVEL_2' | 'NENHUM'

export interface DocumentoLegal {
  id: string
  tipo: TipoDocumento
  versao: number
  titulo: string
  conteudo: string
  vigenteDesde: string
  aceite: RegraAceite
}

/** Endereço amigável de cada tipo, usado nas rotas /termos/:endereco. */
export const ENDERECOS: Record<TipoDocumento, string> = {
  TERMOS_DE_USO: 'uso',
  POLITICA_DE_PRIVACIDADE: 'privacidade',
  TERMO_DE_CONFIDENCIALIDADE: 'confidencialidade',
  POLITICA_DE_MODERACAO: 'moderacao',
  POLITICA_DE_PUBLICIDADE: 'publicidade',
  AVISO_NAO_COMPROMISSO: 'aviso-nao-compromisso',
  AVISO_DADOS_DECLARADOS: 'aviso-dados-declarados',
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Tipo a partir do endereço
 * Objetivo: Converter o trecho da URL (por exemplo "privacidade") no tipo do documento.
 * Decisões: Endereço desconhecido devolve undefined, e a tela cai no primeiro documento.
 */
export function tipoDoEndereco(endereco: string | undefined): TipoDocumento | undefined {
  return (Object.keys(ENDERECOS) as TipoDocumento[]).find((t) => ENDERECOS[t] === endereco)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Documentos vigentes
 * Objetivo: Buscar a versão em vigor de cada documento e aviso.
 * Decisões: Rota pública, sem token.
 */
export function listarVigentes(sinal?: AbortSignal) {
  return requisitar<DocumentoLegal[]>('/api/v1/documentos-legais', { sinal, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Versões de um documento
 * Objetivo: Buscar o histórico de versões já em vigor de um tipo.
 * Decisões: Da mais nova para a mais antiga, como a API devolve.
 */
export function listarVersoes(tipo: TipoDocumento, sinal?: AbortSignal) {
  return requisitar<DocumentoLegal[]>(`/api/v1/documentos-legais/${tipo}/versoes`, { sinal, anonima: true })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Aceites pendentes
 * Objetivo: Buscar o que a conta logada ainda precisa aceitar.
 * Decisões: Usa o token da sessão.
 */
export function listarPendentes() {
  return requisitar<DocumentoLegal[]>('/api/v1/conta/aceites/pendentes')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Registrar aceites
 * Objetivo: Enviar os ids das versões aceitas pela conta logada.
 * Decisões: Depois disso a sessão é renovada para sair do bloqueio.
 */
export function aceitar(documentos: string[]) {
  return requisitar<void>('/api/v1/conta/aceites', { metodo: 'POST', corpo: { documentos } })
}
