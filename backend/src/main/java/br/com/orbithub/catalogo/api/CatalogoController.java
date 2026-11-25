package br.com.orbithub.catalogo.api;

import br.com.orbithub.catalogo.service.MarcacoesDoInvestidor;
import br.com.orbithub.catalogo.service.NavegacaoService;
import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38, KAN-39, KAN-41, KAN-42, KAN-43, KAN-44
 * Título: Navegação e listas pessoais do catálogo
 * Objetivo: Expor a seleção personalizada e as novas oportunidades com filtros, e os favoritos, o histórico e os
 *           descartes da pessoa.
 * Decisões: Qualquer usuário cadastrado navega; as listas pessoais são da pessoa logada. Filtros por parâmetros de
 *           consulta repetíveis (segmento=A&segmento=B).
 */
@RestController
@RequestMapping("/api/v1/catalogo")
@Tag(name = "Catálogo", description = "Apresentação das startups e edições temáticas")
class CatalogoController {

    private final NavegacaoService navegacao;
    private final MarcacoesDoInvestidor marcacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-38
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    CatalogoController(NavegacaoService navegacao, MarcacoesDoInvestidor marcacoes) {
        this.navegacao = navegacao;
        this.marcacoes = marcacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-38, KAN-39
     * Título: Navegar
     * Objetivo: Devolver a lista pedida com os filtros combinados.
     * Decisões: Padrão: seleção personalizada.
     */
    @GetMapping("/startups")
    @Operation(summary = "Startups do catálogo (seleção personalizada ou novas)")
    @SuppressWarnings("java:S107")
    NavegacaoService.Resultado navegar(@AuthenticationPrincipal Jwt token,
                                       @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao,
                                       @RequestParam(defaultValue = "PERSONALIZADA") NavegacaoService.ListaDoCatalogo lista,
                                       @RequestParam(required = false) Set<String> segmento,
                                       @RequestParam(required = false) Set<MaturidadeDoNegocio> maturidade,
                                       @RequestParam(required = false) Set<String> localizacao,
                                       @RequestParam(required = false) Boolean comRodada,
                                       @RequestParam(required = false) Set<String> estagio,
                                       @RequestParam(required = false) Set<String> instrumento,
                                       @RequestParam(required = false) BigDecimal captacaoMinima,
                                       @RequestParam(required = false) BigDecimal captacaoMaxima) {
        return navegacao.navegar(Autenticado.id(token), organizacao, lista, new NavegacaoService.Filtros(segmento, maturidade, localizacao,
                comRodada, estagio, instrumento, captacaoMinima, captacaoMaxima));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41, KAN-44
     * Título: Marcações da startup
     * Objetivo: Dizer se a startup está nos favoritos ou descartada.
     * Decisões: Para os botões da apresentação.
     */
    @GetMapping("/startups/{id}/marcacoes")
    @Operation(summary = "Se a startup está nos favoritos ou descartada")
    MarcacoesDoInvestidor.Estado estado(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return marcacoes.estado(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Favoritar
     * Objetivo: Salvar nos favoritos.
     * Decisões: 204.
     */
    @PutMapping("/favoritos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Salva a startup nos favoritos")
    void favoritar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        marcacoes.favoritar(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Desfavoritar
     * Objetivo: Tirar dos favoritos.
     * Decisões: 204.
     */
    @DeleteMapping("/favoritos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Tira a startup dos favoritos")
    void desfavoritar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        marcacoes.desfavoritar(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-44
     * Título: Descartar
     * Objetivo: Descartar da seleção personalizada.
     * Decisões: 204.
     */
    @PutMapping("/descartadas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Descarta a startup da seleção personalizada")
    void descartar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        marcacoes.descartar(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-44
     * Título: Desfazer descarte
     * Objetivo: Devolver à seleção personalizada.
     * Decisões: 204.
     */
    @DeleteMapping("/descartadas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desfaz o descarte da startup")
    void desfazer(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        marcacoes.desfazerDescarte(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-42, KAN-43, KAN-44
     * Título: Listas pessoais
     * Objetivo: Listar favoritos, histórico ou descartadas.
     * Decisões: Mais recentes primeiro.
     */
    @GetMapping("/minhas/{tipo}")
    @Operation(summary = "Favoritos, histórico ou descartadas da pessoa")
    List<MarcacoesDoInvestidor.Marcada> listar(@PathVariable MarcacoesDoInvestidor.Tipo tipo, @AuthenticationPrincipal Jwt token) {
        return marcacoes.listar(Autenticado.id(token), tipo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-43
     * Título: Limpar histórico
     * Objetivo: Apagar as visitas da pessoa.
     * Decisões: 204.
     */
    @DeleteMapping("/minhas/HISTORICO")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Limpa o histórico de startups visualizadas")
    void limparHistorico(@AuthenticationPrincipal Jwt token) {
        marcacoes.limparHistorico(Autenticado.id(token));
    }
}
