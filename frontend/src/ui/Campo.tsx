import { useId, type InputHTMLAttributes, type ReactNode } from 'react'

interface CampoProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  rotulo: string
  erro?: string
  dica?: ReactNode
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Campo de formulário
 * Objetivo: Exibir rótulo, campo, dica e erro de um input de forma acessível e consistente.
 * Decisões: O rótulo é ligado ao campo por id gerado (useId); a dica e o erro ficam em
 *           aria-describedby, e o erro liga aria-invalid, para o leitor de tela anunciar o problema.
 */
export function Campo({ rotulo, erro, dica, className, ...props }: CampoProps) {
  const id = useId()
  const idDica = `${id}-dica`
  const idErro = `${id}-erro`
  const descricao = [dica ? idDica : null, erro ? idErro : null].filter(Boolean).join(' ') || undefined
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-semibold">{rotulo}</label>
      <input
        id={id}
        aria-invalid={erro ? true : undefined}
        aria-describedby={descricao}
        className={`rounded-md border bg-surface px-3 py-2 outline-none focus:ring-2 focus:ring-primary ${
          erro ? 'border-crit' : 'border-line'
        } ${className ?? ''}`}
        {...props}
      />
      {dica && <p id={idDica} className="text-xs text-muted">{dica}</p>}
      {erro && <p id={idErro} className="text-sm text-crit">{erro}</p>}
    </div>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Mensagem de erro geral
 * Objetivo: Exibir um erro que não pertence a um campo (por exemplo, e-mail já cadastrado).
 * Decisões: role="alert" para ser anunciado ao aparecer.
 */
export function AlertaErro({ children }: { children: ReactNode }) {
  return <div role="alert" className="rounded-md bg-crit-soft px-3 py-2 text-sm text-crit">{children}</div>
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Moldura das telas de conta
 * Objetivo: Centralizar as telas de cadastro, login e senha com a marca no topo.
 * Decisões: Largura estreita, boa para formulário no celular e no computador.
 */
export function MolduraConta({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <main className="mx-auto flex min-h-svh w-full max-w-md flex-col justify-center gap-6 px-4 py-10">
      <a href="/" className="flex items-center gap-2 text-xl font-bold tracking-tight">
        <img src="/favicon.svg" alt="" width={32} height={32} />
        Orbit Hub
      </a>
      <h1 className="font-display text-3xl font-semibold tracking-tight">{titulo}</h1>
      {children}
    </main>
  )
}
