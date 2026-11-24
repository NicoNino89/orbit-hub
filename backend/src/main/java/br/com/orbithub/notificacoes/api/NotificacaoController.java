package br.com.orbithub.notificacoes.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.notificacoes.domain.Notificacao;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao.Canal;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Notificações da pessoa logada
 * Objetivo: Listar os avisos, mostrar o contador do sino, marcar como lidos e escolher as preferências.
 * Decisões: Qualquer conta logada, só os próprios avisos. Não há rota para criar aviso: quem cria são os módulos, pelo
 *           serviço.
 */
@RestController
@RequestMapping("/api/v1/notificacoes")
@Tag(name = "Notificações", description = "Avisos da plataforma e preferências de recebimento")
class NotificacaoController {

    private final NotificacoesService notificacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Criação do controller
     * Objetivo: Receber o serviço de notificações.
     * Decisões: Injeção por construtor.
     */
    NotificacaoController(NotificacoesService notificacoes) {
        this.notificacoes = notificacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Avisos
     * Objetivo: Listar os avisos da pessoa, com o total de não lidos.
     * Decisões: Página de 20.
     */
    @GetMapping
    @Operation(summary = "Avisos da conta logada")
    PaginaResponse listar(@RequestParam(defaultValue = "0") int pagina, @AuthenticationPrincipal Jwt token) {
        UUID eu = Autenticado.id(token);
        Page<Notificacao> p = notificacoes.listar(eu, pagina);
        return new PaginaResponse(p.getContent().stream().map(NotificacaoResponse::de).toList(), p.getNumber(),
                p.getTotalElements(), notificacoes.naoLidas(eu));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Contador do sino
     * Objetivo: Devolver quantos avisos não lidos a pessoa tem.
     * Decisões: Consulta leve, para a tela perguntar de tempos em tempos.
     */
    @GetMapping("/contagem")
    @Operation(summary = "Quantidade de avisos não lidos")
    Map<String, Long> contagem(@AuthenticationPrincipal Jwt token) {
        return Map.of("naoLidas", notificacoes.naoLidas(Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Marcar como lida
     * Objetivo: Marcar um aviso como lido.
     * Decisões: 204 sem corpo.
     */
    @PostMapping("/{id}/leitura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca um aviso como lido")
    void marcarLida(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        notificacoes.marcarLida(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Marcar todas como lidas
     * Objetivo: Zerar o contador do sino.
     * Decisões: 204 sem corpo.
     */
    @PostMapping("/leitura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca todos os avisos como lidos")
    void marcarTodas(@AuthenticationPrincipal Jwt token) {
        notificacoes.marcarTodasLidas(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Preferências
     * Objetivo: Devolver o canal de cada tipo de aviso e o resumo diário.
     * Decisões: Inclui grupo, descrição e se é obrigatório, para a tela montar a lista.
     */
    @GetMapping("/preferencias")
    @Operation(summary = "Preferências de aviso da conta logada")
    PreferenciasResponse preferencias(@AuthenticationPrincipal Jwt token) {
        return PreferenciasResponse.de(notificacoes.preferencias(Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Salvar preferências
     * Objetivo: Gravar o canal de cada tipo e o resumo diário.
     * Decisões: Obrigatório com outro canal é recusado (422).
     */
    @PutMapping("/preferencias")
    @Operation(summary = "Salva as preferências de aviso")
    PreferenciasResponse salvar(@Valid @RequestBody PreferenciasRequest pedido, @AuthenticationPrincipal Jwt token) {
        return PreferenciasResponse.de(notificacoes.salvarPreferencias(Autenticado.id(token),
                pedido.canais() == null ? Map.of() : pedido.canais(), pedido.resumoDiario()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Pedido de preferências
     * Objetivo: Receber o canal por tipo e o resumo diário.
     * Decisões: Tipos não informados mantêm o que estava.
     */
    record PreferenciasRequest(Map<TipoDeNotificacao, Canal> canais,
                               @NotNull(message = "Informe se quer o resumo diário.") Boolean resumoDiario) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Aviso na resposta
     * Objetivo: Expor um aviso para a tela.
     * Decisões: "lida" calculado.
     */
    record NotificacaoResponse(UUID id, TipoDeNotificacao tipo, String titulo, String mensagem, String link,
                               Instant criadaEm, boolean lida) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:34
         * Cartão Jira: KAN-146
         * Título: Conversão do aviso
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Copia os campos.
         */
        static NotificacaoResponse de(Notificacao n) {
            return new NotificacaoResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(), n.getLink(),
                    n.getCriadaEm(), n.getLidaEm() != null);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Página de avisos
     * Objetivo: Levar os avisos, a página, o total e os não lidos.
     * Decisões: Formato próprio.
     */
    record PaginaResponse(List<NotificacaoResponse> itens, int pagina, long total, long naoLidas) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Preferências na resposta
     * Objetivo: Levar a lista de tipos com o canal e o resumo diário.
     * Decisões: Na ordem do catálogo.
     */
    record PreferenciasResponse(List<TipoResponse> tipos, boolean resumoDiario) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:34
         * Cartão Jira: KAN-146
         * Título: Conversão das preferências
         * Objetivo: Montar a resposta a partir das preferências.
         * Decisões: Todos os tipos.
         */
        static PreferenciasResponse de(NotificacoesService.Preferencias p) {
            return new PreferenciasResponse(Arrays.stream(TipoDeNotificacao.values()).map(t -> new TipoResponse(t,
                    t.getGrupo(), t.getDescricao(), t.isObrigatoria(), p.canais().get(t))).toList(), p.resumoDiario());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Tipo na resposta
     * Objetivo: Expor um tipo de aviso com o canal escolhido.
     * Decisões: Obrigatório sinalizado para a tela travar a escolha.
     */
    record TipoResponse(TipoDeNotificacao tipo, String grupo, String descricao, boolean obrigatoria, Canal canal) {
    }
}
