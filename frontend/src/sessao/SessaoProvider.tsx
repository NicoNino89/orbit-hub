import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import * as autenticacao from '../api/autenticacao'
import { definirFornecedorDeToken, definirRenovador } from '../api/cliente'
import type { Papel } from '../api/contas'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA, ContextoSessao, type EstadoSessao, type Sessao } from './contexto'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Provedor da sessão
 * Objetivo: Guardar a sessão da pessoa logada, restaurá-la ao recarregar a página e oferecer entrar, sair
 *           e renovar a todas as telas.
 * Decisões: O access token fica só em memória (nunca em localStorage). Ao recarregar, a sessão é
 *           restaurada pelo cookie HttpOnly, mas só se houver a marca de sessão ativa, para visitantes não
 *           dispararem um 401 a cada visita. O cliente HTTP renova sozinho quando recebe 401; se a renovação
 *           falhar, a sessão vira anônima com "expirou" (KAN-15).
 */
export function SessaoProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSessao>(() =>
    localStorage.getItem(CHAVE_SESSAO_ATIVA) ? { status: 'carregando' } : { status: 'anonimo', expirou: false })
  const [papelEscolhido, setPapelEscolhido] = useState<Papel | null>(
    () => localStorage.getItem(CHAVE_PAPEL_ATIVO) as Papel | null)
  const token = useRef<string | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Aplicar sessão recebida
   * Objetivo: Guardar o token em memória e o usuário no estado.
   * Decisões: Marca a sessão ativa para a restauração depois de recarregar.
   */
  const aplicar = useCallback((sessao: autenticacao.SessaoApi) => {
    token.current = sessao.tokenAcesso
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    setEstado({ status: 'autenticado', usuario: sessao.usuario, termosPendentes: sessao.termosPendentes })
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Encerrar localmente
   * Objetivo: Apagar o token e a marca de sessão e voltar ao estado anônimo.
   * Decisões: "expirou" diferencia o fim por inatividade (mostra aviso) do logout voluntário.
   */
  const encerrarLocal = useCallback((expirou: boolean) => {
    token.current = null
    localStorage.removeItem(CHAVE_SESSAO_ATIVA)
    setEstado({ status: 'anonimo', expirou })
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Renovar a sessão
   * Objetivo: Trocar o refresh token por uma sessão nova; se falhar, encerrar como expirada.
   * Decisões: Usado na restauração, pelo cliente HTTP ao receber 401 e depois de aceitar termos.
   */
  const renovar = useCallback(async () => {
    try {
      aplicar(await autenticacao.renovar())
      return true
    } catch {
      encerrarLocal(token.current !== null || localStorage.getItem(CHAVE_SESSAO_ATIVA) !== null)
      return false
    }
  }, [aplicar, encerrarLocal])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Ligar o cliente HTTP à sessão
   * Objetivo: Informar ao cliente qual token enviar e como renovar.
   * Decisões: Desliga ao desmontar, para um teste não herdar a sessão de outro.
   */
  useEffect(() => {
    definirFornecedorDeToken(() => token.current)
    definirRenovador(renovar)
    return () => {
      definirFornecedorDeToken(() => null)
      definirRenovador(null)
    }
  }, [renovar])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:10
   * Cartão Jira: KAN-13
   * Título: Restaurar ao abrir
   * Objetivo: Recuperar a sessão pelo cookie quando a página é aberta ou recarregada.
   * Decisões: Só quando há a marca de sessão ativa; falha aqui não mostra aviso de expiração.
   */
  useEffect(() => {
    if (!localStorage.getItem(CHAVE_SESSAO_ATIVA)) return
    autenticacao.renovar().then(aplicar).catch(() => encerrarLocal(false))
  }, [aplicar, encerrarLocal])

  const papeis: Papel[] = estado.status === 'autenticado' ? estado.usuario.papeis.filter((p) => p !== 'ADMIN') : []
  const papelAtivo = papelEscolhido && papeis.includes(papelEscolhido) ? papelEscolhido : (papeis[0] ?? null)

  const sessao = useMemo<Sessao>(() => ({
    estado,
    papelAtivo,
    entrar: async (email, senha) => aplicar(await autenticacao.entrar(email, senha)),
    sair: async () => {
      await autenticacao.sair()
      encerrarLocal(false)
    },
    renovar,
    escolherPapel: (papel) => {
      localStorage.setItem(CHAVE_PAPEL_ATIVO, papel)
      setPapelEscolhido(papel)
    },
  }), [estado, papelAtivo, aplicar, encerrarLocal, renovar])

  return <ContextoSessao.Provider value={sessao}>{children}</ContextoSessao.Provider>
}
