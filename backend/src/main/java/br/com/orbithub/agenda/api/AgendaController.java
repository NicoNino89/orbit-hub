package br.com.orbithub.agenda.api;

import br.com.orbithub.agenda.service.AgendaService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.investidores.service.AtuacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
 * Título: Agenda
 * Objetivo: Expor a proposta de reunião, as respostas, a agenda, o convite .ics, a remarcação, o cancelamento, o registro,
 *           a anotação privada e a reunião coletiva da rodada.
 * Decisões: Qualquer usuário cadastrado; o serviço confere se é parte da conexão ou participante da reunião.
 */
@RestController
@Tag(name = "Agenda", description = "Reuniões entre investidores e startups")
class AgendaController {

    private final AgendaService agenda;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    AgendaController(AgendaService agenda) {
        this.agenda = agenda;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-94
     * Título: Propor
     * Objetivo: Propor reunião numa conexão.
     * Decisões: 201.
     */
    @PostMapping("/api/v1/conexoes/{id}/reunioes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Propõe uma reunião na conexão")
    AgendaService.Reuniao propor(@PathVariable UUID id, @RequestBody AgendaService.Proposta pedido, @AuthenticationPrincipal Jwt token,
                                 @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return agenda.propor(Autenticado.id(token), organizacao, id, pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Agenda
     * Objetivo: Listar as minhas reuniões.
     * Decisões: Futuras e passadas.
     */
    @GetMapping("/api/v1/reunioes")
    @Operation(summary = "Minhas reuniões")
    List<AgendaService.Reuniao> minhas(@AuthenticationPrincipal Jwt token) {
        return agenda.minhas(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Aceitar
     * Objetivo: Aceitar um horário.
     * Decisões: Pela ordem da opção.
     */
    @PostMapping("/api/v1/reunioes/{id}/aceite")
    @Operation(summary = "Aceita um horário")
    AgendaService.Reuniao aceitar(@PathVariable UUID id, @RequestBody OrdemRequest pedido, @AuthenticationPrincipal Jwt token) {
        return agenda.aceitar(Autenticado.id(token), id, pedido.ordem());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Contrapropor
     * Objetivo: Propor outros horários.
     * Decisões: Até 3.
     */
    @PostMapping("/api/v1/reunioes/{id}/contraproposta")
    @Operation(summary = "Contrapropõe horários")
    AgendaService.Reuniao contrapropor(@PathVariable UUID id, @RequestBody HorariosRequest pedido, @AuthenticationPrincipal Jwt token) {
        return agenda.contrapropor(Autenticado.id(token), id, pedido.opcoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Recusar
     * Objetivo: Recusar a proposta com motivo.
     * Decisões: Motivo da lista.
     */
    @PostMapping("/api/v1/reunioes/{id}/recusa")
    @Operation(summary = "Recusa a proposta de reunião")
    AgendaService.Reuniao recusar(@PathVariable UUID id, @RequestBody MotivoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return agenda.recusar(Autenticado.id(token), id, pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Cancelar
     * Objetivo: Cancelar a reunião.
     * Decisões: Motivo obrigatório.
     */
    @PostMapping("/api/v1/reunioes/{id}/cancelamento")
    @Operation(summary = "Cancela a reunião")
    AgendaService.Reuniao cancelar(@PathVariable UUID id, @RequestBody MotivoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return agenda.cancelar(Autenticado.id(token), id, pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-96
     * Título: Remarcar
     * Objetivo: Remarcar com novos horários.
     * Decisões: Abre proposta nova.
     */
    @PostMapping("/api/v1/reunioes/{id}/remarcacao")
    @Operation(summary = "Remarca a reunião")
    AgendaService.Reuniao remarcar(@PathVariable UUID id, @RequestBody HorariosRequest pedido, @AuthenticationPrincipal Jwt token,
                                   @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return agenda.remarcar(Autenticado.id(token), organizacao, id, pedido.opcoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-97
     * Título: Registrar
     * Objetivo: Registrar como foi a reunião.
     * Decisões: Por lado.
     */
    @PostMapping("/api/v1/reunioes/{id}/registro")
    @Operation(summary = "Registra o resultado da reunião")
    AgendaService.Reuniao registrar(@PathVariable UUID id, @RequestBody AgendaService.Registro pedido, @AuthenticationPrincipal Jwt token,
                                    @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return agenda.registrar(Autenticado.id(token), organizacao, id, pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-97
     * Título: Anotar
     * Objetivo: Guardar a anotação privada.
     * Decisões: 204.
     */
    @PutMapping("/api/v1/reunioes/{id}/anotacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Guarda a minha anotação da reunião")
    void anotar(@PathVariable UUID id, @RequestBody AnotacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        agenda.anotar(Autenticado.id(token), id, pedido.texto(), Boolean.TRUE.equals(pedido.compartilhar()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-98
     * Título: Convocar reunião da rodada
     * Objetivo: Convocar os participantes da rodada.
     * Decisões: Fundador administrador ou líder.
     */
    @PostMapping("/api/v1/startups/{id}/rodada/reunioes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convoca uma reunião da rodada")
    AgendaService.Reuniao convocar(@PathVariable UUID id, @RequestBody ColetivaRequest pedido, @AuthenticationPrincipal Jwt token,
                                   @RequestHeader(name = AtuacaoService.CABECALHO, required = false) String organizacao) {
        return agenda.convocarDaRodada(Autenticado.id(token), organizacao, id, pedido.inicio(), new AgendaService.Proposta(AgendaService.Tipo.RODADA,
                pedido.formato(), pedido.local(), pedido.pauta(), pedido.duracaoMinutos(), List.of(), List.of()), pedido.participantes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-98
     * Título: Presença
     * Objetivo: Responder a presença na reunião coletiva.
     * Decisões: Confirmado, recusado ou talvez.
     */
    @PostMapping("/api/v1/reunioes/{id}/presenca")
    @Operation(summary = "Responde a presença na reunião coletiva")
    AgendaService.Reuniao presenca(@PathVariable UUID id, @RequestBody PresencaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return agenda.responderPresenca(Autenticado.id(token), id, pedido.resposta());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-95
     * Título: Convite .ics
     * Objetivo: Baixar o convite de calendário.
     * Decisões: text/calendar, como anexo.
     */
    @GetMapping("/api/v1/reunioes/{id}/convite.ics")
    @Operation(summary = "Convite da reunião em .ics")
    ResponseEntity<byte[]> convite(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/calendar; charset=utf-8")).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reuniao.ics").build().toString())
                .body(agenda.convite(Autenticado.id(token), id).getBytes(StandardCharsets.UTF_8));
    }

    record OrdemRequest(int ordem) {
    }

    record HorariosRequest(List<Instant> opcoes) {
    }

    record MotivoRequest(String motivo) {
    }

    record AnotacaoRequest(String texto, Boolean compartilhar) {
    }

    record PresencaRequest(String resposta) {
    }

    record ColetivaRequest(Instant inicio, AgendaService.Formato formato, String local, String pauta, int duracaoMinutos, List<UUID> participantes) {
    }
}
