package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.AlteracaoDeParametro;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Repositório do histórico de parâmetros
 * Objetivo: Gravar e listar as alterações de parâmetros.
 * Decisões: Só inserir e ler (Repository mínimo).
 */
public interface ParametroHistoricoRepository extends Repository<AlteracaoDeParametro, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Gravar alteração
     * Objetivo: Inserir uma alteração no histórico.
     * Decisões: Nunca atualiza.
     */
    AlteracaoDeParametro save(AlteracaoDeParametro alteracao);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Histórico de um parâmetro
     * Objetivo: Listar as alterações de um parâmetro, da mais recente para a mais antiga.
     * Decisões: Sem paginação: um parâmetro muda poucas vezes.
     */
    List<AlteracaoDeParametro> findByChaveOrderByAlteradoEmDesc(String chave);
}
