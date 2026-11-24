import { BrowserRouter, Route, Routes } from 'react-router'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { PaginaCadastro } from './conta/PaginaCadastro'
import { PaginaConfirmacao } from './conta/PaginaConfirmacao'
import { PaginaEntrar } from './conta/PaginaEntrar'
import { PaginaEsqueciSenha, PaginaRedefinirSenha } from './conta/PaginasSenha'
import { Inicio } from './Inicio'
import { AreaLogada, Painel } from './painel/AreaLogada'
import { RotaProtegida } from './sessao/RotaProtegida'
import { SessaoProvider } from './sessao/SessaoProvider'
import { PaginaTermos } from './termos/PaginaTermos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Rotas da aplicação
 * Objetivo: Associar cada endereço à sua tela, separando as públicas das que exigem login.
 * Decisões: Roteador definitivo (React Router) desde o KAN-92; o nginx devolve o index.html para qualquer
 *           rota (SPA). A sessão envolve todas as rotas; as da área logada passam pela RotaProtegida e
 *           pela moldura com o cabeçalho. As rotas podem ser montadas sem o BrowserRouter nos testes.
 */
export function Rotas() {
  return (
    <SessaoProvider>
      <Routes>
        <Route path="/" element={<Inicio />} />
        <Route path="/poc-catalogo" element={<PocCatalogo />} />
        <Route path="/cadastro" element={<PaginaCadastro />} />
        <Route path="/confirmar-email" element={<PaginaConfirmacao />} />
        <Route path="/entrar" element={<PaginaEntrar />} />
        <Route path="/esqueci-senha" element={<PaginaEsqueciSenha />} />
        <Route path="/redefinir-senha" element={<PaginaRedefinirSenha />} />
        <Route path="/termos" element={<PaginaTermos />} />
        <Route path="/termos/:endereco" element={<PaginaTermos />} />
        <Route element={<RotaProtegida />}>
          <Route element={<AreaLogada />}>
            <Route path="/painel" element={<Painel />} />
          </Route>
        </Route>
        <Route path="*" element={<Inicio />} />
      </Routes>
    </SessaoProvider>
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
