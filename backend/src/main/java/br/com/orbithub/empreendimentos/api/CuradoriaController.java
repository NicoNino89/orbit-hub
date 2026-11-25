package br.com.orbithub.empreendimentos.api;

import br.com.orbithub.compartilhado.documentos.Cnpj;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.CuradoriaOuAdmin;
import br.com.orbithub.empreendimentos.domain.RevisaoDeCuradoria;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.service.CuradoriaService;
import br.com.orbithub.verificacao.domain.Selo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66, KAN-84
 * Título: Fila da curadoria
 * Objetivo: Expor a fila, o detalhe, assumir, declarar impedimento e decidir as revisões das startups.
 * Decisões: Backoffice em /api/v1/admin/**, para curadores e administradores. A apresentação completa da startup sai
 *           pela rota do catálogo (ApresentacaoController).
 */
@RestController
@RequestMapping("/api/v1/admin/curadoria")
@CuradoriaOuAdmin
@Tag(name = "Curadoria", description = "Revisão das startups antes do catálogo")
class CuradoriaController {

    private final CuradoriaService curadoria;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Criação
     * Objetivo: Receber o serviço.
     * Decisões: Injeção por construtor.
     */
    CuradoriaController(CuradoriaService curadoria) {
        this.curadoria = curadoria;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Fila
     * Objetivo: Listar as revisões abertas.
     * Decisões: Ordem de chegada.
     */
    @GetMapping
    @Operation(summary = "Fila da curadoria")
    List<ItemResponse> fila(@AuthenticationPrincipal Jwt token) {
        return curadoria.fila(Autenticado.id(token)).stream().map(i -> new ItemResponse(i.revisao().getId(), i.revisao().getStartupId(),
                i.startup(), i.revisao().getTipo(), i.revisao().getSituacao(), i.revisao().getEnviadaEm(), i.prazo(), i.atrasado(),
                i.responsavel(), i.conflitoComigo(), i.semCuradorLivre())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Detalhe
     * Objetivo: Mostrar o que o curador confere.
     * Decisões: CNPJ formatado.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma revisão da curadoria")
    DetalheResponse detalhe(@PathVariable UUID id) {
        CuradoriaService.Detalhe d = curadoria.detalhe(id);
        return new DetalheResponse(RevisaoResponse.de(d.revisao()), d.startup().getId(), d.startup().getNome(),
                d.startup().getCnpj() == null ? null : Cnpj.formatar(d.startup().getCnpj()), d.startup().getSituacao(),
                d.seloDaStartup(), d.responsavel(), d.prazo(),
                d.checklist().stream().map(i -> new ChecklistResponse(i.getCodigo(), i.getRotulo())).toList(), d.fundadores(),
                d.alteracoes(), d.anteriores().stream().map(RevisaoResponse::de).toList());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Assumir
     * Objetivo: Atribuir a revisão a quem chama.
     * Decisões: Administrador pode tomar de outro curador.
     */
    @PostMapping("/{id}/assumir")
    @Operation(summary = "Assume uma revisão da curadoria")
    RevisaoResponse assumir(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return RevisaoResponse.de(curadoria.assumir(id, Autenticado.id(token), Autenticado.temPapel(token, "ADMIN")));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Decidir
     * Objetivo: Aprovar, devolver ou propor a recusa por fraude.
     * Decisões: Checklist completo no pedido.
     */
    @PostMapping("/{id}/decisao")
    @Operation(summary = "Decide uma revisão da curadoria")
    RevisaoResponse decidir(@PathVariable UUID id, @Valid @RequestBody DecisaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RevisaoResponse.de(curadoria.decidir(id, Autenticado.id(token), Autenticado.temPapel(token, "ADMIN"),
                new CuradoriaService.Decisao(pedido.resultado(), pedido.checklist(), pedido.apontamentos(), pedido.parecer())));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-87
     * Título: Impedimento
     * Objetivo: Declarar impedimento na revisão.
     * Decisões: Motivo obrigatório.
     */
    @PostMapping("/{id}/impedimento")
    @Operation(summary = "Declara impedimento numa revisão da curadoria")
    RevisaoResponse impedimento(@PathVariable UUID id, @Valid @RequestBody ImpedimentoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return RevisaoResponse.de(curadoria.declararImpedimento(id, Autenticado.id(token), pedido.motivo()));
    }

    record ImpedimentoRequest(@NotBlank(message = "Diga o motivo.") String motivo) {
    }

    record DecisaoRequest(@NotNull(message = "Escolha a decisão.") RevisaoDeCuradoria.Situacao resultado,
                          List<CuradoriaService.Marcacao> checklist, List<CuradoriaService.ApontamentoNaSecao> apontamentos,
                          String parecer) {
    }

    record ItemResponse(UUID id, UUID startupId, String startup, RevisaoDeCuradoria.Tipo tipo, RevisaoDeCuradoria.Situacao situacao,
                        Instant enviadaEm, Instant prazo, boolean atrasado, String responsavel, boolean conflitoComigo,
                        boolean semCuradorLivre) {
    }

    record ChecklistResponse(String codigo, String rotulo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Revisão na resposta
     * Objetivo: Levar a revisão com o checklist e os apontamentos.
     * Decisões: Identificadores de conta ficam de fora.
     */
    record RevisaoResponse(UUID id, RevisaoDeCuradoria.Tipo tipo, RevisaoDeCuradoria.Situacao situacao, Instant enviadaEm,
                           Instant decididaEm, String parecer, boolean aguardandoSegundaAprovacao, List<RevisaoDeCuradoria.Item> itens) {
        static RevisaoResponse de(RevisaoDeCuradoria r) {
            return new RevisaoResponse(r.getId(), r.getTipo(), r.getSituacao(), r.getEnviadaEm(), r.getDecididaEm(), r.getParecer(),
                    r.getSituacao() == RevisaoDeCuradoria.Situacao.AGUARDANDO_APROVACAO, r.getItens());
        }
    }

    record DetalheResponse(RevisaoResponse revisao, UUID startupId, String startup, String cnpj, SituacaoDaStartup situacaoDaStartup,
                           Selo seloDaStartup, String responsavel, Instant prazo, List<ChecklistResponse> checklist,
                           List<CuradoriaService.Fundador> fundadores, List<CuradoriaService.Alteracao> alteracoes,
                           List<RevisaoResponse> anteriores) {
    }
}
