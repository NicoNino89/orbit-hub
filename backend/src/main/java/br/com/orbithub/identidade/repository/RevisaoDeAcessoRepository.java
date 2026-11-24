package br.com.orbithub.identidade.repository;

import br.com.orbithub.identidade.domain.RevisaoDeAcesso;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:10
 * Cartão Jira: KAN-75
 * Título: Repositório das revisões de acesso
 * Objetivo: Gravar as revisões e achar a mais recente.
 * Decisões: Spring Data; a próxima revisão é calculada a partir da última.
 */
public interface RevisaoDeAcessoRepository extends JpaRepository<RevisaoDeAcesso, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:10
     * Cartão Jira: KAN-75
     * Título: Última revisão
     * Objetivo: Buscar a revisão mais recente.
     * Decisões: Vazio enquanto nenhuma revisão foi feita.
     */
    Optional<RevisaoDeAcesso> findFirstByOrderByRealizadaEmDesc();
}
