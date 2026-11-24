package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.AlteracaoDeItem;
import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
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
import java.util.UUID;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Gestão das listas
 * Objetivo: Deixar o administrador incluir, renomear, desativar e reativar itens das listas e ver o histórico.
 * Decisões: Somente administrador (P15). Não existe rota de exclusão: itens que não devem mais aparecer são
 *           desativados.
 */
@RestController
@RequestMapping("/api/v1/admin/listas")
@SomenteAdmin
@Tag(name = "Listas", description = "Listas gerenciáveis: segmentos, motivos, categorias e checklists")
class ListaController {

    private final ListasService listas;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Criação do controller
     * Objetivo: Receber o serviço de listas e a consulta de nomes.
     * Decisões: Injeção por construtor.
     */
    ListaController(ListasService listas, ConsultaDeContas contas) {
        this.listas = listas;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Resumo das listas
     * Objetivo: Listar as listas com o título e a quantidade de itens.
     * Decisões: Na ordem do catálogo.
     */
    @GetMapping
    @Operation(summary = "Listas gerenciáveis com a quantidade de itens")
    List<ResumoResponse> resumo() {
        return listas.resumo().stream().map(r -> new ResumoResponse(r.lista(), r.lista().getTitulo(),
                r.lista().isComGrupo(), r.ativos(), r.total())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Itens de uma lista
     * Objetivo: Listar todos os itens, inclusive desativados.
     * Decisões: Para o painel.
     */
    @GetMapping("/{lista}")
    @Operation(summary = "Itens de uma lista, inclusive desativados")
    List<ItemResponse> itens(@PathVariable Lista lista) {
        return listas.todos(lista).stream().map(ItemResponse::de).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Incluir item
     * Objetivo: Acrescentar um item à lista.
     * Decisões: Código opcional (gerado do rótulo).
     */
    @PostMapping("/{lista}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inclui um item na lista")
    ItemResponse incluir(@PathVariable Lista lista, @Valid @RequestBody InclusaoRequest pedido,
                         @AuthenticationPrincipal Jwt token) {
        return ItemResponse.de(listas.incluir(lista, pedido.codigo(), pedido.rotulo(), pedido.grupo(),
                Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Renomear item
     * Objetivo: Trocar o rótulo de um item.
     * Decisões: O código não muda.
     */
    @PutMapping("/{lista}/{codigo}")
    @Operation(summary = "Renomeia um item (o código não muda)")
    ItemResponse renomear(@PathVariable Lista lista, @PathVariable String codigo,
                          @Valid @RequestBody RenomeacaoRequest pedido, @AuthenticationPrincipal Jwt token) {
        return ItemResponse.de(listas.renomear(lista, codigo, pedido.rotulo(), Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Desativar item
     * Objetivo: Tirar o item dos formulários.
     * Decisões: Registros antigos continuam com o item.
     */
    @PostMapping("/{lista}/{codigo}/desativacao")
    @Operation(summary = "Desativa um item")
    ItemResponse desativar(@PathVariable Lista lista, @PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        return ItemResponse.de(listas.definirAtivo(lista, codigo, false, Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Reativar item
     * Objetivo: Devolver o item aos formulários.
     * Decisões: Mesmo código de antes.
     */
    @PostMapping("/{lista}/{codigo}/reativacao")
    @Operation(summary = "Reativa um item")
    ItemResponse reativar(@PathVariable Lista lista, @PathVariable String codigo, @AuthenticationPrincipal Jwt token) {
        return ItemResponse.de(listas.definirAtivo(lista, codigo, true, Autenticado.id(token)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Histórico de um item
     * Objetivo: Listar as alterações de um item com o nome do autor.
     * Decisões: Da mais recente para a mais antiga.
     */
    @GetMapping("/{lista}/{codigo}/historico")
    @Operation(summary = "Histórico de um item")
    List<HistoricoResponse> historico(@PathVariable Lista lista, @PathVariable String codigo) {
        List<AlteracaoDeItem> alteracoes = listas.historico(lista, codigo);
        Map<UUID, String> nomes = contas.nomes(alteracoes.stream().map(AlteracaoDeItem::getAutorId).toList());
        return alteracoes.stream().map(a -> new HistoricoResponse(a.getAcao().name(), a.getRotuloAnterior(),
                a.getRotuloNovo(), nomes.getOrDefault(a.getAutorId(), "conta removida"), a.getOcorridoEm())).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Pedido de inclusão
     * Objetivo: Receber o rótulo e, se quiser, o código e o grupo.
     * Decisões: Formato aqui; regras no serviço.
     */
    record InclusaoRequest(
            @Size(max = 60, message = "Use até 60 caracteres no código.") String codigo,
            @NotBlank(message = "Informe o rótulo.") @Size(max = 160, message = "Use até 160 caracteres.") String rotulo,
            @Size(max = 60) String grupo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Pedido de renomeação
     * Objetivo: Receber o rótulo novo.
     * Decisões: Obrigatório.
     */
    record RenomeacaoRequest(
            @NotBlank(message = "Informe o rótulo.") @Size(max = 160, message = "Use até 160 caracteres.") String rotulo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Resumo na resposta
     * Objetivo: Expor uma lista com título e contagens.
     * Decisões: Para a tela inicial das listas.
     */
    record ResumoResponse(Lista lista, String titulo, boolean comGrupo, long ativos, long total) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Item na resposta
     * Objetivo: Expor um item de lista.
     * Decisões: Sem o id interno; o código identifica o item dentro da lista.
     */
    record ItemResponse(String codigo, String rotulo, String grupo, int ordem, boolean ativo) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 11:14
         * Cartão Jira: KAN-79
         * Título: Conversão do item
         * Objetivo: Montar a resposta a partir da entidade.
         * Decisões: Copia os campos.
         */
        static ItemResponse de(ItemDeLista i) {
            return new ItemResponse(i.getCodigo(), i.getRotulo(), i.getGrupo(), i.getOrdem(), i.isAtivo());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Alteração na resposta
     * Objetivo: Expor uma alteração do item com o nome do autor.
     * Decisões: Ação em texto (INCLUIDO, RENOMEADO, DESATIVADO, REATIVADO).
     */
    record HistoricoResponse(String acao, String rotuloAnterior, String rotuloNovo, String autor, Instant ocorridoEm) {
    }
}
