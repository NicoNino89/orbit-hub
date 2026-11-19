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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Curva suave (entrada e saída)
 * Objetivo: Acelerar no início e frear no fim da virada automática, como uma mão virando a folha.
 * Decisões: Cúbica ease-in-out; a linear da primeira versão parecia mecânica.
 */
const suave = (x: number) => (x < 0.5 ? 4 * x ** 3 : 1 - (-2 * x + 2) ** 3 / 2)

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Curva de desaceleração
 * Objetivo: Concluir ou desfazer a virada depois que o usuário solta a folha.
 * Decisões: Cúbica ease-out: a folha já está em movimento, então só freia.
 */
const desacelerar = (x: number) => 1 - (1 - x) ** 3

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Limitar entre 0 e 1
 * Objetivo: Manter o progresso da virada dentro do intervalo válido.
 * Decisões: Função pura e pequena, reaproveitada no arraste e na animação.
 */
const limitar = (x: number) => Math.max(0, Math.min(1, x))

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Polígono para clip-path
 * Objetivo: Converter uma lista de pontos no valor CSS polygon(...) em pixels.
 * Decisões: Com menos de 3 pontos devolve um polígono vazio válido, para esconder a folha inteira
 *           sem trocar de propriedade CSS.
 */
function poligono(pontos: Ponto[]) {
  if (pontos.length < 3) return 'polygon(0 0, 0 0, 0 0)'
  return `polygon(${pontos.map((p) => `${p.x}px ${p.y}px`).join(', ')})`
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Posicionar faixa na linha da dobra
 * Objetivo: Encostar uma faixa (sombra ou brilho) na linha da dobra, do lado que dobrou.
 * Decisões: Só transform, largura e altura; o degradê fica no CSS, então o navegador não refaz o estilo.
 */
function posicionarFaixa(el: HTMLElement, meio: Ponto, normal: Ponto, largura: number, comprimento: number) {
  const angulo = Math.atan2(normal.y, normal.x)
  el.style.width = `${largura}px`
  el.style.height = `${comprimento}px`
  el.style.transform = `translate(${meio.x}px, ${meio.y}px) rotate(${angulo}rad) translate(0, ${-comprimento / 2}px)`
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 22:21
 * Cartão Jira: KAN-58
 * Título: Folheador do catálogo (virada por dobra de canto)
 * Objetivo: Exibir as páginas do catálogo como uma revista: o canto inferior direito levanta, a dobra
 *           atravessa a folha na diagonal, o verso do papel aparece e a folha projeta sombra na página
 *           de baixo.
 * Decisões:
 *   - Implementação própria; react-pageflip reprovada na PoC (sem manutenção, sem suporte a React 19).
 *   - Mouse: botões, pontos e arrastar a folha (ela acompanha o ponteiro).
 *   - Teclado: setas, Page Up/Down, Home e End com o foco no catálogo.
 *   - Toque: arrastar para os lados; deslizar na vertical continua rolando a tela.
 *   - Movimento reduzido: troca a página sem animação.
 *   - A animação escreve estilos direto no DOM a cada quadro, sem renderizar o React de novo.
 *   - Primeira versão (rotateY, 750 ms) reprovada no teste humano de 18/11/2026.
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Pintar a dobra
   * Objetivo: Desenhar a dobra no progresso t (0 = folha deitada, 1 = folha virada): recorte da folha,
   *           verso refletido, brilho e sombra.
   * Decisões: Lê o tamanho do livro a cada quadro (acompanha giro de tela); sem tamanho (jsdom) não
   *           pinta; a intensidade das sombras segue sin(πt), para nascer e sumir sem salto.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Cancelar o próximo quadro
   * Objetivo: Interromper o laço de animação em andamento.
   * Decisões: Usado antes de toda nova animação e ao desmontar, para nunca haver dois laços pintando.
   */
  const cancelarQuadro = () => {
    if (quadro.current !== null) cancelAnimationFrame(quadro.current)
    quadro.current = null
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:25
   * Cartão Jira: KAN-58
   * Título: Limpar a virada
   * Objetivo: Desmontar as camadas da virada quando ela termina.
   * Decisões: O recorte da folha não é tirado aqui, só no efeito de layout, no mesmo commit em que a
   *           folha desce de camada. Tirar antes deixava a tela pintar um quadro com a página antiga
   *           inteira por cima da nova (a piscada no fim da virada).
   */
  const limpar = useCallback(() => {
    viradaRef.current = null
    setVirada(null)
  }, [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Encerrar a virada em andamento
   * Objetivo: Terminar na hora qualquer virada, deixando a página no estado final.
   * Decisões: Cliques rápidos não enfileiram animações: a virada anterior é concluída e a nova começa.
   */
  const encerrar = useCallback(() => {
    cancelarQuadro()
    pendente.current = null
    const fim = aoTerminar.current
    aoTerminar.current = null
    if (fim) fim()
    else if (viradaRef.current) limpar()
  }, [limpar])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Animar a virada
   * Objetivo: Levar o progresso de um valor a outro no tempo dado, pintando a cada quadro.
   * Decisões: requestAnimationFrame com tempo real (não contagem de quadros), para a duração ser a
   *           mesma em telas de 60 Hz e 120 Hz; sem rAF ou sem duração, pinta direto o estado final.
   */
  const animar = useCallback(
    (de: number, para: number, duracao: number, curva: (x: number) => number, fim: () => void) => {
      cancelarQuadro()
      aoTerminar.current = fim
      /**
       * Autor: Nicholas Bruno Pimentel
       * Data: 18/11/2026
       * Hora: 22:21
       * Cartão Jira: KAN-58
       * Título: Concluir a animação
       * Objetivo: Rodar uma única vez a ação de término registrada.
       * Decisões: Zera a referência antes de chamar, para um encerramento concorrente não repetir a ação.
       */
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
      /**
       * Autor: Nicholas Bruno Pimentel
       * Data: 18/11/2026
       * Hora: 22:21
       * Cartão Jira: KAN-58
       * Título: Passo da animação
       * Objetivo: Calcular o progresso do quadro atual, pintar e agendar o próximo.
       * Decisões: O último quadro pinta exatamente o valor final antes de concluir.
       */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Iniciar a virada
   * Objetivo: Montar as camadas da virada e guardar o progresso inicial e a animação pendente.
   * Decisões: O primeiro quadro é pintado no efeito de layout, antes da tela aparecer; ao voltar uma
   *           página, isso evita um quadro com a folha anterior inteira à vista.
   */
  const iniciar = (v: Virada, t: number, depois: (() => void) | null) => {
    viradaRef.current = v
    progresso.current = t
    pendente.current = depois
    setVirada(v)
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:25
   * Cartão Jira: KAN-58
   * Título: Sincronizar camadas e recortes
   * Objetivo: Tirar recortes antigos, pintar o primeiro quadro e disparar a animação pendente.
   * Decisões: Efeito de layout: roda depois que o React trocou as camadas e antes de a tela ser
   *           pintada, o que elimina a piscada.
   */
  useLayoutEffect(() => {
    folhas.current.forEach((folha, i) => {
      if (folha && i !== virada?.folha && folha.style.clipPath) folha.style.clipPath = ''
    })
    if (!virada) return
    pintar(progresso.current)
    const depois = pendente.current
    pendente.current = null
    depois?.()
  }, [virada, pintar])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Parar a animação ao desmontar
   * Objetivo: Cancelar o laço de animação quando o catálogo sai da tela.
   * Decisões: Evita escrever estilos em elementos que já não existem.
   */
  useEffect(() => () => cancelarQuadro(), [])

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Mudar a página atual
   * Objetivo: Atualizar a página atual no estado e na referência.
   * Decisões: A referência permite ler o valor novo no mesmo evento, antes do próximo render.
   */
  const mudarAtual = (indice: number) => {
    atualRef.current = indice
    setAtual(indice)
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Ir para uma página
   * Objetivo: Trocar de página com a virada animada.
   * Decisões: A página atual (e o anúncio para leitor de tela) muda na hora; a animação é só visual.
   *           Indo para a frente, a folha atual dobra e revela a próxima; voltando, a anterior
   *           desdobra por cima.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Próxima página
   * Objetivo: Avançar uma página.
   * Decisões: Lê a referência, não o estado, para cliques seguidos não perderem passos.
   */
  const proxima = () => irPara(atualRef.current + 1)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Página anterior
   * Objetivo: Voltar uma página.
   * Decisões: Mesma leitura pela referência da próxima página.
   */
  const anterior = () => irPara(atualRef.current - 1)

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Navegação pelo teclado
   * Objetivo: Atender ← →, Page Up/Down, Home e End com o foco no catálogo.
   * Decisões: Tabela de teclas em vez de switch; só as teclas tratadas têm o comportamento padrão
   *           bloqueado, para Tab e o leitor de tela continuarem funcionando.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Progresso a partir do arraste
   * Objetivo: Converter o deslocamento horizontal do ponteiro no progresso da virada.
   * Decisões: Arrastar uma largura inteira vira a folha toda; com isso o canto anda o dobro do dedo,
   *           e um gesto curto no celular já basta.
   */
  const progressoDoArraste = (sentido: Sentido, dx: number) => {
    const largura = livroRef.current?.clientWidth || 1
    return sentido === 'frente' ? limitar(-dx / largura) : limitar(1 - dx / largura)
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Início do arraste
   * Objetivo: Registrar onde e quando o ponteiro tocou o catálogo.
   * Decisões: Só o botão principal do mouse; encerra a virada em andamento para a folha obedecer ao dedo.
   */
  function aoPressionar(e: PointerEvent<HTMLDivElement>) {
    if (e.pointerType === 'mouse' && e.button !== 0) return
    encerrar()
    arraste.current = { x: e.clientX, y: e.clientY, instante: performance.now(), ponteiro: e.pointerId, sentido: null }
  }

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Arraste da folha
   * Objetivo: Segurar a folha quando o gesto vira horizontal e fazê-la acompanhar o ponteiro.
   * Decisões: Só segura depois de 8 px e se o gesto for mais horizontal que vertical, para não brigar
   *           com a rolagem da tela; captura o ponteiro para o arraste continuar fora do catálogo.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Fim do arraste
   * Objetivo: Decidir se a folha completa a virada ou volta ao lugar, e animar o restante.
   * Decisões: Vira se passou de 30% do caminho ou num peteleco (menos de 300 ms e 48 px); sem arraste
   *           acompanhado, vale o gesto simples. A duração do restante é proporcional ao que falta.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Arraste interrompido
   * Objetivo: Devolver a folha ao lugar quando o sistema cancela o ponteiro.
   * Decisões: Cancelamento (ex.: o navegador assumiu a rolagem) nunca vira a página.
   */
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
