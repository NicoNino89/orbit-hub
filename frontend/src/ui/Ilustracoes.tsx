/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Ilustrações próprias
 * Objetivo: Desenhar em SVG as ilustrações da nova interface (capa padrão por segmento, foguete, alvo, troféu, gráfico
 *           de crescimento e o banner padrão), sem depender de imagens de terceiros.
 * Decisões: Feitas para o projeto (sem questão de licença; inventário em docs/design/imagens.md). Decorativas ficam
 *           ocultas do leitor de tela (aria-hidden).
 */
const PALETAS: [string, string][] = [
  ['#F5A524', '#B45309'], ['#10B981', '#065F46'], ['#3B82F6', '#1E3A8A'], ['#8B5CF6', '#4C1D95'], ['#EF4444', '#7F1D1D'], ['#14B8A6', '#134E4A'],
]

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Paleta do segmento
 * Objetivo: Escolher sempre a mesma cor para o mesmo segmento.
 * Decisões: Soma dos códigos dos caracteres.
 */
function paleta(chave: string) {
  let soma = 0
  for (const c of chave) soma += c.charCodeAt(0)
  return PALETAS[soma % PALETAS.length]
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182, KAN-170
 * Título: Capa padrão
 * Objetivo: Mostrar uma capa com degradê e a inicial quando a startup não enviou capa.
 * Decisões: Cor pelo segmento.
 */
export function CapaPadrao({ segmento, nome }: { segmento: string; nome: string }) {
  const [a, b] = paleta(segmento)
  const id = `g-${segmento.replace(/\W/g, '')}`
  return (
    <svg viewBox="0 0 320 180" preserveAspectRatio="xMidYMid slice" className="h-full w-full" aria-hidden="true">
      <defs><linearGradient id={id} x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor={a} /><stop offset="1" stopColor={b} /></linearGradient></defs>
      <rect width="320" height="180" fill={`url(#${id})`} />
      <circle cx="260" cy="40" r="70" fill="#fff" opacity=".08" />
      <circle cx="40" cy="170" r="90" fill="#fff" opacity=".06" />
      <text x="24" y="110" fill="#fff" opacity=".9" fontSize="64" fontWeight="700" fontFamily="Instrument Sans, sans-serif">{nome.slice(0, 1).toUpperCase()}</text>
    </svg>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Foguete
 * Objetivo: Ilustração do cartão "Comece agora".
 * Decisões: Decorativa.
 */
export function Foguete({ className = 'h-12 w-12' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <path d="M40 8c8 0 16 8 16 16-6 10-14 18-24 24l-8-8C30 30 38 18 40 8Z" fill="#F5A524" />
      <circle cx="42" cy="22" r="5" fill="#fff" />
      <path d="M24 40l-10 4 6-12 8-2M24 40l-4 10 12-6 2-8" fill="#E5631E" />
      <path d="M18 46c-4 2-6 6-6 10 4 0 8-2 10-6" fill="#FDBA4D" />
    </svg>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Alvo
 * Objetivo: Ilustração do cartão "Seu perfil, seu foco".
 * Decisões: Decorativa.
 */
export function Alvo({ className = 'h-12 w-12' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <circle cx="30" cy="34" r="24" fill="#F5A524" /><circle cx="30" cy="34" r="16" fill="#fff" /><circle cx="30" cy="34" r="8" fill="#F5A524" />
      <path d="M30 34 54 10M48 8l6 2 2 6" stroke="#16181D" strokeWidth="4" fill="none" strokeLinecap="round" />
    </svg>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Troféu
 * Objetivo: Ilustração da chamada "Participe!".
 * Decisões: Decorativa.
 */
export function Trofeu({ className = 'h-12 w-12' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden="true">
      <path d="M18 8h28v14a14 14 0 0 1-28 0Z" fill="#F5A524" />
      <path d="M18 12H8c0 10 6 14 12 14M46 12h10c0 10-6 14-12 14" stroke="#F5A524" strokeWidth="4" fill="none" />
      <rect x="28" y="36" width="8" height="10" fill="#E5631E" /><rect x="18" y="46" width="28" height="8" rx="2" fill="#F5A524" />
    </svg>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182
 * Título: Gráfico de crescimento
 * Objetivo: Marca e ilustração de crescimento (barras em degraus).
 * Decisões: Decorativa.
 */
export function Grafico({ className = 'h-8 w-8' }: { className?: string }) {
  return (
    <svg viewBox="0 0 40 40" className={className} aria-hidden="true">
      <rect x="2" y="24" width="8" height="14" rx="1.5" fill="#F5A524" /><rect x="14" y="16" width="8" height="22" rx="1.5" fill="#F5A524" />
      <rect x="26" y="6" width="8" height="32" rx="1.5" fill="#E5631E" />
    </svg>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-182, KAN-171
 * Título: Fundo padrão do banner
 * Objetivo: Desenhar o fundo do banner quando ele não tem imagem.
 * Decisões: Degradê escuro com linhas de "conexão".
 */
export function FundoDoBanner() {
  return (
    <svg viewBox="0 0 800 260" preserveAspectRatio="xMidYMid slice" className="absolute inset-0 h-full w-full" aria-hidden="true">
      <defs><linearGradient id="fundo-banner" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#16181D" /><stop offset="1" stopColor="#3A2911" /></linearGradient></defs>
      <rect width="800" height="260" fill="url(#fundo-banner)" />
      {[[520, 60, 640, 140], [640, 140, 760, 80], [600, 210, 640, 140], [700, 220, 760, 80]].map(([x1, y1, x2, y2]) => (
        <line key={`${x1}-${y1}`} x1={x1} y1={y1} x2={x2} y2={y2} stroke="#F5A524" strokeOpacity=".35" strokeWidth="2" />
      ))}
      {[[520, 60], [640, 140], [760, 80], [600, 210], [700, 220]].map(([x, y]) => <circle key={`${x}-${y}`} cx={x} cy={y} r="7" fill="#F5A524" />)}
    </svg>
  )
}
