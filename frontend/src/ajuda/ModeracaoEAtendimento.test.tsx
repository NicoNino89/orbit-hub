import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import { AbrirDisputa } from './Denunciar'
import type { Papel } from '../api/contas'
import type { SessaoApi } from '../api/autenticacao'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const AGORA = '2026-11-25T12:00:00Z'
const CATEGORIAS = [{ codigo: 'ACESSO', rotulo: 'Acesso e conta', grupo: null }, { codigo: 'DISPUTA', rotulo: 'Disputa', grupo: null }]
const CHAMADO = {
  id: 'ch1', categoria: 'Acesso e conta', categoriaCodigo: 'ACESSO', servico: 'ONBOARDING', descricao: 'Não entro', situacao: 'RESOLVIDO', nivel: 'N1',
  prioridade: 'ALTA', prazo: AGORA, contato: 'Ana', emailContato: null, responsavel: 'Nina', outraParte: null, souOutraParte: false, referenciaTipo: null,
  referenciaId: null, resultadoDisputa: null, nota: null, comentarioAvaliacao: null, escalonamento: null, criadoEm: AGORA,
  mensagens: [{ autor: 'Atendimento Orbit Hub', equipe: true, interna: false, texto: 'Resolvido', em: AGORA, minha: false }],
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86, KAN-88, KAN-89, KAN-99, KAN-100, KAN-101, KAN-103
 * Título: Abrir com papéis
 * Objetivo: Renderizar o endereço com a sessão (ou sem sessão) e as rotas simuladas.
 * Decisões: Listas públicas simuladas por padrão.
 */
function abrir(endereco: string, papeis: Papel[] | null, rotas: Parameters<typeof simularApi>[0]) {
  if (papeis) {
    const sessao: SessaoApi = {
      tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
      usuario: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', papeis },
    }
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    if (papeis.includes('INVESTIDOR')) localStorage.setItem(CHAVE_PAPEL_ATIVO, 'INVESTIDOR')
    rotas = { 'POST /api/v1/auth/renovar': () => ({ corpo: sessao }), ...rotas }
  }
  const api = simularApi({
    'GET /api/v1/listas/CATEGORIA_CHAMADO': () => ({ corpo: CATEGORIAS }),
    'GET /api/v1/listas/CATEGORIA_DENUNCIA': () => ({ corpo: [{ codigo: 'SPAM', rotulo: 'Spam', grupo: 'NORMAL' }] }),
    'GET /api/v1/listas/MOTIVO_DECISAO_MODERACAO': () => ({ corpo: [{ codigo: 'VIOLACAO_LEVE', rotulo: 'Violação leve', grupo: 'SANCAO' }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-89
 * Título: Corpo da chamada
 * Objetivo: Achar o corpo enviado numa rota simulada.
 * Decisões: Primeira chamada da rota.
 */
function corpoDe(chamadas: { chave: string; corpo: unknown }[], chave: string) {
  return chamadas.find((c) => c.chave === chave)?.corpo
}

describe('Moderação e atendimento (S10)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-88, KAN-89
   * Título: Central de ajuda sem login
   * Objetivo: Mostrar artigos e serviços e enviar o pedido sem conta.
   * Decisões: Disputa não aparece no formulário público.
   */
  it('mostra a central de ajuda e recebe pedido sem conta', async () => {
    const { chamadas } = abrir('/ajuda', null, {
      'GET /api/v1/ajuda/servicos': () => ({ corpo: [{ codigo: 'DISPUTAS', nome: 'Mediação de disputas', publico: 'Todos', prestador: 'N2', canal: 'Chamado',
        prazo: '10 dias úteis' }] }),
      'GET /api/v1/ajuda/artigos': () => ({ corpo: [{ id: 'a1', titulo: 'O que é o Match', conteudo: 'Explicação', publicado: true, atualizadoEm: AGORA }] }),
      'POST /api/v1/ajuda/chamados-publicos': () => ({ status: 202 }),
    })
    expect(await screen.findByText('Mediação de disputas')).toBeInTheDocument()
    expect(screen.getByText('O que é o Match')).toBeInTheDocument()
    const form = screen.getByRole('form', { name: 'Pedir ajuda sem conta' })
    await within(form).findByRole('option', { name: 'Acesso e conta' })
    expect(within(form).queryByRole('option', { name: 'Disputa' })).toBeNull()
    await userEvent.type(within(form).getByLabelText('Seu nome'), 'Zeca')
    await userEvent.type(within(form).getByLabelText('E-mail para resposta'), 'zeca@x.com')
    await userEvent.type(within(form).getByLabelText('O que aconteceu'), 'Perdi o acesso')
    await userEvent.click(within(form).getByRole('button', { name: 'Enviar pedido' }))
    expect(await screen.findByRole('status')).toHaveTextContent('zeca@x.com')
    expect(corpoDe(chamadas, 'POST /api/v1/ajuda/chamados-publicos')).toEqual({ nome: 'Zeca', email: 'zeca@x.com', categoria: 'ACESSO', descricao: 'Perdi o acesso' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Confirmar chamado sem conta
   * Objetivo: Confirmar o e-mail pelo link e mostrar o protocolo.
   * Decisões: Token na URL.
   */
  it('confirma o pedido sem conta pelo link', async () => {
    abrir('/ajuda/confirmar?token=abc', null, {
      'POST /api/v1/ajuda/chamados-publicos/confirmacao': () => ({ corpo: { protocolo: '12345678-aaaa' } }),
    })
    expect(await screen.findByRole('status')).toHaveTextContent('12345678')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89, KAN-101, KAN-86, KAN-99
   * Título: Meus pedidos
   * Objetivo: Abrir um chamado, ver as denúncias e recorrer de uma sanção.
   * Decisões: Uma sanção recorrível simulada.
   */
  it('abre chamado e recorre de sanção', async () => {
    const { chamadas } = abrir('/ajuda/chamados', ['INVESTIDOR'], {
      'GET /api/v1/chamados': () => ({ corpo: [{ id: 'ch1', categoria: 'Acesso e conta', servico: 'ONBOARDING', situacao: 'AGUARDANDO_USUARIO', nivel: 'N1',
        criadoEm: AGORA, atualizadoEm: AGORA, souOutraParte: false }] }),
      'GET /api/v1/denuncias': () => ({ corpo: [{ protocolo: 'abcdef12-0', objetoTipo: 'PERFIL', categoria: 'Spam', criadaEm: AGORA, analisada: true }] }),
      'GET /api/v1/sancoes': () => ({ corpo: [{ sancao: { id: 's1', alvoTipo: 'USUARIO', alvoId: 'u1', tipo: 'SUSPENSAO', dias: 7, inicio: AGORA, fim: AGORA,
        motivo: 'VIOLACAO_LEVE', situacao: 'ATIVA' }, motivo: 'Violação leve', recorrerAte: AGORA, podeRecorrer: true }] }),
      'GET /api/v1/recursos': () => ({ corpo: [{ id: 'r0', origemTipo: 'VERIFICACAO', origemId: 'p', autor: 'Ana', argumentos: 'x', anexos: null, situacao: 'INDEFERIDO',
        prazo: AGORA, criadoEm: AGORA, julgadoPor: 'Alice', julgadoEm: AGORA, motivo: 'Documento ilegível' }] }),
      'POST /api/v1/chamados': () => ({ status: 201, corpo: CHAMADO }),
      'POST /api/v1/recursos': () => ({ status: 201, corpo: {} }),
    })
    expect(await screen.findByText('Aguardando você', { exact: false })).toBeInTheDocument()
    expect(screen.getByText(/abcdef12 · Spam · analisada/)).toBeInTheDocument()
    expect(screen.getByText('Motivo: Documento ilegível')).toBeInTheDocument()
    const novo = screen.getByRole('form', { name: 'Novo chamado' })
    await within(novo).findByRole('option', { name: 'Acesso e conta' })
    await userEvent.selectOptions(within(novo).getByLabelText('Assunto'), 'ACESSO')
    await userEvent.type(within(novo).getByLabelText('Descrição'), 'Não recebo o e-mail')
    await userEvent.click(within(novo).getByRole('button', { name: 'Abrir chamado' }))
    expect(corpoDe(chamadas, 'POST /api/v1/chamados')).toEqual({ categoria: 'ACESSO', descricao: 'Não recebo o e-mail' })
    const sancoes = screen.getByRole('region', { name: 'Minhas sanções' })
    await userEvent.click(within(sancoes).getByRole('button', { name: /^Recorrer/ }))
    await userEvent.type(within(sancoes).getByLabelText('Seus argumentos'), 'Não fui eu que mandei as mensagens')
    await userEvent.click(within(sancoes).getByRole('button', { name: 'Enviar recurso' }))
    expect(await within(sancoes).findByRole('status')).toHaveTextContent('Recurso enviado')
    expect(corpoDe(chamadas, 'POST /api/v1/recursos')).toEqual({ origemTipo: 'SANCAO', origemId: 's1', argumentos: 'Não fui eu que mandei as mensagens' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89
   * Título: Chamado resolvido
   * Objetivo: Responder e avaliar o atendimento.
   * Decisões: Nota padrão 5.
   */
  it('responde e avalia o chamado', async () => {
    const { chamadas } = abrir('/ajuda/chamados/ch1', ['INVESTIDOR'], {
      'GET /api/v1/chamados/ch1': () => ({ corpo: CHAMADO }),
      'POST /api/v1/chamados/ch1/mensagens': () => ({ corpo: { ...CHAMADO, situacao: 'EM_ATENDIMENTO' } }),
      'POST /api/v1/chamados/ch1/avaliacao': () => ({ corpo: { ...CHAMADO, situacao: 'FECHADO', nota: 4 } }),
    })
    expect(await screen.findByText('Resolvido')).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Nota (1 a 5)'), '4')
    await userEvent.click(screen.getByRole('button', { name: 'Avaliar e fechar' }))
    expect(await screen.findByText('Sua avaliação: 4 de 5.')).toBeInTheDocument()
    expect(corpoDe(chamadas, 'POST /api/v1/chamados/ch1/avaliacao')).toEqual({ nota: 4, comentario: '' })
    expect(screen.queryByRole('form', { name: 'Responder' })).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-99
   * Título: Denunciar startup
   * Objetivo: Denunciar a startup pela página do catálogo e mostrar o protocolo.
   * Decisões: Apresentação mínima simulada.
   */
  it('denuncia a startup pelo catálogo', async () => {
    const { chamadas } = abrir('/catalogo/startups/s1', ['EMPREENDEDOR'], {
      'GET /api/v1/catalogo/startups/s1': () => ({ corpo: { modo: 'CATALOGO', comoVerificado: true, cabecalho: { id: 's1', nome: 'Rodante', segmento: 'Fintech',
        estagio: 'MVP', localizacao: null, site: null, redes: [], temLogotipo: false, naoConstituida: false, selo: 'NAO_VERIFICADO' },
        paginas: [{ id: 'apresentacao', titulo: 'Apresentação', blocos: [] }] } }),
      'POST /api/v1/denuncias': () => ({ status: 201, corpo: { protocolo: 'pppppppp-1', recebidaEm: AGORA } }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Denunciar esta startup' }))
    const form = screen.getByRole('form', { name: 'Denunciar' })
    await within(form).findByRole('option', { name: 'Spam' })
    await userEvent.selectOptions(within(form).getByLabelText('Categoria'), 'SPAM')
    await userEvent.type(within(form).getByLabelText('O que aconteceu'), 'Propaganda enganosa')
    await userEvent.click(within(form).getByRole('button', { name: 'Enviar denúncia' }))
    expect(await screen.findByRole('status')).toHaveTextContent('pppppppp')
    expect(corpoDe(chamadas, 'POST /api/v1/denuncias')).toEqual({ objetoTipo: 'STARTUP', objetoId: 's1', categoria: 'SPAM', descricao: 'Propaganda enganosa' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-100, KAN-101, KAN-102
   * Título: Caso de moderação
   * Objetivo: Ver a fila, assumir, aplicar medida, levantar acessos, dar parecer e decidir.
   * Decisões: Administrador (modera e dá parecer).
   */
  it('conduz o caso de moderação', async () => {
    const caso = { id: 'k1', objetoTipo: 'STARTUP', objetoId: 's1', objeto: 'Rodante', prioridade: 'CRITICA', categoria: 'Fraude', situacao: 'ABERTO', prazo: AGORA,
      responsavel: null, parecerJuridico: null, decisao: null, motivo: null, observacao: null, alvoPadrao: 'STARTUP',
      denuncias: [{ id: 'd1', denunciante: 'Ana', categoria: 'Fraude', descricao: 'Golpe', evidencias: null, criadaEm: AGORA, semAcaoAntes: 1 }],
      historico: [], anteriores: [], medidas: [] }
    const { chamadas } = abrir('/admin/moderacao', ['ADMIN'], {
      'GET /api/v1/admin/moderacao/casos': () => ({ corpo: [{ id: 'k1', objetoTipo: 'STARTUP', objeto: 'Rodante', prioridade: 'CRITICA', categoria: 'Fraude',
        situacao: 'ABERTO', prazo: AGORA, vencido: true, responsavel: null, denuncias: 1, criadoEm: AGORA }] }),
      'GET /api/v1/admin/moderacao/casos/k1': () => ({ corpo: caso }),
      'POST /api/v1/admin/moderacao/casos/k1/assumir': () => ({ corpo: { ...caso, situacao: 'EM_ANALISE', responsavel: 'Ana' } }),
      'POST /api/v1/admin/moderacao/casos/k1/medida-preventiva': () => ({ corpo: { ...caso, medidas: [{ id: 'm1', alvoTipo: 'STARTUP', alvoId: 's1',
        tipo: 'PREVENTIVA_DESPUBLICAR', dias: null, inicio: AGORA, fim: null, motivo: 'x', situacao: 'ATIVA' }] } }),
      'GET /api/v1/admin/moderacao/casos/k1/acessos-nivel2': () => ({ corpo: [{ pessoaId: 'p', pessoa: 'Caio', liberadoEm: AGORA, validoAte: AGORA,
        encerradoEm: null, motivo: null, links: 2 }] }),
      'PUT /api/v1/admin/moderacao/casos/k1/parecer': () => ({ corpo: { ...caso, parecerJuridico: 'Violação' } }),
      'POST /api/v1/admin/moderacao/casos/k1/decisao': () => ({ corpo: { ...caso, situacao: 'DECIDIDO', decisao: 'ADVERTENCIA' } }),
    })
    await userEvent.click(await screen.findByRole('link', { name: 'Rodante' }))
    expect(await screen.findByText('Golpe')).toBeInTheDocument()
    expect(screen.getByText(/1 denúncia\(s\) anterior\(es\) sem ação/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Assumir' }))
    const medida = screen.getByRole('form', { name: 'Medida preventiva' })
    await userEvent.selectOptions(within(medida).getByLabelText('Tipo de medida'), 'DESPUBLICAR')
    await userEvent.type(within(medida).getByLabelText('Motivo da medida'), 'Golpe em curso')
    await userEvent.click(within(medida).getByRole('button', { name: 'Aplicar medida' }))
    expect(await screen.findByText(/Startup despublicada \(preventiva\)/)).toBeInTheDocument()
    const acessos = screen.getByRole('form', { name: 'Acessos ao Nível 2' })
    await userEvent.type(within(acessos).getByLabelText('Justificativa da consulta'), 'Vazamento')
    await userEvent.click(within(acessos).getByRole('button', { name: 'Levantar acessos' }))
    expect(await within(acessos).findByText(/Caio/)).toBeInTheDocument()
    const parecer = screen.getByRole('form', { name: 'Parecer jurídico' })
    await userEvent.type(within(parecer).getByLabelText('Parecer jurídico'), 'Violação')
    await userEvent.click(within(parecer).getByRole('button', { name: 'Salvar parecer' }))
    const decisao = screen.getByRole('form', { name: 'Decidir o caso' })
    await userEvent.selectOptions(within(decisao).getByLabelText('Decisão'), 'SUSPENSAO')
    await userEvent.selectOptions(within(decisao).getByLabelText('Duração'), '30')
    await within(decisao).findByRole('option', { name: 'Violação leve' })
    await userEvent.selectOptions(within(decisao).getByLabelText('Motivo'), 'VIOLACAO_LEVE')
    await userEvent.click(within(decisao).getByRole('button', { name: 'Decidir' }))
    expect(await screen.findByText(/decisão: Advertência/)).toBeInTheDocument()
    expect(corpoDe(chamadas, 'POST /api/v1/admin/moderacao/casos/k1/decisao')).toEqual({ decisao: 'SUSPENSAO', motivo: 'VIOLACAO_LEVE', dias: 30 })
    expect(corpoDe(chamadas, 'POST /api/v1/admin/moderacao/casos/k1/medida-preventiva')).toEqual({ tipo: 'DESPUBLICAR', motivo: 'Golpe em curso' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-103
   * Título: Abrir disputa
   * Objetivo: Abrir a disputa sobre a participação contestada.
   * Decisões: Componente sozinho; a outra parte vem do registro.
   */
  it('abre disputa sobre a participação', async () => {
    const { chamadas } = simularApi({ 'POST /api/v1/chamados': () => ({ status: 201, corpo: CHAMADO }) })
    render(<MemoryRouter><AbrirDisputa referenciaTipo="RODADA_INVESTIMENTO" referenciaId="i1" /></MemoryRouter>)
    await userEvent.click(screen.getByRole('button', { name: 'Abrir disputa' }))
    await userEvent.type(screen.getByLabelText('Explique o desacordo'), 'Valor errado')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))
    expect(await screen.findByRole('status')).toHaveTextContent('Disputa aberta')
    expect(corpoDe(chamadas, 'POST /api/v1/chamados')).toEqual({ categoria: 'DISPUTA', descricao: 'Valor errado', referenciaTipo: 'RODADA_INVESTIMENTO', referenciaId: 'i1' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-86
   * Título: Julgar recurso
   * Objetivo: Aceitar um recurso com o motivo.
   * Decisões: Moderador.
   */
  it('julga recurso', async () => {
    const { chamadas } = abrir('/admin/recursos', ['MODERADOR'], {
      'GET /api/v1/admin/recursos': () => ({ corpo: [{ id: 'r1', origemTipo: 'SANCAO', origemId: 's1', autor: 'Caio', argumentos: 'Injusto', anexos: 'link',
        situacao: 'PENDENTE', prazo: AGORA, criadoEm: AGORA, julgadoPor: null, julgadoEm: null, motivo: null }] }),
      'POST /api/v1/admin/recursos/r1/julgamento': () => ({ corpo: {} }),
    })
    expect(await screen.findByText('Injusto')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Motivo do julgamento'), 'Provas frágeis')
    await userEvent.click(screen.getByRole('button', { name: 'Aceitar recurso' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/recursos/r1/julgamento')).toEqual({ deferir: true, motivo: 'Provas frágeis' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-88, KAN-89
   * Título: Painel do atendimento
   * Objetivo: Ver a fila e os indicadores, incluir resposta padrão e artigo.
   * Decisões: Suporte N2 (líder).
   */
  it('mostra o painel do atendimento', async () => {
    const { chamadas } = abrir('/admin/atendimento', ['SUPORTE_N2'], {
      'GET /api/v1/admin/atendimento/chamados': () => ({ corpo: [{ id: 'ch1', categoria: 'Acesso e conta', servico: 'ONBOARDING', situacao: 'ABERTO', nivel: 'N1',
        prioridade: 'ALTA', prazo: AGORA, vencido: true, responsavel: null, contato: 'Ana', criadoEm: AGORA }] }),
      'GET /api/v1/admin/atendimento/indicadores': () => ({ corpo: [{ servico: 'ONBOARDING', chamados: 4, noPrazo: 3, satisfacao: 4.5, avaliacoes: 2 }] }),
      'GET /api/v1/admin/atendimento/respostas': () => ({ corpo: [{ id: 'rp1', titulo: 'Boas-vindas', texto: 'Olá' }] }),
      'GET /api/v1/admin/atendimento/artigos': () => ({ corpo: [{ id: 'a1', titulo: 'Rascunho', conteudo: 'x', publicado: false, atualizadoEm: AGORA }] }),
      'POST /api/v1/admin/atendimento/respostas': () => ({ status: 201, corpo: {} }),
      'PUT /api/v1/admin/atendimento/artigos/a1': () => ({ corpo: {} }),
    })
    expect(await screen.findByText('75%')).toBeInTheDocument()
    expect(screen.getByText('4.5 (2)')).toBeInTheDocument()
    const nova = screen.getByRole('form', { name: 'Nova resposta padrão' })
    await userEvent.type(within(nova).getByLabelText('Título da resposta'), 'Encerramento')
    await userEvent.type(within(nova).getByLabelText('Texto da resposta'), 'Até logo')
    await userEvent.click(within(nova).getByRole('button', { name: 'Incluir resposta' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/respostas')).toEqual({ titulo: 'Encerramento', texto: 'Até logo' })
    await userEvent.click(screen.getByRole('button', { name: 'Rascunho' }))
    const artigo = screen.getByRole('form', { name: 'Artigo' })
    await userEvent.click(within(artigo).getByLabelText('Publicado'))
    await userEvent.click(within(artigo).getByRole('button', { name: 'Salvar artigo' }))
    expect(corpoDe(chamadas, 'PUT /api/v1/admin/atendimento/artigos/a1')).toEqual({ titulo: 'Rascunho', conteudo: 'x', publicado: true })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 16:19
   * Cartão Jira: KAN-89, KAN-103
   * Título: Chamado de disputa para a equipe
   * Objetivo: Responder com resposta padrão, escalar e resolver a disputa sem acordo.
   * Decisões: Suporte N2.
   */
  it('conduz a disputa pela equipe', async () => {
    const disputa = { ...CHAMADO, categoria: 'Disputa', categoriaCodigo: 'DISPUTA', situacao: 'EM_ATENDIMENTO', nivel: 'N2', outraParte: 'Eva',
      emailContato: 'a@x.com', mensagens: [{ autor: 'Nilo', equipe: true, interna: true, texto: 'Ouvir a Eva', em: AGORA, minha: true }] }
    const { chamadas } = abrir('/admin/atendimento/ch1', ['SUPORTE_N2'], {
      'GET /api/v1/admin/atendimento/chamados/ch1': () => ({ corpo: disputa }),
      'GET /api/v1/admin/atendimento/respostas': () => ({ corpo: [{ id: 'rp1', titulo: 'Ouvir as partes', texto: 'Vamos ouvir as duas partes.' }] }),
      'POST /api/v1/admin/atendimento/chamados/ch1/assumir': () => ({ corpo: disputa }),
      'POST /api/v1/admin/atendimento/chamados/ch1/mensagens': () => ({ corpo: disputa }),
      'POST /api/v1/admin/atendimento/chamados/ch1/escalonamento': () => ({ corpo: { ...disputa, nivel: 'ESPECIALISTA' } }),
      'POST /api/v1/admin/atendimento/chamados/ch1/resolucao': () => ({ corpo: { ...disputa, situacao: 'RESOLVIDO' } }),
    })
    expect(await screen.findByText(/nota interna/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Assumir' }))
    await screen.findByRole('option', { name: 'Ouvir as partes' })
    await userEvent.selectOptions(screen.getByLabelText('Resposta padrão'), 'rp1')
    await userEvent.click(screen.getByLabelText('Aguardar o usuário'))
    await userEvent.click(within(screen.getByRole('form', { name: 'Responder' })).getByRole('button', { name: 'Enviar' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/chamados/ch1/mensagens'))
      .toEqual({ texto: 'Vamos ouvir as duas partes.', interna: false, aguardarUsuario: true })
    const escalar = screen.getByRole('form', { name: 'Escalar' })
    await userEvent.selectOptions(within(escalar).getByLabelText('Nível'), 'ESPECIALISTA')
    await userEvent.type(within(escalar).getByLabelText('Motivo do escalonamento'), 'Jurídico')
    await userEvent.click(within(escalar).getByRole('button', { name: 'Escalar' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/chamados/ch1/escalonamento')).toEqual({ nivel: 'ESPECIALISTA', motivo: 'Jurídico' })
    await userEvent.type(screen.getByLabelText('Mensagem'), 'Sem acordo')
    await userEvent.selectOptions(screen.getByLabelText('Resultado da disputa'), 'EM_DISPUTA')
    await userEvent.click(screen.getByRole('button', { name: 'Resolver com a mensagem acima' }))
    expect(corpoDe(chamadas, 'POST /api/v1/admin/atendimento/chamados/ch1/resolucao')).toEqual({ texto: 'Sem acordo', resultadoDisputa: 'EM_DISPUTA' })
  })
})
