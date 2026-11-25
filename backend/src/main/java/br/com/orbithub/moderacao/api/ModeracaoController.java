package br.com.orbithub.moderacao.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.moderacao.service.ModeracaoService;
import br.com.orbithub.moderacao.service.RecursosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-86, KAN-99, KAN-100, KAN-101, KAN-102
 * Título: Moderação e recursos
 * Objetivo: Expor a quem usa a plataforma a denúncia, as sanções recebidas e os recursos; ao moderador, a fila, o caso,
 *           as medidas preventivas, o levantamento de acessos e a decisão; ao jurídico, o parecer; aos times, a fila e o
 *           julgamento dos recursos.
 * Decisões: Regras nos serviços. Rotas da equipe em /api/v1/admin, com o papel conferido aqui.
 */
@RestController
@Tag(name = "Moderação", description = "Denúncias, sanções e recursos")
class ModeracaoController {

    private static final String MODERADOR = "hasAnyRole('ADMIN', 'MODERADOR')";

    private final ModeracaoService moderacao;
    private final RecursosService recursos;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    ModeracaoController(ModeracaoService moderacao, RecursosService recursos) {
        this.moderacao = moderacao;
        this.recursos = recursos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99
     * Título: Denunciar
     * Objetivo: Receber a denúncia e devolver o protocolo.
     * Decisões: Qualquer pessoa com conta.
     */
    @PostMapping("/api/v1/denuncias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Faz uma denúncia")
    ModeracaoService.Recebida denunciar(@RequestBody ModeracaoService.NovaDenuncia pedido, @AuthenticationPrincipal Jwt token) {
        return moderacao.denunciar(Autenticado.id(token), pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-99
     * Título: Minhas denúncias
     * Objetivo: Listar as denúncias da pessoa.
     * Decisões: Só se foram analisadas.
     */
    @GetMapping("/api/v1/denuncias")
    @Operation(summary = "Lista as minhas denúncias")
    List<ModeracaoService.MinhaDenuncia> minhasDenuncias(@AuthenticationPrincipal Jwt token) {
        return moderacao.minhas(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Minhas sanções
     * Objetivo: Listar as sanções da pessoa e das startups que ela administra.
     * Decisões: Com o prazo para recorrer.
     */
    @GetMapping("/api/v1/sancoes")
    @Operation(summary = "Lista as sanções que recebi")
    List<ModeracaoService.MinhaSancao> minhasSancoes(@AuthenticationPrincipal Jwt token) {
        return moderacao.minhasSancoes(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Recorrer
     * Objetivo: Apresentar o recurso contra uma decisão.
     * Decisões: Uma vez por decisão.
     */
    @PostMapping("/api/v1/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Recorre de uma decisão")
    RecursosService.Recurso recorrer(@RequestBody RecursosService.NovoRecurso pedido, @AuthenticationPrincipal Jwt token) {
        return recursos.recorrer(Autenticado.id(token), pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Meus recursos
     * Objetivo: Listar os recursos da pessoa.
     * Decisões: Com o resultado e o motivo.
     */
    @GetMapping("/api/v1/recursos")
    @Operation(summary = "Lista os meus recursos")
    List<RecursosService.Recurso> meusRecursos(@AuthenticationPrincipal Jwt token) {
        return recursos.meus(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100
     * Título: Fila de moderação
     * Objetivo: Listar os casos em aberto.
     * Decisões: Moderador ou administrador.
     */
    @GetMapping("/api/v1/admin/moderacao/casos")
    @PreAuthorize(MODERADOR)
    @Operation(summary = "Fila de moderação")
    List<ModeracaoService.ItemDaFila> fila() {
        return moderacao.fila();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100
     * Título: Caso
     * Objetivo: Mostrar o caso.
     * Decisões: O jurídico também lê, para dar o parecer.
     */
    @GetMapping("/api/v1/admin/moderacao/casos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERADOR', 'JURIDICO')")
    @Operation(summary = "Detalhe do caso")
    ModeracaoService.Caso caso(@PathVariable UUID id) {
        return moderacao.caso(id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-100
     * Título: Assumir
     * Objetivo: Assumir o caso.
     * Decisões: Com conferência de conflito.
     */
    @PostMapping("/api/v1/admin/moderacao/casos/{id}/assumir")
    @PreAuthorize(MODERADOR)
    @Operation(summary = "Assume o caso")
    ModeracaoService.Caso assumir(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return moderacao.assumir(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101, KAN-102
     * Título: Medida preventiva
     * Objetivo: Aplicar a medida preventiva.
     * Decisões: Ver o serviço.
     */
    @PostMapping("/api/v1/admin/moderacao/casos/{id}/medida-preventiva")
    @PreAuthorize(MODERADOR)
    @Operation(summary = "Aplica uma medida preventiva")
    ModeracaoService.Caso preventiva(@PathVariable UUID id, @RequestBody PreventivaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return moderacao.preventiva(Autenticado.id(token), id, pedido.tipo(), pedido.alvoId(), pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102
     * Título: Acessos ao Nível 2
     * Objetivo: Levantar os acessos confidenciais da startup do caso.
     * Decisões: Justificativa obrigatória.
     */
    @GetMapping("/api/v1/admin/moderacao/casos/{id}/acessos-nivel2")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERADOR', 'JURIDICO')")
    @Operation(summary = "Levanta os acessos ao Nível 2")
    List<ModeracaoService.AcessoConfidencial> acessos(@PathVariable UUID id, @RequestParam String justificativa) {
        return moderacao.acessosNivel2(id, justificativa);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-102
     * Título: Parecer jurídico
     * Objetivo: Guardar o parecer do jurídico.
     * Decisões: Jurídico ou administrador.
     */
    @PutMapping("/api/v1/admin/moderacao/casos/{id}/parecer")
    @PreAuthorize("hasAnyRole('ADMIN', 'JURIDICO')")
    @Operation(summary = "Registra o parecer jurídico")
    ModeracaoService.Caso parecer(@PathVariable UUID id, @RequestBody TextoRequest pedido) {
        return moderacao.parecer(id, pedido.texto());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Decidir
     * Objetivo: Decidir o caso.
     * Decisões: Banimento segue para a segunda aprovação.
     */
    @PostMapping("/api/v1/admin/moderacao/casos/{id}/decisao")
    @PreAuthorize(MODERADOR)
    @Operation(summary = "Decide o caso")
    ModeracaoService.Caso decidir(@PathVariable UUID id, @RequestBody ModeracaoService.PedidoDeDecisao pedido, @AuthenticationPrincipal Jwt token) {
        return moderacao.decidir(Autenticado.id(token), id, pedido);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Fila de recursos
     * Objetivo: Listar os recursos que a pessoa pode julgar.
     * Decisões: Analistas, curadores, moderadores e administradores.
     */
    @GetMapping("/api/v1/admin/recursos")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERADOR', 'CURADOR', 'ANALISTA_VERIFICACAO')")
    @Operation(summary = "Fila de recursos")
    List<RecursosService.Recurso> filaDeRecursos(@AuthenticationPrincipal Jwt token) {
        return recursos.fila(Autenticado.id(token), Autenticado.papeis(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86
     * Título: Julgar recurso
     * Objetivo: Deferir ou indeferir o recurso.
     * Decisões: Nunca quem decidiu antes.
     */
    @PostMapping("/api/v1/admin/recursos/{id}/julgamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERADOR', 'CURADOR', 'ANALISTA_VERIFICACAO')")
    @Operation(summary = "Julga o recurso")
    RecursosService.Recurso julgar(@PathVariable UUID id, @RequestBody JulgamentoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return recursos.julgar(Autenticado.id(token), Autenticado.papeis(token), id, Boolean.TRUE.equals(pedido.deferir()), pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-86, KAN-101, KAN-102
     * Título: Pedidos
     * Objetivo: Tipar os corpos das rotas.
     * Decisões: Boolean em vez de boolean, para o corpo sem o campo não falhar.
     */
    record PreventivaRequest(ModeracaoService.Preventiva tipo, UUID alvoId, String motivo) {
    }

    record TextoRequest(String texto) {
    }

    record JulgamentoRequest(Boolean deferir, String motivo) {
    }
}
