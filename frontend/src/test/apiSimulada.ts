import { vi } from 'vitest'

export interface RespostaSimulada {
  status?: number
  corpo?: unknown
}

type Rota = (pedido: { corpo: unknown; cabecalhos: Record<string, string> }) => RespostaSimulada

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: API simulada para testes de tela
 * Objetivo: Responder ao fetch conforme "MÉTODO caminho" e guardar as chamadas feitas.
 * Decisões: Rota não cadastrada responde 500, para o teste falhar de forma visível; o corpo enviado é lido
 *           como JSON, para o teste conferir o que a tela mandou. O contador do sino (KAN-146) responde zero quando
 *           o teste não o simula, porque toda tela logada o consulta.
 */
export function simularApi(rotas: Record<string, Rota>) {
  const chamadas: { chave: string; corpo: unknown; cabecalhos: Record<string, string> }[] = []
  const fetchSimulado = vi.fn(async (url: string, init?: RequestInit) => {
    const caminho = new URL(url, 'http://localhost').pathname
    const chave = `${init?.method ?? 'GET'} ${caminho}`
    const corpo = typeof init?.body === 'string' ? JSON.parse(init.body) : undefined
    const cabecalhos = (init?.headers ?? {}) as Record<string, string>
    chamadas.push({ chave, corpo, cabecalhos })
    const rota = rotas[chave] ?? (chave === 'GET /api/v1/notificacoes/contagem' ? () => ({ corpo: { naoLidas: 0 } }) : undefined)
    const resposta = rota ? rota({ corpo, cabecalhos }) : { status: 500, corpo: { detail: `Rota não simulada: ${chave}` } }
    const status = resposta.status ?? 200
    return new Response(status === 204 ? null : JSON.stringify(resposta.corpo ?? {}), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchSimulado)
  return { chamadas, fetchSimulado }
}
