/**
 * Geometria da virada de página por dobra de canto (KAN-58).
 *
 * A folha tem a lombada na borda esquerda. O canto inferior direito C é levado até um ponto P;
 * a linha da dobra é a mediatriz de C–P. A parte da folha do lado de C dobra por cima do resto:
 * some da frente e reaparece como verso, refletida sobre a linha da dobra.
 *
 * O progresso t vai de 0 (folha deitada) a 1 (folha virada por inteiro, fora do quadro).
 */

export interface Ponto {
  x: number
  y: number
}

export interface Dobra {
  /** Parte da folha que continua no lugar, em px da folha. */
  frente: Ponto[]
  /** Parte que dobrou, em px da folha, antes do reflexo. */
  aba: Ponto[]
  /** Reflexo sobre a linha da dobra, no formato do CSS matrix(a, b, c, d, e, f). */
  matriz: [number, number, number, number, number, number]
  /** Ponto médio da linha da dobra. */
  meio: Ponto
  /** Normal unitária da linha da dobra, apontando para o lado que dobrou. */
  normal: Ponto
}

/** Quanto o canto sobe no meio da virada, em fração da altura. Dá a dobra diagonal de revista. */
const ELEVACAO = 0.35

/** Posição do canto inferior direito no instante t. */
export function cantoNoInstante(t: number, largura: number, altura: number): Ponto {
  const x = largura - 2 * largura * t
  const y = altura - altura * ELEVACAO * Math.sin(Math.PI * t)
  // A folha não se solta da lombada: o canto fica no máximo a uma largura do pé da lombada (0, altura).
  const dy = y - altura
  const distancia = Math.hypot(x, dy)
  if (distancia <= largura) return { x, y }
  return { x: (x * largura) / distancia, y: altura + (dy * largura) / distancia }
}

/** Recorte de polígono convexo por semiplano (Sutherland–Hodgman); mantém onde lado(p) ≥ 0. */
function recortar(poligono: Ponto[], lado: (p: Ponto) => number): Ponto[] {
  const saida: Ponto[] = []
  for (let i = 0; i < poligono.length; i++) {
    const a = poligono[i]
    const b = poligono[(i + 1) % poligono.length]
    const la = lado(a)
    const lb = lado(b)
    if (la >= 0) saida.push(a)
    if (la >= 0 !== lb >= 0) {
      const s = la / (la - lb)
      saida.push({ x: a.x + (b.x - a.x) * s, y: a.y + (b.y - a.y) * s })
    }
  }
  return saida
}

/** Calcula a dobra no instante t. Devolve null quando não há dobra (t ≤ 0 ou folha sem tamanho). */
export function calcularDobra(t: number, largura: number, altura: number): Dobra | null {
  if (t <= 0 || largura <= 0 || altura <= 0) return null
  const canto = { x: largura, y: altura }
  const p = cantoNoInstante(Math.min(t, 1), largura, altura)
  const vx = canto.x - p.x
  const vy = canto.y - p.y
  const comprimento = Math.hypot(vx, vy)
  if (comprimento < 1e-6) return null

  const normal = { x: vx / comprimento, y: vy / comprimento }
  const meio = { x: (canto.x + p.x) / 2, y: (canto.y + p.y) / 2 }
  const lado = (q: Ponto) => (q.x - meio.x) * normal.x + (q.y - meio.y) * normal.y
  const folha = [
    { x: 0, y: 0 },
    { x: largura, y: 0 },
    { x: largura, y: altura },
    { x: 0, y: altura },
  ]

  const k = meio.x * normal.x + meio.y * normal.y
  const nxy = -2 * normal.x * normal.y
  return {
    frente: recortar(folha, (q) => -lado(q)),
    aba: recortar(folha, lado),
    matriz: [1 - 2 * normal.x ** 2, nxy, nxy, 1 - 2 * normal.y ** 2, 2 * k * normal.x, 2 * k * normal.y],
    meio,
    normal,
  }
}

/** Aplica a matriz da dobra a um ponto (útil nos testes e para conferir o reflexo). */
export function refletir(matriz: Dobra['matriz'], q: Ponto): Ponto {
  const [a, b, c, d, e, f] = matriz
  return { x: a * q.x + c * q.y + e, y: b * q.x + d * q.y + f }
}
