package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.domain.ConviteDaStartup;
import br.com.orbithub.empreendimentos.domain.PapelNaStartup;
import br.com.orbithub.empreendimentos.domain.SecaoDaStartup;
import br.com.orbithub.empreendimentos.service.EquipeDaStartupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: API da equipe da startup
 * Objetivo: Expor a equipe, os convites, a troca de papel e a saída de membros.
 * Decisões: Só empreendedores. Gerenciar é do fundador administrador (regra no serviço); sair, qualquer membro.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class EquipeDaStartupController {

    private final EquipeDaStartupService equipe;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Criação do controller
     * Objetivo: Receber o serviço da equipe.
     * Decisões: Injeção por construtor.
     */
    EquipeDaStartupController(EquipeDaStartupService equipe) {
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Equipe
     * Objetivo: Listar os membros ativos.
     * Decisões: Qualquer membro vê.
     */
    @GetMapping("/{id}/equipe")
    @Operation(summary = "Membros da equipe da startup")
    List<EquipeDaStartupService.Membro> listar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return equipe.equipe(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convidar
     * Objetivo: Enviar um convite por e-mail.
     * Decisões: 201 com o convite (sem o token).
     */
    @PostMapping("/{id}/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Convida alguém para a equipe da startup")
    ConviteResponse convidar(@PathVariable UUID id, @Valid @RequestBody ConviteRequest pedido, @AuthenticationPrincipal Jwt token) {
        return ConviteResponse.de(equipe.convidar(Autenticado.id(token), id, pedido.email(), pedido.papel(), pedido.secoes()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convites pendentes
     * Objetivo: Listar os convites ainda válidos.
     * Decisões: Só o administrador.
     */
    @GetMapping("/{id}/convites")
    @Operation(summary = "Convites pendentes da startup")
    List<ConviteResponse> convites(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return equipe.convitesPendentes(Autenticado.id(token), id).stream().map(ConviteResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Cancelar convite
     * Objetivo: Invalidar um convite pendente.
     * Decisões: 204.
     */
    @DeleteMapping("/{id}/convites/{conviteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela um convite da startup")
    void cancelar(@PathVariable UUID id, @PathVariable UUID conviteId, @AuthenticationPrincipal Jwt token) {
        equipe.cancelarConvite(Autenticado.id(token), id, conviteId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Trocar papel
     * Objetivo: Mudar o papel e as seções de um membro.
     * Decisões: 204.
     */
    @PutMapping("/{id}/equipe/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca o papel de um membro da equipe")
    void trocarPapel(@PathVariable UUID id, @PathVariable UUID usuarioId, @RequestBody PapelRequest pedido,
                     @AuthenticationPrincipal Jwt token) {
        equipe.trocarPapel(Autenticado.id(token), id, usuarioId, pedido.papel(), pedido.secoes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Remover ou sair
     * Objetivo: Encerrar o vínculo de um membro (ou o próprio).
     * Decisões: 204.
     */
    @DeleteMapping("/{id}/equipe/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um membro da equipe ou sai dela")
    void encerrar(@PathVariable UUID id, @PathVariable UUID usuarioId, @AuthenticationPrincipal Jwt token) {
        equipe.encerrarVinculo(Autenticado.id(token), id, usuarioId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Pedido de convite
     * Objetivo: Receber o e-mail, o papel e as seções.
     * Decisões: Seções só valem para colaborador.
     */
    record ConviteRequest(@NotBlank(message = "Informe o e-mail.") String email, PapelNaStartup papel, Set<SecaoDaStartup> secoes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Pedido de papel
     * Objetivo: Receber o novo papel e as seções.
     * Decisões: Papel obrigatório (conferido no serviço).
     */
    record PapelRequest(PapelNaStartup papel, Set<SecaoDaStartup> secoes) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convite na resposta
     * Objetivo: Expor um convite pendente sem o token.
     * Decisões: Com as seções do colaborador.
     */
    record ConviteResponse(UUID id, String email, PapelNaStartup papel, Set<SecaoDaStartup> secoes, Instant criadoEm, Instant expiraEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 22:48
         * Cartão Jira: KAN-65
         * Título: Conversão
         * Objetivo: Montar a resposta a partir do convite.
         * Decisões: Sem o hash do token.
         */
        static ConviteResponse de(ConviteDaStartup c) {
            return new ConviteResponse(c.getId(), c.getEmail(), c.getPapel(), c.getSecoes(), c.getCriadoEm(), c.getExpiraEm());
        }
    }
}
