package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.service.EquipeDaStartupService;
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
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Convite para a equipe da startup
 * Objetivo: Mostrar de qual startup é o convite e aceitá-lo.
 * Decisões: Qualquer conta logada: quem ainda não é empreendedor recebe a mensagem que explica como ativar o papel, em
 *           vez de um 403 sem explicação. O token vai no corpo, não no endereço, para não ficar em log.
 */
@RestController
@RequestMapping("/api/v1/startups")
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class ConviteDaStartupController {

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
    ConviteDaStartupController(EquipeDaStartupService equipe) {
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Consultar convite
     * Objetivo: Mostrar de qual startup é o convite.
     * Decisões: Convite inválido: 422.
     */
    @PostMapping("/convites/consulta")
    @Operation(summary = "Mostra de qual startup é um convite")
    EquipeDaStartupService.ResumoConvite consultar(@Valid @RequestBody TokenRequest pedido) {
        return equipe.consultarConvite(pedido.token());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Aceitar convite
     * Objetivo: Tornar a conta logada membro da equipe.
     * Decisões: Devolve a startup, para a tela abri-la.
     */
    @PostMapping("/convites/aceite")
    @Operation(summary = "Aceita um convite para a equipe da startup")
    Aceito aceitar(@Valid @RequestBody TokenRequest pedido, @AuthenticationPrincipal Jwt token) {
        Startup s = equipe.aceitarConvite(Autenticado.id(token), pedido.token());
        return new Aceito(s.getId(), s.getNome());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Pedido com o token
     * Objetivo: Receber o token do link do convite.
     * Decisões: Obrigatório.
     */
    record TokenRequest(@NotBlank(message = "Link de convite incompleto.") String token) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Convite aceito
     * Objetivo: Levar a startup em que a pessoa entrou.
     * Decisões: Identificador e nome.
     */
    record Aceito(UUID id, String nome) {
    }
}
