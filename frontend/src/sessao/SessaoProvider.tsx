import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import * as autenticacao from '../api/autenticacao'
import { definirFornecedorDeToken, definirRenovador } from '../api/cliente'
import { ehPapelPublico, type Papel } from '../api/contas'
import { CHAVE_PAPEL_ATIVO, CHAVE_SESSAO_ATIVA, ContextoSessao, type EstadoSessao, type Sessao } from './contexto'

/** Faltando menos que isto para o fim da sessão, o uso da tela renova a sessão (KAN-15). */
const LIMIAR_RENOVACAO_MS = 5 * 60_000

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:55
 * Cartão Jira: KAN-75
 * Título: Provedor da sessão
 * Objetivo: Guardar a sessão da pessoa logada, restaurá-la ao recarregar a página e oferecer entrar, sair
 *           e renovar a todas as telas.
 * Decisões: O access token fica só em memória (nunca em localStorage). Ao recarregar, a sessão é
 *           restaurada pelo cookie HttpOnly, mas só se houver a marca de sessão ativa, para visitantes não
 *           dispararem um 401 a cada visita. O cliente HTTP renova sozinho quando recebe 401; se a renovação
 *           falhar, a sessão vira anônima com "expirou". A tela acompanha o fim da sessão por inatividade
 *           e o uso real da tela renova a sessão perto do fim (KAN-15). Contas da equipe entram em dois passos:
 *           a senha devolve um desafio, e o código do aplicativo abre a sessão (KAN-75).
 */
export function SessaoProvider({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoSessao>(() =>
    localStorage.getItem(CHAVE_SESSAO_ATIVA) ? { status: 'carregando' } : { status: 'anonimo', expirou: false })
  const [papelEscolhido, setPapelEscolhido] = useState<Papel | null>(
    () => localStorage.getItem(CHAVE_PAPEL_ATIVO) as Papel | null)
  const token = useRef<string | null>(null)
  const [fimDaSessao, setFimDaSessao] = useState<number | null>(null)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Aplicar sessão recebida
   * Objetivo: Guardar o token em memória, o usuário no estado e o fim da sessão por inatividade.
   * Decisões: Marca a sessão ativa para a restauração depois de recarregar.
   */
  const aplicar = useCallback((sessao: autenticacao.SessaoApi) => {
    token.current = sessao.tokenAcesso
    localStorage.setItem(CHAVE_SESSAO_ATIVA, '1')
    setFimDaSessao(Date.parse(sessao.sessaoExpiraEm))
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
    setFimDaSessao(null)
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Expirar por inatividade
   * Objetivo: Encerrar a sessão na tela quando chega o fim informado pela API, levando ao login.
   * Decisões: O fim é a validade do refresh token (definida no servidor); a tela só acompanha. Com a sessão
   *           encerrada, a rota protegida leva ao login com o endereço atual em "voltar".
   */
  useEffect(() => {
    if (fimDaSessao === null) return
    // setTimeout só aceita até 2^31-1 ms (cerca de 24,8 dias); acima disso dispararia na hora
    const espera = Math.min(Math.max(0, fimDaSessao - Date.now()), 2_147_483_647)
    const temporizador = setTimeout(() => encerrarLocal(true), espera)
    return () => clearTimeout(temporizador)
  }, [fimDaSessao, encerrarLocal])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 24/11/2026
   * Hora: 01:20
   * Cartão Jira: KAN-15
   * Título: Uso da tela conta como atividade
   * Objetivo: Renovar a sessão quando a pessoa está usando a tela (clicando, digitando, rolando) e a sessão
   *           está perto do fim, para quem preenche um formulário longo não perder o que digitou.
   * Decisões: Só renova nos últimos minutos da sessão e no máximo uma vez a cada 30 segundos; sem uso, nada é
   *           renovado e a sessão expira de verdade.
   */
  useEffect(() => {
    if (fimDaSessao === null) return
    let ultimaTentativa = 0
    const aoUsar = () => {
      const agora = Date.now()
      if (fimDaSessao - agora > LIMIAR_RENOVACAO_MS || agora - ultimaTentativa < 30_000) return
      ultimaTentativa = agora
      void renovar()
    }
    const eventos = ['pointerdown', 'keydown', 'wheel', 'touchstart'] as const
    eventos.forEach((e) => window.addEventListener(e, aoUsar, { passive: true }))
    return () => eventos.forEach((e) => window.removeEventListener(e, aoUsar))
  }, [fimDaSessao, renovar])

  const papeis: Papel[] = estado.status === 'autenticado' ? estado.usuario.papeis.filter(ehPapelPublico) : []
  const papelAtivo = papelEscolhido && papeis.includes(papelEscolhido) ? papelEscolhido : (papeis[0] ?? null)

  const sessao = useMemo<Sessao>(() => ({
    estado,
    papelAtivo,
    entrar: async (email, senha) => {
      const resposta = await autenticacao.entrar(email, senha)
      if (autenticacao.ehDesafio(resposta)) return resposta
      aplicar(resposta)
      return null
    },
    confirmarSegundoFator: async (desafio, codigo) => aplicar(await autenticacao.confirmarSegundoFator(desafio, codigo)),
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
