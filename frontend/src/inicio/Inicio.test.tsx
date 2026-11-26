import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Rotas } from '../App'
import { SecaoVitrine } from './Vitrine'
import type { SessaoApi } from '../api/autenticacao'
import type { Papel } from '../api/contas'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA } from '../sessao/contexto'
import { simularApi } from '../test/apiSimulada'

const PERFIL = {
  tipo: 'ANJO', tipoOutro: null, ticketMinimo: 1, ticketMaximo: 2, lideranca: 'LIDERA', followOn: true, participacaoNaGestao: 'NAO_PARTICIPA', instrumentos: [],
  segmentos: ['AGRO', 'SAUDE'], estagios: [], maturidades: [], localizacoes: [],
  complementares: { modelos: [], modalidades: [], horizonte: null, perfilRisco: 'CONSERVADOR', experiencia: null, interesses: [] },
}
const ITEM = { startup: { id: 's1', nome: 'AgroTech', segmento: 'Agro', estagio: 'MVP', localizacao: 'São Paulo', temLogotipo: false, temCapa: false,
  chamada: 'Soluções para o agro' }, criterios: [], rodadaAberta: true, favorito: false, publicadaEm: null }

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-166, KAN-167, KAN-168, KAN-170, KAN-171, KAN-172
 * Título: Abrir com papel
 * Objetivo: Renderizar o endereço com a sessão e as rotas simuladas.
 * Decisões: Listas e banners simulados por padrão.
 */
function abrir(endereco: string, papeis: Papel[], rotas: Parameters<typeof simularApi>[0]) {
  const sessao: SessaoApi = {
    tokenAcesso: 't', expiraEm: '2999-01-01T00:00:00Z', sessaoExpiraEm: '2999-01-01T00:00:00Z', termosPendentes: false,
    usuario: { id: 'u1', nome: 'Ana', email: 'a@exemplo.com', papeis },
  }
  localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
  localStorage.setItem(CHAVE_PAPEL_ATIVO, papeis[0])
  const api = simularApi({
    'POST /api/v1/auth/renovar': () => ({ corpo: sessao }),
    'GET /api/v1/listas/SEGMENTO': () => ({ corpo: [{ codigo: 'AGRO', rotulo: 'Agronegócio' }, { codigo: 'SAUDE', rotulo: 'Saúde' }, { codigo: 'FINTECH', rotulo: 'Fintech' }] }),
    'GET /api/v1/listas/PERFIL_RISCO': () => ({ corpo: [{ codigo: 'CONSERVADOR', rotulo: 'Conservador' }] }),
    'GET /api/v1/listas/TIPO_INVESTIDOR': () => ({ corpo: [{ codigo: 'ANJO', rotulo: 'Investidor-anjo' }] }),
    'GET /api/v1/inicio/banners': () => ({ corpo: [
      { id: 'b1', tipo: 'BANNER', titulo: 'Empresas que inspiram.', destaque: 'Investidores que acreditam.', texto: 'Uma rede', botao: 'Saiba mais', link: '/ajuda',
        publico: 'TODOS', inicio: '2026-01-01', fim: null, ordem: 1, temImagem: false, textoAlt: null, ativo: true },
      { id: 'b2', tipo: 'CHAMADA', titulo: 'Conquiste seu espaço', destaque: null, texto: null, botao: 'Participe!', link: '/catalogo', publico: 'TODOS',
        inicio: '2026-01-01', fim: null, ordem: 1, temImagem: false, textoAlt: null, ativo: true }] }),
    ...rotas,
  })
  render(<MemoryRouter initialEntries={[endereco]}><Rotas /></MemoryRouter>)
  return api
}

describe('Novo visual (S12)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-168, KAN-170, KAN-171, KAN-172
   * Título: Página inicial do investidor
   * Objetivo: Ver banner, destaques, chamada, painel do perfil; favoritar, trocar de aba e editar interesses.
   * Decisões: Investidor com tese.
   */
  it('mostra a página inicial do investidor', async () => {
    const { chamadas } = abrir('/painel', ['INVESTIDOR'], {
      'GET /api/v1/investidores/perfil': () => ({ corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: PERFIL, atualizadoPor: null, atualizadoEm: null } }),
      'PUT /api/v1/investidores/perfil': ({ corpo }) => ({ corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: corpo, atualizadoPor: null, atualizadoEm: null } }),
      'GET /api/v1/catalogo/startups': () => ({ corpo: { lista: 'PERSONALIZADA', verificado: true, comPerfil: true, itens: [ITEM] } }),
      'PUT /api/v1/catalogo/favoritos/s1': () => ({ status: 204 }),
    })
    expect(await screen.findByText('Investidores que acreditam.')).toBeInTheDocument()
    const cartao = await screen.findByRole('article', { name: 'AgroTech' })
    expect(within(cartao).getByText('Soluções para o agro')).toBeInTheDocument()
    expect(within(cartao).getByText('Rodada aberta')).toBeInTheDocument()
    await userEvent.click(within(cartao).getByRole('button', { name: 'Salvar AgroTech nos favoritos' }))
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/catalogo/favoritos/s1')).toBe(true)
    expect(screen.getByRole('region', { name: 'Conquiste seu espaço' })).toBeInTheDocument()
    const perfil = await screen.findByRole('region', { name: 'Seu perfil de investidor' })
    expect(await within(perfil).findByText(/Perfil: Conservador/)).toBeInTheDocument()
    await userEvent.click(await within(perfil).findByRole('button', { name: 'Remover Saúde' }))
    expect(chamadas.filter((c) => c.chave === 'PUT /api/v1/investidores/perfil').at(-1)?.corpo).toMatchObject({ segmentos: ['AGRO'] })
    await userEvent.selectOptions(within(perfil).getByLabelText('Novo interesse'), 'FINTECH')
    await userEvent.click(within(perfil).getByRole('button', { name: 'Adicionar' }))
    expect(chamadas.filter((c) => c.chave === 'PUT /api/v1/investidores/perfil').at(-1)?.corpo).toMatchObject({ segmentos: ['AGRO', 'FINTECH'] })
    await userEvent.click(screen.getByRole('tab', { name: 'Empresas' }))
    expect(chamadas.some((c) => c.chave === 'GET /api/v1/catalogo/startups')).toBe(true)
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-166, KAN-167
   * Título: Menu, busca e "Comece agora"
   * Objetivo: Ver o menu lateral, o cartão de primeiro passo sem tese e os resultados da busca global.
   * Decisões: Investidor sem tese.
   */
  it('mostra o menu lateral, o primeiro passo e a busca global', async () => {
    abrir('/painel', ['INVESTIDOR'], {
      'GET /api/v1/investidores/perfil': () => ({ corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: null, atualizadoPor: null, atualizadoEm: null } }),
      'GET /api/v1/catalogo/startups': () => ({ corpo: { lista: 'PERSONALIZADA', verificado: false, comPerfil: false, itens: [] } }),
      'GET /api/v1/busca': () => ({ corpo: { startups: [{ id: 's1', nome: 'AgroTech', detalhe: 'Agro' }], organizacoes: [], artigos: [{ id: 'a1', nome: 'O que é o Match', detalhe: null }] } }),
    })
    const menu = await screen.findByRole('navigation', { name: 'Principal' })
    expect(within(menu).getByRole('link', { name: /Explorar/ })).toHaveAttribute('href', '/catalogo')
    expect(await screen.findByRole('region', { name: 'Comece agora' })).toHaveTextContent('Configurar perfil')
    expect(await screen.findByText(/Defina sua tese de investimento/)).toBeInTheDocument()
    expect(screen.getByText('Nenhuma empresa por aqui ainda. Volte em breve ou explore o catálogo.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Buscar'), 'agro')
    const resultados = await screen.findByRole('listbox', { name: 'Resultados da busca' }, { timeout: 2000 })
    expect(within(resultados).getByText('O que é o Match')).toBeInTheDocument()
    await userEvent.click(within(resultados).getByRole('option', { name: /AgroTech/ }))
    expect(screen.queryByRole('listbox', { name: 'Resultados da busca' })).toBeNull()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-171
   * Título: Banners no backoffice
   * Objetivo: Listar e incluir um banner.
   * Decisões: Administrador.
   */
  it('cadastra banner no backoffice', async () => {
    const { chamadas } = abrir('/admin/banners', ['ADMIN'], {
      'GET /api/v1/admin/banners': () => ({ corpo: [{ id: 'b1', tipo: 'BANNER', titulo: 'Empresas', destaque: null, texto: null, botao: null, link: null, publico: 'TODOS',
        inicio: '2026-01-01', fim: null, ordem: 1, temImagem: false, textoAlt: null, ativo: true }] }),
      'POST /api/v1/admin/banners': () => ({ corpo: {} }),
      'PUT /api/v1/admin/banners/b1': () => ({ corpo: {} }),
    })
    await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))
    const form = screen.getByRole('form', { name: 'Banner' })
    await userEvent.type(within(form).getByLabelText('Destaque (em âmbar)'), 'que inspiram')
    await userEvent.click(within(form).getByRole('button', { name: 'Salvar alterações' }))
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/admin/banners/b1')?.corpo).toMatchObject({ titulo: 'Empresas', destaque: 'que inspiram' })
    await userEvent.type(within(form).getByLabelText('Título'), 'Nova edição')
    await userEvent.click(within(form).getByRole('button', { name: 'Incluir' }))
    expect(chamadas.find((c) => c.chave === 'POST /api/v1/admin/banners')?.corpo).toMatchObject({ titulo: 'Nova edição', tipo: 'BANNER' })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:40
   * Cartão Jira: KAN-170
   * Título: Vitrine da startup
   * Objetivo: Salvar a chamada e enviar a capa.
   * Decisões: Componente sozinho.
   */
  it('salva a chamada e a capa da startup', async () => {
    const { chamadas } = simularApi({
      'GET /api/v1/startups/s1/vitrine': () => ({ corpo: { chamada: null, temCapa: false } }),
      'PUT /api/v1/startups/s1/vitrine': () => ({ corpo: { chamada: 'Agro forte' } }),
      'PUT /api/v1/startups/s1/capa': () => ({ corpo: { chamada: 'Agro forte', temCapa: true } }),
    })
    render(<MemoryRouter><SecaoVitrine startupId="s1" podeEditar /></MemoryRouter>)
    await userEvent.type(await screen.findByLabelText('Chamada (até 120 caracteres)'), 'Agro forte')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar chamada' }))
    expect(chamadas.find((c) => c.chave === 'PUT /api/v1/startups/s1/vitrine')?.corpo).toEqual({ chamada: 'Agro forte' })
    await userEvent.upload(screen.getByLabelText(/Imagem de capa/), new File(['x'], 'c.png', { type: 'image/png' }))
    expect(await screen.findByText('A startup tem capa.')).toBeInTheDocument()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:55
   * Cartão Jira: KAN-170, KAN-171, KAN-166
   * Título: Imagens e menu do empreendedor
   * Objetivo: Trocar de destaque no banner com imagem, ver a capa no cartão, enviar imagem de banner e abrir o menu no celular.
   * Decisões: URL.createObjectURL simulado.
   */
  it('mostra imagens do banner e da capa e o menu do empreendedor', async () => {
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: vi.fn(() => 'blob:x'), revokeObjectURL: vi.fn() }))
    const banner = (id: string, titulo: string) => ({ id, tipo: 'BANNER', titulo, destaque: null, texto: null, botao: null, link: null, publico: 'TODOS',
      inicio: '2026-01-01', fim: null, ordem: 1, temImagem: true, textoAlt: 'Aperto de mãos', ativo: true })
    abrir('/painel', ['INVESTIDOR'], {
      'GET /api/v1/inicio/banners': () => ({ corpo: [banner('b1', 'Primeiro'), banner('b2', 'Segundo')] }),
      'GET /api/v1/inicio/banners/b1/imagem': () => ({ corpo: {} }),
      'GET /api/v1/inicio/banners/b2/imagem': () => ({ corpo: {} }),
      'GET /api/v1/investidores/perfil': () => ({ corpo: { daOrganizacao: false, podeEditar: true, tipoFixo: null, perfil: null, atualizadoPor: null, atualizadoEm: null } }),
      'GET /api/v1/catalogo/startups': () => ({ corpo: { lista: 'PERSONALIZADA', verificado: true, comPerfil: true,
        itens: [{ ...ITEM, startup: { ...ITEM.startup, temCapa: true } }] } }),
      'GET /api/v1/catalogo/startups/s1/capa': () => ({ corpo: {} }),
    })
    expect(await screen.findByText('Primeiro')).toBeInTheDocument()
    expect(await screen.findByAltText('Capa da AgroTech')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Destaque 2' }))
    expect(await screen.findByText('Segundo')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Próximos' }))
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 25/11/2026
   * Hora: 22:55
   * Cartão Jira: KAN-166, KAN-171
   * Título: Empreendedor e imagem de banner
   * Objetivo: Abrir e fechar o menu no celular do empreendedor e enviar a imagem de um banner.
   * Decisões: Duas telas.
   */
  it('abre o menu do empreendedor e envia imagem de banner', async () => {
    const { chamadas } = abrir('/painel', ['EMPREENDEDOR', 'ADMIN'], {
      'GET /api/v1/startups': () => ({ corpo: [] }),
      'GET /api/v1/admin/banners': () => ({ corpo: [{ id: 'b1', tipo: 'BANNER', titulo: 'Empresas', destaque: null, texto: null, botao: null, link: null,
        publico: 'TODOS', inicio: '2026-01-01', fim: null, ordem: 1, temImagem: false, textoAlt: 'x', ativo: false }] }),
      'PUT /api/v1/admin/banners/b1/imagem': () => ({ corpo: {} }),
    })
    expect(await screen.findByText(/Cadastre sua startup/)).toBeInTheDocument()
    expect(await screen.findByRole('region', { name: 'Comece agora' })).toHaveTextContent('Cadastrar startup')
    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }))
    await userEvent.click(screen.getByRole('button', { name: 'Fechar menu' }))
    await userEvent.click(screen.getByRole('link', { name: /Backoffice/ }))
    await userEvent.click(await screen.findByRole('link', { name: 'Banners' }))
    await userEvent.upload(await screen.findByLabelText('Imagem de Empresas'), new File(['x'], 'b.png', { type: 'image/png' }))
    expect(chamadas.some((c) => c.chave === 'PUT /api/v1/admin/banners/b1/imagem')).toBe(true)
  })
})
