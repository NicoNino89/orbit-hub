package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.AlteracaoDeItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Repositório do histórico dos itens de lista
 * Objetivo: Gravar e listar as alterações dos itens.
 * Decisões: Só inserir e ler (Repository mínimo).
 */
public interface AlteracaoDeItemRepository extends Repository<AlteracaoDeItem, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Gravar alteração
     * Objetivo: Inserir uma alteração no histórico.
     * Decisões: Nunca atualiza.
     */
    AlteracaoDeItem save(AlteracaoDeItem alteracao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Histórico de um item
     * Objetivo: Listar as alterações de um item, da mais recente para a mais antiga.
     * Decisões: Sem paginação.
     */
    List<AlteracaoDeItem> findByItemIdOrderByOcorridoEmDesc(UUID itemId);
}
