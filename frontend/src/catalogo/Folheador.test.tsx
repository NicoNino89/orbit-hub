import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { definirMovimentoReduzido } from '../test/setup'
import { Folheador } from './Folheador'

const PAGINAS = [
  { id: 'a', titulo: 'Alfa' },
  { id: 'b', titulo: 'Beta' },
  { id: 'c', titulo: 'Gama' },
  { id: 'd', titulo: 'Delta' },
]

function montar() {
  render(<Folheador paginas={PAGINAS} rotulo="Catálogo de teste" renderizar={(p) => <h3>{p.titulo}</h3>} />)
  return screen.getByRole('region', { name: 'Catálogo de teste' })
}

const anuncio = () => screen.getByText(/^Página \d+ de 4/)

describe('Folheador', () => {
  it('começa na primeira página, com o botão de voltar desligado', () => {
    montar()
    expect(anuncio()).toHaveTextContent('Página 1 de 4: Alfa')
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeEnabled()
  })

  it('avança e volta pelos botões', async () => {
    const usuario = userEvent.setup()
    montar()
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))
    expect(anuncio()).toHaveTextContent('Página 3 de 4: Gama')

    await usuario.click(screen.getByRole('button', { name: 'Página anterior' }))
    expect(anuncio()).toHaveTextContent('Página 2 de 4: Beta')
  })

  it('não passa da última página', async () => {
    const usuario = userEvent.setup()
    montar()
    const proxima = screen.getByRole('button', { name: 'Próxima página' })
    for (let i = 0; i < 3; i++) await usuario.click(proxima)
    expect(anuncio()).toHaveTextContent('Página 4 de 4: Delta')
    expect(proxima).toBeDisabled()
  })

  it('navega pelo teclado: setas, Page Up/Down, Home e End', async () => {
    const usuario = userEvent.setup()
    const livro = montar()
    livro.focus()

    await usuario.keyboard('{ArrowRight}')
    expect(anuncio()).toHaveTextContent('Página 2')
    await usuario.keyboard('{PageDown}')
    expect(anuncio()).toHaveTextContent('Página 3')
    await usuario.keyboard('{ArrowLeft}')
    expect(anuncio()).toHaveTextContent('Página 2')
    await usuario.keyboard('{End}')
    expect(anuncio()).toHaveTextContent('Página 4')
    await usuario.keyboard('{PageUp}')
    expect(anuncio()).toHaveTextContent('Página 3')
    await usuario.keyboard('{Home}')
    expect(anuncio()).toHaveTextContent('Página 1')
  })

  it('vira a página ao deslizar para os lados', () => {
    const livro = montar()
    fireEvent.pointerDown(livro, { clientX: 300, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 150, clientY: 110 })
    expect(anuncio()).toHaveTextContent('Página 2')

    fireEvent.pointerDown(livro, { clientX: 100, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 260, clientY: 100 })
    expect(anuncio()).toHaveTextContent('Página 1')
  })

  it('não vira a página quando o gesto é de rolagem vertical', () => {
    const livro = montar()
    fireEvent.pointerDown(livro, { clientX: 200, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 140, clientY: 300 })
    expect(anuncio()).toHaveTextContent('Página 1')
  })

  describe('arrastando a folha', () => {
    function montarComTamanho() {
      const livro = montar()
      Object.defineProperty(livro, 'clientWidth', { configurable: true, value: 400 })
      Object.defineProperty(livro, 'clientHeight', { configurable: true, value: 300 })
      return livro
    }

    it('vira a página quando a folha passa do ponto sem volta', () => {
      const livro = montarComTamanho()
      fireEvent.pointerDown(livro, { clientX: 380, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 300, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 200, clientY: 245 })
      // durante o arraste a página ainda não mudou
      expect(anuncio()).toHaveTextContent('Página 1')
      fireEvent.pointerUp(livro, { clientX: 200, clientY: 245 })
      expect(anuncio()).toHaveTextContent('Página 2')
    })

    it('mostra o verso da folha enquanto ela dobra', () => {
      const livro = montarComTamanho()
      fireEvent.pointerDown(livro, { clientX: 380, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 250, clientY: 250 })
      expect(livro.querySelector('.dobra-papel')).not.toBeNull()
      expect(screen.getByTestId('folha-a').style.clipPath).toMatch(/^polygon\(/)
    })

    it('devolve a folha ao lugar quando o arraste é curto e lento', () => {
      const livro = montarComTamanho()
      const agora = vi.spyOn(performance, 'now').mockReturnValue(0)
      fireEvent.pointerDown(livro, { clientX: 380, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 340, clientY: 250 })
      agora.mockReturnValue(1000)
      fireEvent.pointerUp(livro, { clientX: 340, clientY: 250 })
      expect(anuncio()).toHaveTextContent('Página 1')
      agora.mockRestore()
    })

    it('volta uma página arrastando para a direita', async () => {
      const usuario = userEvent.setup()
      const livro = montarComTamanho()
      await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))
      fireEvent.pointerDown(livro, { clientX: 20, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 100, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 220, clientY: 250 })
      fireEvent.pointerUp(livro, { clientX: 220, clientY: 250 })
      expect(anuncio()).toHaveTextContent('Página 1')
    })

    it('não segura a folha num arraste vertical', () => {
      const livro = montarComTamanho()
      fireEvent.pointerDown(livro, { clientX: 200, clientY: 50 })
      fireEvent.pointerMove(livro, { clientX: 190, clientY: 200 })
      expect(livro.querySelector('.dobra-papel')).toBeNull()
      fireEvent.pointerUp(livro, { clientX: 190, clientY: 200 })
      expect(anuncio()).toHaveTextContent('Página 1')
    })
  })

  it('pula direto para uma página pelos pontos', async () => {
    const usuario = userEvent.setup()
    montar()
    const ponto = screen.getByRole('button', { name: 'Página 3: Gama' })
    await usuario.click(ponto)
    expect(anuncio()).toHaveTextContent('Página 3 de 4: Gama')
    expect(ponto).toHaveAttribute('aria-current', 'true')
  })

  it('deixa só a página atual acessível e esconde as folhas distantes', async () => {
    const usuario = userEvent.setup()
    montar()
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))

    expect(screen.getByTestId('folha-b')).toHaveAttribute('aria-hidden', 'false')
    expect(screen.getByTestId('folha-a')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.getByTestId('folha-a')).toHaveClass('virada')
    expect(screen.getByTestId('folha-d')).not.toBeVisible()
  })

  it('troca as páginas sem animação quando o sistema pede movimento reduzido', () => {
    definirMovimentoReduzido(true)
    const { container } = render(<Folheador paginas={PAGINAS} rotulo="x" renderizar={(p) => p.titulo} />)
    expect(container.querySelector('.folheador')).toHaveAttribute('data-movimento', 'reduzido')
  })

  it('anima normalmente quando não há preferência de movimento reduzido', () => {
    const { container } = render(<Folheador paginas={PAGINAS} rotulo="x" renderizar={(p) => p.titulo} />)
    expect(container.querySelector('.folheador')).toHaveAttribute('data-movimento', 'normal')
  })
})
