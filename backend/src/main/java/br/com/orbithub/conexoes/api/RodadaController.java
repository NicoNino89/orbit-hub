package br.com.orbithub.conexoes.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.conexoes.domain.ResultadoDaRodada;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.domain.SituacaoDaRodada;
import br.com.orbithub.conexoes.domain.TermoDaRodada;
import br.com.orbithub.conexoes.repository.HistoricoDaRodada;
import br.com.orbithub.conexoes.service.RodadasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: API das rodadas
 * Objetivo: Expor o catálogo dos termos por instrumento e a criação, edição, abertura, negociação, encerramento e
 *           histórico da rodada.
 * Decisões: Só empreendedores (a visão do investidor chega com o catálogo, S6, e o Match, S7). Regras no serviço.
 */
@RestController
@RequestMapping("/api/v1")
@SomenteEmpreendedor
@Tag(name = "Rodadas", description = "Rodadas de investimento das startups")
class RodadaController {

    private final RodadasService rodadas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criação do controller
     * Objetivo: Receber o serviço das rodadas.
     * Decisões: Injeção por construtor.
     */
    RodadaController(RodadasService rodadas) {
        this.rodadas = rodadas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termos por instrumento
     * Objetivo: Listar os termos específicos de cada instrumento, para a tela montar o formulário.
     * Decisões: Instrumento nulo = termo genérico, usado pelos instrumentos sem termos próprios.
     */
    @GetMapping("/rodadas/termos")
    @Operation(summary = "Termos específicos de cada instrumento")
    List<TermoResponse> termos() {
        return Arrays.stream(TermoDaRodada.values()).map(t -> new TermoResponse(t, t.getInstrumento(), t.getRotulo(), t.getTipo())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodadas da startup
     * Objetivo: Listar as rodadas da startup.
     * Decisões: Equipe da startup.
     */
    @GetMapping("/startups/{id}/rodadas")
    @Operation(summary = "Rodadas da startup")
    List<RodadaResponse> listar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return rodadas.listar(Autenticado.id(token), id).stream().map(RodadaResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criar rodada
     * Objetivo: Começar uma rodada em rascunho.
     * Decisões: 201.
     */
    @PostMapping("/startups/{id}/rodadas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma rodada em rascunho")
    RodadaResponse criar(@PathVariable UUID id, @RequestBody TermosRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RodadaResponse.de(rodadas.criar(Autenticado.id(token), id, pedido.termos()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Editar rodada
     * Objetivo: Trocar os termos da rodada.
     * Decisões: Rascunho ou aberta.
     */
    @PutMapping("/rodadas/{rodadaId}")
    @Operation(summary = "Edita os termos da rodada")
    RodadaResponse editar(@PathVariable UUID rodadaId, @RequestBody TermosRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RodadaResponse.de(rodadas.editar(Autenticado.id(token), rodadaId, pedido.termos()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Abrir rodada
     * Objetivo: Abrir a rodada para a captação.
     * Decisões: Termos completos.
     */
    @PostMapping("/rodadas/{rodadaId}/abertura")
    @Operation(summary = "Abre a rodada")
    RodadaResponse abrir(@PathVariable UUID rodadaId, @AuthenticationPrincipal Jwt token) {
        return RodadaResponse.de(rodadas.abrir(Autenticado.id(token), rodadaId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Iniciar negociação
     * Objetivo: Marcar a rodada como em negociação.
     * Decisões: Manual até os participantes (S7).
     */
    @PostMapping("/rodadas/{rodadaId}/negociacao")
    @Operation(summary = "Marca a rodada como em negociação")
    RodadaResponse negociar(@PathVariable UUID rodadaId, @AuthenticationPrincipal Jwt token) {
        return RodadaResponse.de(rodadas.iniciarNegociacao(Autenticado.id(token), rodadaId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Encerrar rodada
     * Objetivo: Encerrar a rodada como concluída ou cancelada.
     * Decisões: Motivo opcional.
     */
    @PostMapping("/rodadas/{rodadaId}/encerramento")
    @Operation(summary = "Encerra a rodada")
    RodadaResponse encerrar(@PathVariable UUID rodadaId, @RequestBody EncerramentoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RodadaResponse.de(rodadas.encerrar(Autenticado.id(token), rodadaId, pedido.resultado(), pedido.motivo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Histórico da rodada
     * Objetivo: Listar as mudanças de situação da rodada.
     * Decisões: Equipe da startup.
     */
    @GetMapping("/rodadas/{rodadaId}/historico")
    @Operation(summary = "Histórico da rodada")
    List<HistoricoDaRodada.Mudanca> historico(@PathVariable UUID rodadaId, @AuthenticationPrincipal Jwt token) {
        return rodadas.historico(Autenticado.id(token), rodadaId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Pedido de termos
     * Objetivo: Receber os termos comuns e os específicos.
     * Decisões: Específicos como mapa termo → texto (números com ponto ou vírgula).
     */
    record TermosRequest(String estagio, String instrumento, BigDecimal valorAlvo, BigDecimal valorMinimo, BigDecimal valorMaximo,
                         LocalDate prazo, Map<TermoDaRodada, String> especificos) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:51
         * Cartão Jira: KAN-59
         * Título: Para termos
         * Objetivo: Converter o pedido nos termos do domínio.
         * Decisões: Sem conferência (fica no serviço).
         */
        Rodada.Termos termos() {
            return new Rodada.Termos(estagio, instrumento, valorAlvo, valorMinimo, valorMaximo, prazo, especificos);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Pedido de encerramento
     * Objetivo: Receber o resultado e o motivo.
     * Decisões: Resultado obrigatório (conferido no serviço).
     */
    record EncerramentoRequest(ResultadoDaRodada resultado, String motivo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termo na resposta
     * Objetivo: Expor um termo específico do catálogo.
     * Decisões: Com o instrumento e o tipo.
     */
    record TermoResponse(TermoDaRodada termo, String instrumento, String rotulo, TermoDaRodada.Tipo tipo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Rodada na resposta
     * Objetivo: Expor a rodada com os termos, a situação e as datas.
     * Decisões: "semLider" verdadeiro até os participantes e o líder chegarem (S7).
     */
    record RodadaResponse(UUID id, SituacaoDaRodada situacao, ResultadoDaRodada resultado, String estagio, String instrumento,
                          BigDecimal valorAlvo, BigDecimal valorMinimo, BigDecimal valorMaximo, LocalDate prazo,
                          Map<TermoDaRodada, String> especificos, boolean semLider, Instant abertaEm, Instant encerradaEm,
                          Instant atualizadaEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:51
         * Cartão Jira: KAN-59
         * Título: Conversão
         * Objetivo: Montar a resposta a partir da rodada.
         * Decisões: Termos específicos como texto normalizado.
         */
        static RodadaResponse de(Rodada r) {
            Rodada.Termos t = r.termos();
            return new RodadaResponse(r.getId(), r.getSituacao(), r.getResultado(), t.estagio(), t.instrumento(), t.valorAlvo(),
                    t.valorMinimo(), t.valorMaximo(), t.prazo(), t.especificos(), true, r.getAbertaEm(), r.getEncerradaEm(),
                    r.getAtualizadaEm());
        }
    }
}
