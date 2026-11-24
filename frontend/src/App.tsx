import { BrowserRouter, Route, Routes } from 'react-router'
import { AreaAdmin, InicioAdmin, RotaInterna } from './admin/AreaAdmin'
import { PaginaAuditoria } from './admin/PaginaAuditoria'
import { PaginaEquipe } from './admin/PaginaEquipe'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { PaginaCadastro } from './conta/PaginaCadastro'
import { PaginaConfirmacao } from './conta/PaginaConfirmacao'
import { PaginaConta } from './conta/PaginaConta'
import { PaginaConviteEquipe } from './conta/PaginaConviteEquipe'
import { PaginaEntrar } from './conta/PaginaEntrar'
import { PaginaNovoEmail } from './conta/PaginaNovoEmail'
import { PaginaEsqueciSenha, PaginaRedefinirSenha } from './conta/PaginasSenha'
import { Inicio } from './Inicio'
import { AreaLogada, Painel } from './painel/AreaLogada'
import { AreaEmpreendedor, AreaInvestidor, RotaDePapel } from './painel/AreasDePapel'
import { RotaProtegida } from './sessao/RotaProtegida'
import { SessaoProvider } from './sessao/SessaoProvider'
import { PaginaTermos } from './termos/PaginaTermos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:15
 * Cartão Jira: KAN-75
 * Título: Rotas da aplicação
 * Objetivo: Associar cada endereço à sua tela, separando as públicas das que exigem login.
 * Decisões: Roteador definitivo (React Router) desde o KAN-92; o nginx devolve o index.html para qualquer
 *           rota (SPA). A sessão envolve todas as rotas; as da área logada passam pela RotaProtegida e
 *           pela moldura com o cabeçalho. As rotas podem ser montadas sem o BrowserRouter nos testes. O
 *           backoffice fica em /admin, atrás da RotaInterna, e cada tela confere o papel exigido (KAN-80). O
 *           aceite do convite da equipe é público, protegido pelo token do link (KAN-75).
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
        <Route path="/confirmar-novo-email" element={<PaginaNovoEmail />} />
        <Route path="/equipe/convite" element={<PaginaConviteEquipe />} />
        <Route path="/termos" element={<PaginaTermos />} />
        <Route path="/termos/:endereco" element={<PaginaTermos />} />
        <Route element={<RotaProtegida />}>
          <Route element={<AreaLogada />}>
            <Route path="/painel" element={<Painel />} />
            <Route path="/conta" element={<PaginaConta />} />
            <Route element={<RotaDePapel papel="INVESTIDOR" />}>
              <Route path="/investidor" element={<AreaInvestidor />} />
            </Route>
            <Route element={<RotaDePapel papel="EMPREENDEDOR" />}>
              <Route path="/empreendedor" element={<AreaEmpreendedor />} />
            </Route>
            <Route element={<RotaInterna />}>
              <Route path="/admin" element={<AreaAdmin />}>
                <Route index element={<InicioAdmin />} />
                <Route element={<RotaInterna papeis={['ADMIN']} />}>
                  <Route path="equipe" element={<PaginaEquipe />} />
                </Route>
                <Route element={<RotaInterna papeis={['ADMIN', 'ENCARREGADO_DADOS']} />}>
                  <Route path="auditoria" element={<PaginaAuditoria />} />
                </Route>
              </Route>
            </Route>
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
