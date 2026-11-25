package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.domain.MudancaDeSituacao;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.service.PublicacaoDaStartupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: API da publicação da startup
 * Objetivo: Expor o progresso do cadastro, o envio para publicação, o cancelamento do envio, a despublicação e o
 *           histórico das situações.
 * Decisões: Ações devolvem a situação nova.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class PublicacaoDaStartupController {

    private final PublicacaoDaStartupService publicacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Criação do controller
     * Objetivo: Receber o serviço da publicação.
     * Decisões: Injeção por construtor.
     */
    PublicacaoDaStartupController(PublicacaoDaStartupService publicacao) {
        this.publicacao = publicacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Progresso
     * Objetivo: Devolver o progresso do cadastro e o que falta para enviar.
     * Decisões: Quem não é da equipe recebe 404.
     */
    @GetMapping("/{id}/progresso")
    @Operation(summary = "Progresso do cadastro da startup")
    PublicacaoDaStartupService.Progresso progresso(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return publicacao.progresso(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Enviar
     * Objetivo: Enviar a startup para a revisão da plataforma.
     * Decisões: Cadastro incompleto: 422 com o que falta.
     */
    @PostMapping("/{id}/envio")
    @Operation(summary = "Envia a startup para publicação")
    SituacaoResponse enviar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return new SituacaoResponse(publicacao.enviar(Autenticado.id(token), id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Cancelar envio
     * Objetivo: Tirar a startup da revisão.
     * Decisões: Só em revisão.
     */
    @PostMapping("/{id}/envio/cancelamento")
    @Operation(summary = "Cancela o envio da startup para publicação")
    SituacaoResponse cancelarEnvio(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return new SituacaoResponse(publicacao.cancelarEnvio(Autenticado.id(token), id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Despublicar
     * Objetivo: Tirar a startup publicada do catálogo.
     * Decisões: Motivo opcional.
     */
    @PostMapping("/{id}/despublicacao")
    @Operation(summary = "Despublica a startup")
    SituacaoResponse despublicar(@PathVariable UUID id, @RequestBody(required = false) MotivoRequest pedido,
                                 @AuthenticationPrincipal Jwt token) {
        return new SituacaoResponse(publicacao.despublicar(Autenticado.id(token), id, pedido == null ? null : pedido.motivo()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Histórico
     * Objetivo: Listar as mudanças de situação.
     * Decisões: Sem o autor (dado interno); "pela plataforma" quando foi rotina.
     */
    @GetMapping("/{id}/situacoes")
    @Operation(summary = "Histórico das situações da startup")
    List<MudancaResponse> historico(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return publicacao.historico(Autenticado.id(token), id).stream().map(MudancaResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Pedido com motivo
     * Objetivo: Receber o motivo opcional da despublicação.
     * Decisões: Texto livre.
     */
    record MotivoRequest(String motivo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Situação na resposta
     * Objetivo: Levar a situação nova da startup.
     * Decisões: Só a situação.
     */
    record SituacaoResponse(SituacaoDaStartup situacao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Mudança na resposta
     * Objetivo: Expor uma mudança de situação.
     * Decisões: "pelaPlataforma" verdadeiro quando foi rotina.
     */
    record MudancaResponse(SituacaoDaStartup de, SituacaoDaStartup para, String motivo, boolean pelaPlataforma, Instant em) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:40
         * Cartão Jira: KAN-27
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do histórico.
         * Decisões: Sem o identificador do autor.
         */
        static MudancaResponse de(MudancaDeSituacao m) {
            return new MudancaResponse(m.getDe(), m.getPara(), m.getMotivo(), m.getPor() == null, m.getEm());
        }
    }
}
