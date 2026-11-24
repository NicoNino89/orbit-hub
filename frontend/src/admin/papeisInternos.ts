import type { Papel, PapelInterno } from '../api/contas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:15
 * Cartão Jira: KAN-75
 * Título: Papéis da equipe interna
 * Objetivo: Dizer quais papéis dão acesso ao backoffice.
 * Decisões: Todos os papéis internos do MVP (seção 1.2 de processos-e-operacao.md), na ordem em que aparecem
 *           na tela de convite.
 */
export const PAPEIS_INTERNOS: PapelInterno[] = [
  'ADMIN', 'ANALISTA_VERIFICACAO', 'CURADOR', 'MODERADOR', 'SUPORTE_N1', 'SUPORTE_N2', 'ENCARREGADO_DADOS', 'JURIDICO',
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:15
 * Cartão Jira: KAN-75
 * Título: Itens do menu do backoffice
 * Objetivo: Listar as telas do backoffice e quem pode ver cada uma.
 * Decisões: O menu só mostra o que o papel permite; o servidor confere de novo em cada rota. A trilha de
 *           auditoria é do administrador e do encarregado de dados (KAN-80).
 */
export const MENU_BACKOFFICE: { caminho: string; rotulo: string; papeis: Papel[] }[] = [
  { caminho: '/admin/equipe', rotulo: 'Equipe', papeis: ['ADMIN'] },
  { caminho: '/admin/auditoria', rotulo: 'Auditoria', papeis: ['ADMIN', 'ENCARREGADO_DADOS'] },
]
