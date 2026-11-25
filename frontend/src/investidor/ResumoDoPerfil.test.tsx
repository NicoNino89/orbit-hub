import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'a1', nome: 'Ana Investidora', email: 'ana@exemplo.com', papeis: ['INVESTIDOR'] },
}

describe('Resumo do perfil de investimento (KAN-20)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 17:06
   * Cartão Jira: KAN-20
   * Título: Resumo na área do investidor
   * Objetivo: Conferir que, com a tese concluída, a área mostra o resumo com os rótulos das listas, o ticket em reais e
   *           "Qualquer" para a maturidade em branco, e o link para editar.
   * Decisões: Listas simuladas com rótulos próprios, para conferir que o resumo usa os rótulos das listas.
   */
  it('mostra o resumo do perfil atual', async () => {
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
    simularApi({
      'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
      'GET /api/v1/organizacoes/minhas': () => ({ corpo: [] }),
      'GET /api/v1/investidores/eu': () => ({
        corpo: { teseCadastrada: true, proximoPasso: '/investidor', atuandoPor: null, onboarding: 'CONCLUIDO' },
      }),
      'GET /api/v1/investidores/perfil': () => ({
        corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, atualizadoPor: 'Ana', atualizadoEm: '2026-11-24T17:00:00Z',
          perfil: { tipo: 'ANJO', tipoOutro: null, ticketMinimo: 50000, ticketMaximo: 250000, lideranca: 'SO_COINVESTE', followOn: true,
            participacaoNaGestao: 'ACEITA_OBSERVADOR', instrumentos: ['MUTUO_CONVERSIVEL'], segmentos: ['FINTECH'], estagios: ['SEED'],
            maturidades: [], localizacoes: ['SP'],
            complementares: { modelos: [], modalidades: [], horizonte: 'DE_5_A_8_ANOS', perfilRisco: null, experiencia: null, interesses: [] } } },
      }),
      'GET /api/v1/listas/TIPO_INVESTIDOR': () => ({ corpo: [{ codigo: 'ANJO', rotulo: 'Investidor-anjo', grupo: null }] }),
      'GET /api/v1/listas/INSTRUMENTO': () => ({ corpo: [{ codigo: 'MUTUO_CONVERSIVEL', rotulo: 'Mútuo conversível', grupo: null }] }),
      'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [{ codigo: 'FINTECH', rotulo: 'Serviços financeiros', grupo: null }] }),
      'GET /api/v1/listas/ESTAGIO_RODADA': () => ({ corpo: [{ codigo: 'SEED', rotulo: 'Seed', grupo: null }] }),
      'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [{ codigo: 'SP', rotulo: 'São Paulo', grupo: 'SUDESTE' }] }),
      'GET /api/v1/listas/HORIZONTE_INVESTIMENTO': () => ({ corpo: [{ codigo: 'DE_5_A_8_ANOS', rotulo: 'De 5 a 8 anos', grupo: null }] }),
    })
    render(<MemoryRouter initialEntries={['/investidor']}><Rotas /></MemoryRouter>)
    const lista = await screen.findByLabelText('Resumo do perfil de investimento')
    expect(await within(lista).findByText('Serviços financeiros')).toBeInTheDocument()
    expect(within(lista).getByText('Investidor-anjo')).toBeInTheDocument()
    expect(within(lista).getByText(/R\$\s?50\.000 a R\$\s?250\.000/)).toBeInTheDocument()
    expect(within(lista).getByText('Qualquer')).toBeInTheDocument()
    expect(await within(lista).findByText('De 5 a 8 anos')).toBeInTheDocument()
    expect(within(lista).queryByText('Perfil de risco')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Editar o perfil' })).toHaveAttribute('href', '/investidor/perfil')
  })
})
