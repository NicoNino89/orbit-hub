package br.com.orbithub.administracao.api;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Opções das listas para os formulários
 * Objetivo: Devolver os itens ativos de uma lista, para os formulários de cadastro, tese, startup e atendimento.
 * Decisões: Leitura pública e só de itens ativos (são opções de formulário, sem dado pessoal); o painel do
 *           administrador usa a rota do backoffice, que mostra também os desativados.
 */
@RestController
@RequestMapping("/api/v1/listas")
@Tag(name = "Listas", description = "Listas gerenciáveis: segmentos, motivos, categorias e checklists")
class ListaPublicaController {

    private final ListasService listas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Criação do controller
     * Objetivo: Receber o serviço de listas.
     * Decisões: Injeção por construtor.
     */
    ListaPublicaController(ListasService listas) {
        this.listas = listas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Opções ativas
     * Objetivo: Listar código, rótulo e grupo dos itens ativos.
     * Decisões: Na ordem da lista ("Outros" por último).
     */
    @GetMapping("/{lista}")
    @Operation(summary = "Opções ativas de uma lista")
    List<OpcaoResponse> opcoes(@PathVariable Lista lista) {
        return listas.ativos(lista).stream().map(i -> new OpcaoResponse(i.getCodigo(), i.getRotulo(), i.getGrupo()))
                .toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Opção na resposta
     * Objetivo: Expor uma opção de formulário.
     * Decisões: Só código, rótulo e grupo.
     */
    record OpcaoResponse(String codigo, String rotulo, String grupo) {
    }
}
