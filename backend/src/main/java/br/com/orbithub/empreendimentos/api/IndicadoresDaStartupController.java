package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteEmpreendedor;
import br.com.orbithub.empreendimentos.domain.IndicadorDaStartup;
import br.com.orbithub.empreendimentos.service.IndicadoresDaStartupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: API dos indicadores da startup
 * Objetivo: Listar os indicadores de tração com o histórico e declarar um novo valor.
 * Decisões: Regras no serviço.
 */
@RestController
@RequestMapping("/api/v1/startups")
@SomenteEmpreendedor
@Tag(name = "Startups", description = "Cadastro e gestão das startups")
class IndicadoresDaStartupController {

    private final IndicadoresDaStartupService indicadores;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Criação do controller
     * Objetivo: Receber o serviço dos indicadores.
     * Decisões: Injeção por construtor.
     */
    IndicadoresDaStartupController(IndicadoresDaStartupService indicadores) {
        this.indicadores = indicadores;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Listar
     * Objetivo: Devolver os indicadores com o atual, a marca de desatualizado e o histórico.
     * Decisões: Quem não é da equipe recebe 404.
     */
    @GetMapping("/{id}/indicadores")
    @Operation(summary = "Indicadores de tração da startup")
    List<IndicadoresDaStartupService.Indicador> listar(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return indicadores.listar(Autenticado.id(token), id);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Declarar valor
     * Objetivo: Registrar um novo valor de um indicador.
     * Decisões: 201 com o indicador atualizado.
     */
    @PostMapping("/{id}/indicadores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Declara um valor de indicador de tração")
    IndicadoresDaStartupService.Indicador registrar(@PathVariable UUID id, @RequestBody ValorRequest pedido,
                                                    @AuthenticationPrincipal Jwt token) {
        return indicadores.registrar(Autenticado.id(token), id, pedido.indicador(), pedido.valor(), pedido.referencia());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Pedido de valor
     * Objetivo: Receber o indicador, o número e o mês de referência (AAAA-MM).
     * Decisões: Referência como texto, para a mensagem de erro ser clara.
     */
    record ValorRequest(IndicadorDaStartup indicador, BigDecimal valor, String referencia) {
    }
}
