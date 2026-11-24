package br.com.orbithub.investidores.repository;

import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:16
 * Cartão Jira: KAN-61
 * Título: Repositório dos perfis de investimento
 * Objetivo: Buscar o perfil de uma pessoa ou de uma organização.
 * Decisões: Um perfil por dono (restrições únicas no banco).
 */
public interface PerfilDeInvestimentoRepository extends JpaRepository<PerfilDeInvestimento, UUID> {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Perfil pessoal
     * Objetivo: Buscar o perfil da pessoa física.
     * Decisões: Vazio se ainda não cadastrado.
     */
    Optional<PerfilDeInvestimento> findByPessoaId(UUID pessoaId);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Perfil da organização
     * Objetivo: Buscar o perfil de uma organização.
     * Decisões: Vazio se ainda não cadastrado.
     */
    Optional<PerfilDeInvestimento> findByOrganizacaoId(UUID organizacaoId);
}
