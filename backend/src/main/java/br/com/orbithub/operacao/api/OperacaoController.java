package br.com.orbithub.operacao.api;

import br.com.orbithub.administracao.service.TimesService;
import br.com.orbithub.atendimento.service.TitularidadeService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.operacao.service.PainelDeOperacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-76, KAN-77, KAN-82
 * Título: Operação
 * Objetivo: Expor à equipe o painel de operação, os times (líderes, ausências, devolução de casos) e, ao atendimento N2,
 *           o cadastro assistido de organização e a proposta de transferência de titularidade.
 * Decisões: Rotas do backoffice; o papel exato é conferido aqui.
 */
@RestController
@Tag(name = "Operação", description = "Painel, times e titularidade")
class OperacaoController {

    private static final String N2 = "hasAnyRole('ADMIN', 'SUPORTE_N2')";

    private final PainelDeOperacaoService painel;
    private final TimesService times;
    private final TitularidadeService titularidade;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    OperacaoController(PainelDeOperacaoService painel, TimesService times, TitularidadeService titularidade) {
        this.painel = painel;
        this.times = times;
        this.titularidade = titularidade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-82
     * Título: Painel
     * Objetivo: Devolver o painel de operação.
     * Decisões: Toda a equipe vê (sem dados confidenciais).
     */
    @GetMapping("/api/v1/admin/operacao/painel")
    @Operation(summary = "Painel de operação")
    PainelDeOperacaoService.Painel painel() {
        return painel.painel();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Times
     * Objetivo: Listar os times.
     * Decisões: Toda a equipe vê.
     */
    @GetMapping("/api/v1/admin/times")
    @Operation(summary = "Times internos")
    List<TimesService.VisaoDoTime> times() {
        return times.times();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Definir líder
     * Objetivo: Escolher o líder do time.
     * Decisões: Só o administrador.
     */
    @PutMapping("/api/v1/admin/times/{time}/lider")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Define o líder do time")
    void definirLider(@PathVariable TimesService.Time time, @RequestBody MembroRequest pedido) {
        times.definirLider(time, pedido.membroId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Minhas ausências
     * Objetivo: Listar as ausências da pessoa.
     * Decisões: Atuais e futuras.
     */
    @GetMapping("/api/v1/admin/times/ausencias")
    @Operation(summary = "Minhas ausências")
    List<TimesService.Ausencia> ausencias(@AuthenticationPrincipal Jwt token) {
        return times.ausencias(Autenticado.id(token));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Registrar ausência
     * Objetivo: Marcar uma ausência da pessoa (ou de um membro, pelo administrador).
     * Decisões: Sem membro informado, vale para quem pede.
     */
    @PostMapping("/api/v1/admin/times/ausencias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma ausência")
    TimesService.Ausencia registrarAusencia(@RequestBody AusenciaRequest pedido, @AuthenticationPrincipal Jwt token) {
        UUID eu = Autenticado.id(token);
        UUID membro = pedido.membroId() != null && Autenticado.papeis(token).contains(Papel.ADMIN) ? pedido.membroId() : eu;
        return times.registrarAusencia(membro, pedido.inicio(), pedido.fim(), pedido.motivo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Remover ausência
     * Objetivo: Apagar uma ausência.
     * Decisões: A própria; o administrador, qualquer uma.
     */
    @DeleteMapping("/api/v1/admin/times/ausencias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma ausência")
    void removerAusencia(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        times.removerAusencia(Autenticado.id(token), id, Autenticado.papeis(token).contains(Papel.ADMIN));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76
     * Título: Devolver casos
     * Objetivo: Devolver à fila os casos abertos de um membro.
     * Decisões: Só o administrador.
     */
    @PostMapping("/api/v1/admin/times/membros/{id}/devolucao")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Devolve à fila os casos de um membro")
    Map<String, Integer> devolver(@PathVariable UUID id) {
        return Map.of("casos", times.devolverCasos(id));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Convite institucional
     * Objetivo: Criar a organização convidada e convidar o responsável.
     * Decisões: Atendimento N2 ou administrador.
     */
    @PostMapping("/api/v1/admin/atendimento/organizacoes-assistidas")
    @PreAuthorize(N2)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria a organização convidada")
    Map<String, UUID> convidarOrganizacao(@RequestBody AssistidaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return Map.of("organizacaoId", titularidade.convidarOrganizacao(Autenticado.id(token),
                new OrganizacoesService.Dados(pedido.nome(), pedido.tipo(), pedido.cnpj(), pedido.site(), pedido.descricao()), pedido.emailResponsavel()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77, KAN-81
     * Título: Propor transferência
     * Objetivo: Propor a transferência de titularidade.
     * Decisões: Segunda aprovação.
     */
    @PostMapping("/api/v1/admin/atendimento/transferencias")
    @PreAuthorize(N2)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Propõe a transferência de titularidade")
    Map<String, UUID> transferir(@RequestBody TransferenciaRequest pedido, @AuthenticationPrincipal Jwt token) {
        return Map.of("propostaId", titularidade.propor(Autenticado.id(token), pedido.alvo(), pedido.alvoId(), pedido.emailNovoTitular(),
                pedido.comprovacao()).getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-76, KAN-77
     * Título: Pedidos
     * Objetivo: Tipar os corpos das rotas.
     * Decisões: Datas no formato ISO.
     */
    record MembroRequest(UUID membroId) {
    }

    record AusenciaRequest(UUID membroId, LocalDate inicio, LocalDate fim, String motivo) {
    }

    record AssistidaRequest(String nome, String tipo, String cnpj, String site, String descricao, String emailResponsavel) {
    }

    record TransferenciaRequest(TitularidadeService.Alvo alvo, UUID alvoId, String emailNovoTitular, String comprovacao) {
    }
}
