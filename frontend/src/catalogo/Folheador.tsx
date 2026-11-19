import { useCallback, useRef, useState, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react'
import { direcaoDoGesto } from './gesto'
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

/**
 * Virada de páginas própria, com CSS 3D (decisão da PoC KAN-58).
 *
 * - Mouse: botões, pontos e arrastar.
 * - Teclado: setas, Page Up/Down, Home e End com o foco no catálogo.
 * - Toque: deslizar para os lados; deslizar na vertical continua rolando a tela.
 * - Movimento reduzido: troca a página sem animação.
 *
 * Só são animadas propriedades que a GPU compõe (transform), e só as folhas vizinhas da atual
 * ficam no DOM visível, para manter a fluidez em celulares de entrada.
 */
export function Folheador<P extends PaginaFolheavel>({ paginas, rotulo, renderizar }: Props<P>) {
  const [atual, setAtual] = useState(0)
  const reduzido = useMovimentoReduzido()
  const inicioGesto = useRef<{ x: number; y: number } | null>(null)
  const total = paginas.length

  const irPara = useCallback((indice: number) => {
    setAtual(Math.max(0, Math.min(total - 1, indice)))
  }, [total])

  const proxima = () => irPara(atual + 1)
  const anterior = () => irPara(atual - 1)

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

  function aoPressionar(e: PointerEvent<HTMLDivElement>) {
    inicioGesto.current = { x: e.clientX, y: e.clientY }
  }

  function aoSoltar(e: PointerEvent<HTMLDivElement>) {
    const inicio = inicioGesto.current
    inicioGesto.current = null
    if (!inicio) return
    const direcao = direcaoDoGesto(e.clientX - inicio.x, e.clientY - inicio.y)
    if (direcao === 'proxima') proxima()
    if (direcao === 'anterior') anterior()
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
          className="folheador-livro"
          role="region"
          aria-roledescription="catálogo"
          aria-label={rotulo}
          tabIndex={0}
          onKeyDown={aoTeclar}
          onPointerDown={aoPressionar}
          onPointerUp={aoSoltar}
          onPointerCancel={() => (inicioGesto.current = null)}
        >
          {paginas.map((pagina, i) => {
            const distancia = Math.abs(i - atual)
            return (
              <section
                key={pagina.id}
                className={`folha${i < atual ? ' virada' : ''}`}
                style={{ zIndex: i < atual ? i : total - i }}
                aria-hidden={i !== atual}
                inert={i !== atual}
                hidden={distancia > 1}
                data-testid={`folha-${pagina.id}`}
              >
                <div className="folha-face">{renderizar(pagina)}</div>
              </section>
            )
          })}
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
