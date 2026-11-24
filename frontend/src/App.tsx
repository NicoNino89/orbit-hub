import { BrowserRouter, Route, Routes } from 'react-router'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { Inicio } from './Inicio'
import { PaginaTermos } from './termos/PaginaTermos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Rotas da aplicação
 * Objetivo: Associar cada endereço à sua tela.
 * Decisões: Roteador definitivo (React Router) no lugar das rotas por hash da S0, como previsto. O nginx
 *           já devolve o index.html para qualquer rota (SPA), então endereços como /termos/privacidade
 *           funcionam ao recarregar a página. As rotas podem ser montadas sem o BrowserRouter nos testes.
 */
export function Rotas() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/poc-catalogo" element={<PocCatalogo />} />
      <Route path="/termos" element={<PaginaTermos />} />
      <Route path="/termos/:endereco" element={<PaginaTermos />} />
      <Route path="*" element={<Inicio />} />
    </Routes>
  )
}

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Aplicação
 * Objetivo: Montar o roteador do navegador com as rotas.
 * Decisões: BrowserRouter (URLs sem #).
 */
export default function App() {
  return (
    <BrowserRouter>
      <Rotas />
    </BrowserRouter>
  )
}
