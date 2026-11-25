import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const SESSAO: SessaoApi = {
  tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
  usuario: { id: 'e1', nome: 'Eva Empreendedora', email: 'eva@exemplo.com', papeis: ['EMPREENDEDOR'] },
}

const STARTUP = {
  id: 's1', nome: 'NovaSafra', nomeFantasia: null, razaoSocial: null, cnpj: null, segmento: 'AGRONEGOCIO', localizacao: 'MT', site: null,
  redes: {}, fundadores: [], situacao: 'RASCUNHO', constituida: false, temLogotipo: false, atualizadaEm: '2026-11-24T18:00:00Z',
  meuPapel: 'FUNDADOR_ADMINISTRADOR', minhasSecoes: [],
}

const CATALOGO = [
  { campo: 'PROBLEMA', secao: 'PROBLEMA_E_SOLUCAO', rotulo: 'Qual problema a startup resolve', tipo: 'TEXTO', limite: 1000, lista: null, ajuda: null, obrigatorio: true },
]

const PROGRESSO = {
  percentual: 50, podeEnviar: false, pendencias: ['qual problema a startup resolve'], situacao: 'RASCUNHO',
  secoes: [{ secao: 'CADASTRO', preenchidos: 1, total: 1, completa: true }, { secao: 'PROBLEMA_E_SOLUCAO', preenchidos: 0, total: 1, completa: false }],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Abrir como empreendedora
 * Objetivo: Renderizar o endereço com a sessão da empreendedora e as rotas informadas.
 * Decisões: Papel ativo de empreendedor; listas do formulário cadastral vazias.
 */
function abrir(endereco: string, rotas: Parameters<typeof simularApi>[0]) {
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, 'EMPREENDEDOR')
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: SESSAO }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [] }),
    'GET /api/v1/listas/LOCALIZACAO': () => ({ corpo: [] }),
    'GET /api/v1/startups/campos': () => ({ corpo: CATALOGO }),
    'GET /api/v1/startups/s1/secoes': () => ({ corpo: {} }),
    'GET /api/v1/startups/s1/situacoes': () => ({ corpo: [] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Publicação da startup (KAN-27)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-27
   * Título: Progresso e envio
   * Objetivo: Conferir a barra de progresso, a marca de etapa completa, o obrigatório, as pendências que travam o envio
   *           e o envio depois de completar.
   * Decisões: O progresso simulado muda depois de salvar a seção.
   */
  it('mostra o progresso, trava o envio com pendências e envia quando completo', async () => {
    let progresso = PROGRESSO
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: STARTUP }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: progresso }),
      'PUT /api/v1/startups/s1/secoes/PROBLEMA_E_SOLUCAO': ({ corpo }) => {
        progresso = { ...PROGRESSO, percentual: 100, podeEnviar: true, pendencias: [],
          secoes: PROGRESSO.secoes.map((x) => ({ ...x, completa: true })) }
        return { corpo }
      },
      'POST /api/v1/startups/s1/envio': () => {
        progresso = { ...progresso, situacao: 'EM_REVISAO' }
        return { corpo: { situacao: 'EM_REVISAO' } }
      },
    })
    expect(await screen.findByRole('progressbar', { name: 'Progresso do cadastro' })).toHaveAttribute('aria-valuenow', '50')
    expect(screen.getByRole('button', { name: 'Dados cadastrais' })).toHaveTextContent('✓')

    await userEvent.click(screen.getByRole('button', { name: 'Publicação' }))
    const painel = await screen.findByRole('region', { name: 'Publicação' })
    expect(within(painel).getByRole('list', { name: 'Pendências' })).toHaveTextContent('qual problema a startup resolve')
    expect(within(painel).getByRole('button', { name: 'Enviar para publicação' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Problema e solução' }))
    const form = await screen.findByRole('form', { name: 'Problema e solução' })
    expect(within(form).getByText('Obrigatório para publicar')).toBeInTheDocument()
    await userEvent.type(within(form).getByLabelText('Qual problema a startup resolve'), 'Frete caro')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar problema e solução' }))
    await screen.findByText('Problema e solução: salvo.')
    expect(await screen.findByRole('progressbar', { name: 'Progresso do cadastro' })).toHaveAttribute('aria-valuenow', '100')

    await userEvent.click(screen.getByRole('button', { name: 'Publicação' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Enviar para publicação' }))
    expect(await screen.findByText(/A curadoria da plataforma está revisando/)).toBeInTheDocument()
    expect(chamadas.some((c) => c.chave === 'POST /api/v1/startups/s1/envio')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-27
   * Título: Despublicar
   * Objetivo: Conferir o histórico, a confirmação com motivo e a despublicação.
   * Decisões: Startup publicada simulada.
   */
  it('despublica com confirmação e mostra o histórico', async () => {
    let situacao = 'PUBLICADA'
    const { chamadas } = abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, situacao } }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { ...PROGRESSO, podeEnviar: true, pendencias: [], situacao } }),
      'GET /api/v1/startups/s1/situacoes': () => ({
        corpo: [{ de: 'EM_REVISAO', para: 'PUBLICADA', motivo: null, pelaPlataforma: false, em: '2026-11-20T12:00:00Z' }],
      }),
      'POST /api/v1/startups/s1/despublicacao': () => {
        situacao = 'DESPUBLICADA'
        return { corpo: { situacao } }
      },
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Publicação' }))
    const painel = await screen.findByRole('region', { name: 'Publicação' })
    expect(await within(painel).findByText(/Em revisão → Publicada/)).toBeInTheDocument()
    await userEvent.click(within(painel).getByRole('button', { name: 'Despublicar' }))
    const grupo = within(painel).getByRole('group', { name: 'Confirmar despublicação' })
    await userEvent.type(within(grupo).getByLabelText('Motivo (opcional)'), 'Pausa')
    await userEvent.click(within(grupo).getByRole('button', { name: 'Despublicar agora' }))
    expect(await screen.findByText(/A startup saiu do catálogo/)).toBeInTheDocument()
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/startups/s1/despublicacao')?.corpo).toEqual({ motivo: 'Pausa' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-27
   * Título: Recusa do envio e cancelamento
   * Objetivo: Conferir que o erro do servidor aparece e que o envio em revisão pode ser cancelado.
   * Decisões: O servidor simulado recusa o primeiro envio.
   */
  it('mostra a recusa do servidor e cancela o envio em revisão', async () => {
    let situacao = 'EM_REVISAO'
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, situacao } }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { ...PROGRESSO, podeEnviar: true, pendencias: [], situacao } }),
      'POST /api/v1/startups/s1/envio/cancelamento': () => {
        situacao = 'RASCUNHO'
        return { corpo: { situacao } }
      },
      'POST /api/v1/startups/s1/envio': () => ({ status: 422, corpo: { detail: 'Preencha antes de enviar: estágio do negócio.', codigo: 'cadastro_incompleto' } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Publicação' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Cancelar envio' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Enviar para publicação' }))
    expect(await screen.findByText('Preencha antes de enviar: estágio do negócio.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 23:40
   * Cartão Jira: KAN-27
   * Título: Quem não é administrador
   * Objetivo: Conferir que o fundador acompanha a publicação sem os botões.
   * Decisões: Papel de fundador.
   */
  it('mostra a publicação sem ações para quem não é administrador', async () => {
    abrir('/empreendedor/startups/s1', {
      'GET /api/v1/startups/s1': () => ({ corpo: { ...STARTUP, meuPapel: 'FUNDADOR', situacao: 'EM_REVISAO' } }),
      'GET /api/v1/startups/s1/progresso': () => ({ corpo: { ...PROGRESSO, situacao: 'EM_REVISAO' } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Publicação' }))
    expect(await screen.findByText(/Só o fundador administrador envia/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar envio' })).not.toBeInTheDocument()
  })
})
