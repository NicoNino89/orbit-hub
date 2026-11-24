import { afterEach, describe, expect, it, vi } from 'vitest'
import { simularApi } from '../test/apiSimulada'
import { definirFornecedorDeToken, definirRenovador, ErroApi, requisitar } from './cliente'

describe('cliente da API', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    definirFornecedorDeToken(() => null)
    definirRenovador(null)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Renova e repete depois de 401
   * Objetivo: Garantir que, com o access token vencido, a sessão é renovada uma vez e a chamada repetida.
   * Decisões: Duas chamadas simultâneas recebendo 401 disparam uma só renovação.
   */
  it('renova a sessão uma vez e repete as chamadas que receberam 401', async () => {
    let token = 'velho'
    simularApi({
      'GET /api/v1/dados': ({ cabecalhos }) => (cabecalhos.Authorization === 'Bearer novo'
        ? { corpo: { ok: true } } : { status: 401, corpo: { detail: 'expirado' } }),
    })
    definirFornecedorDeToken(() => token)
    const renovador = vi.fn(async () => {
      token = 'novo'
      return true
    })
    definirRenovador(renovador)

    const [a, b] = await Promise.all([requisitar('/api/v1/dados'), requisitar('/api/v1/dados')])

    expect(a).toEqual({ ok: true })
    expect(b).toEqual({ ok: true })
    expect(renovador).toHaveBeenCalledTimes(1)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Renovação falhou
   * Objetivo: Garantir que, se a renovação falhar, o 401 chega a quem chamou.
   * Decisões: Rota anônima nunca tenta renovar.
   */
  it('devolve o 401 quando a renovação falha e não renova rota anônima', async () => {
    simularApi({ 'GET /api/v1/dados': () => ({ status: 401, corpo: { detail: 'Sessão expirada.' } }) })
    const renovador = vi.fn(async () => false)
    definirRenovador(renovador)

    await expect(requisitar('/api/v1/dados')).rejects.toMatchObject({ status: 401, message: 'Sessão expirada.' })
    await expect(requisitar('/api/v1/dados', { anonima: true })).rejects.toBeInstanceOf(ErroApi)
    expect(renovador).toHaveBeenCalledTimes(1)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-92
   * Título: Erro sem corpo legível
   * Objetivo: Garantir a mensagem genérica quando a resposta de erro não é JSON.
   * Decisões: Resposta 502 em texto, como um proxy fora do ar.
   */
  it('monta mensagem genérica para erro sem JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('Bad Gateway', { status: 502 })))
    await expect(requisitar('/api/v1/dados')).rejects.toMatchObject({ status: 502, message: 'A API respondeu 502' })
  })
})
