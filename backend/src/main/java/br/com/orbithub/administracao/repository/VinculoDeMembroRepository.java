package br.com.orbithub.administracao.repository;

import br.com.orbithub.administracao.domain.VinculoDeMembro;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Repositório dos vínculos da equipe
 * Objetivo: Listar os vínculos declarados por membro ou de toda a equipe.
 * Decisões: Mais novos primeiro.
 */
public interface VinculoDeMembroRepository extends JpaRepository<VinculoDeMembro, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Vínculos de um membro
     * Objetivo: Listar o que um membro declarou.
     * Decisões: Mais novos primeiro.
     */
    List<VinculoDeMembro> findByMembroIdOrderByCriadoEmDesc(UUID membroId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Todos os vínculos
     * Objetivo: Listar os vínculos de toda a equipe, para a revisão de acessos.
     * Decisões: Agrupados por membro.
     */
    List<VinculoDeMembro> findAllByOrderByMembroIdAscCriadoEmDesc();
}
