package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AlteracaoDeItem;
import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.repository.AlteracaoDeItemRepository;
import br.com.orbithub.administracao.repository.ItemDeListaRepository;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Listas gerenciáveis
 * Objetivo: Deixar o administrador incluir, renomear, desativar e reativar itens das listas, com histórico, e dar aos
 *           outros módulos e às telas os itens ativos.
 * Decisões: Não existe exclusão, para preservar o histórico de quem já usou o item. Código repetido na mesma lista é
 *           recusado. Os outros módulos usam {@link #exigirAtivo} para recusar um código desativado num cadastro
 *           novo, mas continuam mostrando o rótulo dos itens antigos (por {@link #rotulo}).
 */
@Service
public class ListasService {

    static final String OBJETO = "lista_item";

    private final ItemDeListaRepository itens;
    private final AlteracaoDeItemRepository historico;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Criação do serviço
     * Objetivo: Receber os repositórios e o relógio.
     * Decisões: Injeção por construtor.
     */
    public ListasService(ItemDeListaRepository itens, AlteracaoDeItemRepository historico, Clock relogio) {
        this.itens = itens;
        this.historico = historico;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Itens ativos
     * Objetivo: Devolver os itens que aparecem nos formulários.
     * Decisões: Para as telas e os outros módulos.
     */
    @Transactional(readOnly = true)
    public List<ItemDeLista> ativos(Lista lista) {
        return itens.findByListaAndAtivoTrueOrderByOrdemAscRotuloAsc(lista);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Todos os itens
     * Objetivo: Devolver os itens de uma lista, inclusive desativados.
     * Decisões: Para o painel do administrador.
     */
    @Transactional(readOnly = true)
    public List<ItemDeLista> todos(Lista lista) {
        return itens.findByListaOrderByOrdemAscRotuloAsc(lista);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Resumo das listas
     * Objetivo: Contar os itens ativos e o total de cada lista.
     * Decisões: Na ordem do enum.
     */
    @Transactional(readOnly = true)
    public List<Resumo> resumo() {
        return Arrays.stream(Lista.values())
                .map(l -> new Resumo(l, itens.countByListaAndAtivoTrue(l), itens.countByLista(l))).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Rótulo de um código
     * Objetivo: Devolver o rótulo de um item, mesmo desativado, para exibir registros antigos.
     * Decisões: Código desconhecido devolve o próprio código.
     */
    @Transactional(readOnly = true)
    public String rotulo(Lista lista, String codigo) {
        return itens.findByListaAndCodigo(lista, codigo).map(ItemDeLista::getRotulo).orElse(codigo);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Exigir item ativo
     * Objetivo: Recusar, num cadastro novo, um código inexistente ou desativado.
     * Decisões: Mensagem de negócio, porque chega aos formulários.
     */
    @Transactional(readOnly = true)
    public ItemDeLista exigirAtivo(Lista lista, String codigo) {
        return itens.findByListaAndCodigo(lista, codigo).filter(ItemDeLista::isAtivo)
                .orElseThrow(() -> new RegraDeNegocioException("opcao_indisponivel",
                        "Esta opção não está disponível. Escolha outra da lista."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Incluir item
     * Objetivo: Acrescentar um item ativo à lista.
     * Decisões: Código gerado do rótulo quando não informado; entra antes de "Outros".
     */
    @Transactional
    public ItemDeLista incluir(Lista lista, String codigo, String rotulo, String grupo, UUID autor) {
        Instant agora = relogio.instant();
        ItemDeLista item = regra(() -> ItemDeLista.novo(lista, codigo, rotulo, grupo, itens.proximaOrdem(lista), agora));
        ContextoDeAuditoria.descrever("LISTA_ITEM_INCLUIDO", OBJETO, lista + ":" + item.getCodigo(), item.getRotulo());
        if (itens.findByListaAndCodigo(lista, item.getCodigo()).isPresent()) {
            throw new RegraDeNegocioException("codigo_em_uso", "Já existe um item com o código " + item.getCodigo()
                    + " nesta lista. Reative-o ou use outro rótulo.");
        }
        itens.save(item);
        historico.save(AlteracaoDeItem.registrar(item.getId(), AlteracaoDeItem.Tipo.INCLUIDO, null, item.getRotulo(),
                autor, agora));
        return item;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Renomear item
     * Objetivo: Trocar o rótulo de um item, mantendo o código.
     * Decisões: Registra antes e depois.
     */
    @Transactional
    public ItemDeLista renomear(Lista lista, String codigo, String novoRotulo, UUID autor) {
        ItemDeLista item = buscar(lista, codigo);
        Instant agora = relogio.instant();
        String anterior = regra(() -> item.renomear(novoRotulo, agora));
        historico.save(AlteracaoDeItem.registrar(item.getId(), AlteracaoDeItem.Tipo.RENOMEADO, anterior,
                item.getRotulo(), autor, agora));
        ContextoDeAuditoria.descrever("LISTA_ITEM_RENOMEADO", OBJETO, lista + ":" + codigo,
                anterior + " → " + item.getRotulo());
        return item;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Desativar ou reativar item
     * Objetivo: Tirar o item dos formulários ou devolvê-lo.
     * Decisões: Registros antigos continuam com o item; só cadastros novos deixam de vê-lo.
     */
    @Transactional
    public ItemDeLista definirAtivo(Lista lista, String codigo, boolean ativo, UUID autor) {
        ItemDeLista item = buscar(lista, codigo);
        Instant agora = relogio.instant();
        regra(() -> {
            item.definirAtivo(ativo, agora);
            return item;
        });
        historico.save(AlteracaoDeItem.registrar(item.getId(),
                ativo ? AlteracaoDeItem.Tipo.REATIVADO : AlteracaoDeItem.Tipo.DESATIVADO, item.getRotulo(),
                item.getRotulo(), autor, agora));
        ContextoDeAuditoria.descrever(ativo ? "LISTA_ITEM_REATIVADO" : "LISTA_ITEM_DESATIVADO", OBJETO,
                lista + ":" + codigo, item.getRotulo());
        return item;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Histórico de um item
     * Objetivo: Listar as alterações de um item.
     * Decisões: Da mais recente para a mais antiga.
     */
    @Transactional(readOnly = true)
    public List<AlteracaoDeItem> historico(Lista lista, String codigo) {
        return historico.findByItemIdOrderByOcorridoEmDesc(buscar(lista, codigo).getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Buscar item
     * Objetivo: Achar o item pelo código.
     * Decisões: Inexistente: 404.
     */
    private ItemDeLista buscar(Lista lista, String codigo) {
        return itens.findByListaAndCodigo(lista, codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado nesta lista."));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Regra da entidade
     * Objetivo: Traduzir a recusa da entidade em erro de negócio.
     * Decisões: Código "item_invalido" com a mensagem da entidade.
     */
    private static <T> T regra(Supplier<T> operacao) {
        try {
            return operacao.get();
        } catch (IllegalArgumentException invalido) {
            throw new RegraDeNegocioException("item_invalido", invalido.getMessage());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Resumo de uma lista
     * Objetivo: Levar a lista com a quantidade de itens ativos e o total.
     * Decisões: Para a tela inicial das listas.
     */
    public record Resumo(Lista lista, long ativos, long total) {
    }
}
