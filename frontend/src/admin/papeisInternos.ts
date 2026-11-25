import type { Papel, PapelInterno } from '../api/contas'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
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
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Itens do menu do backoffice
 * Objetivo: Listar as telas do backoffice e quem pode ver cada uma.
 * Decisões: O menu só mostra o que o papel permite; o servidor confere de novo em cada rota. A trilha de A fila de verificação entrou com o KAN-83; "Meus vínculos", com o KAN-87.
 *           auditoria é do administrador e do encarregado de dados (KAN-80); a fila de segunda aprovação é de toda a
 *           equipe, e quem decide cada proposta é conferido no servidor (KAN-81).
 */
export const MENU_BACKOFFICE: { caminho: string; rotulo: string; papeis: Papel[] }[] = [
  { caminho: '/admin/aprovacoes', rotulo: 'Segunda aprovação', papeis: PAPEIS_INTERNOS },
  { caminho: '/admin/verificacao', rotulo: 'Verificação', papeis: ['ADMIN', 'ANALISTA_VERIFICACAO'] },
  { caminho: '/admin/curadoria', rotulo: 'Curadoria', papeis: ['ADMIN', 'CURADOR'] },
  { caminho: '/admin/edicoes', rotulo: 'Edições do catálogo', papeis: ['ADMIN', 'CURADOR'] },
  { caminho: '/admin/moderacao', rotulo: 'Moderação', papeis: ['ADMIN', 'MODERADOR', 'JURIDICO'] },
  { caminho: '/admin/recursos', rotulo: 'Recursos', papeis: ['ADMIN', 'MODERADOR', 'CURADOR', 'ANALISTA_VERIFICACAO'] },
  { caminho: '/admin/atendimento', rotulo: 'Atendimento', papeis: ['ADMIN', 'SUPORTE_N1', 'SUPORTE_N2', 'MODERADOR', 'ENCARREGADO_DADOS', 'JURIDICO'] },
  { caminho: '/admin/privacidade', rotulo: 'Privacidade', papeis: ['ADMIN', 'ENCARREGADO_DADOS'] },
  { caminho: '/admin/times', rotulo: 'Times', papeis: PAPEIS_INTERNOS },
  { caminho: '/admin/vinculos', rotulo: 'Meus vínculos', papeis: PAPEIS_INTERNOS },
  { caminho: '/admin/equipe', rotulo: 'Equipe', papeis: ['ADMIN'] },
  { caminho: '/admin/parametros', rotulo: 'Parâmetros', papeis: ['ADMIN'] },
  { caminho: '/admin/listas', rotulo: 'Listas', papeis: ['ADMIN'] },
  { caminho: '/admin/rotinas', rotulo: 'Rotinas', papeis: ['ADMIN'] },
  { caminho: '/admin/auditoria', rotulo: 'Auditoria', papeis: ['ADMIN', 'ENCARREGADO_DADOS'] },
]
