import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router'
import { contarNaoLidas, EVENTO_AVISOS } from '../api/notificacoes'

const INTERVALO_MS = 60_000

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Sino de avisos
 * Objetivo: Mostrar no cabeçalho quantos avisos não lidos a pessoa tem e levar à central.
 * Decisões: Reconta a cada minuto, a cada troca de tela e quando a central avisa que algo foi lido. Falha na
 *           contagem não mostra erro: o sino só fica sem número.
 */
export function Sino() {
  const [naoLidas, setNaoLidas] = useState(0)
  const [versao, setVersao] = useState(0)
  const { pathname } = useLocation()

  useEffect(() => {
    const recontar = () => setVersao((v) => v + 1)
    const relogio = window.setInterval(recontar, INTERVALO_MS)
    window.addEventListener(EVENTO_AVISOS, recontar)
    return () => {
      window.clearInterval(relogio)
      window.removeEventListener(EVENTO_AVISOS, recontar)
    }
  }, [])

  useEffect(() => {
    let ativo = true
    contarNaoLidas()
      .then((r) => { if (ativo) setNaoLidas(r.naoLidas) })
      .catch(() => { if (ativo) setNaoLidas(0) })
    return () => { ativo = false }
  }, [versao, pathname])

  const rotulo = naoLidas > 0 ? `Avisos: ${naoLidas} não lido(s)` : 'Avisos'
  return (
    <Link to="/notificacoes" aria-label={rotulo} title={rotulo}
      className="relative inline-flex items-center rounded-md border border-line px-2 py-1">
      <svg aria-hidden="true" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2"
        strokeLinecap="round" strokeLinejoin="round">
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      {naoLidas > 0 && (
        <span className="absolute -right-2 -top-2 min-w-5 rounded-full bg-primary px-1 text-center text-xs font-bold text-on-primary">
          {naoLidas > 99 ? '99+' : naoLidas}
        </span>
      )}
    </Link>
  )
}
