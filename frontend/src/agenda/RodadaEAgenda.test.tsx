import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const APRESENTACAO = {
  modo: 'CATALOGO', comoVerificado: true,
  cabecalho: { id: 's1', nome: 'Rodante', segmento: 'Fintech', estagio: 'MVP', localizacao: null, site: null, redes: [], temLogotipo: false,
    naoConstituida: false, selo: 'NAO_VERIFICADO' },
  paginas: [{ id: 'apresentacao', titulo: 'Apresentação', blocos: [] }],
}

const amanha = new Date(Date.now() + 2 * 86400000).toISOString()

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-59, KAN-64, KAN-109, KAN-94, KAN-95, KAN-96, KAN-97
 * Título: Abrir com papel
 * Objetivo: Renderizar o endereço com a sessão e o papel pedidos.
 * Decisões: Lista de motivos de recusa simulada.
 */
function abrir(endereco: string, papel: 'INVESTIDOR' | 'EMPREENDEDOR', rotas: Parameters<typeof simularApi>[0]) {
  const sessao: SessaoApi = {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', papeis: [papel] },
  }
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, papel)
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao }),
    'GET /api/v1/listas/MOTIVO_RECUSA_REUNIAO': () => ({ corpo: [{ codigo: 'OUTRO', rotulo: 'Outro motivo', grupo: null }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Rodada e agenda (S9)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 14:23
   * Cartão Jira: KAN-59, KAN-64
   * Título: Investidor líder na rodada
   * Objetivo: Atualizar a intenção, convidar co-investidor, pedir o encerramento e confirmar o investimento.
   * Decisões: Investidor simulado como líder.
   */
  it('mostra a rodada ao investidor líder com as ações', async () => {
    const { chamadas } = abrir('/catalogo/startups/s1', 'INVESTIDOR', {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: APRESENTACAO }),
      'GET /api/v1/catalogo/startups/s1/rodada': () => ({ corpo: { id: 'r1', situacao: 'EM_NEGOCIACAO', politica: 'LIDER_PRIMEIRO', lider: 'Ana',
        convidadoParaLiderar: false, souLider: true, podeEntrar: false,
        minhaParticipacao: { id: 'p1', investidor: 'Ana', situacao: 'ATIVO', intencao: 200000, condicoes: null, entrouEm: amanha },
        participantes: [{ id: 'p1', investidor: 'Ana', situacao: 'ATIVO' }, { id: 'p2', investidor: 'Bia', situacao: 'ATIVO' }], somaIntencoes: 350000,
        meusInvestimentos: [{ id: 'i1', participanteId: 'p1', investidor: 'Ana', valor: 200000, situacao: 'PENDENTE', criadoEm: amanha }] } }),
      'PUT /api/v1/rodadas/r1/participantes/minha': () => ({ corpo: {} }),
      'POST /api/v1/rodadas/r1/convites': () => ({ status: 201, corpo: {} }),
      'POST /api/v1/rodadas/r1/pedido-de-encerramento': () => ({ status: 204 }),
      'POST /api/v1/rodadas/investimentos/i1/resposta': () => ({ corpo: {} }),
    })
    const rodada = await screen.findByRole('region', { name: 'Rodada' })
    expect(rodada).toHaveTextContent('Bia')
    await userEvent.type(within(rodada).getByLabelText('Intenção (R$)'), '250000')
    await userEvent.click(within(rodada).getByRole('button', { name: 'Atualizar intenção' }))
    await userEvent.type(within(rodada).getByLabelText('E-mail do co-investidor'), 'cai@x.com')
    await userEvent.click(within(rodada).getByRole('button', { name: 'Convidar' }))
    await userEvent.click(within(rodada).getByRole('button', { name: 'Pedir o encerramento da rodada' }))
    await userEvent.click(within(rodada).getByRole('button', { name: 'Confirmar' }))
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/rodadas/r1/participantes/minha')?.corpo).toEqual({ intencao: 250000, condicoes: '' })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/r1/convites')?.corpo).toEqual({ email: 'cai@x.com' })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/rodadas/investimentos/i1/resposta')?.corpo).toEqual({ aprovar: true })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 14:23
   * Cartão Jira: KAN-95, KAN-96, KAN-97
   * Título: Agenda
   * Objetivo: Aceitar um horário de uma proposta, cancelar outra, registrar uma passada e anotar.
   * Decisões: Três reuniões simuladas.
   */
  it('mostra a agenda com as ações de cada reunião', async () => {
    const base = { conexaoId: 'c1', rodadaId: null, startupId: 's1', startup: 'Rodante', tipo: 'APRESENTACAO', formato: 'VIDEO', local: 'https://x',
      pauta: 'Conhecer', duracaoMinutos: 45, coletiva: false, ladoDaVez: null, contrapropostas: 0, motivo: null, tardia: false, resumo: null,
      participantes: [{ usuarioId: 'u1', nome: 'Ana', lado: 'INVESTIDOR', resposta: 'PENDENTE' }], registros: [], minhaAnotacao: null, aguardaVoce: false }
    const { chamadas } = abrir('/agenda', 'INVESTIDOR', {
      'GET /api/v1/reunioes': () => ({ corpo: [
        { ...base, id: 'm1', situacao: 'PROPOSTA', inicio: null, aguardaVoce: true, ladoDaVez: 'INVESTIDOR', opcoes: [{ ordem: 1, inicio: amanha }] },
        { ...base, id: 'm2', situacao: 'CONFIRMADA', inicio: '2020-01-01T12:00:00Z', opcoes: [] },
      ] }),
      'POST /api/v1/reunioes/m1/aceite': () => ({ corpo: {} }),
      'POST /api/v1/reunioes/m2/registro': () => ({ corpo: {} }),
      'PUT /api/v1/reunioes/m2/anotacao': () => ({ status: 204 }),
    })
    const proximas = await screen.findByRole('region', { name: 'Próximas' })
    await userEvent.click(within(proximas).getByRole('button', { name: /^Aceitar/ }))
    const anteriores = screen.getByRole('region', { name: 'Anteriores' })
    const form = within(anteriores).getByRole('form', { name: 'Registrar reunião' })
    await userEvent.selectOptions(within(form).getByLabelText('Próximo passo'), 'ANALISE')
    await userEvent.click(within(form).getByLabelText('Confirmo a mudança na conexão'))
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar' }))
    await userEvent.type(within(anteriores).getByLabelText('Minha anotação (privada)'), 'Boa conversa')
    await userEvent.click(within(anteriores).getByRole('button', { name: 'Salvar anotação' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/reunioes/m1/aceite')?.corpo).toEqual({ ordem: 1 })
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/reunioes/m2/registro')?.corpo)
      .toEqual({ resultado: 'REALIZADA', proximoPasso: 'ANALISE', aplicarNaConexao: true, resumo: null })
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/reunioes/m2/anotacao')?.corpo).toEqual({ texto: 'Boa conversa', compartilhar: false })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 14:23
   * Cartão Jira: KAN-94
   * Título: Propor reunião pela conexão
   * Objetivo: Abrir o formulário no painel de conexões e enviar a proposta com um horário.
   * Decisões: Conexão em Match simulada.
   */
  it('propõe reunião a partir do painel de conexões', async () => {
    const { chamadas } = abrir('/conexoes', 'INVESTIDOR', {
      'GET /api/v1/listas/MOTIVO_ENCERRAMENTO': () => ({ corpo: [] }),
      'GET /api/v1/conexoes': () => ({ corpo: [{ id: 'c1', startupId: 's1', startup: 'Rodante', situacao: 'MATCH', valorPreliminar: null, condicoes: null,
        pedido: 'NENHUM', mensagem: null, criadaEm: amanha, atualizadaEm: amanha, aguardaVoce: true, motivo: null, investidor: null, contatos: [], feitaPor: null }] }),
      'POST /api/v1/conexoes/c1/reunioes': () => ({ status: 201, corpo: {} }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Propor reunião' }))
    const form = screen.getByRole('form', { name: 'Propor reunião' })
    await userEvent.type(within(form).getByLabelText('Link da reunião'), 'https://meet.x/y')
    await userEvent.type(within(form).getByLabelText('Pauta'), 'Conhecer')
    await userEvent.type(within(form).getByLabelText('Horário 1'), '2030-01-10T10:00')
    await userEvent.click(within(form).getByRole('button', { name: 'Enviar proposta' }))
    const corpo = chamadas.find((c) => c.chave === 'POST /api/v1/conexoes/c1/reunioes')?.corpo as { opcoes: string[]; local: string }
    expect(corpo.local).toBe('https://meet.x/y')
    expect(corpo.opcoes).toHaveLength(1)
  })
})
