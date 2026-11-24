package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.Parametro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Repositório dos parâmetros
 * Objetivo: Ler e gravar os parâmetros.
 * Decisões: Lista por grupo e chave, para a tela.
 */
public interface ParametroRepository extends JpaRepository<Parametro, String> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Catálogo em ordem
     * Objetivo: Listar todos os parâmetros por grupo e chave.
     * Decisões: Ordem estável para a tela.
     */
    List<Parametro> findAllByOrderByGrupoAscChaveAsc();
}
