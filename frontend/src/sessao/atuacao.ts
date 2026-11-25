import { useEffect, useState } from 'react'

export const CHAVE_ATUACAO = 'orbit_atuacao'
export const EVENTO_ATUACAO = 'orbit:atuacao'
export const EVENTO_ORGANIZACOES = 'orbit:organizacoes'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Atuação escolhida
 * Objetivo: Tipar a organização em nome da qual o investidor está atuando.
 * Decisões: Guarda o dono da escolha, para outra pessoa no mesmo navegador não herdar a organização.
 */
export interface Atuacao {
  usuario: string
  organizacao: string
  nome: string
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Ler a atuação
 * Objetivo: Devolver a organização escolhida pela pessoa, ou nulo quando ela atua por si.
 * Decisões: Escolha de outra conta é ignorada; o servidor confere o vínculo a cada requisição.
 */
export function lerAtuacao(usuario: string): Atuacao | null {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_ATUACAO) ?? 'null') as Atuacao | null
    return salvo && salvo.usuario === usuario ? salvo : null
  } catch {
    return null
  }
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Escolher a atuação
 * Objetivo: Passar a agir em nome de uma organização, ou voltar a agir por si.
 * Decisões: Avisa as telas abertas pelo evento orbit:atuacao.
 */
export function escolherAtuacao(usuario: string, organizacao: { id: string; nome: string } | null) {
  if (organizacao) {
    localStorage.setItem(CHAVE_ATUACAO, JSON.stringify({ usuario, organizacao: organizacao.id, nome: organizacao.nome }))
  } else {
    localStorage.removeItem(CHAVE_ATUACAO)
  }
  window.dispatchEvent(new Event(EVENTO_ATUACAO))
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Esquecer a atuação
 * Objetivo: Apagar a escolha ao sair da conta.
 * Decisões: Sem evento: a sessão está acabando.
 */
export function esquecerAtuacao() {
  localStorage.removeItem(CHAVE_ATUACAO)
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Acompanhar a atuação
 * Objetivo: Dar à tela a atuação atual e atualizá-la quando mudar.
 * Decisões: Escuta o evento orbit:atuacao.
 */
export function useAtuacao(usuario: string | null) {
  const [versao, setVersao] = useState(0)
  useEffect(() => {
    const mudar = () => setVersao((v) => v + 1)
    window.addEventListener(EVENTO_ATUACAO, mudar)
    return () => window.removeEventListener(EVENTO_ATUACAO, mudar)
  }, [])
  return { atuacao: usuario ? lerAtuacao(usuario) : null, versao }
}
