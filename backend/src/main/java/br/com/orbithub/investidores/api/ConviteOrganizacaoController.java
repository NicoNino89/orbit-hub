package br.com.orbithub.investidores.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.investidores.domain.Organizacao;
import br.com.orbithub.investidores.service.OrganizacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convite para organização
 * Objetivo: Mostrar de qual organização é o convite e aceitá-lo.
 * Decisões: Qualquer conta logada: quem ainda não é investidor recebe a mensagem que explica como ativar o papel, em
 *           vez de um 403 sem explicação. O token vai no corpo, não no endereço, para não ficar em log.
 */
@RestController
@RequestMapping("/api/v1/organizacoes/convites")
@Tag(name = "Organizações", description = "Fundos, redes de anjos e outras entidades investidoras")
class ConviteOrganizacaoController {

    private final OrganizacoesService organizacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criação do controller
     * Objetivo: Receber o serviço das organizações.
     * Decisões: Injeção por construtor.
     */
    ConviteOrganizacaoController(OrganizacoesService organizacoes) {
        this.organizacoes = organizacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Consultar convite
     * Objetivo: Devolver a organização, o tipo, o papel, o e-mail de destino e a validade.
     * Decisões: Convite inválido: 422 com mensagem única.
     */
    @PostMapping("/consulta")
    @Operation(summary = "Mostra de qual organização é um convite")
    OrganizacoesService.ResumoConvite consultar(@Valid @RequestBody TokenRequest pedido) {
        return organizacoes.consultarConvite(pedido.token());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Aceitar convite
     * Objetivo: Tornar a conta logada membro da organização.
     * Decisões: Devolve o identificador e o nome, para a tela abrir a organização.
     */
    @PostMapping("/aceite")
    @Operation(summary = "Aceita um convite para organização")
    Aceito aceitar(@Valid @RequestBody TokenRequest pedido, @AuthenticationPrincipal Jwt token) {
        Organizacao o = organizacoes.aceitarConvite(Autenticado.id(token), pedido.token());
        return new Aceito(o.getId(), o.getNome());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pedido com o token
     * Objetivo: Receber o token do link do convite.
     * Decisões: Obrigatório.
     */
    record TokenRequest(@NotBlank(message = "Link de convite incompleto.") String token) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convite aceito
     * Objetivo: Levar a organização em que a pessoa entrou.
     * Decisões: Identificador e nome.
     */
    record Aceito(UUID id, String nome) {
    }
}
