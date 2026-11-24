package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.AlteracaoDeParametro;
import br.com.orbithub.administracao.domain.Parametro;
import br.com.orbithub.administracao.service.ParametrosService;
import br.com.orbithub.compartilhado.seguranca.Autenticado;
import br.com.orbithub.compartilhado.seguranca.Permissoes.SomenteAdmin;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
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
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Parâmetros configuráveis
 * Objetivo: Deixar o administrador ver o catálogo de parâmetros, alterar valores com justificativa e consultar o
 *           histórico.
 * Decisões: Somente administrador (P15). Não há rota para criar nem apagar parâmetro: o catálogo vem com as regras
 *           que o usam.
 */
@RestController
@RequestMapping("/api/v1/admin/parametros")
@SomenteAdmin
@Tag(name = "Parâmetros", description = "Prazos e limites de regra de negócio, com histórico")
class ParametroController {

    private final ParametrosService parametros;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do controller
     * Objetivo: Receber o serviço de parâmetros e a consulta de nomes.
     * Decisões: Injeção por construtor.
     */
    ParametroController(ParametrosService parametros, ConsultaDeContas contas) {
        this.parametros = parametros;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Catálogo
     * Objetivo: Listar os parâmetros com valor, padrão, unidade, limites e se são sensíveis.
     * Decisões: Agrupados por grupo na tela.
     */
    @GetMapping
    @Operation(summary = "Catálogo de parâmetros")
    List<ParametroResponse> listar() {
        return parametros.listar().stream().map(ParametroResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Alterar
     * Objetivo: Pedir a alteração de um parâmetro.
     * Decisões: Sensível volta "aplicado": false, com o id da proposta de segunda aprovação.
     */
    @PutMapping("/{chave}")
    @Operation(summary = "Altera um parâmetro (sensível vai para segunda aprovação)")
    AlteracaoResponse alterar(@PathVariable String chave, @Valid @RequestBody AlteracaoRequest pedido,
                              @AuthenticationPrincipal Jwt token) {
        ParametrosService.Resultado r = parametros.alterar(chave, pedido.valor(), Autenticado.id(token),
                pedido.justificativa());
        return new AlteracaoResponse(r.aplicado(), r.propostaId(), ParametroResponse.de(r.parametro()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Histórico
     * Objetivo: Listar as alterações de um parâmetro com os nomes de autor e aprovador.
     * Decisões: Da mais recente para a mais antiga.
     */
    @GetMapping("/{chave}/historico")
    @Operation(summary = "Histórico de alterações de um parâmetro")
    List<HistoricoResponse> historico(@PathVariable String chave) {
        List<AlteracaoDeParametro> alteracoes = parametros.historico(chave);
        Map<UUID, String> nomes = contas.nomes(alteracoes.stream()
                .flatMap(a -> Stream.of(a.getAutorId(), a.getAprovadoPor())).toList());
        return alteracoes.stream().map(a -> new HistoricoResponse(a.getValorAnterior(), a.getValorNovo(),
                nomes.getOrDefault(a.getAutorId(), "conta removida"),
                a.getAprovadoPor() == null ? null : nomes.getOrDefault(a.getAprovadoPor(), "conta removida"),
                a.getJustificativa(), a.getAlteradoEm())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Pedido de alteração
     * Objetivo: Receber o valor novo e a justificativa.
     * Decisões: Valor em texto (inteiro, lista "7,21" ou opção).
     */
    record AlteracaoRequest(
            @NotBlank(message = "Informe o valor.") @Size(max = 200) String valor,
            @NotBlank(message = "Explique o motivo da alteração.") @Size(max = 1000) String justificativa) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Parâmetro na resposta
     * Objetivo: Expor o parâmetro para a tela.
     * Decisões: Unidade em texto, pronta para exibir.
     */
    record ParametroResponse(String chave, String grupo, String descricao, String tipo, String unidade, String valor,
                             String valorPadrao, Long minimo, Long maximo, List<String> opcoes, boolean sensivel,
                             Instant atualizadoEm) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 11:08
         * Cartão Jira: KAN-78
         * Título: Conversão do parâmetro
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Copia os campos.
         */
        static ParametroResponse de(Parametro p) {
            return new ParametroResponse(p.getChave(), p.getGrupo(), p.getDescricao(), p.getTipo().name(),
                    p.getUnidade().getTexto(), p.getValor(), p.getValorPadrao(), p.getMinimo(), p.getMaximo(),
                    p.opcoesAceitas(), p.isSensivel(), p.getAtualizadoEm());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Resultado da alteração
     * Objetivo: Dizer se o valor já vale ou se foi para segunda aprovação.
     * Decisões: Leva o parâmetro como ficou.
     */
    record AlteracaoResponse(boolean aplicado, UUID propostaId, ParametroResponse parametro) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Alteração no histórico
     * Objetivo: Expor uma alteração com os nomes.
     * Decisões: Aprovador nulo nos não sensíveis.
     */
    record HistoricoResponse(String valorAnterior, String valorNovo, String autor, String aprovadoPor,
                             String justificativa, Instant alteradoEm) {
    }
}
