import { requisitar } from './cliente'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-167, KAN-170, KAN-171
 * Título: Página inicial
 * Objetivo: Tipar e chamar a busca global, os banners, a vitrine da startup e as capas.
 * Decisões: Mesmos nomes da API.
 */
export interface Achado { id: string; nome: string; detalhe: string | null }
export interface ResultadoDaBusca { startups: Achado[]; organizacoes: Achado[]; artigos: Achado[] }

export interface Banner {
  id: string; tipo: 'BANNER' | 'CHAMADA'; titulo: string; destaque: string | null; texto: string | null; botao: string | null; link: string | null
  publico: 'TODOS' | 'INVESTIDOR' | 'EMPREENDEDOR'; inicio: string; fim: string | null; ordem: number; temImagem: boolean; textoAlt: string | null; ativo: boolean
}

export type DadosDoBanner = Omit<Banner, 'id' | 'temImagem'>

export function buscar(q: string) {
  return requisitar<ResultadoDaBusca>(`/api/v1/busca?q=${encodeURIComponent(q)}`)
}

export function bannersVigentes(papel: string | null) {
  return requisitar<Banner[]>(`/api/v1/inicio/banners${papel ? `?papel=${papel}` : ''}`)
}

export function imagemDoBanner(id: string) {
  return requisitar<Blob>(`/api/v1/inicio/banners/${id}/imagem`, { comoArquivo: true })
}

export function todosOsBanners() {
  return requisitar<Banner[]>('/api/v1/admin/banners')
}

export function salvarBanner(id: string | null, dados: DadosDoBanner) {
  return requisitar<Banner>(id ? `/api/v1/admin/banners/${id}` : '/api/v1/admin/banners', { metodo: id ? 'PUT' : 'POST', corpo: dados })
}

export function trocarImagemDoBanner(id: string, arquivo: File) {
  const formulario = new FormData()
  formulario.append('arquivo', arquivo)
  return requisitar<Banner>(`/api/v1/admin/banners/${id}/imagem`, { metodo: 'PUT', formulario })
}

export function lerVitrine(startupId: string) {
  return requisitar<{ chamada: string | null; temCapa: boolean }>(`/api/v1/startups/${startupId}/vitrine`)
}

export function definirChamada(startupId: string, chamada: string) {
  return requisitar<{ chamada?: string }>(`/api/v1/startups/${startupId}/vitrine`, { metodo: 'PUT', corpo: { chamada } })
}

export function trocarCapa(startupId: string, arquivo: File) {
  const formulario = new FormData()
  formulario.append('arquivo', arquivo)
  return requisitar<{ chamada: string | null; temCapa: boolean }>(`/api/v1/startups/${startupId}/capa`, { metodo: 'PUT', formulario })
}

export function capaNoCatalogo(startupId: string) {
  return requisitar<Blob>(`/api/v1/catalogo/startups/${startupId}/capa`, { comoArquivo: true })
}

export function capaDaEquipe(startupId: string) {
  return requisitar<Blob>(`/api/v1/startups/${startupId}/capa`, { comoArquivo: true })
}
