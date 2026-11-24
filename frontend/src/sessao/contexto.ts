import { createContext, useContext } from 'react'
import type { UsuarioResumo } from '../api/autenticacao'
import type { Papel } from '../api/contas'

export type EstadoSessao =
  | { status: 'carregando' }
  | { status: 'anonimo'; expirou: boolean }
  | { status: 'autenticado'; usuario: UsuarioResumo; termosPendentes: boolean }

export interface Sessao {
  estado: EstadoSessao
  /** Papel com que a pessoa está atuando agora (KAN-12), entre os que a conta tem. */
  papelAtivo: Papel | null
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => Promise<void>
  /** Renova a sessão agora (depois de aceitar termos, por exemplo); devolve se conseguiu. */
  renovar: () => Promise<boolean>
  escolherPapel: (papel: Papel) => void
}

export const ContextoSessao = createContext<Sessao | null>(null)

/** Marca (não sensível) de que há uma sessão a restaurar ao recarregar a página. */
export const CHAVE_SESSAO_ATIVA = 'orbit_sessao_ativa'
/** Último papel escolhido pela pessoa, para manter a escolha entre visitas. */
export const CHAVE_PAPEL_ATIVO = 'orbit_papel_ativo'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Usar a sessão
 * Objetivo: Dar às telas acesso ao estado da sessão e às ações de entrar, sair e renovar.
 * Decisões: Falha cedo se usado fora do SessaoProvider, para o erro aparecer no desenvolvimento.
 */
export function useSessao(): Sessao {
  const sessao = useContext(ContextoSessao)
  if (!sessao) throw new Error('useSessao precisa estar dentro do SessaoProvider')
  return sessao
}
