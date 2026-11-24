package br.com.orbithub.investidores.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import br.com.orbithub.investidores.domain.PapelNaOrganizacao;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.OrganizacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Área do investidor
 * Objetivo: Ponto de entrada das funções de investidor, acessível só a quem tem o papel.
 * Decisões: A regra de papel está na classe inteira (@SomenteInvestidor), então toda rota nova do módulo
 *           nasce protegida. O conteúdo (tese de investimento, KAN-18) chega na S3. A situação diz em nome de quem a
 *           pessoa está atuando (ela mesma ou uma organização, KAN-60).
 */
@RestController
@RequestMapping("/api/v1/investidores")
@SomenteInvestidor
@Tag(name = "Investidores", description = "Funções de quem atua como investidor")
class AreaInvestidorController {

    private final AtuacaoService atuacao;
    private final OrganizacoesService organizacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criação do controller
     * Objetivo: Receber os serviços de atuação e de organizações.
     * Decisões: Injeção por construtor.
     */
    AreaInvestidorController(AtuacaoService atuacao, OrganizacoesService organizacoes) {
        this.atuacao = atuacao;
        this.organizacoes = organizacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Situação do investidor
     * Objetivo: Dizer se a tese está cadastrada, qual o próximo passo e em nome de quem a pessoa está atuando.
     * Decisões: A organização vem do cabeçalho X-Orbit-Organizacao; quem saiu dela recebe 403 e a tela volta para a
     *           atuação pessoal.
     */
    @GetMapping("/eu")
    @Operation(summary = "Situação da conta como investidor (somente investidor)")
    SituacaoInvestidor eu(@RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                          @AuthenticationPrincipal Jwt token) {
        AtuacaoService.Atuacao a = atuacao.paraVer(Autenticado.id(token), organizacao);
        Atuando atuando = !a.emNomeDaOrganizacao() ? null : new Atuando(a.organizacao(),
                organizacoes.detalhe(a.pessoa(), a.organizacao()).organizacao().getNome(), a.papelNaOrganizacao());
        return new SituacaoInvestidor(false, "/investidor/onboarding", atuando);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Situação do investidor
     * Objetivo: Levar se há tese, o próximo passo e a atuação.
     * Decisões: "atuandoPor" nulo = a própria pessoa.
     */
    record SituacaoInvestidor(boolean teseCadastrada, String proximoPasso, Atuando atuandoPor) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Atuação em nome de organização
     * Objetivo: Levar a organização e o papel da pessoa nela.
     * Decisões: Nome incluído para a tela.
     */
    record Atuando(UUID organizacao, String nome, PapelNaOrganizacao papel) {
    }
}
