import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const AGORA = '2026-11-25T12:00:00Z'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91
 * Título: Abrir com papéis
 * Objetivo: Renderizar o endereço com a sessão e as rotas simuladas.
 * Decisões: Papel público ativo quando houver.
 */
function abrir(endereco: string, papeis: Papel[], rotas: Parameters<typeof simularApi>[0]) {
  const sessao: SessaoApi = {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', papeis },
  }
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  if (papeis.includes('INVESTIDOR')) localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
  const api = simularApi({ 'POST /api/v1/auth/renovar': () => ({ corpo: sessao }), ...rotas })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90
 * Título: Corpo da chamada
 * Objetivo: Achar o corpo enviado numa rota simulada.
 * Decisões: Última chamada da rota.
 */
function corpoDe(chamadas: { chave: string; corpo: unknown }[], chave: string) {
  return chamadas.filter((c) => c.chave === chave).at(-1)?.corpo
}

describe('LGPD e operação (S11)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-90, KAN-17
   * Título: Privacidade na conta
   * Objetivo: Baixar os dados, fazer pedido ao encarregado e excluir a conta.
   * Decisões: Download simulado.
   */
  it('baixa os dados, pede ao encarregado e exclui a conta', async () => {
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
    const { chamadas } = abrir('/conta', ['INVESTIDOR'], {
      'GET /api/v1/conta': () => ({ corpo: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', telefone: null, temFoto: false, papeis: ['INVESTIDOR'],
        criadoEm: AGORA } }),
      'GET /api/v1/conta/historico': () => ({ corpo: [] }),
      'GET /api/v1/privacidade/pedidos': () => ({ corpo: [{ id: 'p1', tipo: 'CORRECAO', titular: 'Ana', detalhes: null, situacao: 'CONCLUIDO', prazo: AGORA,
        vencido: false, responsavel: null, resposta: 'Corrigido', respondidoEm: AGORA, criadoEm: AGORA }] }),
      'GET /api/v1/conta/exclusao': () => ({ corpo: { pendencias: [] } }),
      'POST /api/v1/privacidade/meus-dados': () => ({ corpo: { conta: [] } }),
      'POST /api/v1/privacidade/pedidos': () => ({ status: 201, corpo: {} }),
      'POST /api/v1/conta/exclusao': () => ({ status: 204 }),
      'POST /api/v1/auth/sair': () => ({ status: 204 }),
    })
    const secao = await screen.findByRole('region', { name: 'Privacidade' })
    expect(await within(secao).findByText(/Corrigir dados · Concluído · Corrigido/)).toBeInTheDocument()
    await userEvent.type(within(secao).getByLabelText('Senha da conta'), 'segredo')
    await userEvent.click(within(secao).getByRole('button', { name: 'Baixar meus dados' }))
    expect(await within(secao).findByText('Cópia dos dados baixada.')).toBeInTheDocument()
    expect(corpoDe(chamadas, 'POST /api/v1/privacidade/meus-dados')).toEqual({ senha: 'segredo' })
    await userEvent.type(within(secao).getByLabelText('Senha da conta'), 'segredo')
    await userEvent.selectOptions(within(secao).getByLabelText('Pedido'), 'COMPARTILHAMENTOS')
    await userEvent.click(within(secao).getByRole('button', { name: 'Enviar pedido' }))
    expect(corpoDe(chamadas, 'POST /api/v1/privacidade/pedidos')).toEqual({ tipo: 'COMPARTILHAMENTOS', detalhes: '', senha: 'segredo' })
    await userEvent.type(within(secao).getByLabelText('Senha da conta'), 'segredo')
    await userEvent.type(within(secao).getByLabelText('Digite EXCLUIR para confirmar'), 'EXCLUIR')
    await userEvent.click(within(secao).getByRole('button', { name: 'Excluir minha conta' }))
    expect(corpoDe(chamadas, 'POST /api/v1/conta/exclusao')).toEqual({ senha: 'segredo', confirmacao: 'EXCLUIR' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-17
   * Título: Exclusão com pendência
   * Objetivo: Mostrar o que impede excluir a conta.
   * Decisões: Startup com outros membros.
   */
  it('mostra a pendência que impede a exclusão', async () => {
    abrir('/conta', ['EMPREENDEDOR'], {
      'GET /api/v1/conta': () => ({ corpo: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', telefone: null, temFoto: false, papeis: ['EMPREENDEDOR'],
        criadoEm: AGORA } }),
      'GET /api/v1/conta/historico': () => ({ corpo: [] }),
      'GET /api/v1/privacidade/pedidos': () => ({ corpo: [] }),
      'GET /api/v1/conta/exclusao': () => ({ corpo: { pendencias: ['Rodante'] } }),
    })
    expect(await screen.findByText(/passe a administração de Rodante/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Excluir minha conta' })).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-82, KAN-91
   * Título: Painel de operação
   * Objetivo: Mostrar filas e indicadores e registrar a suspeita de incidente.
   * Decisões: Moderador.
   */
  it('mostra o painel de operação e registra incidente', async () => {
    const { chamadas } = abrir('/admin', ['MODERADOR'], {
      'GET /api/v1/admin/operacao/painel': () => ({ corpo: {
        filas: [{ fila: 'MODERACAO', nome: 'Denúncia', abertos: 3, idadeMediaHoras: 5, pertoDoPrazo: 1, vencidos: 1, porResponsavel: { Mel: 2, 'Sem responsável': 1 } }],
        indicadores: { casosNoMes: 9, horasMediasParaResolver: 4.5, percentualNoPrazo: 80, percentualDeRecursosAceitos: null, satisfacao: 4.2 },
        ecossistema: { novosCadastros: 12, startupsPublicadas: 4, matches: 6, reunioesRealizadas: 3, rodadasAbertas: 1, investimentosConcretizados: 2 },
        geradoEm: AGORA } }),
      'POST /api/v1/admin/incidentes': () => ({ status: 201, corpo: {} }),
    })
    expect(await screen.findByText('Mel: 2 · Sem responsável: 1')).toBeInTheDocument()
    expect(screen.getByText('80%')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
    const form = screen.getByRole('form', { name: 'Registrar incidente' })
    await userEvent.type(within(form).getByLabelText('Título do incidente'), 'Planilha exposta')
    await userEvent.type(within(form).getByLabelText('O que aconteceu'), 'Link público')
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar' }))
    expect(await within(form).findByRole('status')).toHaveTextContent('encarregado')
    expect(corpoDe(chamadas, 'POST /api/v1/admin/incidentes')).toEqual({ titulo: 'Planilha exposta', descricao: 'Link público' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-90, KAN-91
   * Título: Privacidade do encarregado
   * Objetivo: Responder pedido, propor exclusão e conduzir o incidente.
   * Decisões: Encarregado de dados.
   */
  it('conduz pedidos e incidentes', async () => {
    const pedido = { id: 'p1', tipo: 'ELIMINACAO', titular: 'Bia', detalhes: 'Apaguem tudo', situacao: 'EM_ANALISE', prazo: AGORA, vencido: true,
      responsavel: 'Enzo', resposta: null, respondidoEm: null, criadoEm: AGORA }
    const incidente = { id: 'i1', titulo: 'Planilha', descricao: 'Link', dadosAfetados: null, titularesAfetados: null, detectadoEm: AGORA, contencao: null,
      risco: null, decisoes: null, prazoComunicacao: AGORA, comunicacaoAtrasada: false, comunicadoAnpdEm: null, comunicadoTitularesEm: null, licoes: null,
      acoesPreventivas: null, situacao: 'ABERTO', historico: [] }
    const { chamadas } = abrir('/admin/privacidade', ['ENCARREGADO_DADOS'], {
      'GET /api/v1/admin/privacidade/pedidos': () => ({ corpo: [pedido] }),
      'GET /api/v1/admin/incidentes': () => ({ corpo: [incidente] }),
      'POST /api/v1/admin/privacidade/pedidos/p1/resposta': () => ({ corpo: pedido }),
      'POST /api/v1/admin/privacidade/pedidos/p1/exclusao': () => ({ corpo: pedido }),
      'PUT /api/v1/admin/incidentes/i1/avaliacao': () => ({ corpo: incidente }),
      'POST /api/v1/admin/incidentes/i1/comunicacao': () => ({ corpo: incidente }),
      'POST /api/v1/admin/incidentes/i1/encerramento': () => ({ corpo: incidente }),
    })
    expect(await screen.findByText('Apaguem tudo')).toBeInTheDocument()
    const pedidos = screen.getByRole('region', { name: 'Pedidos do titular' })
    await userEvent.type(within(pedidos).getByLabelText('Resposta ou justificativa'), 'Pedido confirmado por telefone')
    await userEvent.click(within(pedidos).getByRole('button', { name: 'Propor exclusão da conta' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/privacidade/pedidos/p1/exclusao')).toEqual({ justificativa: 'Pedido confirmado por telefone' })
    await userEvent.click(within(pedidos).getByRole('button', { name: 'Recusar' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/privacidade/pedidos/p1/resposta')).toEqual({ resposta: 'Pedido confirmado por telefone', recusar: true })
    const inc = screen.getByRole('region', { name: 'Incidentes' })
    await userEvent.type(within(inc).getByLabelText('Texto para Planilha'), 'Link revogado')
    await userEvent.click(within(inc).getByRole('button', { name: 'Risco alto' }))
    expect(corpoDe(chamadas, 'PUT /api/v1/admin/incidentes/i1/avaliacao')).toEqual({ risco: 'ALTO', contencao: 'Link revogado' })
    await userEvent.click(within(inc).getByRole('button', { name: 'Comunicado à ANPD' }))
    await userEvent.click(within(inc).getByRole('button', { name: 'Comunicado aos titulares' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/incidentes/i1/comunicacao')).toEqual({ destino: 'TITULARES' })
    await userEvent.click(within(inc).getByRole('button', { name: 'Encerrar' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/incidentes/i1/encerramento')).toEqual({ licoes: 'Link revogado', acoesPreventivas: 'Link revogado' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-76
   * Título: Times
   * Objetivo: Definir líder, devolver casos e registrar e remover ausência.
   * Decisões: Administrador.
   */
  it('organiza os times', async () => {
    const { chamadas } = abrir('/admin/times', ['ADMIN'], {
      'GET /api/v1/admin/times': () => ({ corpo: [{ time: 'MODERACAO', nome: 'Moderação', liderId: null, lider: null, distribuicao: 'RODIZIO',
        membros: [{ id: 'm1', nome: 'Mel', ausenteHoje: true }] }] }),
      'GET /api/v1/admin/times/ausencias': () => ({ corpo: [{ id: 'a1', membroId: 'u1', inicio: '2026-12-01', fim: '2026-12-10', motivo: 'Férias' }] }),
      'PUT /api/v1/admin/times/MODERACAO/lider': () => ({ status: 204 }),
      'POST /api/v1/admin/times/membros/m1/devolucao': () => ({ corpo: { casos: 2 } }),
      'POST /api/v1/admin/times/ausencias': () => ({ status: 201, corpo: {} }),
      'DELETE /api/v1/admin/times/ausencias/a1': () => ({ status: 204 }),
    })
    const time = await screen.findByRole('article', { name: 'Moderação' })
    expect(within(time).getByText(/Mel \(ausente hoje\)/)).toBeInTheDocument()
    await userEvent.click(within(time).getByRole('button', { name: 'Tornar líder' }))
    expect(corpoDe(chamadas, 'PUT /api/v1/admin/times/MODERACAO/lider')).toEqual({ membroId: 'm1' })
    await userEvent.click(within(time).getByRole('button', { name: 'Devolver casos à fila' }))
    expect(await screen.findByRole('status')).toHaveTextContent('devolvidos')
    const form = screen.getByRole('form', { name: 'Minhas ausências' })
    await userEvent.type(within(form).getByLabelText('Início'), '2026-12-20')
    await userEvent.type(within(form).getByLabelText('Fim'), '2026-12-24')
    await userEvent.click(within(form).getByRole('button', { name: 'Registrar ausência' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/times/ausencias')).toEqual({ inicio: '2026-12-20', fim: '2026-12-24', motivo: '' })
    await userEvent.click(within(form).getByRole('button', { name: 'Remover' }))
    expect(chamadas.some((c) => c.chave === 'DELETE /api/v1/admin/times/ausencias/a1')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 20:54
   * Cartão Jira: KAN-77
   * Título: Titularidade no atendimento
   * Objetivo: Criar organização convidada e propor transferência.
   * Decisões: Suporte N2.
   */
  it('faz cadastro assistido e propõe transferência', async () => {
    const { chamadas } = abrir('/admin/atendimento', ['SUPORTE_N2'], {
      'GET /api/v1/admin/atendimento/chamados': () => ({ corpo: [] }),
      'GET /api/v1/admin/atendimento/indicadores': () => ({ corpo: [] }),
      'GET /api/v1/admin/atendimento/respostas': () => ({ corpo: [] }),
      'GET /api/v1/admin/atendimento/artigos': () => ({ corpo: [] }),
      'POST /api/v1/admin/atendimento/organizacoes-assistidas': () => ({ status: 201, corpo: { organizacaoId: 'o1' } }),
      'POST /api/v1/admin/atendimento/transferencias': () => ({ status: 201, corpo: { propostaId: 'p1' } }),
    })
    const org = await screen.findByRole('form', { name: 'Convite institucional' })
    await userEvent.type(within(org).getByLabelText('Nome da organização'), 'Fundo X')
    await userEvent.type(within(org).getByLabelText('CNPJ'), '11222333000181')
    await userEvent.type(within(org).getByLabelText('E-mail do responsável'), 'r@x.com')
    await userEvent.click(within(org).getByRole('button', { name: 'Criar e convidar' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/organizacoes-assistidas'))
      .toEqual({ nome: 'Fundo X', tipo: 'FUNDO_VC', cnpj: '11222333000181', emailResponsavel: 'r@x.com' })
    const tr = screen.getByRole('form', { name: 'Transferência de titularidade' })
    await userEvent.type(within(tr).getByLabelText('Identificador da startup ou organização'), 's1')
    await userEvent.type(within(tr).getByLabelText('E-mail do novo titular (membro ativo)'), 'c@x.com')
    await userEvent.type(within(tr).getByLabelText('Comprovação conferida'), 'Documentos')
    await userEvent.click(within(tr).getByRole('button', { name: 'Propor transferência' }))
    expect(await screen.findByText('Transferência enviada para a segunda aprovação.')).toBeInTheDocument()
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/transferencias'))
      .toEqual({ alvo: 'STARTUP', alvoId: 's1', emailNovoTitular: 'c@x.com', comprovacao: 'Documentos' })
  })
})
