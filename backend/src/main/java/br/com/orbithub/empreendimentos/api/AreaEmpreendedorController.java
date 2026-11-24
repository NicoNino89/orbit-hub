package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Minhas startups
     * Objetivo: Listar as startups da conta e o próximo passo.
     * Decisões: Até a S4 não há startups; a lista vem vazia e o próximo passo é o cadastro.
     */
    @GetMapping("/meus")
    @Operation(summary = "Startups da conta (somente empreendedor)")
    MeusEmpreendimentos meus() {
        return new MeusEmpreendimentos(List.of(), "/empreendedor/onboarding");
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
