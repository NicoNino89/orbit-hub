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
