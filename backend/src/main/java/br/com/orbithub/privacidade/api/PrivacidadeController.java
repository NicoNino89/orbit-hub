package br.com.orbithub.privacidade.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.identidade.service.ExclusaoDeConta;
import br.com.orbithub.privacidade.service.IncidentesService;
import br.com.orbithub.privacidade.service.PrivacidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-17, KAN-90, KAN-91
 * Título: Privacidade
 * Objetivo: Expor ao titular os pedidos, a cópia dos dados e a exclusão da conta; ao encarregado, a fila, a resposta e
 *           a exclusão fora do fluxo; à equipe, o registro e o acompanhamento dos incidentes.
 * Decisões: Regras nos serviços. Qualquer pessoa da equipe registra a suspeita de incidente; o resto é do encarregado ou
 *           do administrador.
 */
@RestController
@Tag(name = "Privacidade", description = "Direitos do titular, exclusão de conta e incidentes")
class PrivacidadeController {

    private static final String ENCARREGADO = "hasAnyRole('ADMIN', 'ENCARREGADO_DADOS')";

    private final PrivacidadeService privacidade;
    private final IncidentesService incidentes;
    private final ExclusaoDeConta exclusao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    PrivacidadeController(PrivacidadeService privacidade, IncidentesService incidentes, ExclusaoDeConta exclusao) {
        this.privacidade = privacidade;
        this.incidentes = incidentes;
        this.exclusao = exclusao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Fazer pedido
     * Objetivo: Registrar o pedido do titular.
     * Decisões: Identidade pela senha.
     */
    @PostMapping("/api/v1/privacidade/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Faz um pedido ao encarregado de dados")
    PrivacidadeService.Pedido pedir(@RequestBody PedidoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return privacidade.pedir(Autenticado.id(token), pedido.tipo(), pedido.detalhes(), pedido.senha());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Meus pedidos
     * Objetivo: Listar os pedidos do titular.
     * Decisões: Com a resposta.
     */
    @GetMapping("/api/v1/privacidade/pedidos")
    @Operation(summary = "Lista os meus pedidos de privacidade")
    List<PrivacidadeService.Pedido> meus(@AuthenticationPrincipal Jwt token) {
        return privacidade.meus(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Cópia dos dados
     * Objetivo: Baixar a cópia dos dados pessoais em JSON.
     * Decisões: Sem cache, como anexo.
     */
    @PostMapping("/api/v1/privacidade/meus-dados")
    @Operation(summary = "Baixa a cópia dos meus dados")
    ResponseEntity<Map<String, Object>> meusDados(@RequestBody SenhaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("meus-dados-orbit-hub.json").build().toString())
                .body(privacidade.meusDados(Autenticado.id(token), pedido.senha()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Pendências da exclusão
     * Objetivo: Mostrar o que impede excluir a conta.
     * Decisões: Lista vazia = pode excluir.
     */
    @GetMapping("/api/v1/conta/exclusao")
    @Operation(summary = "Mostra o que impede excluir a conta")
    Map<String, List<String>> pendencias(@AuthenticationPrincipal Jwt token) {
        return Map.of("pendencias", exclusao.pendencias(Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17
     * Título: Excluir conta
     * Objetivo: Excluir a própria conta.
     * Decisões: Senha e a palavra EXCLUIR.
     */
    @PostMapping("/api/v1/conta/exclusao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui a minha conta")
    void excluir(@RequestBody ExclusaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        exclusao.excluirAPedido(Autenticado.id(token), pedido.senha(), pedido.confirmacao());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Fila do encarregado
     * Objetivo: Listar os pedidos em aberto.
     * Decisões: Encarregado ou administrador.
     */
    @GetMapping("/api/v1/admin/privacidade/pedidos")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Fila dos pedidos do titular")
    List<PrivacidadeService.Pedido> fila() {
        return privacidade.fila();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Responder pedido
     * Objetivo: Responder ou recusar o pedido.
     * Decisões: Resposta obrigatória.
     */
    @PostMapping("/api/v1/admin/privacidade/pedidos/{id}/resposta")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Responde ao pedido do titular")
    PrivacidadeService.Pedido responder(@PathVariable UUID id, @RequestBody RespostaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return privacidade.responder(Autenticado.id(token), id, pedido.resposta(), Boolean.TRUE.equals(pedido.recusar()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90, KAN-81
     * Título: Propor exclusão
     * Objetivo: Propor a exclusão da conta fora do fluxo normal.
     * Decisões: Segunda aprovação.
     */
    @PostMapping("/api/v1/admin/privacidade/pedidos/{id}/exclusao")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Propõe a exclusão da conta do titular")
    PrivacidadeService.Pedido proporExclusao(@PathVariable UUID id, @RequestBody JustificativaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return privacidade.proporExclusao(Autenticado.id(token), id, pedido.justificativa());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Registrar incidente
     * Objetivo: Registrar a suspeita de incidente.
     * Decisões: Qualquer pessoa da equipe (rota do backoffice).
     */
    @PostMapping("/api/v1/admin/incidentes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra a suspeita de incidente")
    IncidentesService.Incidente registrar(@RequestBody IncidentesService.NovoIncidente pedido, @AuthenticationPrincipal Jwt token) {
        return incidentes.registrar(Autenticado.id(token), pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Listar incidentes
     * Objetivo: Listar os incidentes.
     * Decisões: Encarregado ou administrador.
     */
    @GetMapping("/api/v1/admin/incidentes")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Lista os incidentes")
    List<IncidentesService.Incidente> incidentes() {
        return incidentes.listar();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Avaliar incidente
     * Objetivo: Registrar contenção, risco e decisões.
     * Decisões: Encarregado ou administrador.
     */
    @PutMapping("/api/v1/admin/incidentes/{id}/avaliacao")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Avalia o incidente")
    IncidentesService.Incidente avaliar(@PathVariable UUID id, @RequestBody IncidentesService.Avaliacao pedido, @AuthenticationPrincipal Jwt token) {
        return incidentes.avaliar(Autenticado.id(token), id, pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Registrar comunicação
     * Objetivo: Marcar a comunicação à ANPD ou aos titulares.
     * Decisões: Encarregado ou administrador.
     */
    @PostMapping("/api/v1/admin/incidentes/{id}/comunicacao")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Registra a comunicação do incidente")
    IncidentesService.Incidente comunicar(@PathVariable UUID id, @RequestBody ComunicacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return incidentes.comunicar(Autenticado.id(token), id, pedido.destino());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-91
     * Título: Encerrar incidente
     * Objetivo: Encerrar com lições e ações preventivas.
     * Decisões: Encarregado ou administrador.
     */
    @PostMapping("/api/v1/admin/incidentes/{id}/encerramento")
    @PreAuthorize(ENCARREGADO)
    @Operation(summary = "Encerra o incidente")
    IncidentesService.Incidente encerrar(@PathVariable UUID id, @RequestBody EncerramentoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return incidentes.encerrar(Autenticado.id(token), id, pedido.licoes(), pedido.acoesPreventivas());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-90, KAN-91
     * Título: Pedidos
     * Objetivo: Tipar os corpos das rotas.
     * Decisões: Boolean em vez de boolean.
     */
    record PedidoRequest(PrivacidadeService.Tipo tipo, String detalhes, String senha) {
    }

    record SenhaRequest(String senha) {
    }

    record ExclusaoRequest(String senha, String confirmacao) {
    }

    record RespostaRequest(String resposta, Boolean recusar) {
    }

    record JustificativaRequest(String justificativa) {
    }

    record ComunicacaoRequest(String destino) {
    }

    record EncerramentoRequest(String licoes, String acoesPreventivas) {
    }
}
