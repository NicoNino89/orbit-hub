import type { Papel } from '../api/contas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Papéis da equipe interna
 * Objetivo: Dizer quais papéis dão acesso ao backoffice.
 * Decisões: Lista única, ampliada com os papéis internos da KAN-75.
 */
export const PAPEIS_INTERNOS: Papel[] = ['ADMIN']

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Itens do menu do backoffice
 * Objetivo: Listar as telas do backoffice e quem pode ver cada uma.
 * Decisões: O menu só mostra o que o papel permite; o servidor confere de novo em cada rota.
 */
export const MENU_BACKOFFICE: { caminho: string; rotulo: string; papeis: Papel[] }[] = [
  { caminho: '/admin/auditoria', rotulo: 'Auditoria', papeis: ['ADMIN'] },
]
