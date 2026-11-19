import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent,
  type PointerEvent,
  type ReactNode,
} from 'react'
import { calcularDobra, type Ponto } from './dobra'
import { direcaoDoGesto, LIMIAR_GESTO } from './gesto'
import { useMovimentoReduzido } from './useMovimentoReduzido'
import './folheador.css'

export interface PaginaFolheavel {
  id: string
  titulo: string
}

interface Props<P extends PaginaFolheavel> {
  paginas: P[]
  rotulo: string
  renderizar: (pagina: P) => ReactNode
}

/** Duração de uma virada completa. */
export const DURACAO_VIRADA_MS = 1200
/** Deslocamento, em px, a partir do qual um arraste horizontal passa a segurar a folha. */
const INICIO_ARRASTE = 8
/** Fração da virada a partir da qual soltar a folha completa a virada. */
const PONTO_SEM_VOLTA = 0.3
/** Um arraste curto e rápido também vira a página. */
const PETELECO_MS = 300

type Sentido = 'frente' | 'tras'

/** Folha que está dobrando e a página que ela cobre ou revela. */
interface Virada {
  folha: number
  base: number
}

interface Arraste {
  x: number
  y: number
  instante: number
  ponteiro: number
  sentido: Sentido | null
}

const suave = (x: number) => (x < 0.5 ? 4 * x ** 3 : 1 - (-2 * x + 2) ** 3 / 2)
const desacelerar = (x: number) => 1 - (1 - x) ** 3
const limitar = (x: number) => Math.max(0, Math.min(1, x))

function poligono(pontos: Ponto[]) {
  if (pontos.length < 3) return 'polygon(0 0, 0 0, 0 0)'
  return `polygon(${pontos.map((p) => `${p.x}px ${p.y}px`).join(', ')})`
}

/** Posiciona uma faixa encostada na linha da dobra, do lado que dobrou, para sombras e brilhos. */
function posicionarFaixa(el: HTMLElement, meio: Ponto, normal: Ponto, largura: number, comprimento: number) {
  const angulo = Math.atan2(normal.y, normal.x)
  el.style.width = `${largura}px`
  el.style.height = `${comprimento}px`
  el.style.transform = `translate(${meio.x}px, ${meio.y}px) rotate(${angulo}rad) translate(0, ${-comprimento / 2}px)`
}

/**
 * Virada de páginas própria (decisão da PoC KAN-58), por dobra de canto: o canto inferior direito
 * levanta, a dobra atravessa a folha na diagonal, o verso do papel aparece e a folha projeta sombra
 * sobre a página de baixo.
 *
 * - Mouse: botões, pontos e arrastar a folha (ela acompanha o ponteiro).
 * - Teclado: setas, Page Up/Down, Home e End com o foco no catálogo.
 * - Toque: arrastar para os lados; deslizar na vertical continua rolando a tela.
 * - Movimento reduzido: troca a página sem animação.
 *
 * A animação escreve estilos direto no DOM a cada quadro, sem renderizar o React de novo.
 */
export function Folheador<P extends PaginaFolheavel>({ paginas, rotulo, renderizar }: Props<P>) {
  const [atual, setAtual] = useState(0)
  const [virada, setVirada] = useState<Virada | null>(null)
  const reduzido = useMovimentoReduzido()
  const total = paginas.length

  const atualRef = useRef(0)
  const viradaRef = useRef<Virada | null>(null)
  const progresso = useRef(0)
  const quadro = useRef<number | null>(null)
  const aoTerminar = useRef<(() => void) | null>(null)
  const pendente = useRef<(() => void) | null>(null)
  const arraste = useRef<Arraste | null>(null)

  const livroRef = useRef<HTMLDivElement>(null)
  const folhas = useRef<(HTMLElement | null)[]>([])
  const sombraRef = useRef<HTMLDivElement>(null)
  const abaRef = useRef<HTMLDivElement>(null)
  const papelRef = useRef<HTMLDivElement>(null)
  const brilhoRef = useRef<HTMLDivElement>(null)

  /** Desenha a dobra no progresso t (0 = folha deitada, 1 = folha virada). */
  const pintar = useCallback((t: number) => {
    progresso.current = t
    const v = viradaRef.current
    const livro = livroRef.current
    const aba = abaRef.current
    const papel = papelRef.current
    const brilho = brilhoRef.current
    const sombra = sombraRef.current
    if (!v || !livro || !aba || !papel || !brilho || !sombra) return
    const largura = livro.clientWidth
    const altura = livro.clientHeight
    if (!largura || !altura) return

    const folha = folhas.current[v.folha]
    const dobra = calcularDobra(t, largura, altura)
    if (folha) folha.style.clipPath = dobra ? poligono(dobra.frente) : ''
    if (!dobra) {
      aba.style.visibility = 'hidden'
      sombra.style.visibility = 'hidden'
      return
    }

    const diagonal = Math.hypot(largura, altura)
    const intensidade = Math.sin(Math.PI * limitar(t))

    aba.style.visibility = 'visible'
    aba.style.filter = `drop-shadow(0 0 ${(4 + 10 * intensidade).toFixed(1)}px rgba(19, 32, 41, ${(0.35 * intensidade).toFixed(3)}))`
    papel.style.width = `${largura}px`
    papel.style.height = `${altura}px`
    papel.style.transform = `matrix(${dobra.matriz.join(', ')})`
    papel.style.clipPath = poligono(dobra.aba)
    posicionarFaixa(brilho, dobra.meio, dobra.normal, diagonal, diagonal * 2)

    sombra.style.visibility = 'visible'
    sombra.style.opacity = intensidade.toFixed(3)
    posicionarFaixa(sombra, dobra.meio, dobra.normal, Math.max(48, largura * 0.35), diagonal * 2)
  }, [])

  const cancelarQuadro = () => {
    if (quadro.current !== null) cancelAnimationFrame(quadro.current)
    quadro.current = null
  }

  const limpar = useCallback(() => {
    const v = viradaRef.current
    if (v) {
      const folha = folhas.current[v.folha]
      if (folha) folha.style.clipPath = ''
    }
    viradaRef.current = null
    setVirada(null)
  }, [])

  /** Encerra na hora qualquer virada em andamento, deixando a página no estado final. */
  const encerrar = useCallback(() => {
    cancelarQuadro()
    pendente.current = null
    const fim = aoTerminar.current
    aoTerminar.current = null
    if (fim) fim()
    else if (viradaRef.current) limpar()
  }, [limpar])

  const animar = useCallback(
    (de: number, para: number, duracao: number, curva: (x: number) => number, fim: () => void) => {
      cancelarQuadro()
      aoTerminar.current = fim
      const concluir = () => {
        const f = aoTerminar.current
        aoTerminar.current = null
        f?.()
      }
      if (duracao <= 0 || typeof requestAnimationFrame !== 'function') {
        pintar(para)
        concluir()
        return
      }
      const inicio = performance.now()
      const passo = (agora: number) => {
        const x = limitar((agora - inicio) / duracao)
        pintar(de + (para - de) * curva(x))
        if (x < 1) quadro.current = requestAnimationFrame(passo)
        else {
          quadro.current = null
          concluir()
        }
      }
      quadro.current = requestAnimationFrame(passo)
    },
    [pintar],
  )

  /** Monta as camadas da virada; o primeiro quadro é pintado antes da tela aparecer. */
  const iniciar = (v: Virada, t: number, depois: (() => void) | null) => {
    viradaRef.current = v
    progresso.current = t
    pendente.current = depois
    setVirada(v)
  }

  useLayoutEffect(() => {
    if (!virada) return
    pintar(progresso.current)
    const depois = pendente.current
    pendente.current = null
    depois?.()
  }, [virada, pintar])

  useEffect(() => () => cancelarQuadro(), [])

  const mudarAtual = (indice: number) => {
    atualRef.current = indice
    setAtual(indice)
  }

  const irPara = (indice: number) => {
    const alvo = Math.max(0, Math.min(total - 1, indice))
    encerrar()
    const de = atualRef.current
    if (alvo === de) return
    mudarAtual(alvo)
    if (reduzido) return
    // Indo para a frente, a folha atual dobra e revela a próxima; voltando, a anterior desdobra por cima.
    const frente = alvo > de
    const v = frente ? { folha: de, base: alvo } : { folha: alvo, base: de }
    const [t0, t1] = frente ? [0, 1] : [1, 0]
    iniciar(v, t0, () => animar(t0, t1, DURACAO_VIRADA_MS, suave, limpar))
  }

  const proxima = () => irPara(atualRef.current + 1)
  const anterior = () => irPara(atualRef.current - 1)

  function aoTeclar(e: KeyboardEvent<HTMLDivElement>) {
    const acoes: Record<string, () => void> = {
      ArrowRight: proxima,
      PageDown: proxima,
      ArrowLeft: anterior,
      PageUp: anterior,
      Home: () => irPara(0),
      End: () => irPara(total - 1),
    }
    const acao = acoes[e.key]
    if (acao) {
      e.preventDefault()
      acao()
    }
  }

  const progressoDoArraste = (sentido: Sentido, dx: number) => {
    const largura = livroRef.current?.clientWidth || 1
    return sentido === 'frente' ? limitar(-dx / largura) : limitar(1 - dx / largura)
  }

  function aoPressionar(e: PointerEvent<HTMLDivElement>) {
    if (e.pointerType === 'mouse' && e.button !== 0) return
    encerrar()
    arraste.current = { x: e.clientX, y: e.clientY, instante: performance.now(), ponteiro: e.pointerId, sentido: null }
  }

  function aoMover(e: PointerEvent<HTMLDivElement>) {
    const a = arraste.current
    if (!a || reduzido) return
    const dx = e.clientX - a.x
    const dy = e.clientY - a.y

    if (!a.sentido) {
      if (Math.abs(dx) < INICIO_ARRASTE || Math.abs(dx) <= Math.abs(dy)) return
      const sentido: Sentido = dx < 0 ? 'frente' : 'tras'
      const de = atualRef.current
      if ((sentido === 'frente' && de >= total - 1) || (sentido === 'tras' && de <= 0)) return
      a.sentido = sentido
      try {
        livroRef.current?.setPointerCapture?.(a.ponteiro)
      } catch {
        /* sem captura, o arraste ainda funciona dentro do catálogo */
      }
      const v = sentido === 'frente' ? { folha: de, base: de + 1 } : { folha: de - 1, base: de }
      iniciar(v, progressoDoArraste(sentido, dx), null)
      return
    }
    pintar(progressoDoArraste(a.sentido, dx))
  }

  function aoSoltar(e: PointerEvent<HTMLDivElement>) {
    const a = arraste.current
    arraste.current = null
    if (!a) return
    const dx = e.clientX - a.x
    const dy = e.clientY - a.y

    if (!a.sentido) {
      // Sem arraste acompanhado (toque rápido ou ambiente sem pointermove): vale o gesto.
      const direcao = direcaoDoGesto(dx, dy)
      if (direcao === 'proxima') proxima()
      if (direcao === 'anterior') anterior()
      return
    }

    const t = progressoDoArraste(a.sentido, dx)
    const frente = a.sentido === 'frente'
    const andou = frente ? t : 1 - t
    const peteleco = performance.now() - a.instante < PETELECO_MS && Math.abs(dx) >= LIMIAR_GESTO
    const confirma = andou >= PONTO_SEM_VOLTA || peteleco
    const para = confirma === frente ? 1 : 0
    if (confirma) mudarAtual(atualRef.current + (frente ? 1 : -1))
    const duracao = Math.max(200, Math.abs(para - t) * DURACAO_VIRADA_MS * 0.8)
    animar(t, para, duracao, desacelerar, limpar)
  }

  function aoInterromper() {
    const a = arraste.current
    arraste.current = null
    if (!a?.sentido) return
    const t = progresso.current
    animar(t, a.sentido === 'frente' ? 0 : 1, 300, desacelerar, limpar)
  }

  const paginaAtual = paginas[atual]

  return (
    <div className="folheador" data-movimento={reduzido ? 'reduzido' : 'normal'}>
      <div className="folheador-palco">
        <button
          type="button"
          className="folheador-seta"
          onClick={anterior}
          disabled={atual === 0}
          aria-label="Página anterior"
        >
          ‹
        </button>

        <div
          ref={livroRef}
          className="folheador-livro"
          role="region"
          aria-roledescription="catálogo"
          aria-label={rotulo}
          tabIndex={0}
          onKeyDown={aoTeclar}
          onPointerDown={aoPressionar}
          onPointerMove={aoMover}
          onPointerUp={aoSoltar}
          onPointerCancel={aoInterromper}
        >
          {paginas.map((pagina, i) => {
            const naVirada = virada !== null && (i === virada.folha || i === virada.base)
            const camada = virada ? (i === virada.folha ? 4 : i === virada.base ? 2 : 1) : i === atual ? 2 : 1
            return (
              <section
                key={pagina.id}
                ref={(el) => {
                  folhas.current[i] = el
                }}
                className={`folha${i < atual ? ' virada' : ''}`}
                style={{ zIndex: camada }}
                aria-hidden={i !== atual}
                inert={i !== atual}
                hidden={Math.abs(i - atual) > 1 && !naVirada}
                data-testid={`folha-${pagina.id}`}
              >
                <div className="folha-face">{renderizar(pagina)}</div>
              </section>
            )
          })}

          {virada && (
            <>
              <div ref={sombraRef} className="dobra-sombra" aria-hidden="true" />
              <div ref={abaRef} className="dobra-aba" aria-hidden="true">
                <div ref={papelRef} className="dobra-papel">
                  <div ref={brilhoRef} className="dobra-brilho" />
                </div>
              </div>
            </>
          )}
        </div>

        <button
          type="button"
          className="folheador-seta"
          onClick={proxima}
          disabled={atual === total - 1}
          aria-label="Próxima página"
        >
          ›
        </button>
      </div>

      <div className="folheador-rodape">
        <span className="folheador-contador">
          {atual + 1} / {total}
        </span>
        <nav className="folheador-pontos" aria-label="Ir para a página">
          {paginas.map((pagina, i) => (
            <button
              key={pagina.id}
              type="button"
              aria-label={`Página ${i + 1}: ${pagina.titulo}`}
              aria-current={i === atual ? 'true' : undefined}
              onClick={() => irPara(i)}
            />
          ))}
        </nav>
      </div>

      <p className="sr-only" aria-live="polite">
        Página {atual + 1} de {total}: {paginaAtual.titulo}
      </p>
    </div>
  )
}
