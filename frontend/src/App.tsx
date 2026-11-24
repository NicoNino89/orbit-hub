import { BrowserRouter, Route, Routes } from 'react-router'
import { AreaAdmin, InicioAdmin, RotaInterna } from './admin/AreaAdmin'
import { PaginaAprovacoes } from './admin/PaginaAprovacoes'
import { PaginaAuditoria } from './admin/PaginaAuditoria'
import { PaginaEquipe } from './admin/PaginaEquipe'
import { PaginaListas } from './admin/PaginaListas'
import { PaginaParametros } from './admin/PaginaParametros'
import { PaginaRotinas } from './admin/PaginaRotinas'
import { PocCatalogo } from './catalogo/PocCatalogo'
import { PaginaCadastro } from './conta/PaginaCadastro'
import { PaginaConfirmacao } from './conta/PaginaConfirmacao'
import { PaginaConta } from './conta/PaginaConta'
import { PaginaConviteEquipe } from './conta/PaginaConviteEquipe'
import { PaginaEntrar } from './conta/PaginaEntrar'
import { PaginaNovoEmail } from './conta/PaginaNovoEmail'
import { PaginaEsqueciSenha, PaginaRedefinirSenha } from './conta/PaginasSenha'
import { Inicio } from './Inicio'
import { PaginaNotificacoes } from './notificacoes/PaginaNotificacoes'
import { AreaLogada, Painel } from './painel/AreaLogada'
import { AreaEmpreendedor, AreaInvestidor, RotaDePapel } from './painel/AreasDePapel'
import { RotaProtegida } from './sessao/RotaProtegida'
import { SessaoProvider } from './sessao/SessaoProvider'
import { PaginaTermos } from './termos/PaginaTermos'

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Rotas da aplicação
 * Objetivo: Associar cada endereço à sua tela, separando as públicas das que exigem login.
 * Decisões: Roteador definitivo (React Router) desde o KAN-92; o nginx devolve o index.html para qualquer
 *           rota (SPA). A sessão envolve todas as rotas; as da área logada passam pela RotaProtegida e
 *           pela moldura com o cabeçalho. As rotas podem ser montadas sem o BrowserRouter nos testes. O
 *           backoffice fica em /admin, atrás da RotaInterna, e cada tela confere o papel exigido (KAN-80). O
 *           aceite do convite da equipe é público, protegido pelo token do link (KAN-75). A fila de segunda
 *           aprovação é de toda a equipe (KAN-81); parâmetros, listas e rotinas são do administrador (KAN-78, KAN-79, KAN-104).
 *           A central de notificações é de qualquer conta logada (KAN-146).
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
            <Route path="/notificacoes" element={<PaginaNotificacoes />} />
            <Route element={<RotaDePapel papel="INVESTIDOR" />}>
              <Route path="/investidor" element={<AreaInvestidor />} />
            </Route>
            <Route element={<RotaDePapel papel="EMPREENDEDOR" />}>
              <Route path="/empreendedor" element={<AreaEmpreendedor />} />
            </Route>
            <Route element={<RotaInterna />}>
              <Route path="/admin" element={<AreaAdmin />}>
                <Route index element={<InicioAdmin />} />
                <Route path="aprovacoes" element={<PaginaAprovacoes />} />
                <Route element={<RotaInterna papeis={['ADMIN']} />}>
                  <Route path="equipe" element={<PaginaEquipe />} />
                  <Route path="parametros" element={<PaginaParametros />} />
                  <Route path="listas" element={<PaginaListas />} />
                  <Route path="rotinas" element={<PaginaRotinas />} />
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
