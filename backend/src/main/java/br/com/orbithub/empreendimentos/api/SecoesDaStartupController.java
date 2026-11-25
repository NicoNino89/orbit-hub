package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.service.SecoesDaStartupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: API das seções da startup
 * Objetivo: Expor o catálogo de campos, os valores preenchidos e o salvamento de uma seção.
 * Decisões: O catálogo vem do servidor, para a tela não repetir rótulos, tipos e limites.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class SecoesDaStartupController {

    private final SecoesDaStartupService secoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Criação do controller
     * Objetivo: Receber o serviço das seções.
     * Decisões: Injeção por construtor.
     */
    SecoesDaStartupController(SecoesDaStartupService secoes) {
        this.secoes = secoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Catálogo de campos
     * Objetivo: Listar os campos de cada seção, na ordem do formulário.
     * Decisões: Igual para todas as startups.
     */
    @GetMapping("/campos")
    @Operation(summary = "Campos das seções do cadastro da startup")
    List<CampoResponse> catalogo() {
        return Arrays.stream(CampoDaStartup.values()).map(CampoResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Valores das seções
     * Objetivo: Devolver os campos preenchidos.
     * Decisões: Mapa campo → valor; quem não é da equipe recebe 404.
     */
    @GetMapping("/{id}/secoes")
    @Operation(summary = "Valores das seções do cadastro")
    Map<CampoDaStartup, String> ler(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return secoes.ler(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Salvar seção
     * Objetivo: Gravar os campos de uma seção.
     * Decisões: Devolve todos os valores, já normalizados.
     */
    @PutMapping("/{id}/secoes/{secao}")
    @Operation(summary = "Salva uma seção do cadastro")
    Map<CampoDaStartup, String> salvar(@PathVariable UUID id, @PathVariable SecaoDaStartup secao,
                                       @RequestBody Map<String, String> valores, @AuthenticationPrincipal Jwt token) {
        return secoes.salvar(Autenticado.id(token), id, secao, valores);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:26
     * Cartão Jira: KAN-25
     * Título: Campo na resposta
     * Objetivo: Expor um campo do catálogo para a tela montar o formulário.
     * Decisões: Lista só para o tipo LISTA; limite só para texto e inteiro.
     */
    record CampoResponse(CampoDaStartup campo, SecaoDaStartup secao, String rotulo, CampoDaStartup.Tipo tipo, Integer limite,
                         Lista lista, String ajuda) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:26
         * Cartão Jira: KAN-25
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do catálogo.
         * Decisões: Limite de caracteres no texto e máximo no inteiro (KAN-25); nulo nos outros.
         */
        static CampoResponse de(CampoDaStartup c) {
            return new CampoResponse(c, c.getSecao(), c.getRotulo(), c.getTipo(),
                    c.getTipo() == CampoDaStartup.Tipo.TEXTO || c.getTipo() == CampoDaStartup.Tipo.INTEIRO ? c.getLimite() : null,
                    c.getLista(), c.getAjuda());
        }
    }
}
