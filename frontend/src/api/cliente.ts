/**
 * Endereço da API. Em desenvolvimento fica vazio e o Vite repassa /api para o back-end local.
 * Em homologação e produção vem de VITE_API_URL, definido no build.
 */
export const API_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export interface Sobre {
  nome: string
  versao: string
  ambiente: string
}

export async function buscarSobre(sinal?: AbortSignal): Promise<Sobre> {
  const resposta = await fetch(`${API_URL}/api/v1/sobre`, { signal: sinal })
  if (!resposta.ok) {
    throw new Error(`A API respondeu ${resposta.status}`)
  }
  return resposta.json() as Promise<Sobre>
}
