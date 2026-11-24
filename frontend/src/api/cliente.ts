/**
 * Endereço da API. Fica vazio em todos os ambientes: no desenvolvimento o Vite repassa /api para o
 * back-end local, e no Docker Compose e na Azure quem repassa é o nginx da imagem da web.
 * VITE_API_URL só é usado para apontar o build para outra API.
 */
export const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export interface Sobre {
  nome: string
  versao: string
  ambiente: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Erro devolvido pela API
 * Objetivo: Levar para a tela o status, a mensagem, o código da regra e os campos inválidos.
 * Decisões: Lê o formato Problem Details (RFC 9457) da API; sem corpo legível, usa uma mensagem genérica.
 */
export class ErroApi extends Error {
  readonly status: number
  readonly codigo?: string
  readonly campos: Record<string, string>

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 00:40
   * Cartão Jira: KAN-92
   * Título: Criação do erro
   * Objetivo: Guardar status, mensagem, código e campos.
   * Decisões: Campos vazios por padrão, para a tela não precisar testar nulo.
   */
  constructor(status: number, mensagem: string, codigo?: string, campos: Record<string, string> = {}) {
    super(mensagem)
    this.name = 'ErroApi'
    this.status = status
    this.codigo = codigo
    this.campos = campos
  }
}

/** Fonte do access token atual; registrada pela sessão (KAN-13). */
let fornecedorDeToken: () => string | null = () => null
/** Renova a sessão quando o access token vence; devolve se conseguiu (KAN-13). */
let renovador: (() => Promise<boolean>) | null = null
let renovacaoEmAndamento: Promise<boolean> | null = null

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Registrar o renovador da sessão
 * Objetivo: Permitir que o cliente renove a sessão sozinho quando a API responder 401.
 * Decisões: A renovação só acontece quando a pessoa usa a plataforma (uma chamada recebe 401), nunca por
 *           temporizador: assim a sessão expira de verdade por inatividade (KAN-15).
 */
export function definirRenovador(funcao: (() => Promise<boolean>) | null) {
  renovador = funcao
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Renovar uma vez
 * Objetivo: Garantir uma única renovação mesmo com várias chamadas recebendo 401 ao mesmo tempo.
 * Decisões: As chamadas simultâneas esperam a mesma promessa.
 */
function renovarUmaVez(): Promise<boolean> {
  if (!renovador) return Promise.resolve(false)
  renovacaoEmAndamento ??= renovador().finally(() => {
    renovacaoEmAndamento = null
  })
  return renovacaoEmAndamento
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Registrar a fonte do token
 * Objetivo: Permitir que a sessão informe ao cliente qual access token enviar.
 * Decisões: O token vive só em memória (nunca em localStorage), para um script injetado não lê-lo.
 */
export function definirFornecedorDeToken(fornecedor: () => string | null) {
  fornecedorDeToken = fornecedor
}

export interface OpcoesRequisicao {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  corpo?: unknown
  sinal?: AbortSignal
  /** Não envia o access token (rotas públicas e de autenticação). */
  anonima?: boolean
  /** Envia um formulário com arquivo (multipart) no lugar de JSON (KAN-16). */
  formulario?: FormData
  /** Lê a resposta como arquivo (Blob), por exemplo uma imagem protegida por login (KAN-16). */
  comoArquivo?: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Chamada à API
 * Objetivo: Fazer a requisição com JSON, token e cookie, e converter erros em ErroApi.
 * Decisões: credentials 'include' para o cookie de sessão ir nas rotas de autenticação; 204 devolve
 *           undefined; a mensagem de erro vem do "detail" da API, que já é escrita para o usuário. Desde o
 *           KAN-13, um 401 numa rota autenticada renova a sessão uma vez e repete a chamada.
 */
export async function requisitar<T>(caminho: string, opcoes: OpcoesRequisicao = {}): Promise<T> {
  try {
    return await executar<T>(caminho, opcoes)
  } catch (erro) {
    if (erro instanceof ErroApi && erro.status === 401 && !opcoes.anonima && (await renovarUmaVez())) {
      return executar<T>(caminho, opcoes)
    }
    throw erro
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Executar a requisição
 * Objetivo: Fazer uma chamada à API, sem nova tentativa.
 * Decisões: Separado de requisitar para a nova tentativa após renovar a sessão reaproveitar o mesmo código.
 *           Com formulário, o navegador monta o Content-Type (multipart com o separador); com comoArquivo,
 *           devolve o Blob (KAN-16).
 */
async function executar<T>(caminho: string, opcoes: OpcoesRequisicao): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: opcoes.comoArquivo ? '*/*' : 'application/json' }
  if (opcoes.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'
  const token = opcoes.anonima ? null : fornecedorDeToken()
  if (token) cabecalhos.Authorization = `Bearer ${token}`

  const resposta = await fetch(`${API_URL}${caminho}`, {
    method: opcoes.metodo ?? 'GET',
    headers: cabecalhos,
    body: opcoes.formulario ?? (opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo)),
    credentials: 'include',
    signal: opcoes.sinal,
  })
  if (!resposta.ok) {
    throw await converterErro(resposta)
  }
  if (resposta.status === 204) return undefined as T
  if (opcoes.comoArquivo) return (await resposta.blob()) as T
  return (await resposta.json()) as T
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Converter resposta de erro
 * Objetivo: Ler o Problem Details da resposta e montar o ErroApi.
 * Decisões: Corpo que não é JSON (proxy fora do ar, por exemplo) vira mensagem genérica com o status.
 */
async function converterErro(resposta: Response): Promise<ErroApi> {
  try {
    const problema = (await resposta.json()) as { detail?: string; codigo?: string; campos?: Record<string, string> }
    return new ErroApi(resposta.status, problema.detail ?? `A API respondeu ${resposta.status}`, problema.codigo,
      problema.campos ?? {})
  } catch {
    return new ErroApi(resposta.status, `A API respondeu ${resposta.status}`)
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Buscar informações da API
 * Objetivo: Consultar GET /api/v1/sobre e devolver nome, versão e ambiente.
 * Decisões: Resposta fora de 2xx vira Error com o status, para a tela mostrar o motivo; aceita
 *           AbortSignal para cancelar a chamada quando a tela é desmontada; a barra final de
 *           VITE_API_URL é removida para não gerar "//api".
 */
export async function buscarSobre(sinal?: AbortSignal): Promise<Sobre> {
  const resposta = await fetch(`${API_URL}/api/v1/sobre`, { signal: sinal })
  if (!resposta.ok) {
    throw new Error(`A API respondeu ${resposta.status}`)
  }
  return resposta.json() as Promise<Sobre>
}
