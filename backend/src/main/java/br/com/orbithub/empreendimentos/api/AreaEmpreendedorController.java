package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.service.StartupsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Área do empreendedor
 * Objetivo: Ponto de entrada das funções de empreendedor, acessível só a quem tem o papel.
 * Decisões: Regra de papel na classe (@SomenteEmpreendedor). A edição de cada startup (S4) acrescenta a regra
 *           de dono: @PreAuthorize("@acesso.ehDono('startup', #id)"), com um DonoDeRecurso do módulo.
 */
@RestController
@RequestMapping("/api/v1/empreendimentos")
@SomenteEmpreendedor
@Tag(name = "Empreendimentos", description = "Funções de quem atua como empreendedor")
class AreaEmpreendedorController {

    private final StartupsService startups;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Criação do controller
     * Objetivo: Receber o serviço das startups.
     * Decisões: Injeção por construtor.
     */
    AreaEmpreendedorController(StartupsService startups) {
        this.startups = startups;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Minhas startups
     * Objetivo: Listar as startups da conta e o próximo passo.
     * Decisões: Lista as startups de que a pessoa faz parte; sem nenhuma, o próximo passo é cadastrar a primeira (KAN-21).
     */
    @GetMapping("/meus")
    @Operation(summary = "Startups da conta (somente empreendedor)")
    MeusEmpreendimentos meus(@AuthenticationPrincipal Jwt token) {
        List<UUID> ids = startups.minhas(Autenticado.id(token)).stream().map(v -> v.startup().getId()).toList();
        return new MeusEmpreendimentos(ids, ids.isEmpty() ? "/empreendedor/startups/nova" : "/empreendedor");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Startups na API
     * Objetivo: Ids das startups da conta e o próximo passo.
     * Decisões: Cresce com o KAN-21.
     */
    record MeusEmpreendimentos(List<UUID> startups, String proximoPasso) {
    }
}
