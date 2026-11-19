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

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Montar o folheador de teste
 * Objetivo: Renderizar o folheador com quatro páginas e devolver a região do catálogo.
 * Decisões: Quatro páginas bastam para testar começo, meio, fim e folhas distantes.
 */
function montar() {
  render(<Folheador paginas={PAGINAS} rotulo="Catálogo de teste" renderizar={(p) => <h3>{p.titulo}</h3>} />)
  return screen.getByRole('region', { name: 'Catálogo de teste' })
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Anúncio da página atual
 * Objetivo: Localizar o texto lido pelo leitor de tela ("Página n de 4").
 * Decisões: Os testes verificam a página pelo anúncio, o que também cobre a acessibilidade.
 */
const anuncio = () => screen.getByText(/^Página \d+ de 4/)

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 21:02
 * Cartão Jira: KAN-58
 * Título: Testes do folheador
 * Objetivo: Verificar navegação, gestos, acessibilidade e movimento reduzido.
 * Decisões: Testes pelo comportamento visível, não pela implementação da animação.
 */
describe('Folheador', () => {
  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Estado inicial
   * Objetivo: Garantir que o catálogo abra na página 1 com o botão de voltar desligado.
   * Decisões: Botão desligado em vez de escondido, para o layout não pular.
   */
  it('começa na primeira página, com o botão de voltar desligado', () => {
    montar()
    expect(anuncio()).toHaveTextContent('Página 1 de 4: Alfa')
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeEnabled()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Botões de navegação
   * Objetivo: Garantir que os botões avancem e voltem uma página.
   * Decisões: Cliques reais do user-event, com foco e eventos de ponteiro.
   */
  it('avança e volta pelos botões', async () => {
    const usuario = userEvent.setup()
    montar()
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))
    expect(anuncio()).toHaveTextContent('Página 3 de 4: Gama')

    await usuario.click(screen.getByRole('button', { name: 'Página anterior' }))
    expect(anuncio()).toHaveTextContent('Página 2 de 4: Beta')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Limite da última página
   * Objetivo: Garantir que não se avance além da última página.
   * Decisões: Confere também que o botão de avançar fica desligado.
   */
  it('não passa da última página', async () => {
    const usuario = userEvent.setup()
    montar()
    const proxima = screen.getByRole('button', { name: 'Próxima página' })
    for (let i = 0; i < 3; i++) await usuario.click(proxima)
    expect(anuncio()).toHaveTextContent('Página 4 de 4: Delta')
    expect(proxima).toBeDisabled()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Navegação pelo teclado
   * Objetivo: Garantir setas, Page Up/Down, Home e End com o foco no catálogo.
   * Decisões: Sequência única que passa por todas as teclas.
   */
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

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Gesto de deslizar
   * Objetivo: Garantir que um deslize horizontal vire para a frente e para trás.
   * Decisões: Só pressionar e soltar, sem movimento intermediário: cobre o caminho do gesto simples.
   */
  it('vira a página ao deslizar para os lados', () => {
    const livro = montar()
    fireEvent.pointerDown(livro, { clientX: 300, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 150, clientY: 110 })
    expect(anuncio()).toHaveTextContent('Página 2')

    fireEvent.pointerDown(livro, { clientX: 100, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 260, clientY: 100 })
    expect(anuncio()).toHaveTextContent('Página 1')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Rolagem vertical preservada
   * Objetivo: Garantir que um gesto mais vertical que horizontal não vire a página.
   * Decisões: Protege a rolagem da tela no celular.
   */
  it('não vira a página quando o gesto é de rolagem vertical', () => {
    const livro = montar()
    fireEvent.pointerDown(livro, { clientX: 200, clientY: 100 })
    fireEvent.pointerUp(livro, { clientX: 140, clientY: 300 })
    expect(anuncio()).toHaveTextContent('Página 1')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 22:21
   * Cartão Jira: KAN-58
   * Título: Testes do arraste da folha
   * Objetivo: Verificar a folha que acompanha o ponteiro.
   * Decisões: O livro recebe tamanho fixo (400 x 300), que o jsdom não calcula.
   */
  describe('arrastando a folha', () => {
    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Montar com tamanho
     * Objetivo: Montar o folheador e dar tamanho ao livro.
     * Decisões: Sem tamanho, a dobra não é calculada e o arraste não teria o que verificar.
     */
    function montarComTamanho() {
      const livro = montar()
      Object.defineProperty(livro, 'clientWidth', { configurable: true, value: 400 })
      Object.defineProperty(livro, 'clientHeight', { configurable: true, value: 300 })
      return livro
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Ponto sem volta
     * Objetivo: Garantir que soltar depois de 30% do caminho vire a página, e só ao soltar.
     * Decisões: Confere que a página não muda durante o arraste.
     */
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Verso durante a dobra
     * Objetivo: Garantir que o verso apareça e a folha seja recortada durante o arraste.
     * Decisões: Verifica a presença das camadas, não os números da geometria (cobertos em dobra.test.ts).
     */
    it('mostra o verso da folha enquanto ela dobra', () => {
      const livro = montarComTamanho()
      fireEvent.pointerDown(livro, { clientX: 380, clientY: 250 })
      fireEvent.pointerMove(livro, { clientX: 250, clientY: 250 })
      expect(livro.querySelector('.dobra-papel')).not.toBeNull()
      expect(screen.getByTestId('folha-a').style.clipPath).toMatch(/^polygon\(/)
    })

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Folha volta ao lugar
     * Objetivo: Garantir que um arraste curto e lento não vire a página.
     * Decisões: Relógio simulado, para o teste não depender da velocidade da máquina.
     */
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Voltar arrastando
     * Objetivo: Garantir que arrastar para a direita volte uma página.
     * Decisões: Avança antes pelo botão, para haver página anterior.
     */
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 22:21
     * Cartão Jira: KAN-58
     * Título: Arraste vertical não segura a folha
     * Objetivo: Garantir que um movimento vertical não crie as camadas da dobra.
     * Decisões: Complementa o teste de rolagem do gesto simples.
     */
    it('não segura a folha num arraste vertical', () => {
      const livro = montarComTamanho()
      fireEvent.pointerDown(livro, { clientX: 200, clientY: 50 })
      fireEvent.pointerMove(livro, { clientX: 190, clientY: 200 })
      expect(livro.querySelector('.dobra-papel')).toBeNull()
      fireEvent.pointerUp(livro, { clientX: 190, clientY: 200 })
      expect(anuncio()).toHaveTextContent('Página 1')
    })
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Navegação pelos pontos
   * Objetivo: Garantir que o ponto de uma página leve direto a ela.
   * Decisões: Confere também o aria-current do ponto ativo.
   */
  it('pula direto para uma página pelos pontos', async () => {
    const usuario = userEvent.setup()
    montar()
    const ponto = screen.getByRole('button', { name: 'Página 3: Gama' })
    await usuario.click(ponto)
    expect(anuncio()).toHaveTextContent('Página 3 de 4: Gama')
    expect(ponto).toHaveAttribute('aria-current', 'true')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Só a página atual acessível
   * Objetivo: Garantir que só a página atual seja lida e que as distantes fiquem ocultas.
   * Decisões: Folhas fora da tela com aria-hidden e inert; as distantes com hidden, fora da renderização.
   */
  it('deixa só a página atual acessível e esconde as folhas distantes', async () => {
    const usuario = userEvent.setup()
    montar()
    await usuario.click(screen.getByRole('button', { name: 'Próxima página' }))

    expect(screen.getByTestId('folha-b')).toHaveAttribute('aria-hidden', 'false')
    expect(screen.getByTestId('folha-a')).toHaveAttribute('aria-hidden', 'true')
    expect(screen.getByTestId('folha-a')).toHaveClass('virada')
    expect(screen.getByTestId('folha-d')).not.toBeVisible()
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Movimento reduzido ligado
   * Objetivo: Garantir o modo sem animação quando o sistema pede movimento reduzido.
   * Decisões: Usa o matchMedia simulado da preparação dos testes.
   */
  it('troca as páginas sem animação quando o sistema pede movimento reduzido', () => {
    definirMovimentoReduzido(true)
    const { container } = render(<Folheador paginas={PAGINAS} rotulo="x" renderizar={(p) => p.titulo} />)
    expect(container.querySelector('.folheador')).toHaveAttribute('data-movimento', 'reduzido')
  })

  /**
   * Autor: Nicholas Bruno Pimentel
   * Data: 18/11/2026
   * Hora: 21:02
   * Cartão Jira: KAN-58
   * Título: Movimento reduzido desligado
   * Objetivo: Garantir o modo animado sem a preferência.
   * Decisões: Par do teste anterior, para o atributo não ficar fixo num valor.
   */
  it('anima normalmente quando não há preferência de movimento reduzido', () => {
    const { container } = render(<Folheador paginas={PAGINAS} rotulo="x" renderizar={(p) => p.titulo} />)
    expect(container.querySelector('.folheador')).toHaveAttribute('data-movimento', 'normal')
  })
})
