import { requisitar } from './cliente'
import type { SituacaoDoPedido } from './verificacoes'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Item da fila
 * Objetivo: Tipar um pedido na fila de verificação.
 * Decisões: Prazo e atraso calculados no servidor.
 */
export interface ItemDaFila {
  id: string
  sujeitoTipo: 'PESSOA' | 'ORGANIZACAO' | 'STARTUP' | 'FUNDADOR'
  sujeito: string
  enviadoEm: string
  prazo: string
  atrasado: boolean
  responsavel: string | null
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Pedido no backoffice
 * Objetivo: Tipar a situação e a decisão de um pedido.
 * Decisões: Inclui o encaminhamento à moderação.
 */
export interface PedidoNoBackoffice {
  id: string
  situacao: SituacaoDoPedido
  criadoEm: string
  enviadoEm: string | null
  decididoEm: string | null
  motivo: string | null
  observacao: string | null
  validoAte: string | null
  prazoComplementoAte: string | null
  encaminhadoModeracao: boolean
  documentosApagados: boolean
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Detalhe do pedido
 * Objetivo: Tipar o pedido com o sujeito, os documentos, o histórico e os motivos.
 * Decisões: O grupo do motivo é a decisão a que ele pertence.
 */
export interface DetalheDoPedido {
  pedido: PedidoNoBackoffice
  sujeitoTipo: ItemDaFila['sujeitoTipo']
  sujeito: string
  responsavel: string | null
  documentos: { id: string; tipo: string; nome: string; formato: string; tamanho: number; enviadoEm: string }[]
  historico: PedidoNoBackoffice[]
  motivos: { codigo: string; rotulo: string; grupo: 'VERIFICADO' | 'COMPLEMENTO' | 'RECUSA' | 'FRAUDE' }[]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Fila
 * Objetivo: Buscar os pedidos em análise.
 * Decisões: Ordem de chegada.
 */
export function filaDeVerificacao() {
  return requisitar<ItemDaFila[]>('/api/v1/admin/verificacoes')
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Detalhe
 * Objetivo: Buscar um pedido da fila.
 * Decisões: Sem cache.
 */
export function detalheDoPedido(id: string) {
  return requisitar<DetalheDoPedido>(`/api/v1/admin/verificacoes/${id}`)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Assumir
 * Objetivo: Tornar-se responsável pelo pedido.
 * Decisões: O administrador pode redistribuir.
 */
export function assumirPedido(id: string) {
  return requisitar<PedidoNoBackoffice>(`/api/v1/admin/verificacoes/${id}/assumir`, { metodo: 'POST' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Decidir
 * Objetivo: Registrar a decisão com motivo e observação.
 * Decisões: Observação obrigatória no complemento.
 */
export function decidirPedido(id: string, resultado: 'VERIFICADO' | 'PENDENTE_COMPLEMENTO' | 'RECUSADO', motivo: string, observacao: string) {
  return requisitar<PedidoNoBackoffice>(`/api/v1/admin/verificacoes/${id}/decisao`, {
    metodo: 'POST', corpo: { resultado, motivo, observacao: observacao || null },
  })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Abrir documento
 * Objetivo: Baixar o arquivo de um documento com a sessão e abrir numa aba.
 * Decisões: O endereço do arquivo nunca sai sem o token; a aba recebe um endereço local temporário.
 */
export async function abrirDocumento(pedido: string, documento: string) {
  const arquivo = await requisitar<Blob>(`/api/v1/admin/verificacoes/${pedido}/documentos/${documento}/arquivo`, { comoArquivo: true })
  const endereco = URL.createObjectURL(arquivo)
  window.open(endereco, '_blank', 'noopener')
  setTimeout(() => URL.revokeObjectURL(endereco), 60_000)
}
