package br.com.orbithub.investidores.api;

import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteInvestidor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Área do investidor
 * Objetivo: Ponto de entrada das funções de investidor, acessível só a quem tem o papel.
 * Decisões: A regra de papel está na classe inteira (@SomenteInvestidor), então toda rota nova do módulo
 *           nasce protegida. O conteúdo (tese de investimento, KAN-18) chega na S3.
 */
@RestController
@RequestMapping("/api/v1/investidores")
@SomenteInvestidor
@Tag(name = "Investidores", description = "Funções de quem atua como investidor")
class AreaInvestidorController {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Situação do investidor
     * Objetivo: Informar se a tese já foi cadastrada e qual é o próximo passo.
     * Decisões: Até a S3 não há tese; o próximo passo é sempre o onboarding.
     */
    @GetMapping("/eu")
    @Operation(summary = "Situação da conta como investidor (somente investidor)")
    SituacaoInvestidor eu() {
        return new SituacaoInvestidor(false, "/investidor/onboarding");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Situação do investidor na API
     * Objetivo: Se a tese existe e o próximo passo.
     * Decisões: Cresce com o KAN-18.
     */
    record SituacaoInvestidor(boolean teseCadastrada, String proximoPasso) {
    }
}
