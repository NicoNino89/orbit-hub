package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
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
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-67, KAN-45
 * Título: Bloqueios e preferência de investidores da startup
 * Objetivo: Bloquear e desbloquear investidores, ver o histórico e escolher se a startup aceita só verificados.
 * Decisões: Regras no serviço (fundador administrador).
 */
@RestController
@RequestMapping("/api/v1/startups/{id}")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class BloqueiosController {

    private final BloqueiosDaStartup bloqueios;
    private final StartupsService startups;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Criação
     * Objetivo: Receber os serviços e o relógio.
     * Decisões: Injeção por construtor.
     */
    BloqueiosController(BloqueiosDaStartup bloqueios, StartupsService startups, Clock relogio) {
        this.bloqueios = bloqueios;
        this.startups = startups;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Bloqueios
     * Objetivo: Listar os bloqueios e o histórico, e a preferência de só verificados.
     * Decisões: Qualquer membro.
     */
    @GetMapping("/bloqueios")
    @Operation(summary = "Bloqueios da startup")
    PainelResponse painel(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        BloqueiosDaStartup.Painel p = bloqueios.painel(Autenticado.id(token), id);
        return new PainelResponse(p.ativos(), p.historico(), startups.ler(id).isApenasVerificados());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Bloquear
     * Objetivo: Bloquear uma pessoa (e-mail) ou organização (CNPJ).
     * Decisões: 201.
     */
    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bloqueia um investidor ou organização")
    BloqueiosDaStartup.Bloqueio bloquear(@PathVariable UUID id, @Valid @RequestBody BloqueioRequest pedido, @AuthenticationPrincipal Jwt token) {
        return bloqueios.bloquear(Autenticado.id(token), id, pedido.tipo(), pedido.identificador());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Desbloquear
     * Objetivo: Tirar o bloqueio.
     * Decisões: 204.
     */
    @DeleteMapping("/bloqueios/{tipo}/{alvo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desbloqueia um investidor ou organização")
    void desbloquear(@PathVariable UUID id, @PathVariable String tipo, @PathVariable UUID alvo, @AuthenticationPrincipal Jwt token) {
        bloqueios.desbloquear(Autenticado.id(token), id, tipo, alvo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-62
     * Título: Só verificados
     * Objetivo: Escolher se a startup aceita só investidores verificados.
     * Decisões: Só o fundador administrador.
     */
    @PutMapping("/apenas-verificados")
    @Transactional
    @Operation(summary = "Define se a startup aceita só investidores verificados")
    PreferenciaResponse apenasVerificados(@PathVariable UUID id, @Valid @RequestBody PreferenciaRequest pedido, @AuthenticationPrincipal Jwt token) {
        startups.exigirAdministrador(Autenticado.id(token), id);
        startups.ler(id).definirApenasVerificados(pedido.apenasVerificados(), relogio.instant());
        return new PreferenciaResponse(pedido.apenasVerificados());
    }

    record BloqueioRequest(@NotBlank(message = "Escolha pessoa ou organização.") String tipo,
                           @NotBlank(message = "Informe o e-mail ou o CNPJ.") String identificador) {
    }

    record PreferenciaRequest(@NotNull(message = "Diga se aceita só verificados.") Boolean apenasVerificados) {
    }

    record PreferenciaResponse(boolean apenasVerificados) {
    }

    record PainelResponse(java.util.List<BloqueiosDaStartup.Bloqueio> ativos, java.util.List<BloqueiosDaStartup.Mudanca> historico,
                          boolean apenasVerificados) {
    }
}
