package br.com.orbithub.administracao.api;

import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.rotinas.ExecucaoDeRotina;
import br.com.orbithub.compartilhado.rotinas.ExecucaoDeRotinaRepository;
import br.com.orbithub.compartilhado.rotinas.ExecutorDeRotinas;
import br.com.orbithub.compartilhado.rotinas.RotinaAgendada;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Painel das rotinas automáticas
 * Objetivo: Mostrar ao administrador a última execução e o status de cada rotina, e deixar pausar, retomar ou executar
 *           agora.
 * Decisões: Somente administrador; tudo em /api/v1/admin, então cada ação entra na trilha de auditoria. O painel de
 *           operação completo (KAN-82) reaproveita estas rotas.
 */
@RestController
@RequestMapping("/api/v1/admin/rotinas")
@SomenteAdmin
@Tag(name = "Rotinas", description = "Rotinas automáticas: execuções, pausa e execução manual")
class RotinaController {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final ExecutorDeRotinas executor;
    private final ExecucaoDeRotinaRepository execucoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação do controller
     * Objetivo: Receber o executor e as execuções.
     * Decisões: Injeção por construtor.
     */
    RotinaController(ExecutorDeRotinas executor, ExecucaoDeRotinaRepository execucoes) {
        this.executor = executor;
        this.execucoes = execucoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotinas e status
     * Objetivo: Listar as rotinas com frequência, parâmetros, pausa, última execução e próxima execução prevista.
     * Decisões: Em ordem de código.
     */
    @GetMapping
    @Operation(summary = "Rotinas automáticas com a última execução")
    List<RotinaResponse> listar() {
        Instant agora = Instant.now();
        return executor.rotinas().values().stream().map(r -> new RotinaResponse(r.codigo(), r.descricao(), r.cron(),
                r.modulo(), r.parametros(), executor.pausada(r.codigo()),
                CronExpression.parse(r.cron()).next(agora.atZone(BRASILIA)).toInstant(),
                execucoes.findFirstByRotinaOrderByIniciadaEmDesc(r.codigo()).map(ExecucaoResponse::de).orElse(null)))
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Execuções de uma rotina
     * Objetivo: Listar as 20 execuções mais recentes.
     * Decisões: Inclui as manuais.
     */
    @GetMapping("/{codigo}/execucoes")
    @Operation(summary = "Últimas execuções de uma rotina")
    List<ExecucaoResponse> execucoes(@PathVariable String codigo) {
        rotina(codigo);
        return execucoes.findTop20ByRotinaOrderByIniciadaEmDesc(codigo).stream().map(ExecucaoResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Pausar
     * Objetivo: Parar as execuções automáticas da rotina.
     * Decisões: O vigia não acusa atraso de rotina pausada.
     */
    @PostMapping("/{codigo}/pausa")
    @Operation(summary = "Pausa uma rotina")
    void pausar(@PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        ContextoDeAuditoria.descrever("ROTINA_PAUSADA", "rotina", rotina(codigo).codigo(), null);
        executor.definirPausa(codigo, true, Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Retomar
     * Objetivo: Voltar a executar a rotina no horário.
     * Decisões: A próxima execução segue o cron.
     */
    @PostMapping("/{codigo}/retomada")
    @Operation(summary = "Retoma uma rotina pausada")
    void retomar(@PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        ContextoDeAuditoria.descrever("ROTINA_RETOMADA", "rotina", rotina(codigo).codigo(), null);
        executor.definirPausa(codigo, false, Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Executar agora
     * Objetivo: Rodar a rotina na hora, a pedido do administrador.
     * Decisões: A rotina é idempotente, então rodar fora do horário não duplica efeito.
     */
    @PostMapping("/{codigo}/execucao")
    @Operation(summary = "Executa uma rotina agora")
    RotinaAgendada.Resultado executar(@PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        rotina(codigo);
        RotinaAgendada.Resultado resultado = executor.executarManual(codigo, Autenticado.id(token));
        ContextoDeAuditoria.descrever("ROTINA_EXECUTADA_MANUALMENTE", "rotina", codigo,
                resultado.itens() + " item(ns), " + resultado.erros() + " erro(s)");
        return resultado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina pelo código
     * Objetivo: Achar a rotina ou responder 404.
     * Decisões: Código desconhecido é recurso inexistente.
     */
    private RotinaAgendada rotina(String codigo) {
        try {
            return executor.exigir(codigo);
        } catch (IllegalArgumentException desconhecida) {
            throw new RecursoNaoEncontradoException("Rotina não encontrada.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotina na resposta
     * Objetivo: Expor a rotina com status e última execução.
     * Decisões: Próxima execução calculada pelo cron.
     */
    record RotinaResponse(String codigo, String descricao, String cron, String modulo, List<String> parametros,
                          boolean pausada, Instant proximaExecucao, ExecucaoResponse ultimaExecucao) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Execução na resposta
     * Objetivo: Expor uma execução de rotina.
     * Decisões: Sem o id interno.
     */
    record ExecucaoResponse(String disparo, Instant iniciadaEm, Instant terminadaEm, int itens, int erros,
                            String resultado, String mensagem) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:17
         * Cartão Jira: KAN-104
         * Título: Conversão da execução
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Copia os campos.
         */
        static ExecucaoResponse de(ExecucaoDeRotina e) {
            return new ExecucaoResponse(e.getDisparo(), e.getIniciadaEm(), e.getTerminadaEm(), e.getItens(),
                    e.getErros(), e.getResultado(), e.getMensagem());
        }
    }
}
