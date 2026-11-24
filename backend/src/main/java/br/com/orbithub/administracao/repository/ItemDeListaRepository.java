package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.ItemDeLista;
import br.com.orbithub.administracao.domain.Lista;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Repositório dos itens de lista
 * Objetivo: Ler e gravar os itens das listas gerenciáveis.
 * Decisões: Spring Data; ordem da lista e depois rótulo.
 */
public interface ItemDeListaRepository extends JpaRepository<ItemDeLista, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Itens de uma lista
     * Objetivo: Listar todos os itens de uma lista, inclusive desativados.
     * Decisões: Para o painel do administrador.
     */
    List<ItemDeLista> findByListaOrderByOrdemAscRotuloAsc(Lista lista);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Itens ativos
     * Objetivo: Listar só os itens ativos, para os formulários.
     * Decisões: Mesma ordem do painel.
     */
    List<ItemDeLista> findByListaAndAtivoTrueOrderByOrdemAscRotuloAsc(Lista lista);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Item pelo código
     * Objetivo: Achar um item de uma lista pelo código estável.
     * Decisões: O código é único dentro da lista.
     */
    Optional<ItemDeLista> findByListaAndCodigo(Lista lista, String codigo);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Próxima ordem
     * Objetivo: Descobrir a posição de um item novo, antes de "Outros".
     * Decisões: Maior ordem abaixo de 99 mais um; lista vazia começa em 1.
     */
    @Query("select coalesce(max(i.ordem), 0) + 1 from ItemDeLista i where i.lista = :lista and i.ordem < 99")
    int proximaOrdem(@Param("lista") Lista lista);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Quantidade de itens
     * Objetivo: Contar os itens ativos e o total de uma lista, para o resumo do painel.
     * Decisões: Uma consulta por lista; são poucas.
     */
    long countByListaAndAtivoTrue(Lista lista);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Total de itens
     * Objetivo: Contar todos os itens de uma lista.
     * Decisões: Inclui desativados.
     */
    long countByLista(Lista lista);
}
