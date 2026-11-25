package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.service.CuradoriaService;
import br.com.orbithub.empreendimentos.service.VisibilidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28, KAN-66, KAN-85
 * Título: Visibilidade e curadoria para a equipe da startup
 * Objetivo: Ler e trocar o que a startup mostra no catálogo, a participação nas edições temáticas, e ver as revisões
 *           da curadoria com os comentários.
 * Decisões: Mesmo prefixo das demais rotas da startup; as regras de quem pode o quê ficam nos serviços.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class VisibilidadeDaStartupController {

    private final VisibilidadeService visibilidade;
    private final CuradoriaService curadoria;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    VisibilidadeDaStartupController(VisibilidadeService visibilidade, CuradoriaService curadoria) {
        this.visibilidade = visibilidade;
        this.curadoria = curadoria;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Ler visibilidade
     * Objetivo: Listar os itens com o padrão e a escolha atual.
     * Decisões: Qualquer membro.
     */
    @GetMapping("/{id}/visibilidade")
    @Operation(summary = "O que a startup mostra no catálogo")
    VisibilidadeService.Configuracao ler(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return visibilidade.ler(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Salvar visibilidade
     * Objetivo: Trocar quais itens são públicos.
     * Decisões: Mapa código → público.
     */
    @PutMapping("/{id}/visibilidade")
    @Operation(summary = "Define o que a startup mostra no catálogo")
    VisibilidadeService.Configuracao salvar(@PathVariable UUID id, @RequestBody Map<String, Boolean> escolhas,
                                            @AuthenticationPrincipal Jwt token) {
        return visibilidade.salvar(Autenticado.id(token), id, escolhas);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Participar das edições
     * Objetivo: Registrar se a startup aceita as edições temáticas.
     * Decisões: Só o fundador administrador.
     */
    @PutMapping("/{id}/participacao-edicoes")
    @Operation(summary = "Define se a startup participa das edições temáticas")
    VisibilidadeService.Configuracao participar(@PathVariable UUID id, @Valid @RequestBody ParticipacaoRequest pedido,
                                                @AuthenticationPrincipal Jwt token) {
        return visibilidade.participarDeEdicoes(Autenticado.id(token), id, pedido.participa());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Revisões da curadoria
     * Objetivo: Listar as revisões da startup, com resultado e comentários.
     * Decisões: Qualquer membro.
     */
    @GetMapping("/{id}/curadoria")
    @Operation(summary = "Revisões da curadoria da startup")
    List<CuradoriaService.RevisaoParaEquipe> curadoria(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return curadoria.daStartup(Autenticado.id(token), id);
    }

    record ParticipacaoRequest(@NotNull(message = "Diga se participa.") Boolean participa) {
    }
}
